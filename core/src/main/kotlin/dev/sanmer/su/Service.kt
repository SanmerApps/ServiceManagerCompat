package dev.sanmer.su

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class Service<T : IService>(
    private val cls: Class<T>
) : Parcelable {
    val original: T
        get() = cls.getDeclaredConstructor().let {
            it.isAccessible = true
            it.newInstance()
        }
}