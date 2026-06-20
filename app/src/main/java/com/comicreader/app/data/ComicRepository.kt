package com.comicreader.app.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import java.io.File

class ComicRepository(private val context: Context) {

    private val comicsDir = File(context.filesDir, "comics")
    private val metadataFile = File(context.filesDir, "library.json")

    init {
        comicsDir.mkdirs()
    }

    fun getAll(): List<Comic> {
        if (!metadataFile.exists()) return emptyList()
        val text = try {
            metadataFile.readText()
        } catch (_: Exception) { return emptyList() }
        val jsonArray = try {
            JSONArray(text)
        } catch (_: Exception) { return emptyList() }
        return (0 until jsonArray.length()).map { index ->
            Comic.fromJson(jsonArray.getJSONObject(index))
        }.sortedByDescending { it.addedAt }
    }

    fun getById(id: String): Comic? {
        return getAll().find { it.id == id }
    }

    fun save(comic: Comic) {
        val all = getAll().toMutableList()
        val index = all.indexOfFirst { it.id == comic.id }
        if (index >= 0) {
            all[index] = comic
        } else {
            all.add(comic)
        }
        writeAll(all)
    }

    fun delete(id: String) {
        val all = getAll().filter { it.id != id }
        writeAll(all)
        val dir = File(comicsDir, id)
        if (dir.exists()) {
            dir.deleteRecursively()
        }
        val zipFile = File(comicsDir, "${id}.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }
    }

    private fun writeAll(comics: List<Comic>) {
        val jsonArray = JSONArray(comics.map { it.toJson() })
        metadataFile.writeText(jsonArray.toString(2))
    }

    /**
     * 从 zip 文件导入漫画
     */
    fun importZip(uri: Uri, title: String): Comic? {
        val id = java.util.UUID.randomUUID().toString()
        val zipFile = File(comicsDir, "${id}.zip")
        val extractDir = File(comicsDir, id)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                zipFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            } ?: return null

            val imageFiles = com.comicreader.app.reader.ZipExtractor.extract(zipFile, extractDir)
            if (imageFiles.isEmpty()) {
                zipFile.delete()
                extractDir.deleteRecursively()
                return null
            }

            val coverPath = imageFiles.firstOrNull()
            val comic = Comic(
                id = id,
                title = title.ifBlank { zipFile.nameWithoutExtension },
                coverPath = coverPath,
                pageCount = imageFiles.size,
                addedAt = System.currentTimeMillis(),
                directoryPath = extractDir.absolutePath
            )

            save(comic)
            // 保存后删除 zip 以节省空间
            zipFile.delete()
            comic
        } catch (e: Exception) {
            zipFile.delete()
            extractDir.deleteRecursively()
            null
        }
    }

    /**
     * 更新阅读进度
     */
    fun updateProgress(comicId: String, page: Int) {
        val comic = getById(comicId) ?: return
        save(comic.copy(lastReadPage = page))
    }
}
