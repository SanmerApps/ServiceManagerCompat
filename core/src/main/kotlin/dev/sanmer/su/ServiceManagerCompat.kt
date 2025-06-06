package dev.sanmer.su

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.os.IInterface
import android.os.Parcel
import android.os.ServiceManager
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ipc.RootService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import rikka.shizuku.Shizuku
import java.io.FileDescriptor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object ServiceManagerCompat {
    private const val TIMEOUT_MILLIS = 15_000L

    internal const val BINDER_TRANSACTION = 84398154

    interface IProvider {
        val name: String
        suspend fun isAvailable(): Boolean
        suspend fun isAuthorized(): Boolean
        fun bind(connection: ServiceConnection)
        fun unbind(connection: ServiceConnection)
    }

    private suspend fun get(provider: IProvider) = withTimeout(TIMEOUT_MILLIS) {
        suspendCancellableCoroutine { continuation ->
            val connection = object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName, binder: IBinder) {
                    val service = IServiceManager.Stub.asInterface(binder)
                    continuation.resume(service)
                }

                override fun onServiceDisconnected(name: ComponentName) {
                    continuation.resumeWithException(
                        IllegalStateException("IServiceManager destroyed")
                    )
                }

                override fun onBindingDied(name: ComponentName?) {
                    continuation.resumeWithException(
                        IllegalStateException("IServiceManager destroyed")
                    )
                }
            }

            provider.bind(connection)
            continuation.invokeOnCancellation {
                provider.unbind(connection)
            }
        }
    }

    suspend fun from(provider: IProvider): IServiceManager = withContext(Dispatchers.Main) {
        when {
            !provider.isAvailable() -> throw IllegalStateException("${provider.name} not available")
            !provider.isAuthorized() -> throw IllegalStateException("${provider.name} not authorized")
            else -> get(provider)
        }
    }

    class LibSuProvider(
        private val context: Context
    ) : IProvider {
        override val name = "LibSu"

        override suspend fun isAvailable() = true

        override suspend fun isAuthorized() = withContext(Dispatchers.IO) {
            suspendCancellableCoroutine { continuation ->
                runCatching {
                    Shell.getShell()
                }.onSuccess {
                    continuation.resume(true)
                }.onFailure {
                    continuation.resume(false)
                }
            }
        }

        override fun bind(connection: ServiceConnection) {
            RootService.bind(service, connection)
        }

        override fun unbind(connection: ServiceConnection) {
            RootService.stop(service)
        }

        private val service by lazy {
            Intent().apply {
                component = ComponentName(
                    context.packageName,
                    Service::class.java.name
                )
            }
        }

        init {
            Shell.enableVerboseLogging = true
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setInitializers(SuShellInitializer::class.java)
                    .setTimeout(10)
            )
        }

        private class SuShellInitializer : Shell.Initializer() {
            override fun onInit(context: Context, shell: Shell) = shell.isRoot
        }

        private class Service : RootService() {
            override fun onBind(intent: Intent): IBinder {
                return ServiceManagerImpl()
            }
        }
    }

    suspend fun fromLibSu() = from(LibSuProvider(ContextCompat.getContext()))

    class ShizukuProvider(
        private val context: Context
    ) : IProvider {
        override val name = "Shizuku"

        override suspend fun isAvailable(): Boolean {
            return Shizuku.pingBinder()
        }

        override suspend fun isAuthorized() = when {
            isGranted -> true
            else -> suspendCancellableCoroutine { continuation ->
                val listener = object : Shizuku.OnRequestPermissionResultListener {
                    override fun onRequestPermissionResult(
                        requestCode: Int,
                        grantResult: Int
                    ) {
                        Shizuku.removeRequestPermissionResultListener(this)
                        continuation.resume(isGranted)
                    }
                }

                Shizuku.addRequestPermissionResultListener(listener)
                continuation.invokeOnCancellation {
                    Shizuku.removeRequestPermissionResultListener(listener)
                }
                Shizuku.requestPermission(listener.hashCode())
            }
        }

        override fun bind(connection: ServiceConnection) {
            Shizuku.bindUserService(service, connection)
        }

        override fun unbind(connection: ServiceConnection) {
            Shizuku.unbindUserService(service, connection, true)
        }

        private val isGranted get() = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

        private val service by lazy {
            Shizuku.UserServiceArgs(
                ComponentName(
                    context.packageName,
                    ServiceManagerImpl::class.java.name
                )
            ).apply {
                daemon(false)
                debuggable(false)
                processNameSuffix("shizuku")
            }
        }
    }

    suspend fun fromShizuku() = from(ShizukuProvider(ContextCompat.getContext()))

    fun <T : IBinder> T.proxyBy(service: IServiceManager) = object : IBinder {
        override fun getInterfaceDescriptor() = this@proxyBy.interfaceDescriptor

        override fun pingBinder() = this@proxyBy.pingBinder()

        override fun isBinderAlive() = this@proxyBy.isBinderAlive

        override fun queryLocalInterface(descriptor: String) = null

        override fun dump(fd: FileDescriptor, args: Array<out String>?) =
            this@proxyBy.dump(fd, args)

        override fun dumpAsync(fd: FileDescriptor, args: Array<out String>?) =
            this@proxyBy.dumpAsync(fd, args)

        override fun linkToDeath(recipient: IBinder.DeathRecipient, flags: Int) =
            this@proxyBy.linkToDeath(recipient, flags)

        override fun unlinkToDeath(recipient: IBinder.DeathRecipient, flags: Int) =
            this@proxyBy.unlinkToDeath(recipient, flags)

        override fun transact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            val serviceBinder = service.asBinder()
            val newData = Parcel.obtain()

            try {
                newData.apply {
                    writeInterfaceToken(IServiceManager.DESCRIPTOR)
                    writeStrongBinder(this@proxyBy)
                    writeInt(code)
                    writeInt(flags)
                    appendFrom(data, 0, data.dataSize())
                }

                serviceBinder.transact(BINDER_TRANSACTION, newData, reply, 0)
            } finally {
                newData.recycle()
            }

            return true
        }
    }

    fun <T : IInterface> T.proxyBy(service: IServiceManager) =
        asBinder().proxyBy(service)

    fun <T : IServiceManager> T.getSystemService(name: String) =
        ServiceManager.getService(name).proxyBy(this)

    fun <T : IServiceManager, S : IService> T.addService(cls: Class<S>): IBinder? =
        addService(Service(cls))
}