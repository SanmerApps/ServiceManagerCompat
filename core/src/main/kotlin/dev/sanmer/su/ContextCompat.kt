package dev.sanmer.su

import android.app.ActivityThread
import android.content.Context
import android.content.ContextWrapper

internal object ContextCompat {
    fun getContext(): Context {
        var context: Context = ActivityThread.currentApplication()
        while (context is ContextWrapper) {
            context = context.baseContext
        }

        return context
    }
}