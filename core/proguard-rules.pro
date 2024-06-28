-dontwarn android.app.ActivityThread
-dontwarn android.os.SELinux
-dontwarn android.os.ServiceManager

-keepclassmembers class * extends dev.sanmer.su.IService { public <init>(); }