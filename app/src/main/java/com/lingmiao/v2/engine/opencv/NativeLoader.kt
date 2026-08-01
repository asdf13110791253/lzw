package com.lingmiao.v2.engine.opencv

import android.content.Context
import com.lingmiao.v2.core.log.LogManager
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Native 库加载器
 *
 * 加载策略（三级兜底）:
 * 1. System.loadLibrary() → 从 APK 的 lib/arm64-v8a/ 自动加载（标准方式）
 * 2. 从 assets/native/ 解压到私有目录后加载（兼容旧版）
 * 3. 从 jniLibs/ 同级私有目录加载（调试用）
 *
 * .so 文件正确位置: app/src/main/jniLibs/arm64-v8a/
 *   - libopencv_java4.so
 *   - liblingmiao_engine.so
 */
object NativeLoader {

    private const val TAG = "NativeLoader"
    private var loaded = false
    private var extractDir: File? = null

    fun loadAll() {
        if (loaded) {
            LogManager.native("Native 库已加载, 跳过")
            return
        }

        // 1. 加载 OpenCV
        var ok = tryLoad("opencv_java4", "libopencv_java4.so")
        if (!ok) {
            LogManager.w(TAG, "⚠️ libopencv_java4.so 标准加载失败, 尝试兜底...")
            ok = loadFromAssets(null, "libopencv_java4.so")
        }
        if (!ok) {
            LogManager.e(TAG, "❌ libopencv_java4.so 所有加载方式均失败!")
        }

        // 2. 加载自研引擎
        ok = tryLoad("lingmiao_engine", "liblingmiao_engine.so")
        if (!ok) {
            LogManager.w(TAG, "⚠️ liblingmiao_engine.so 标准加载失败, 尝试兜底...")
            ok = loadFromAssets(null, "liblingmiao_engine.so")
        }
        if (!ok) {
            LogManager.e(TAG, "❌ liblingmiao_engine.so 所有加载方式均失败!")
        }

        loaded = true
        LogManager.native("✅ NativeLoader 初始化完成")
    }

    /**
     * 标准方式: System.loadLibrary (从 APK lib/ 目录加载)
     */
    private fun tryLoad(libName: String, fileName: String): Boolean {
        return try {
            System.loadLibrary(libName)
            LogManager.native("✅ $fileName 加载成功 (System.loadLibrary)")
            true
        } catch (e: UnsatisfiedLinkError) {
            LogManager.w(TAG, "⚠️ $fileName loadLibrary 失败: ${e.message}")
            false
        }
    }

    /**
     * 兜底方式: 从 assets 解压到私有目录后加载
     * 搜索路径:
     *   - assets/native/$fileName
     *   - assets/jniLibs/arm64-v8a/$fileName
     */
    fun loadFromAssets(context: Context?, fileName: String): Boolean {
        if (context == null) {
            LogManager.e(TAG, "❌ Context 为 null, 无法从 assets 加载")
            return false
        }
        return loadFromAssets(context, fileName)
    }

    private fun loadFromAssets(context: Context?, fileName: String): Boolean {
        if (context == null) return false

        extractDir = File(context.filesDir, "native")
        if (!extractDir!!.exists()) extractDir!!.mkdirs()

        val outFile = File(extractDir, fileName)
        if (outFile.exists() && outFile.length() > 1024) {
            // 已有有效文件, 直接加载
            return tryLoadFromFile(outFile, fileName)
        }

        // 尝试多个 assets 路径
        val assetPaths = listOf(
            "native/$fileName",
            "jniLibs/arm64-v8a/$fileName"
        )

        for (assetPath in assetPaths) {
            try {
                context.assets.open(assetPath).use { ins ->
                    FileOutputStream(outFile).use { fos ->
                        val buf = ByteArray(8192)
                        var len: Int
                        while (ins.read(buf).also { len = it } > 0) {
                            fos.write(buf, 0, len)
                        }
                    }
                }
                LogManager.native("📦 解压 $assetPath → ${outFile.absolutePath} (${outFile.length()}B)")
                return tryLoadFromFile(outFile, fileName)
            } catch (e: IOException) {
                LogManager.w(TAG, "assets 中未找到 $assetPath: ${e.message}")
            }
        }

        LogManager.e(TAG, "❌ 所有路径均未找到 $fileName")
        return false
    }

    private fun tryLoadFromFile(file: File, fileName: String): Boolean {
        return try {
            System.load(file.absolutePath)
            LogManager.native("✅ $fileName 加载成功 (from ${file.absolutePath})")
            true
        } catch (e: UnsatisfiedLinkError) {
            LogManager.e(TAG, "❌ $fileName 加载失败: ${e.message}")
            false
        }
    }

    fun isLoaded(): Boolean = loaded

    // ── Native 方法声明 ──

    external fun checkOpenCVVersion(): String
    external fun initEngine(configPath: String): Boolean
    external fun releaseEngine()

    // 球检测
    external fun detectBalls(
        pixelData: ByteArray, width: Int, height: Int,
        stride: Int, mode: Int
    ): FloatArray?

    // 透视变换
    external fun setHomography(matrix: FloatArray)
    external fun perspectiveTransform(src: FloatArray): FloatArray?

    // 瞄准计算
    external fun calcAim(
        cueX: Float, cueY: Float,
        targetX: Float, targetY: Float,
        pocketX: Float, pocketY: Float,
        maxBanks: Int
    ): FloatArray?
}
