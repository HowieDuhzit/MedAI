package com.example.medai.voice

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

class OllamaApiClient(baseUrl: String = "http://10.0.2.2:11434/") {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val service = retrofit.create(OllamaService::class.java)

    fun chat(model: String, message: String, stream: Boolean = false): Flow<String> = flow {
        val request = ChatRequest(
            model = model,
            messages = listOf(Message(role = "user", content = message)),
            stream = stream
        )

        val response = service.chat(request)
        if (response.isSuccessful) {
            response.body()?.let { chatResponse ->
                emit(chatResponse.message.content)
            }
        } else {
            throw Exception("API error: ${response.code()} - ${response.message()}")
        }
    }

    fun streamingChat(model: String, message: String): Flow<String> = flow {
        val request = ChatRequest(
            model = model,
            messages = listOf(Message(role = "user", content = message)),
            stream = true
        )

        val json = """
            {
                "model": "$model",
                "messages": [{"role": "user", "content": "$message"}],
                "stream": true
            }
        """.trimIndent()

        val requestBody = json.toRequestBody("application/json".toMediaType())
        val req = Request.Builder()
            .url("${retrofit.baseUrl()}api/chat")
            .post(requestBody)
            .build()

        client.newCall(req).execute().use { response ->
            if (!response.isSuccessful) {
                throw Exception("API error: ${response.code}")
            }

            response.body?.let { body ->
                val buffer = StringBuilder()
                body.byteStream().bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        buffer.append(line)
                        if (line.contains("\"done\":true")) {
                            val jsonStr = buffer.toString()
                            // Extract content from last chunk
                            val contentMatch = Regex("\"content\":\"([^\"]+)\"").findAll(jsonStr)
                            contentMatch.forEach { match ->
                                emit(match.groupValues[1].replace("\\n", "\n"))
                            }
                            buffer.clear()
                        }
                    }
                }
            }
        }
    }
}

interface OllamaService {
    @POST("api/chat")
    suspend fun chat(@Body request: ChatRequest): Response<ChatResponse>
}

data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val stream: Boolean = false
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val model: String,
    val message: Message,
    val done: Boolean
)
