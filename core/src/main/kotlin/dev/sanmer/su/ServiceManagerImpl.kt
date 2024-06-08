package dev.sanmer.su

import android.content.Context
import android.os.Binder
import android.os.IBinder
import android.os.Parcel
import android.os.SELinux
import android.system.Os
import android.util.Log
import kotlin.system.exitProcess

internal class ServiceManagerImpl(
    private val context: Context
) : IServiceManager.Stub() {
    private val services = mutableMapOf<String, IBinder>()

    override fun getUid(): Int {
        return Os.getuid()
    }

    override fun getPid(): Int {
        return Os.getpid()
    }

    override fun getSELinuxContext(): String {
        return SELinux.getContext()
    }

    override fun addService(cls: String): Boolean {
        Log.d(TAG, "addService<$cls>")

        val serviceClass = try {
            context.classLoader.loadClass(cls)
        } catch (e: ClassNotFoundException) {
            Log.w(TAG, "loadClass<$cls>", e)
            return false
        }

        runCatching {
            val service = serviceClass.getConstructor(Context::class.java)
                .newInstance(context) as IBinder

            services[cls] = service
            return true
        }

        runCatching {
            val service = serviceClass.getConstructor()
                .newInstance() as IBinder

            services[cls] = service
            return true
        }.onFailure {
            Log.w(TAG, "newInstance<$cls>", it)
        }

        return false
    }

    override fun getService(cls: String): IBinder? {
        if (!services.contains(cls)) {
            addService(cls)
        }

        return services[cls]
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