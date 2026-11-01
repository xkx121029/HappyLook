# 保留 JS 桥接接口
-keepclassmembers class com.xkx.happylook.** {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.xkx.happylook.viewer.** { *; }
