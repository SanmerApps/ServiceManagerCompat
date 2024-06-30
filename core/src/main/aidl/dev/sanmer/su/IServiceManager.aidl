package dev.sanmer.su;

import dev.sanmer.su.Service;

interface IServiceManager {
    int getUid() = 0;
    int getPid() = 1;
    String getSELinuxContext() = 2;

    IBinder addService(in Service service) = 3;
    IBinder getService(String name) = 4;

    void destroy() = 16777114;
}