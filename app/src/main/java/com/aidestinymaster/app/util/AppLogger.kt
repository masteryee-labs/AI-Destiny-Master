package com.aidestinymaster.app.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private fun logFile(ctx: Context): File {
        val dir = File(ctx.filesDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "app.log")
    }

    fun log(ctx: Context, msg: String) {
        try {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            logFile(ctx).appendText("[$ts] $msg\n")
        } catch (_: Throwable) {
            // best-effort logging
        }
    }
}
