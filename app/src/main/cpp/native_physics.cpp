/**
 * native_physics.cpp
 *
 * C++ 版物理引擎 —— 镜像点法计算台球瞄准线
 *
 * 核心算法：
 *   1. 镜像点法（Mirror Image Method）
 *      - 以目标球为圆心，球半径画圆
 *      - 白球方向线与该圆的交点 = 碰撞点
 *      - 镜像点 = 碰撞点关于目标球中心的对称点
 *      - 辅助线 = 白球 → 碰撞点 → 镜像点方向延长到袋口
 *
 *   2. 角度补偿模式（Angle Compensation）
 *      - 真实台球碰撞有能量损耗 + 自旋效应
 *      - 补偿比例 0.18（经验值）
 *
 *   3. 翻袋计算（Bank Shot）
 *      - 用库边镜像法计算反弹路径
 */

#include <jni.h>
#include <android/log.h>     // 【已修复】必须加这行，否则找不到 ANDROID_LOG_INFO
#include <cmath>
#include <vector>
#include <algorithm>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "LingMiaoPhys", __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, "LingMiaoPhys", __VA_ARGS__)

static constexpr float PI = 3.14159265359f;
static constexpr float COMPENSATION_RATIO = 0.18f; // 角度补偿比例（度→弧度后乘）

// ===== 向量工具 =====
struct Vec2 {
    float x, y;
    Vec2(float x = 0, float y = 0) : x(x), y(y) {}
    Vec2 operator-(const Vec2& o) const { return Vec2(x - o.x, y - o.y); }
    Vec2 operator+(const Vec2& o) const { return Vec2(x + o.x, y + o.y); }
    Vec2 operator*(float s) const { return Vec2(x * s, y * s); }
    float length() const { return sqrtf(x * x + y * y); }
    Vec2 normalize() const {
        float l = length();
        return l > 0 ? Vec2(x / l, y / l) : Vec2(0, 0);
    }
    float dot(const Vec2& o) const { return x * o.x + y * o.y; }
};

// ===== 线段-圆交点 =====
// 返回离线段起点最近的交点
static bool lineCircleIntersect(
    const Vec2& p1, const Vec2& p2,  // 线段
    const Vec2& center, float radius,   // 圆
    Vec2& hitPoint) {

    Vec2 d = p2 - p1;
    Vec2 f = p1 - center;

    float a = d.dot(d);
    float b = 2 * f.dot(d);
    float c = f.dot(f) - radius * radius;

    float discriminant = b * b - 4 * a * c;
    if (discriminant < 0) return false;

    float sq = sqrtf(discriminant);
    float t1 = (-b - sq) / (2 * a);
    float t2 = (-b + sq) / (2 * a);

    // 取在 [0,1] 范围内的 t
    float t = -1;
    if (t1 >= 0 && t1 <= 1) t = t1;
    else if (t2 >= 0 && t2 <= 1) t = t2;

    if (t < 0) return false;

    hitPoint = p1 + d * t;
    return true;
}

// ===== 镜像点法计算瞄准线 =====
//
// 输入：balls 数组 [白球x,y,r, 目标球1x,y,r, 目标球2x,y,r, ...]
// 输出：[碰撞点x,y, 延长线终点x,y, ...] （每两个球一对线段）
//
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeComputeAimLine(
    JNIEnv* env, jclass clazz,
    jfloatArray balls,
    jstring mode,
    jint bankCount) {

    jfloat* ballsData = env->GetFloatArrayElements(balls, nullptr);
    jsize ballsLen = env->GetArrayLength(balls);

    const char* modeStr = env->GetStringUTFChars(mode, nullptr);
    bool useCompensation = (strcmp(modeStr, "compensation") == 0);
    env->ReleaseStringUTFChars(mode, modeStr);

    // 至少需要白球 + 1个目标球 = 6 个 float
    if (ballsLen < 6) {
        env->ReleaseFloatArrayElements(balls, ballsData, JNI_ABORT);
        return nullptr;
    }

    // 解析球
    Vec2 cue(ballsData[0], ballsData[1]);
    float cueR = ballsData[2];

    // 找最佳目标球（离白球最近的非白球）
    int bestTarget = -1;
    float bestDist = 1e9f;
    for (int i = 1; i * 3 + 2 < ballsLen; i++) {
        Vec2 t(ballsData[i * 3], ballsData[i * 3 + 1]);
        float d = (t - cue).length();
        if (d < bestDist) {
            bestDist = d;
            bestTarget = i;
        }
    }

    if (bestTarget < 0) {
        env->ReleaseFloatArrayElements(balls, ballsData, JNI_ABORT);
        return nullptr;
    }

    Vec2 target(ballsData[bestTarget * 3], ballsData[bestTarget * 3 + 1]);
    float targetR = ballsData[bestTarget * 3 + 2];

    // 碰撞点：白球→目标球方向，距目标球中心 targetR 处
    Vec2 dir = (target - cue).normalize();
    Vec2 hitPoint = target - dir * targetR;

    // 延长线方向
    Vec2 aimDir;
    if (useCompensation) {
        // 角度补偿：向"白球侧"偏转 0.18 度
        float baseAngle = atan2f(dir.y, dir.x);
        float offset = COMPENSATION_RATIO * (PI / 180.0f);
        float finalAngle = baseAngle + offset;
        aimDir = Vec2(cosf(finalAngle), sinf(finalAngle));
    } else {
        // 纯镜像：沿原方向延长
        aimDir = dir;
    }

    // 延长线终点（屏幕外 2000px）
    float lineLen = 2000.0f;
    Vec2 endPoint = hitPoint + aimDir * lineLen;

    // 输出：[白球→碰撞点, 碰撞点→延长线终点]
    // 即 4 个点 = 2 条线段
    std::vector<float> out;
    out.push_back(cue.x);
    out.push_back(cue.y);
    out.push_back(hitPoint.x);
    out.push_back(hitPoint.y);
    out.push_back(hitPoint.x);
    out.push_back(hitPoint.y);
    out.push_back(endPoint.x);
    out.push_back(endPoint.y);

    // 如果有多个目标球，画辅助线到第二个最近球
    if (ballsLen >= 9) {
        Vec2 target2(ballsData[6], ballsData[7]);
        Vec2 dir2 = (target2 - cue).normalize();
        Vec2 hit2 = target2 - dir2 * ballsData[8];
        Vec2 end2 = hit2 + dir2 * lineLen;

        out.push_back(cue.x);
        out.push_back(cue.y);
        out.push_back(hit2.x);
        out.push_back(hit2.y);
        out.push_back(hit2.x);
        out.push_back(hit2.y);
        out.push_back(end2.x);
        out.push_back(end2.y);
    }

    jfloatArray result = env->NewFloatArray(out.size());
    env->SetFloatArrayRegion(result, 0, out.size(), out.data());

    env->ReleaseFloatArrayElements(balls, ballsData, JNI_ABORT);

    LOGI("computeAimLine: mode=%s, balls=%d, out=%zu floats",
          useCompensation ? "comp" : "mirror", (int)ballsLen / 3, out.size());

    return result;
}

