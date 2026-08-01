====================================================
  灵喵 LingMiao V2.0 - Assets 资源总目录
====================================================

本目录存放运行时需要的全部资源文件:

cascades/          (3个 Haar Cascade XML) ✅ 已就绪
  ├── haarcascade_ball_white.xml
  ├── haarcascade_ball_striped.xml
  └── haarcascade_ball_solid.xml

models/            (4个 TFLite 模型) ✅ 已就绪
  ├── ball_detect_v2.tflite
  ├── ball_classify.tflite
  ├── cue_ball_seg.tflite
  └── pocket_detect.tflite

felt_textures/     (5套桌布纹理 PNG) ✅ 已就绪
  ├── felt_1_dark_green.png
  ├── felt_2_blue.png
  ├── felt_3_burgundy.png
  ├── felt_4_navy.png
  └── felt_5_olive.png

====================================================
  当前状态: 全部文件已就绪 ✅
====================================================

Haar XML    → 用于 OpenCV CascadeClassifier 球检测
TFLite模型  → 用于深度学习球检测/分类/分割
桌布纹理    → 校准页透视矫正背景 & 主题切换

Native .so 文件位置: app/src/main/jniLibs/arm64-v8a/
  - libopencv_java4.so    (需替换为裁剪版, 见jniLibs目录README)
  - liblingmiao_engine.so  (需替换为编译产物, 见jniLibs目录README)
