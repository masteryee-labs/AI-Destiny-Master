package com.aidestinymaster.billing

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object BillingLogger {
    private fun file(ctx: Context): File {
        val dir = File(ctx.filesDir, "logs")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, "app.log")
    }
    fun log(ctx: Context, msg: String) {
        runCatching {
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date())
            file(ctx).appendText("[$ts][Billing] $msg\n")
        }
    }
}
