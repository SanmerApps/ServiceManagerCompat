package dev.sanmer.su

import android.os.IBinder

interface IService {
    val name: String
    fun create(service: IServiceManager): IBinder
}