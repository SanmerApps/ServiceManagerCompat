package dev.sanmer.su

import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SELinux
import android.system.Os
import android.util.Log
import kotlin.system.exitProcess

internal class ServiceManagerImpl : IServiceManager.Stub() {
    private val services = hashMapOf<String, IBinder>()

    init {
        Log.d(TAG, "init: uid = $uid, pid = $pid, context = $seLinuxContext")
    }

    override fun getUid(): Int = Os.getuid()

    override fun getPid(): Int = Os.getpid()

    override fun getSELinuxContext(): String = SELinux.getContext()

    override fun bind(cls: ClassWrapper<*>): IBinder? =
        runCatching {
            services.getOrPut(cls.original.name) {
                val binder = cls.original.getConstructor().newInstance()
                require(binder is IBinder) { "${cls.original} is not IBinder" }

                binder
            }

        }.onFailure {
            Log.e(TAG, Log.getStackTraceString(it))

        }.getOrNull()

    override fun delegate(cls: ClassWrapper<*>): IBinder? =
        runCatching {
            val service = cls.original.getConstructor().newInstance()
            require(service is IService) { "${cls.original} is not IService" }

            services.getOrPut(service.name) {
                service.create(this@ServiceManagerImpl)
            }

        }.onFailure {
            Log.e(TAG, Log.getStackTraceString(it))

        }.getOrNull()

    override fun destroy() {
        exitProcess(0)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int) =
        if (code == ServiceManagerCompat.BINDER_TRANSACTION) {
            data.enforceInterface(DESCRIPTOR)
            val targetBinder = data.readStrongBinder()
            val targetCode = data.readInt()
            val targetFlags = data.readInt()
            val newData = Parcel.obtain()

            try {
                newData.appendFrom(data, data.dataPosition(), data.dataAvail())

                val id = Binder.clearCallingIdentity()
                targetBinder.transact(targetCode, newData, reply, targetFlags)
                Binder.restoreCallingIdentity(id)
            } finally {
                newData.recycle()
            }

            true
        } else {
            super.onTransact(code, data, reply, flags)
        }

    companion object {
        private const val TAG = "ServiceManagerImpl"
    }
}