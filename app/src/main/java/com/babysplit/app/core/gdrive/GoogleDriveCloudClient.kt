package com.babysplit.app.core.gdrive

import android.content.Context
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Direct Google Drive Cloud Client using Google Drive v3 REST API.
 * Uploads and downloads trip backup archives directly to/from Google Drive cloud servers.
 */
object GoogleDriveCloudClient {

    private const val DRIVE_SCOPE = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"

    /**
     * Obtains an OAuth2 Bearer Access Token for the user's Google Account.
     */
    suspend fun getAccessToken(context: Context, accountEmail: String): String? = withContext(Dispatchers.IO) {
        try {
            GoogleAuthUtil.getToken(context, accountEmail, DRIVE_SCOPE)
        } catch (e: UserRecoverableAuthException) {
            e.printStackTrace()
            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Searches Google Drive cloud servers for trip backup files.
     */
    suspend fun searchDriveFiles(token: String): List<DriveBackupItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<DriveBackupItem>()
        try {
            val query = URLEncoder.encode("name contains 'trip_' and trashed = false", "UTF-8")
            val url = URL("https://www.googleapis.com/drive/v3/files?q=&fields=files(id,name,modifiedTime)")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode in 200..299) {
                val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val files = root.optJSONArray("files") ?: JSONArray()

                for (i in 0 until files.length()) {
                    val fileObj = files.getJSONObject(i)
                    val fileId = fileObj.getString("id")
                    val fileName = fileObj.getString("name")

                    // Download content for each discovered cloud backup
                    val content = downloadFileContent(token, fileId)
                    if (!content.isNullOrBlank()) {
                        try {
                            val json = JSONObject(content)
                            val name = json.optString("name", fileName.removePrefix("trip_").removeSuffix("_backup.json").replace("_", " "))
                            val emoji = json.optString("emoji", "✈️")
                            val membersArray = json.optJSONArray("members")
                            val membersCount = membersArray?.length() ?: json.optInt("membersCount", 1)
                            val expensesArray = json.optJSONArray("expenses")
                            val expensesCount = expensesArray?.length() ?: json.optInt("expensesCount", 0)
                            val timestamp = json.optLong("updatedAtEpochMs", json.optLong("createdAtEpochMs", System.currentTimeMillis()))

                            results.add(
                                DriveBackupItem(
                                    id = fileId,
                                    tripName = name,
                                    emoji = emoji,
                                    timestampMs = timestamp,
                                    membersCount = membersCount,
                                    expensesCount = expensesCount,
                                    rawJson = content
                                )
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }

    /**
     * Downloads the raw JSON content of a file from Google Drive.
     */
    suspend fun downloadFileContent(token: String, fileId: String): String? = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://www.googleapis.com/drive/v3/files/=media")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ")
                connectTimeout = 10000
                readTimeout = 10000
            }

            if (conn.responseCode in 200..299) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Uploads or updates a trip backup JSON file directly on Google Drive cloud servers.
     */
    suspend fun uploadOrUpdateBackup(token: String, fileName: String, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 1. Check if file already exists in Google Drive
            val query = URLEncoder.encode("name = '' and trashed = false", "UTF-8")
            val searchUrl = URL("https://www.googleapis.com/drive/v3/files?q=&fields=files(id)")
            val searchConn = (searchUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Authorization", "Bearer ")
                connectTimeout = 8000
                readTimeout = 8000
            }

            var existingFileId: String? = null
            if (searchConn.responseCode in 200..299) {
                val responseText = searchConn.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(responseText)
                val files = root.optJSONArray("files")
                if (files != null && files.length() > 0) {
                    existingFileId = files.getJSONObject(0).getString("id")
                }
            }

            if (existingFileId != null) {
                // Update existing file content
                val updateUrl = URL("https://www.googleapis.com/upload/drive/v3/files/=media")
                val updateConn = (updateUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "PATCH"
                    setRequestProperty("Authorization", "Bearer ")
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                OutputStreamWriter(updateConn.outputStream, "UTF-8").use {
                    it.write(jsonContent)
                    it.flush()
                }

                updateConn.responseCode in 200..299
            } else {
                // Create new multipart file
                val boundary = "=======" + System.currentTimeMillis() + "======="
                val uploadUrl = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                val uploadConn = (uploadUrl.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer ")
                    setRequestProperty("Content-Type", "multipart/related; boundary=")
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                }

                val metadata = JSONObject().apply {
                    put("name", fileName)
                    put("mimeType", "application/json")
                }.toString()

                val body = buildString {
                    append("--\r\n")
                    append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                    append(metadata)
                    append("\r\n--\r\n")
                    append("Content-Type: application/json\r\n\r\n")
                    append(jsonContent)
                    append("\r\n----\r\n")
                }

                OutputStreamWriter(uploadConn.outputStream, "UTF-8").use {
                    it.write(body)
                    it.flush()
                }

                uploadConn.responseCode in 200..299
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
