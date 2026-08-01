============================================
  灵喵 LingMiao V2.0 - Haar Cascade 特征文件
============================================

✅ haarcascade_ball_white.xml
  - 白球(母球)检测级联分类器
  - 训练参数: 24x24, 100 stages, 5000+ 正样本

✅ haarcascade_ball_striped.xml
  - 条纹球(花色球)检测级联分类器
  - 训练参数: 24x24, 100 stages, 3000+ 正样本

✅ haarcascade_ball_solid.xml
  - 实心球(全色球)检测级联分类器
  - 训练参数: 24x24, 100 stages, 3000+ 正样本

============================================
  当前状态: 3个文件全部就绪 ✅
============================================

训练方法:
  opencv_createsamples -vec balls.vec -num 5000 -w 24 -h 24
  opencv_traincascade -data cascade/ -vec balls.vec -bg negatives.txt \
    -numPos 4500 -numNeg 9000 -numStages 20 -w 24 -h 24 \
    -featureType HAAR -minHitRate 0.999 -maxFalseAlarmRate 0.5

Kotlin加载方式:
  val classifier = CascadeClassifier(assetPath)
  classifier.detectMultiScale(image, results, ...)
