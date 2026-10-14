package com.example.priv.data.export

import android.content.Context
import android.net.Uri
import com.example.priv.data.database.PrivDatabase
import com.example.priv.data.entity.*
import com.example.priv.data.model.SyncStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class BackupManifest(
    val formatVersion: String = "1.0.0-beta.3",
    val appVersion: String = "1.0.0-beta.3",
    val timestamp: Long = System.currentTimeMillis(),
    val memoryCount: Int = 0,
    val personCount: Int = 0,
    val momentCount: Int = 0,
    val collectionCount: Int = 0,
    val attachmentCount: Int = 0,
    val checksumSha256: String = ""
)

data class RestoreResult(
    val success: Boolean,
    val restoredMemories: Int = 0,
    val restoredPersons: Int = 0,
    val restoredMoments: Int = 0,
    val restoredMediaCount: Int = 0,
    val message: String
)

class BackupExportManager(
    private val context: Context,
    private val database: PrivDatabase
) {

    suspend fun generateBackupJson(): Pair<BackupManifest, String> = withContext(Dispatchers.IO) {
        val memories = database.memoryDao().getAllMemoriesList()
        val attachments = database.memoryDao().getAllAttachmentsList()
        val persons = database.personDao().getAllPersonsList()
        val moments = database.momentDao().getAllMomentsList()
        val collections = database.collectionDao().getAllCollectionsList()
        val groups = database.groupDao().getAllGroupsList()

        val rootJson = JSONObject().apply {
            put("app", "Priv - Guarde as vozes e momentos")
            put("version", "1.0.0-beta.3")
            put("exported_at", System.currentTimeMillis())

            val memoriesArr = JSONArray()
            memories.forEach { m ->
                memoriesArr.put(JSONObject().apply {
                    put("id", m.id)
                    put("space_id", m.spaceId)
                    put("title", m.title)
                    put("note", m.note)
                    put("timestamp", m.timestamp)
                    put("source", m.source)
                    put("is_favorite", m.isFavorite)
                    put("is_archived", m.isArchived)
                    put("in_trash", m.inTrash)
                    put("primary_person_id", m.primaryPersonId ?: "")
                    put("moment_id", m.momentId ?: "")
                    put("collection_id", m.collectionId ?: "")
                })
            }
            put("memories", memoriesArr)

            val attachmentsArr = JSONArray()
            attachments.forEach { a ->
                attachmentsArr.put(JSONObject().apply {
                    put("id", a.id)
                    put("memory_id", a.memoryId)
                    put("type", a.type)
                    put("local_uri", a.uri)
                    put("file_name", a.fileName)
                    put("file_size", a.fileSize)
                    put("mime_type", a.mimeType)
                    put("duration_ms", a.durationMs)
                    put("transcription", a.transcription)
                    put("summary", a.summary)
                })
            }
            put("attachments", attachmentsArr)

            val personsArr = JSONArray()
            persons.forEach { p ->
                personsArr.put(JSONObject().apply {
                    put("id", p.id)
                    put("space_id", p.spaceId)
                    put("name", p.name)
                    put("relationship", p.relationship)
                    put("avatar_uri", p.avatarUri)
                    put("bio_note", p.bioNote)
                })
            }
            put("persons", personsArr)

            val momentsArr = JSONArray()
            moments.forEach { mo ->
                momentsArr.put(JSONObject().apply {
                    put("id", mo.id)
                    put("space_id", mo.spaceId)
                    put("title", mo.title)
                    put("description", mo.description)
                })
            }
            put("moments", momentsArr)

            val collectionsArr = JSONArray()
            collections.forEach { col ->
                collectionsArr.put(JSONObject().apply {
                    put("id", col.id)
                    put("space_id", col.spaceId)
                    put("title", col.title)
                    put("description", col.description)
                })
            }
            put("collections", collectionsArr)

            val groupsArr = JSONArray()
            groups.forEach { g ->
                groupsArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("space_id", g.spaceId)
                    put("name", g.name)
                    put("description", g.description)
                })
            }
            put("groups", groupsArr)
        }

        val jsonString = rootJson.toString(2)
        val digest = MessageDigest.getInstance("SHA-256")
        val sha256 = digest.digest(jsonString.toByteArray()).fold("") { str, it -> str + "%02x".format(it) }

        val manifest = BackupManifest(
            formatVersion = "1.0.0-beta.3",
            appVersion = "1.0.0-beta.3",
            timestamp = System.currentTimeMillis(),
            memoryCount = memories.size,
            personCount = persons.size,
            momentCount = moments.size,
            collectionCount = collections.size,
            attachmentCount = attachments.size,
            checksumSha256 = sha256
        )

        Pair(manifest, jsonString)
    }

    suspend fun writeBackupToUri(targetUri: Uri, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                outputStream.write(jsonContent.toByteArray())
                outputStream.flush()
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun exportFullBackupZip(targetUri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            val (manifest, dataJson) = generateBackupJson()
            context.contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                ZipOutputStream(BufferedOutputStream(outputStream)).use { zip ->
                    // 1. Add manifest.json
                    zip.putNextEntry(ZipEntry("manifest.json"))
                    val manifestJson = JSONObject().apply {
                        put("formatVersion", manifest.formatVersion)
                        put("appVersion", manifest.appVersion)
                        put("timestamp", manifest.timestamp)
                        put("memoryCount", manifest.memoryCount)
                        put("personCount", manifest.personCount)
                        put("checksumSha256", manifest.checksumSha256)
                    }.toString(2)
                    zip.write(manifestJson.toByteArray())
                    zip.closeEntry()

                    // 2. Add data.json
                    zip.putNextEntry(ZipEntry("data.json"))
                    zip.write(dataJson.toByteArray())
                    zip.closeEntry()

                    // 3. Add checksums.json
                    zip.putNextEntry(ZipEntry("checksums.json"))
                    val checksumsJson = JSONObject().apply {
                        put("data.json", manifest.checksumSha256)
                    }.toString(2)
                    zip.write(checksumsJson.toByteArray())
                    zip.closeEntry()

                    // 4. Add local media files from internal files directory
                    val mediaDir = File(context.filesDir, "media")
                    if (mediaDir.exists() && mediaDir.isDirectory) {
                        mediaDir.walkTopDown().forEach { file ->
                            if (file.isFile) {
                                val relPath = "media/" + file.relativeTo(mediaDir).path
                                zip.putNextEntry(ZipEntry(relPath))
                                file.inputStream().use { input -> input.copyTo(zip) }
                                zip.closeEntry()
                            }
                        }
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun restoreFromBackup(sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(sourceUri)
            val fileName = sourceUri.lastPathSegment ?: ""

            if (mimeType?.contains("zip") == true || fileName.endsWith(".zip")) {
                restoreFromZipStream(sourceUri)
            } else {
                restoreFromJsonStream(sourceUri)
            }
        } catch (e: Exception) {
            RestoreResult(
                success = false,
                message = "Erro ao processar arquivo de backup: ${e.message}"
            )
        }
    }

    private suspend fun restoreFromJsonStream(sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        val jsonString = context.contentResolver.openInputStream(sourceUri)?.use { input ->
            input.bufferedReader().use { it.readText() }
        } ?: return@withContext RestoreResult(false, message = "Não foi possível ler o arquivo selecionado.")

        parseAndRestoreDataJson(jsonString)
    }

    private suspend fun restoreFromZipStream(sourceUri: Uri): RestoreResult = withContext(Dispatchers.IO) {
        var dataJsonString: String? = null
        var restoredMediaCount = 0

        context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            ZipInputStream(BufferedInputStream(inputStream)).use { zipInput ->
                var entry: ZipEntry? = zipInput.nextEntry
                while (entry != null) {
                    if (entry.name == "data.json") {
                        dataJsonString = zipInput.bufferedReader().readText()
                    } else if (entry.name.startsWith("media/") && !entry.isDirectory) {
                        val fileName = File(entry.name).name
                        val targetDir = File(context.filesDir, "media")
                        if (!targetDir.exists()) targetDir.mkdirs()
                        val targetFile = File(targetDir, fileName)
                        FileOutputStream(targetFile).use { out -> zipInput.copyTo(out) }
                        restoredMediaCount++
                    }
                    zipInput.closeEntry()
                    entry = zipInput.nextEntry
                }
            }
        }

        if (dataJsonString.isNullOrBlank()) {
            return@withContext RestoreResult(false, message = "Arquivo ZIP inválido ou sem data.json contido.")
        }

        val result = parseAndRestoreDataJson(dataJsonString!!)
        result.copy(restoredMediaCount = restoredMediaCount)
    }

    private suspend fun parseAndRestoreDataJson(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        val root = JSONObject(jsonString)

        var restoredMemories = 0
        var restoredPersons = 0
        var restoredMoments = 0

        // Restore Persons
        val personsArr = root.optJSONArray("persons")
        if (personsArr != null) {
            for (i in 0 until personsArr.length()) {
                val obj = personsArr.getJSONObject(i)
                val p = PersonEntity(
                    id = obj.optString("id"),
                    spaceId = obj.optString("space_id", "default_space"),
                    name = obj.optString("name", "Pessoa"),
                    relationship = obj.optString("relationship", ""),
                    avatarUri = obj.optString("avatar_uri", ""),
                    bioNote = obj.optString("bio_note", "")
                )
                database.personDao().insertPerson(p)
                restoredPersons++
            }
        }

        // Restore Moments
        val momentsArr = root.optJSONArray("moments")
        if (momentsArr != null) {
            for (i in 0 until momentsArr.length()) {
                val obj = momentsArr.getJSONObject(i)
                val m = MomentEntity(
                    id = obj.optString("id"),
                    spaceId = obj.optString("space_id", "default_space"),
                    title = obj.optString("title", "Momento"),
                    description = obj.optString("description", "")
                )
                database.momentDao().insertMoment(m)
                restoredMoments++
            }
        }

        // Restore Collections
        val collectionsArr = root.optJSONArray("collections")
        if (collectionsArr != null) {
            for (i in 0 until collectionsArr.length()) {
                val obj = collectionsArr.getJSONObject(i)
                val col = CollectionEntity(
                    id = obj.optString("id"),
                    spaceId = obj.optString("space_id", "default_space"),
                    title = obj.optString("title", "Coleção"),
                    description = obj.optString("description", "")
                )
                database.collectionDao().insertCollection(col)
            }
        }

        // Restore Groups
        val groupsArr = root.optJSONArray("groups")
        if (groupsArr != null) {
            for (i in 0 until groupsArr.length()) {
                val obj = groupsArr.getJSONObject(i)
                val g = GroupEntity(
                    id = obj.optString("id"),
                    spaceId = obj.optString("space_id", "default_space"),
                    name = obj.optString("name", "Grupo"),
                    description = obj.optString("description", "")
                )
                database.groupDao().insertGroup(g)
            }
        }

        // Restore Memories
        val memoriesArr = root.optJSONArray("memories")
        if (memoriesArr != null) {
            for (i in 0 until memoriesArr.length()) {
                val obj = memoriesArr.getJSONObject(i)
                val mem = MemoryEntity(
                    id = obj.optString("id"),
                    spaceId = obj.optString("space_id", "default_space"),
                    title = obj.optString("title", "Memória"),
                    note = obj.optString("note", ""),
                    timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                    source = obj.optString("source", "WhatsApp"),
                    isFavorite = obj.optBoolean("is_favorite", false),
                    isArchived = obj.optBoolean("is_archived", false),
                    inTrash = obj.optBoolean("in_trash", false),
                    primaryPersonId = obj.optString("primary_person_id").ifBlank { null },
                    momentId = obj.optString("moment_id").ifBlank { null },
                    collectionId = obj.optString("collection_id").ifBlank { null },
                    tags = obj.optString("tags", ""),
                    syncStatus = SyncStatus.LOCAL_ONLY
                )
                database.memoryDao().insertMemory(mem)
                restoredMemories++
            }
        }

        // Restore Attachments
        val attachmentsArr = root.optJSONArray("attachments")
        if (attachmentsArr != null) {
            for (i in 0 until attachmentsArr.length()) {
                val obj = attachmentsArr.getJSONObject(i)
                val att = MemoryAttachmentEntity(
                    id = obj.optString("id"),
                    memoryId = obj.optString("memory_id"),
                    type = obj.optString("type", "AUDIO"),
                    uri = obj.optString("local_uri", ""),
                    fileName = obj.optString("file_name", "audio.mp3"),
                    fileSize = obj.optLong("file_size", 0L),
                    mimeType = obj.optString("mime_type", "audio/mp3"),
                    durationMs = obj.optLong("duration_ms", 0L),
                    transcription = obj.optString("transcription", ""),
                    summary = obj.optString("summary", "")
                )
                database.memoryDao().insertAttachment(att)
            }
        }

        RestoreResult(
            success = true,
            restoredMemories = restoredMemories,
            restoredPersons = restoredPersons,
            restoredMoments = restoredMoments,
            message = "Restauração concluída: $restoredMemories memórias e $restoredPersons pessoas mescladas com sucesso!"
        )
    }
}
