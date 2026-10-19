package com.example.data.remote

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.random.Random

data class PollinationsModelItem(
    val id: String,
    val name: String,
    val description: String
)

class PollinationsService(private val context: Context) {

    val availableModels = listOf(
        PollinationsModelItem("flux", "FLUX Standard", "High detail general image generation"),
        PollinationsModelItem("flux-realism", "FLUX Realism", "Photorealistic rendering and cinematic lighting"),
        PollinationsModelItem("any-dark", "Any Dark", "Dark, moody, high-contrast artistic renders"),
        PollinationsModelItem("turbo", "Turbo Fast", "Ultra-fast generation model")
    )

    fun getDimensionsForAspect(aspect: String): Pair<Int, Int> {
        return when (aspect) {
            "16:9" -> Pair(1280, 720)
            "9:16" -> Pair(720, 1280)
            "4:3" -> Pair(1024, 768)
            "3:4" -> Pair(768, 1024)
            else -> Pair(1024, 1024) // 1:1
        }
    }

    suspend fun generateAndSaveImage(
        prompt: String,
        model: String = "flux",
        aspectRatio: String = "1:1",
        seed: Long = Random.nextLong(1, 9999999)
    ): Result<Pair<String, String>> = withContext(Dispatchers.IO) {
        try {
            val (width, height) = getDimensionsForAspect(aspectRatio)
            val encodedPrompt = URLEncoder.encode(prompt, "UTF-8")
            val imageUrlString = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&model=$model&seed=$seed&nologo=true"

            val url = URL(imageUrlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 30000
            connection.readTimeout = 45000
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode == 200) {
                val inputStream = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(inputStream)
                inputStream.close()

                if (bitmap != null) {
                    val imagesDir = File(context.filesDir, "pollinations_images")
                    if (!imagesDir.exists()) imagesDir.mkdirs()

                    val fileName = "pollination_${System.currentTimeMillis()}_$seed.jpg"
                    val localFile = File(imagesDir, fileName)

                    val outputStream = FileOutputStream(localFile)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
                    outputStream.flush()
                    outputStream.close()

                    Result.success(Pair(localFile.absolutePath, imageUrlString))
                } else {
                    Result.failure(Exception("Failed to decode generated image bitmap."))
                }
            } else {
                Result.failure(Exception("Pollinations API returned error HTTP ${connection.responseCode}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception(e.localizedMessage ?: "Image generation failed. Check connection."))
        }
    }
}
