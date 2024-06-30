-dontwarn android.app.ActivityThread
-dontwarn android.os.SELinux
-dontwarn android.os.ServiceManager

-keep,allowobfuscation class * extends dev.sanmer.su.IService { *; }