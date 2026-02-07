package com.back.standard.util

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import tools.jackson.databind.ObjectMapper
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*
import javax.imageio.ImageIO

object Ut {
    object JWT {
        fun toString(secret: String, expireSeconds: Int, body: Map<String, Any>): String {
            val issuedAt = Date()
            val expiration = Date(issuedAt.time + 1000L * expireSeconds)

            val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

            return Jwts.builder()
                .claims(body)
                .issuedAt(issuedAt)
                .expiration(expiration)
                .signWith(secretKey)
                .compact()
        }

        fun isValid(secret: String, jwtStr: String): Boolean {
            return try {
                val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwtStr)

                true
            } catch (_: Exception) {
                false
            }
        }

        fun payload(secret: String, jwtStr: String): Map<String, Any>? {
            return try {
                val secretKey = Keys.hmacShaKeyFor(secret.toByteArray())

                @Suppress("UNCHECKED_CAST")
                Jwts
                    .parser()
                    .verifyWith(secretKey)
                    .build()
                    .parse(jwtStr)
                    .payload as Map<String, Any>

            } catch (_: Exception) {
                null
            }
        }
    }

    object JSON {
        lateinit var objectMapper: ObjectMapper

        fun toString(obj: Any, defaultValue: String = ""): String {
            return try {
                objectMapper.writeValueAsString(obj)
            } catch (_: Exception) {
                defaultValue
            }
        }

        inline fun <reified T> fromMap(map: Any?): T {
            return objectMapper.convertValue(map, T::class.java)
        }

        fun <T> fromString(json: String, cls: Class<T>): T {
            return objectMapper.readValue(json, cls)
        }

        inline fun <reified T> fromString(json: String): T {
            return objectMapper.readValue(json, T::class.java)
        }
    }

    object CMD {
        fun run(vararg args: String) {
            val isWindows = System
                .getProperty("os.name")
                .lowercase(Locale.getDefault())
                .contains("win")

            val builder = ProcessBuilder(
                args
                    .map { it.replace("{{DOT_CMD}}", if (isWindows) ".cmd" else "") }
                    .toList()
            )

            // 에러 스트림도 출력 스트림과 함께 병합
            builder.redirectErrorStream(true)

            // 프로세스 시작
            val process = builder.start()

            process.inputStream.bufferedReader().useLines { lines ->
                lines.forEach { println(it) }
            }

            val exitCode = process.waitFor()

            println("run exit code: $exitCode")
        }

        fun runAsync(vararg args: String) {
            kotlin.concurrent.thread { run(*args) }
        }
    }

    object FILE {
        private val MIME_TYPE_MAP: LinkedHashMap<String, String> = linkedMapOf(
            "application/json" to "json",
            "text/plain" to "txt",
            "text/html" to "html",
            "text/css" to "css",
            "application/javascript" to "js",
            "image/jpeg" to "jpg",
            "image/png" to "png",
            "image/gif" to "gif",
            "image/webp" to "webp",
            "image/svg+xml" to "svg",
            "application/pdf" to "pdf",
            "application/xml" to "xml",
            "application/zip" to "zip",
            "application/gzip" to "gz",
            "application/x-tar" to "tar",
            "application/x-7z-compressed" to "7z",
            "application/vnd.rar" to "rar",
            "audio/mpeg" to "mp3",
            "audio/mp4" to "m4a",
            "audio/x-m4a" to "m4a",
            "audio/wav" to "wav",
            "video/quicktime" to "mov",
            "video/mp4" to "mp4",
            "video/webm" to "webm",
            "video/x-msvideo" to "avi"
        )

        fun getFileExt(filePath: String): String {
            val filename = Path.of(filePath).fileName.toString()

            return if (filename.contains("."))
                filename.substring(filename.lastIndexOf('.') + 1)
            else
                ""
        }

        fun getFileExtTypeCodeFromFileExt(ext: String): String {
            return when (ext) {
                "jpeg", "jpg", "gif", "png", "svg", "webp" -> "img"
                "mp4", "avi", "mov" -> "video"
                "mp3", "m4a" -> "audio"
                else -> "etc"
            }
        }

        fun getFileExtType2CodeFromFileExt(ext: String): String {
            return when (ext) {
                "jpeg", "jpg" -> "jpg"
                else -> ext
            }
        }

        fun copy(filePath: String, newFilePath: String) {
            mkdir(Paths.get(newFilePath).parent.toString())

            Files.copy(
                Path.of(filePath),
                Path.of(newFilePath),
                StandardCopyOption.REPLACE_EXISTING
            )
        }

        fun mkdir(dirPath: String) {
            val path = Path.of(dirPath)

            if (Files.exists(path)) return

            Files.createDirectories(path)
        }

        fun getContentType(fileExt: String): String {
            return MIME_TYPE_MAP.entries
                .find { it.value == fileExt }
                ?.key ?: "application/octet-stream"
        }

        /**
         * 이미지 파일의 메타데이터(width, height) 추출
         */
        fun getMetadata(filePath: String): Map<String, Any> {
            val ext = getFileExt(filePath)
            val fileExtTypeCode = getFileExtTypeCodeFromFileExt(ext)

            return if (fileExtTypeCode == "img") getImgMetadata(filePath) else emptyMap()
        }

        private fun getImgMetadata(filePath: String): Map<String, Any> {
            val metadata = LinkedHashMap<String, Any>()

            try {
                ImageIO.createImageInputStream(File(filePath)).use { input ->
                    val readers = ImageIO.getImageReaders(input)

                    if (!readers.hasNext()) {
                        throw IOException("지원되지 않는 이미지 형식: $filePath")
                    }

                    val reader = readers.next()
                    reader.input = input

                    val width = reader.getWidth(0)
                    val height = reader.getHeight(0)

                    metadata["width"] = width
                    metadata["height"] = height

                    reader.dispose()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

            return metadata
        }

        /**
         * 썸네일 생성
         * @param srcFilePath 원본 이미지 경로
         * @param destFilePath 썸네일 저장 경로
         * @param maxWidth 최대 너비
         * @param maxHeight 최대 높이
         * @return 썸네일 생성 성공 여부
         */
        fun makeThumbnail(
            srcFilePath: String,
            destFilePath: String,
            maxWidth: Int,
            maxHeight: Int = maxWidth
        ): Boolean {
            return try {
                val srcFile = File(srcFilePath)
                val originalImage = ImageIO.read(srcFile) ?: return false

                val originalWidth = originalImage.width
                val originalHeight = originalImage.height

                // 원본이 이미 작으면 그대로 복사
                if (originalWidth <= maxWidth && originalHeight <= maxHeight) {
                    copy(srcFilePath, destFilePath)
                    return true
                }

                // 비율 유지하며 크기 계산
                val widthRatio = maxWidth.toDouble() / originalWidth
                val heightRatio = maxHeight.toDouble() / originalHeight
                val ratio = minOf(widthRatio, heightRatio)

                val newWidth = (originalWidth * ratio).toInt()
                val newHeight = (originalHeight * ratio).toInt()

                // 리사이즈
                val resizedImage = BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_RGB)
                val g2d = resizedImage.createGraphics()

                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.drawImage(originalImage, 0, 0, newWidth, newHeight, null)
                g2d.dispose()

                // 저장
                val destFile = File(destFilePath)
                mkdir(destFile.parent)

                val formatName = getFileExt(destFilePath).lowercase().let {
                    if (it == "jpg") "jpeg" else it
                }

                ImageIO.write(resizedImage, formatName, destFile)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

        fun resizeImage(
            srcFilePath: String,
            destFilePath: String,
            width: Int,
            height: Int
        ): Boolean {
            return makeThumbnail(srcFilePath, destFilePath, width, height)
        }
    }
}
