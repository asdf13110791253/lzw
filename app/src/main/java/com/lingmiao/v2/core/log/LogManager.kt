package com.lingmiao.v2.core.log

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 灵喵日志系统 - 分级日志 + 文件轮转
 * VERBOSE < DEBUG < INFO < WARN < ERROR
 */
object LogManager {

    const val VERBOSE = 0
    const val DEBUG = 1
    const val INFO = 2
    const val WARN = 3
    const val ERROR = 4

    private const val TAG_PREFIX = "LingMiao"
    private const val MAX_LOG_SIZE = 2 * 1024 * 1024L // 2MB
    private const val MAX_LOG_FILES = 3

    private var minLevel = VERBOSE
    private var fileLogging = false
    private var writer: FileWriter? = null
    private var logFile: File? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun init(logDir: String?) {
        if (logDir == null) return
        val dir = File(logDir)
        if (!dir.exists()) dir.mkdirs()

        rotateLogs(dir)

        val today = fileDateFormat.format(Date())
        logFile = File(dir, "lingmiao_${today}.log")

        try {
            writer = FileWriter(logFile, true)
            fileLogging = true
            i("Logger", "📝 日志系统初始化完成")
        } catch (e: IOException) {
            writer = null
            fileLogging = false
        }
    }

    fun setMinLevel(level: Int) { minLevel = level }

    // ── 公共接口 ──
    fun v(tag: String, msg: String) = log(VERBOSE, tag, msg)
    fun d(tag: String, msg: String) = log(DEBUG, tag, msg)
    fun i(tag: String, msg: String) = log(INFO, tag, msg)
    fun w(tag: String, msg: String) = log(WARN, tag, msg)
    fun e(tag: String, msg: String) = log(ERROR, tag, msg)
    fun e(tag: String, msg: String, t: Throwable?) {
        log(ERROR, tag, msg)
        if (t != null) log(ERROR, tag, getStackTrace(t))
    }

    // ── 快速标签 ──
    fun aim(msg: String) = i("AimEngine", msg)
    fun detect(msg: String) = i("BallDetect", msg)
    fun geo(msg: String) = i("Geometry", msg)
    fun render(msg: String) = i("Renderer", msg)
    fun native(msg: String) = i("Native", msg)
    fun service(msg: String) = i("Service", msg)
    fun config(msg: String) = i("Config", msg)

    // ── 测试/桌面模式（无 Android）──
    fun v(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("V/$TAG_PREFIX-$tag: $msg") else v(tag, msg)
    }
    fun d(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("D/$TAG_PREFIX-$tag: $msg") else d(tag, msg)
    }
    fun i(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("I/$TAG_PREFIX-$tag: $msg") else i(tag, msg)
    }
    fun w(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("W/$TAG_PREFIX-$tag: $msg") else w(tag, msg)
    }
    fun e(tag: String, msg: String, desktop: Boolean) {
        if (desktop) println("E/$TAG_PREFIX-$tag: $msg") else e(tag, msg)
    }

    private fun log(level: Int, tag: String, msg: String) {
        if (level < minLevel) return

        val fullTag = "$TAG_PREFIX/$tag"
        val line = "${dateFormat.format(Date())} [${levelChar(level)}] $fullTag: $msg\n"

        when (level) {
            VERBOSE -> Log.v(fullTag, msg)
            DEBUG -> Log.d(fullTag, msg)
            INFO -> Log.i(fullTag, msg)
            WARN -> Log.w(fullTag, msg)
            ERROR -> Log.e(fullTag, msg)
        }

        if (fileLogging && writer != null) {
            try {
                writer?.write(line)
                writer?.flush()
                checkRotate()
            } catch (_: IOException) {}
        }
    }

    private fun levelChar(level: Int): Char = when (level) {
        VERBOSE -> 'V'; DEBUG -> 'D'; INFO -> 'I'
        WARN -> 'W'; ERROR -> 'E'; else -> '?'
    }

    private fun getStackTrace(t: Throwable): String {
        val sb = StringBuilder(t.toString() + "\n")
        t.stackTrace.forEach { sb.append("    at $it\n") }
        return sb.toString()
    }

    private fun checkRotate() {
        val f = logFile ?: return
        if (!f.exists() || f.length() <= MAX_LOG_SIZE) return

        try { writer?.close() } catch (_: IOException) {}
        writer = null

        val dir = f.parentFile ?: return
        val files = dir.listFiles { _, name -> name.startsWith("lingmiao_") && name.endsWith(".log") }
        files?.let {
            it.sortBy { f2 -> f2.lastModified() }
            if (it.size >= MAX_LOG_FILES) {
                for (i in 0..it.size - MAX_LOG_FILES) it[i].delete()
            }
        }

        val today = fileDateFormat.format(Date())
        logFile = File(dir, "lingmiao_${today}_${System.currentTimeMillis()}.log")
        try {
            writer = FileWriter(logFile, true)
        } catch (_: IOException) {
            writer = null
            fileLogging = false
        }
    }

    private fun rotateLogs(dir: File) {
        val files = dir.listFiles { _, name -> name.startsWith("lingmiao_") && name.endsWith(".log") }
        files?.let {
            it.sortBy { f2 -> f2.lastModified() }
            if (it.size >= MAX_LOG_FILES) {
                for (i in 0..it.size - MAX_LOG_FILES) it[i].delete()
            }
        }
    }

    fun shutdown() {
        if (writer != null) {
            try { writer?.flush(); writer?.close() } catch (_: IOException) {}
            writer = null
        }
        fileLogging = false
    }
}
