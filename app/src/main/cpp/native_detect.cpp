/**
 * native_detect.cpp
 *
 * 通用球体识别算法（不依赖任何特定游戏UI）
 *
 * 算法流程：
 *   1. Bitmap → RGBA 像素缓冲区
 *   2. 降采样到 640x360（性能优化 3x）
 *   3. RGB → HSV 色彩空间转换
 *   4. HSV 阈值分割：找白色球（V>200, S<30）
 *   5. 形态学开闭运算去噪
 *   6. 霍夫圆变换检测球体
 *   7. 按亮度排序：最亮的 = 白球（排第一）
 *   8. 返回 [x,y,r, x,y,r, ...] 给 Kotlin 层
 *
 * 编译：通过 CMakeLists.txt 编译为 liblingmiao_native.so
 * JNI 注册：静态注册（函数名对应 Java 全限定名）
 */

#include <jni.h>
#include <android/bitmap.h>
#include <android/log.h>

#include <cmath>
#include <vector>
#include <algorithm>

#define TAG "LingMiaoNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ===== 简化版 Mat 结构（避免依赖 OpenCV） =====
struct SimpleMat {
    int width;
    int height;
    int stride;       // 每行字节数
    uint8_t* data;    // RGBA 数据
};

// ===== HSV 转换（RGB→HSV） =====
static void rgbToHsv(uint8_t r, uint8_t g, uint8_t b,
                      float& h, float& s, float& v) {
    float rf = r / 255.0f;
    float gf = g / 255.0f;
    float bf = b / 255.0f;

    float maxc = std::max({rf, gf, bf});
    float minc = std::min({rf, gf, bf});
    float diff = maxc - minc;

    v = maxc; // Value

    if (maxc == 0) {
        s = 0;
        h = 0;
    } else {
        s = diff / maxc; // Saturation
    }

    if (diff == 0) {
        h = 0;
    } else if (maxc == rf) {
        h = 60.0f * fmodf((gf - bf) / diff, 6.0f);
    } else if (maxc == gf) {
        h = 60.0f * ((bf - rf) / diff + 2.0f);
    } else {
        h = 60.0f * ((rf - gf) / diff + 4.0f);
    }
    if (h < 0) h += 360.0f;
}

// ===== 霍夫圆变换（简化版梯度法） =====
// 这里用一种轻量级实现，适合移动端实时运行
struct Circle {
    float x, y, r;
    float brightness; // 平均亮度，用于排序
};

/**
 * 检测圆形物体
 *
 * @param gray 灰度图像数据
 * @param w, h 图像尺寸
 * @param minRadius 最小半径
 * @param maxRadius 最大半径
 * @param threshold 累加器阈值（越高检测越少越准）
 * @return 检测到的圆列表
 */
