package com.aidestinymaster.sync

import android.content.Context
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as GDriveFile
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

class DriveService(private val context: Context) {

    private fun buildDrive(): Drive? {
        val token = GoogleAuthManager.getAccessToken(context) ?: return null
        val transport = AndroidHttp.newCompatibleTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        val initializer = HttpRequestInitializer { req ->
            req.headers.authorization = "Bearer $token"
            req.connectTimeout = 60_000
            req.readTimeout = 60_000
        }
        return Drive.Builder(transport, jsonFactory, initializer)
            .setApplicationName("AIDestinyMaster")
            .build()
    }

    fun ensureAppFolder(): String = "appDataFolder"

    fun uploadJson(name: String, json: String, encrypt: Boolean = true) {
        val drive = buildDrive() ?: return
        val meta = GDriveFile().apply {
            this.name = name
            this.parents = listOf("appDataFolder")
            this.mimeType = if (encrypt) "application/octet-stream" else "application/json"
        }
        val content = if (encrypt) {
            val bytes = CryptoHelper.encryptToBytes(context, json)
            ByteArrayContent("application/octet-stream", bytes)
        } else {
            ByteArrayContent.fromString("application/json", json)
        }
        drive.files().create(meta, content)
            .setFields("id,name")
            .execute()
    }

    fun downloadJson(name: String, decrypt: Boolean = true): String? {
        val drive = buildDrive() ?: return null
        val list = drive.files().list()
            .setSpaces("appDataFolder")
            .setQ("name='${name.replace("'", "\\'")}' and trashed=false")
            .setFields("files(id,name)")
            .execute()
        val file = list.files?.firstOrNull() ?: return null
        val out = ByteArrayOutputStream()
        drive.files().get(file.id).executeMediaAndDownloadTo(out)
        val bytes = out.toByteArray()
        return if (decrypt) {
            CryptoHelper.decryptFromBytes(context, bytes)
        } else {
            bytes.toString(StandardCharsets.UTF_8)
        }
    }
}
