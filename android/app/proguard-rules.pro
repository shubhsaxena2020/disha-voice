# TensorFlow Lite interpreter entry points are resolved by JNI.
-keep class org.tensorflow.lite.** { *; }
-keep class org.dishavoice.app.detection.** { *; }
-dontwarn org.tensorflow.lite.**

# Keep the domain policy names readable in crash reports and stable for tests.
-keepnames class org.dishavoice.core.** { *; }
