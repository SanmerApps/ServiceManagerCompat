package dev.sanmer.su;

interface IServiceManager {
    int getUid() = 0;
    int getPid() = 1;
    String getSELinuxContext() = 2;

    IBinder addService(String name, String className) = 3;
    IBinder getService(String name) = 4;

    void destroy() = 16777114;
}