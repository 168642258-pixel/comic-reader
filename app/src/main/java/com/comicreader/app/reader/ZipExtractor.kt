package com.comicreader.app.reader

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipFile

object ZipExtractor {

    private val imageExtensions = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif", "avif")

    /**
     * 解压 zip 到目标目录，返回排序后的图片文件路径列表
     */
    fun extract(zipFile: File, outputDir: File): List<String> {
        outputDir.mkdirs()
        val imageFiles = mutableListOf<String>()

        try {
            ZipFile(zipFile).use { zip ->
                val entries = zip.entries().asSequence()
                    .filter { !it.isDirectory }
                    .filter { isImageFile(it.name) }
                    .sortedBy { normalizeSortKey(it.name) }

                for (entry in entries) {
                    val outputFile = File(outputDir, entry.name)
                    outputFile.parentFile?.mkdirs()

                    zip.getInputStream(entry).use { input ->
                        FileOutputStream(outputFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    imageFiles.add(outputFile.absolutePath)
                }
            }
        } catch (e: Exception) {
            throw e
        }

        return imageFiles
    }

    /**
     * 获取目录下的所有图片文件，按文件名排序
     */
    fun getImageFiles(directory: File): List<String> {
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        return directory.walkTopDown()
            .filter { it.isFile && isImageFile(it.name) }
            .map { it.absolutePath }
            .sortedWith(compareBy { normalizeSortKey(File(it).name) })
            .toList()
    }

    private fun isImageFile(name: String): Boolean {
        val ext = name.substringAfterLast('.', "").lowercase()
        return ext in imageExtensions
    }

    /**
     * 对文件名做自然排序（同时处理字母和数字）
     * 例: "page_2.jpg" < "page_10.jpg"
     */
    private fun normalizeSortKey(name: String): List<Comparable<*>> {
        val parts = mutableListOf<Comparable<*>>()
        val buffer = StringBuilder()
        var isDigit = false

        for (ch in name) {
            if (ch.isDigit()) {
                if (!isDigit && buffer.isNotEmpty()) {
                    parts.add(buffer.toString())
                    buffer.clear()
                }
                isDigit = true
                buffer.append(ch)
            } else {
                if (isDigit && buffer.isNotEmpty()) {
                    parts.add(buffer.toString().toInt())
                    buffer.clear()
                }
                isDigit = false
                buffer.append(ch.lowercaseChar())
            }
        }
        if (buffer.isNotEmpty()) {
            parts.add(if (isDigit) buffer.toString().toInt() else buffer.toString())
        }

        return parts
    }
}
