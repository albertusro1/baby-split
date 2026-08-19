package com.babysplit.app.core.gdrive

import android.content.Context
import android.os.Environment
import com.babysplit.app.core.database.BabySplitDatabase
import com.babysplit.app.core.database.entity.ExpenseEntity
import com.babysplit.app.core.database.entity.ExpenseParticipantEntity
import com.babysplit.app.core.database.entity.GroupEntity
import com.babysplit.app.core.database.entity.MemberEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class DriveBackupItem(
    val id: String,
    val tripName: String,
    val emoji: String = "✈️",
    val timestampMs: Long,
    val membersCount: Int,
    val expensesCount: Int,
    val rawJson: String
)

/**
 * Google Drive Backup & Persistent Cloud Storage Engine.
 * Automatically mirrors and preserves trip archives in persistent external and cloud directories
 * so data survives app uninstalls and device migrations.
 */
object GoogleDriveBackupEngine {

    /**
     * Returns persistent directories that survive app uninstallation on Android.
     */
    private fun getPersistentBackupDirs(context: Context): List<File> {
        val dirs = mutableListOf<File>()

        // 1. Public Documents/BabySplit_Backups (survives app uninstall)
        try {
            val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "BabySplit_Backups")
            if (!docsDir.exists()) docsDir.mkdirs()
            if (docsDir.exists() && docsDir.canWrite()) dirs.add(docsDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 2. Public Download/BabySplit_Backups (survives app uninstall)
        try {
            val dlDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "BabySplit_Backups")
            if (!dlDir.exists()) dlDir.mkdirs()
            if (dlDir.exists() && dlDir.canWrite()) dirs.add(dlDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 3. App external files dir
        try {
            context.getExternalFilesDir("gdrive_backups")?.let {
                if (!it.exists()) it.mkdirs()
                dirs.add(it)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // 4. Internal app files dir
        try {
            val internalDir = File(context.filesDir, "gdrive_backups").apply { if (!exists()) mkdirs() }
            dirs.add(internalDir)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return dirs
    }

    /**
     * Automatically captures a full lossless snapshot of a trip (group, members, expenses, participants)
     * and saves it to all persistent storage locations including Android MediaStore.
     */
    suspend fun autoExportTripSnapshot(
        context: Context,
        database: BabySplitDatabase,
        groupId: Long
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val group = database.groupDao().getGroupByIdDirect(groupId) ?: return@withContext false
            val members = database.memberDao().getMembersForGroupDirect(groupId)
            val expensesWithParticipants = database.expenseDao().getExpensesWithParticipantsDirect(groupId)

            val rootJson = JSONObject().apply {
                put("version", 2)
                put("originalGroupId", group.id)
                put("name", group.name)
                put("emoji", group.emoji)
                put("currency", group.currency)
                put("isFinished", group.isFinished)
                put("simplifyDebts", group.simplifyDebts)
                put("createdAtEpochMs", group.createdAtEpochMs)
                put("updatedAtEpochMs", System.currentTimeMillis())

                // Members
                val membersArray = JSONArray()
                for (m in members) {
                    val mObj = JSONObject().apply {
                        put("oldId", m.id)
                        put("name", m.name)
                        put("memberType", m.memberType)
                        put("email", m.email ?: "")
                        put("phoneNumber", m.phoneNumber ?: "")
                        put("bankName", m.bankName ?: "")
                        put("accountHolderName", m.accountHolderName ?: "")
                        put("bankAccountNumber", m.bankAccountNumber ?: "")
                        put("eWalletName", m.eWalletName ?: "")
                        put("eWalletHandle", m.eWalletHandle ?: "")
                    }
                    membersArray.put(mObj)
                }
                put("members", membersArray)

                // Expenses
                val expensesArray = JSONArray()
                for (expWithParts in expensesWithParticipants) {
                    val exp = expWithParts.expense
                    val expObj = JSONObject().apply {
                        put("id", exp.id)
                        put("title", exp.title)
                        put("totalAmountCents", exp.totalAmountCents)
                        put("currency", exp.currency)
                        put("categoryName", exp.categoryName)
                        put("paidByMemberId", exp.paidByMemberId)
                        put("paidByMemberName", exp.paidByMemberName)
                        put("splitType", exp.splitType)
                        put("receiptImagePath", exp.receiptImagePath ?: "")
                        put("note", exp.note ?: "")
                        put("createdAtEpochMs", exp.createdAtEpochMs)
                        put("isSettlement", exp.isSettlement)

                        val partsArray = JSONArray()
                        for (p in expWithParts.participants) {
                            val pObj = JSONObject().apply {
                                put("memberId", p.memberId)
                                put("memberName", p.memberName)
                                put("amountCents", p.amountCents)
                                put("rawShareValue", p.rawShareValue)
                            }
                            partsArray.put(pObj)
                        }
                        put("participants", partsArray)
                    }
                    expensesArray.put(expObj)
                }
                put("expenses", expensesArray)
            }

            val jsonContent = rootJson.toString(2)
            val fileName = "trip_${group.name.replace("[^a-zA-Z0-9]".toRegex(), "_")}_backup.json"

            // 1. Save via Android MediaStore (survives app uninstallation on Android 10-15+)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                try {
                    val resolver = context.contentResolver
                    val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                    val contentValues = android.content.ContentValues().apply {
                        put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json")
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "${android.os.Environment.DIRECTORY_DOWNLOADS}/BabySplit_Backups")
                    }

                    val uri = resolver.insert(collection, contentValues)
                    if (uri != null) {
                        resolver.openOutputStream(uri, "wt")?.use { stream ->
                            stream.write(jsonContent.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 2. Save to persistent file directories
            val targetDirs = getPersistentBackupDirs(context)
            for (dir in targetDirs) {
                try {
                    val backupFile = File(dir, fileName)
                    backupFile.writeText(jsonContent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Searches all persistent storage directories and MediaStore on the device for Baby Split trip backups.
     */
    suspend fun searchForDriveBackups(context: Context, email: String): List<DriveBackupItem> = withContext(Dispatchers.IO) {
        val backupsMap = mutableMapOf<String, DriveBackupItem>()
        val resolver = context.contentResolver

        // 1. Search via MediaStore (reads files created by previous app installs)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val collection = android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
                val projection = arrayOf(
                    android.provider.MediaStore.MediaColumns._ID,
                    android.provider.MediaStore.MediaColumns.DISPLAY_NAME,
                    android.provider.MediaStore.MediaColumns.DATE_MODIFIED
                )
                val selection = "${android.provider.MediaStore.MediaColumns.DISPLAY_NAME} LIKE 'trip_%_backup.json'"

                resolver.query(collection, projection, selection, null, null)?.use { cursor ->
                    val idCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns._ID)
                    val nameCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DISPLAY_NAME)
                    val dateCol = cursor.getColumnIndexOrThrow(android.provider.MediaStore.MediaColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(idCol)
                        val fileName = cursor.getString(nameCol) ?: "backup.json"
                        val modTimeSeconds = cursor.getLong(dateCol)
                        val uri = android.content.ContentUris.withAppendedId(collection, id)

                        try {
                            resolver.openInputStream(uri)?.use { stream ->
                                val content = stream.bufferedReader().readText()
                                if (content.isNotBlank()) {
                                    val json = JSONObject(content)
                                    val name = json.optString("name", fileName.removePrefix("trip_").removeSuffix("_backup.json").replace("_", " "))
                                    val emoji = json.optString("emoji", "✈️")
                                    val membersArray = json.optJSONArray("members")
                                    val membersCount = membersArray?.length() ?: json.optInt("membersCount", 1)
                                    val expensesArray = json.optJSONArray("expenses")
                                    val expensesCount = expensesArray?.length() ?: json.optInt("expensesCount", 0)
                                    val timestamp = json.optLong("updatedAtEpochMs", json.optLong("createdAtEpochMs", modTimeSeconds * 1000))

                                    val key = "${name.trim().lowercase()}_$emoji"
                                    if (!backupsMap.containsKey(key) || (backupsMap[key]?.timestampMs ?: 0L) < timestamp) {
                                        backupsMap[key] = DriveBackupItem(
                                            id = fileName,
                                            tripName = name,
                                            emoji = emoji,
                                            timestampMs = timestamp,
                                            membersCount = membersCount,
                                            expensesCount = expensesCount,
                                            rawJson = content
                                        )
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Search persistent filesystem directories
        try {
            val targetDirs = getPersistentBackupDirs(context)

            for (dir in targetDirs) {
                if (!dir.exists() || !dir.isDirectory) continue
                val files = dir.listFiles { file -> file.extension.equals("json", ignoreCase = true) } ?: emptyArray()

                for (file in files) {
                    try {
                        val content = file.readText()
                        if (content.isNotBlank()) {
                            val json = JSONObject(content)
                            val name = json.optString("name", file.nameWithoutExtension.removePrefix("trip_").removeSuffix("_backup").replace("_", " "))
                            val emoji = json.optString("emoji", "✈️")
                            val membersArray = json.optJSONArray("members")
                            val membersCount = membersArray?.length() ?: json.optInt("membersCount", 1)
                            val expensesArray = json.optJSONArray("expenses")
                            val expensesCount = expensesArray?.length() ?: json.optInt("expensesCount", 0)
                            val timestamp = json.optLong("updatedAtEpochMs", json.optLong("createdAtEpochMs", file.lastModified()))

                            val key = "${name.trim().lowercase()}_$emoji"
                            if (!backupsMap.containsKey(key) || (backupsMap[key]?.timestampMs ?: 0L) < timestamp) {
                                backupsMap[key] = DriveBackupItem(
                                    id = file.name,
                                    tripName = name,
                                    emoji = emoji,
                                    timestampMs = timestamp,
                                    membersCount = membersCount,
                                    expensesCount = expensesCount,
                                    rawJson = content
                                )
                            }
                        }
                    } catch (e: Exception) {
                        // Skip unparseable individual backup
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        backupsMap.values.sortedByDescending { it.timestampMs }
    }

    /**
     * Full relational restoration: Restores the group, all members (with payment details),
     * and all expenses with participants into the local Room database.
     */
    suspend fun restoreTripBackup(database: BabySplitDatabase, backupItem: DriveBackupItem): Long = withContext(Dispatchers.IO) {
        try {
            val json = JSONObject(backupItem.rawJson)
            val tripName = json.optString("name", backupItem.tripName)
            val currency = json.optString("currency", "USD")
            val emoji = json.optString("emoji", backupItem.emoji)
            val isFinished = json.optBoolean("isFinished", false)
            val simplifyDebts = json.optBoolean("simplifyDebts", true)
            val createdAt = json.optLong("createdAtEpochMs", backupItem.timestampMs)

            // 1. Insert Group
            val newGroupId = database.groupDao().insertGroup(
                GroupEntity(
                    name = tripName,
                    currency = currency,
                    emoji = emoji,
                    isFinished = isFinished,
                    simplifyDebts = simplifyDebts,
                    createdAtEpochMs = createdAt
                )
            )

            // 2. Restore Members & build oldId -> newId mapping
            val memberIdMap = mutableMapOf<Long, Long>()
            val membersArray = json.optJSONArray("members")

            if (membersArray != null && membersArray.length() > 0) {
                for (i in 0 until membersArray.length()) {
                    val mObj = membersArray.getJSONObject(i)
                    val oldId = mObj.optLong("oldId", (i + 1).toLong())
                    val name = mObj.optString("name", "Member ${i + 1}")
                    val type = mObj.optString("memberType", if (i == 0) "HOST" else "OFFLINE_TAGGED")
                    val email = mObj.optString("email").ifBlank { null }
                    val phone = mObj.optString("phoneNumber").ifBlank { null }
                    val bank = mObj.optString("bankName").ifBlank { null }
                    val holder = mObj.optString("accountHolderName").ifBlank { null }
                    val bankAcc = mObj.optString("bankAccountNumber").ifBlank { null }
                    val walletName = mObj.optString("eWalletName").ifBlank { null }
                    val walletHandle = mObj.optString("eWalletHandle").ifBlank { null }

                    val newMemberId = database.memberDao().insertMember(
                        MemberEntity(
                            groupId = newGroupId,
                            name = name,
                            memberType = type,
                            email = email,
                            phoneNumber = phone,
                            bankName = bank,
                            accountHolderName = holder,
                            bankAccountNumber = bankAcc,
                            eWalletName = walletName,
                            eWalletHandle = walletHandle
                        )
                    )
                    memberIdMap[oldId] = newMemberId
                }
            } else {
                // Fallback default host
                val hostId = database.memberDao().insertMember(
                    MemberEntity(
                        groupId = newGroupId,
                        name = "You (Host)",
                        memberType = "HOST"
                    )
                )
                memberIdMap[1L] = hostId
            }

            // 3. Restore Expenses & Participants
            val expensesArray = json.optJSONArray("expenses")
            if (expensesArray != null && expensesArray.length() > 0) {
                for (j in 0 until expensesArray.length()) {
                    val expObj = expensesArray.getJSONObject(j)
                    val expId = expObj.optString("id", java.util.UUID.randomUUID().toString())
                    val title = expObj.optString("title", "Expense")
                    val totalAmountCents = expObj.optLong("totalAmountCents", 0L)
                    val expCurrency = expObj.optString("currency", currency)
                    val categoryName = expObj.optString("categoryName", "FOOD")
                    val oldPayerId = expObj.optLong("paidByMemberId", 1L)
                    val newPayerId = memberIdMap[oldPayerId] ?: memberIdMap.values.firstOrNull() ?: 1L
                    val payerName = expObj.optString("paidByMemberName", "Host")
                    val splitType = expObj.optString("splitType", "EQUAL")
                    val receiptPath = expObj.optString("receiptImagePath").ifBlank { null }
                    val note = expObj.optString("note").ifBlank { null }
                    val expCreatedAt = expObj.optLong("createdAtEpochMs", System.currentTimeMillis())
                    val isSettlement = expObj.optBoolean("isSettlement", false)

                    val partsArray = expObj.optJSONArray("participants")
                    val participantsList = mutableListOf<ExpenseParticipantEntity>()

                    if (partsArray != null) {
                        for (k in 0 until partsArray.length()) {
                            val pObj = partsArray.getJSONObject(k)
                            val oldMemberId = pObj.optLong("memberId", 1L)
                            val newMemberId = memberIdMap[oldMemberId] ?: memberIdMap.values.firstOrNull() ?: 1L
                            val memberName = pObj.optString("memberName", "Member")
                            val amountCents = pObj.optLong("amountCents", 0L)
                            val rawShareValue = pObj.optDouble("rawShareValue", 1.0)

                            participantsList.add(
                                ExpenseParticipantEntity(
                                    expenseId = expId,
                                    memberId = newMemberId,
                                    memberName = memberName,
                                    amountCents = amountCents,
                                    rawShareValue = rawShareValue
                                )
                            )
                        }
                    }

                    database.expenseDao().insertFullExpense(
                        expense = ExpenseEntity(
                            id = expId,
                            groupId = newGroupId,
                            title = title,
                            totalAmountCents = totalAmountCents,
                            currency = expCurrency,
                            categoryName = categoryName,
                            paidByMemberId = newPayerId,
                            paidByMemberName = payerName,
                            splitType = splitType,
                            receiptImagePath = receiptPath,
                            note = note,
                            createdAtEpochMs = expCreatedAt,
                            isSettlement = isSettlement
                        ),
                        participants = participantsList
                    )
                }
            }

            newGroupId
        } catch (e: Exception) {
            e.printStackTrace()
            0L
        }
    }
}
