package tracker.io

import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.IOException

class DiscordUploader(private val webhookUrl: String) {

    private val client = OkHttpClient()

    fun uploadCsv(file: File, username: String = "MouseTracker", message: String = "New mouse data") {
        if (!file.exists()) {
            println("File not found: ${file.absolutePath}")
            return
        }

        val payloadJson = """
            {
              "content": "$message",
              "username": "$username"
            }
        """.trimIndent()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("payload_json", null, payloadJson.toRequestBody("application/json".toMediaType()))
            .addFormDataPart(
                "file",
                file.name,
                file.asRequestBody("text/csv".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(webhookUrl)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                println("Upload failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                println("Upload status: ${response.code} ${response.message}")
                response.close()
            }
        })
    }
}
