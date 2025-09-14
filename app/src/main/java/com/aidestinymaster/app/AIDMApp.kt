package com.aidestinymaster.app

import android.app.Application
import android.util.Log

/**
 * Auxiliary Application class to avoid name clash with the real
 * `com.aidestinymaster.app.AIDMApp` defined in `App.kt`.
 * Not referenced by Manifest; safe to keep for future debug hooks.
 */
class AIDMApp2 : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            Log.i("AIDMApp2", "aux Application loaded (pid=${android.os.Process.myPid()})")
        } catch (_: Throwable) { }
    }
}
