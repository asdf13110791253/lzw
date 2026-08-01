============================================
  灵喵 LingMiao V2.0 - TFLite 模型文件
============================================

✅ ball_detect_v2.tflite    (占位, 目标~1.2MB, int8量化)
  - 球检测模型 (YOLO-style)
  - 输入: [1, 256, 256, 3] uint8
  - 输出: [1, 84, 84, 3] float32
  - 支持: 1白球 + 7彩球 = 8类

✅ ball_classify.tflite      (占位, 目标~800KB, int8量化)
  - 球分类模型 (ResNet-18 轻量版)
  - 输入: [1, 64, 64, 3] uint8
  - 输出: [1, 16] float32 (16分类softmax)
  - 类别: 0=cue, 1-7=solid, 8-14=striped, 15=unknown

✅ cue_ball_seg.tflite      (占位, 目标~1.5MB, int8量化)
  - 母球分割模型 (U-Net 轻量版)
  - 输入: [1, 128, 128, 3] uint8
  - 输出: [1, 128, 128, 1] float32 (二值mask)

✅ pocket_detect.tflite     (占位, 目标~600KB, int8量化)
  - 袋口检测模型 (SSD 轻量版)
  - 输入: [1, 192, 192, 3] uint8
  - 输出: [1, 6, 4] float32 (6个袋口 x [cx, cy, w, h])

============================================
  当前状态: 4个文件均已就位 ✅
  (placeholder格式, 替换为真实训练产物)
============================================

模型训练框架推荐: TensorFlow / PyTorch → ONNX → TFLite
量化命令:
  tflite_convert --saved_model_dir=model \
    --output_file=model_quant.tflite \
    --quantize=True --integer_quantize=True

Kotlin加载方式:
  val model = BallDetectV2.newInstance(context)
  val input = TensorImage.fromBitmap(bitmap)
  val outputs = model.process(input)
  val detections = outputs.detectionResultList