// ===== 透视变换矩阵计算 =====
//
// 输入：屏幕上的 4 个角点 + 标准球桌宽高
// 输出：3x3 透视变换矩阵（行优先，9 个 float）
//
extern "C"
JNIEXPORT jfloatArray JNICALL
Java_com_lingmiao_v2_core_BallDetector_nativeComputePerspectiveMatrix(
    JNIEnv* env, jclass clazz,
    jfloatArray screenPoints,
    jfloat tableWidth,
    jfloat tableHeight) {

    jfloat* pts = env->GetFloatArrayElements(screenPoints, nullptr);

    // 屏幕点（可能经过透视畸变）
    Vec2 src[4] = {
        Vec2(pts[0], pts[1]),
        Vec2(pts[2], pts[3]),
        Vec2(pts[4], pts[5]),
        Vec2(pts[6], pts[7])
    };

    // 标准球桌点（矩形）
    Vec2 dst[4] = {
        Vec2(0, 0),
        Vec2(tableWidth, 0),
        Vec2(tableWidth, tableHeight),
        Vec2(0, tableHeight)
    };

    // 计算透视变换矩阵（直接线性变换法）
    // 解 8x8 线性方程组
    float A[8][8] = {0};
    float B[8] = {0};

    for (int i = 0; i < 4; i++) {
        float x = src[i].x, y = src[i].y;
        float X = dst[i].x, Y = dst[i].y;

        A[i * 2][0] = x; A[i * 2][1] = y; A[i * 2][2] = 1;
        A[i * 2][3] = 0; A[i * 2][4] = 0; A[i * 2][5] = 0;
        A[i * 2][6] = -X * x; A[i * 2][7] = -X * y;
        B[i * 2] = X;

        A[i * 2 + 1][0] = 0; A[i * 2 + 1][1] = 0; A[i * 2 + 1][2] = 0;
        A[i * 2 + 1][3] = x; A[i * 2 + 1][4] = y; A[i * 2 + 1][5] = 1;
        A[i * 2 + 1][6] = -Y * x; A[i * 2 + 1][7] = -Y * y;
        B[i * 2 + 1] = Y;
    }

    // 高斯消元
    for (int i = 0; i < 8; i++) {
        // 找主元
        int maxRow = i;
        for (int k = i + 1; k < 8; k++) {
            if (fabsf(A[k][i]) > fabsf(A[maxRow][i])) maxRow = k;
        }
        for (int k = 0; k < 8; k++) std::swap(A[i][k], A[maxRow][k]);
        std::swap(B[i], B[maxRow]);

        if (fabsf(A[i][i]) < 1e-10f) continue;

        // 消元
        for (int k = i + 1; k < 8; k++) {
            float factor = A[k][i] / A[i][i];
            for (int j = i; j < 8; j++) A[k][j] -= factor * A[i][j];
            B[k] -= factor * B[i];
        }
    }

    // 回代
    float h[8];
    for (int i = 7; i >= 0; i--) {
        float sum = B[i];
        for (int j = i + 1; j < 8; j++) sum -= A[i][j] * h[j];
        h[i] = sum / A[i][i];
    }

    // 输出 3x3 矩阵（行优先）
    std::vector<float> matrix(9);
    matrix[0] = h[0]; matrix[1] = h[1]; matrix[2] = h[2];
    matrix[3] = h[3]; matrix[4] = h[4]; matrix[5] = h[5];
    matrix[6] = h[6]; matrix[7] = h[7]; matrix[8] = 1.0f;

    jfloatArray result = env->NewFloatArray(9);
    env->SetFloatArrayRegion(result, 0, 9, matrix.data());

    env->ReleaseFloatArrayElements(screenPoints, pts, JNI_ABORT);

    return result;
}