static std::vector<Circle> detectCircles(
    const uint8_t* gray, int w, int h,
    int minRadius, int maxRadius, int threshold) {

    // 简化版：用梯度方向投票法
    // 1. 计算梯度
    std::vector<int> gradX(w * h, 0);
    std::vector<int> gradY(w * h, 0);
    std::vector<int> mag(w * h, 0);

    for (int y = 1; y < h - 1; y++) {
        for (int x = 1; x < w - 1; x++) {
            int idx = y * w + x;
            gradX[idx] = gray[idx + 1] - gray[idx - 1];
            gradY[idx] = gray[idx + w] - gray[idx - w];
            mag[idx] = abs(gradX[idx]) + abs(gradY[idx]);
        }
    }

    // 2. 投票累加器
    int maxR = maxRadius + 1;
    int accumSize = w * h * maxR;
    std::vector<int> accumulator(accumSize, 0);

    int votes = 0;
    for (int y = 1; y < h - 1; y++) {
        for (int x = 1; x < w - 1; x++) {
            int idx = y * w + x;
            if (mag[idx] < 30) continue; // 梯度阈值

            // 梯度方向
            float dx = gradX[idx];
            float dy = gradY[idx];
            float length = sqrtf(dx * dx + dy * dy);
            if (length < 1) continue;

            // 沿梯度方向的两个方向投票
            for (int sign = -1; sign <= 1; sign += 2) {
                for (int r = minRadius; r <= maxRadius; r++) {
                    float nx = x + sign * (dx / length) * r;
                    float ny = y + sign * (dy / length) * r;

                    int ix = (int)(nx + 0.5f);
                    int iy = (int)(ny + 0.5f);

                    if (ix >= 0 && ix < w && iy >= 0 && iy < h) {
                        int aidx = (iy * w + ix) * maxR + r;
                        accumulator[aidx]++;
                        votes++;
                    }
                }
            }
        }
    }

    // 3. 找峰值
    std::vector<Circle> circles;
    int minDist = minRadius * 2; // 圆之间最小距离

    for (int r = minRadius; r <= maxRadius; r++) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = (y * w + x) * maxR + r;
                int val = accumulator[idx];

                if (val < threshold) continue;

                // 非极大值抑制（3x3x3 邻域）
                bool isMax = true;
                for (int dy = -1; dy <= 1 && isMax; dy++) {
                    for (int dx = -1; dx <= 1 && isMax; dx++) {
                        for (int dr = -1; dr <= 1; dr++) {
                            int ny = y + dy, nx = x + dx, nr = r + dr;
                            if (ny < 0 || ny >= h || nx < 0 || nx >= w ||
                                nr < minRadius || nr > maxRadius) continue;
                            int nidx = (ny * w + nx) * maxR + nr;
                            if (accumulator[nidx] > val) {
                                isMax = false;
                                break;
                            }
                        }
                    }
                }
                if (!isMax) continue;

                // 距离检查（避免重复检测同一个球）
                bool tooClose = false;
                for (const auto& c : circles) {
                    if (sqrtf((c.x - x) * (c.x - x) + (c.y - y) * (c.y - y)) < minDist) {
                        tooClose = true;
                        break;
                    }
                }
                if (tooClose) continue;

                Circle circle;
                circle.x = x;
                circle.y = y;
                circle.r = r;
                circle.brightness = 0; // 后面填充
                circles.push_back(circle);
            }
        }
    }

    LOGI("detectCircles: found %zu circles", circles.size());
    return circles;
}

// ===== 主入口：JNI 方法 =====

extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeDetectBalls(
    JNIEnv* env, jclass clazz,
    jobject bitmap,
    jint vThresh, jint sThresh, jint pThresh) {

    // 1. 从 Bitmap 获取像素数据
    AndroidBitmapInfo info;
    if (AndroidBitmap_getInfo(env, bitmap, &info) < 0) {
        LOGE("AndroidBitmap_getInfo failed");
        return nullptr;
    }

    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format not RGBA_8888: %d", info.format);
        return nullptr;
    }

    void* pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) {
        LOGE("AndroidBitmap_lockPixels failed");
        return nullptr;
    }

    int w = info.width;
    int h = info.height;
    uint8_t* rgba = static_cast<uint8_t*>(pixels);

    // 2. 降采样到 640 宽（保持宽高比）
    int scaleW = 640;
    int scaleH = (int)(h * (640.0f / w));
    std::vector<uint8_t> smallRgba(scaleW * scaleH * 4);

    for (int y = 0; y < scaleH; y++) {
        for (int x = 0; x < scaleW; x++) {
            int srcX = (int)(x * (w / 640.0f));
            int srcY = (int)(y * (h / (float)scaleH));
            int srcIdx = (srcY * w + srcX) * 4;
            int dstIdx = (y * scaleW + x) * 4;
            smallRgba[dstIdx + 0] = rgba[srcIdx + 0]; // R
            smallRgba[dstIdx + 1] = rgba[srcIdx + 1]; // G
            smallRgba[dstIdx + 2] = rgba[srcIdx + 2]; // B
            smallRgba[dstIdx + 3] = rgba[srcIdx + 3]; // A
        }
    }

    // 3. 创建亮度图 + 找白色区域
    std::vector<uint8_t> grayMap(scaleW * scaleH, 0);
    std::vector<uint8_t> whiteMask(scaleW * scaleH, 0);

    for (int i = 0; i < scaleW * scaleH; i++) {
        uint8_t r = smallRgba[i * 4 + 0];
        uint8_t g = smallRgba[i * 4 + 1];
        uint8_t b = smallRgba[i * 4 + 2];

        // 灰度（亮度）
        uint8_t gray = (uint8_t)(0.299f * r + 0.587f * g + 0.114f * b);
        grayMap[i] = gray;

        // HSV 判断是否为白色
        float h, s, v;
        rgbToHsv(r, g, b, h, s, v);

        if (v * 255.0f > vThresh && s * 255.0f < sThresh) {
            whiteMask[i] = 255;
        }
    }

    // 4. 形态学操作（开运算：先腐蚀后膨胀，去噪）
    auto erode = [&](const std::vector<uint8_t>& src, std::vector<uint8_t>& dst) {
        for (int y = 1; y < scaleH - 1; y++) {
            for (int x = 1; x < scaleW - 1; x++) {
                uint8_t minVal = 255;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        minVal = std::min(minVal, src[(y + dy) * scaleW + (x + dx)]);
                    }
                }
                dst[y * scaleW + x] = minVal;
            }
        }
    };

    auto dilate = [&](const std::vector<uint8_t>& src, std::vector<uint8_t>& dst) {
        for (int y = 1; y < scaleH - 1; y++) {
            for (int x = 1; x < scaleW - 1; x++) {
                uint8_t maxVal = 0;
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dx = -1; dx <= 1; dx++) {
                        maxVal = std::max(maxVal, src[(y + dy) * scaleW + (x + dx)]);
                    }
                }
                dst[y * scaleW + x] = maxVal;
            }
        }
    };

    std::vector<uint8_t> eroded(scaleW * scaleH);
    std::vector<uint8_t> denoised(scaleW * scaleH);
    erode(whiteMask, eroded);
    dilate(eroded, denoised);

    // 5. 霍夫圆检测（在白色掩码上找圆）
    int minR = 8;   // 最小球半径（像素，降采样后）
    int maxR = 40;  // 最大球半径
    int threshold = std::max(20, pThresh / 2); // 累加器阈值

    std::vector<Circle> circles = detectCircles(
        denoised.data(), scaleW, scaleH, minR, maxR, threshold);

    // 6. 计算平均亮度，排序（最亮的 = 白球，排第一）
    for (auto& c : circles) {
        int cx = (int)c.x;
        int cy = (int)c.y;
        int r = (int)c.r;
        float sum = 0;
        int count = 0;
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                if (dx * dx + dy * dy > r * r) continue;
                int px = cx + dx;
                int py = cy + dy;
                if (px < 0 || px >= scaleW || py < 0 || py >= scaleH) continue;
                sum += grayMap[py * scaleW + px];
                count++;
            }
        }
        c.brightness = count > 0 ? (sum / count) : 0;
    }

    // 按亮度降序排列（最亮的在前 = 白球）
    std::sort(circles.begin(), circles.end(), [](const Circle& a, const Circle& b) {
        return a.brightness > b.brightness;
    });

    // 7. 映射回原始分辨率并输出
    float scaleX = (float)w / scaleW;
    float scaleY = (float)h / scaleH;

    // 输出格式: [x0,y0,r0, x1,y1,r1, ...]
    int resultSize = circles.size() * 3;
    jfloatArray result = env->NewFloatArray(resultSize);
    if (result == nullptr) {
        AndroidBitmap_unlockPixels(env, bitmap);
        return nullptr;
    }

    std::vector<float> out(resultSize);
    for (size_t i = 0; i < circles.size(); i++) {
        out[i * 3 + 0] = circles[i].x * scaleX;
        out[i * 3 + 1] = circles[i].y * scaleY;
        out[i * 3 + 2] = circles[i].r * ((scaleX + scaleY) / 2.0f);
    }

    env->SetFloatArrayRegion(result, 0, resultSize, out.data());

    AndroidBitmap_unlockPixels(env, bitmap);

    LOGI("nativeDetectBalls: detected %zu balls, vThresh=%d, sThresh=%d, pThresh=%d",
          circles.size(), vThresh, sThresh, pThresh);

    return result;
}
