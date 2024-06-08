package dev.sanmer.su;

interface IServiceManager {
    int getUid() = 0;
    int getPid() = 1;
    String getSELinuxContext() = 2;
    boolean addService(String cls) = 3;
    IBinder getService(String cls) = 4;
    void destroy() = 16777114;
}