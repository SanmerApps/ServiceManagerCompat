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

    override fun getUid(): Int {
        return Os.getuid()
    }

    override fun getPid(): Int {
        return Os.getpid()
    }

    override fun getSELinuxContext(): String {
        return SELinux.getContext()
    }

    override fun addService(name: String, className: String): IBinder? {
        Log.i(TAG, "addService<$name>: service = $className")
        runCatching {
            val clazz = Class.forName(className)
            val service = clazz.getConstructor().newInstance() as IBinder
            services[name] = service

            return service
        }.onFailure {
            Log.w(TAG, Log.getStackTraceString(it))
        }

        return null
    }

    override fun getService(name: String): IBinder? {
        return services[name].also {
            if (it == null) {
                Log.w(TAG, "getService<$name>: service = null")
            }
        }
    }

    override fun destroy() {
        exitProcess(0)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int) =
        if (code == ServiceManagerCompat.BINDER_TRANSACTION) {
            data.enforceInterface(DESCRIPTOR)
            val targetBinder = data.readStrongBinder()
            val targetCode = data.readInt()
            val targetFlags = data.readInt()

            val newData = Parcel.obtain().apply {
                runCatching {
                    appendFrom(data, data.dataPosition(), data.dataAvail())
                }
            }

            try {
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