package dev.sanmer.su

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class ClassWrapper<T>(
    internal val original: Class<T>
) : Parcelable {
    companion object {
        fun Class<*>.wrap() = ClassWrapper(this)
    }
}