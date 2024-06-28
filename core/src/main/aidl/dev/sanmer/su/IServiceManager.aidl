package dev.sanmer.su;

import dev.sanmer.su.ClassWrapper;

interface IServiceManager {
    int getUid() = 0;
    int getPid() = 1;
    String getSELinuxContext() = 2;

    IBinder bind(in ClassWrapper cls) = 3;
    IBinder delegate(in ClassWrapper cls) = 4;

    void destroy() = 16777114;
}