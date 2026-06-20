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
     * 对文件名做自然排序，数字部分补零以便字符串比较
     * 例: "page_2.jpg" -> "page_0000000002.jpg"
     */
    private fun normalizeSortKey(name: String): String {
        // 把文件名中的数字替换为固定宽度（补零），实现自然排序
        return name.replace(Regex("""\d+""")) {
            it.value.padStart(10, '0')
        }.lowercase()
    }
}
