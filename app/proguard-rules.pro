# Keep Native WiFi JNI
-keep class com.flipperdroid.modules.wifi.NativeWiFi { *; }
-keep class com.flipperdroid.modules.wifi.NativeWiFi$Companion { *; }

# Keep Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Keep Gson/Serialization if added later
-keepattributes Signature
-keepattributes *Annotation*