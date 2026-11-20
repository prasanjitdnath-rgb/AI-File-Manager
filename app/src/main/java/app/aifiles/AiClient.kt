package app.aifiles

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import java.util.UUID

enum class AiProvider(val label: String) {
    GEMINI("Gemini"),
    OPENAI("OpenAI-compatible"),
}

enum class ChatRole { USER, ASSISTANT, ERROR }

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val text: String,
    val timeMs: Long = System.currentTimeMillis(),
) {
    val isUser: Boolean get() = role == ChatRole.USER
    val isError: Boolean get() = role == ChatRole.ERROR
}

sealed interface AiResult {
    data class Success(val text: String) : AiResult
    data class Failure(val message: String) : AiResult
}

private class ApiException(message: String) : IOException(message)

/** Small, dependency-free client for Gemini and OpenAI-compatible chat endpoints. */
object AiClient {
    suspend fun send(config: AiConfig, history: List<ChatMessage>): AiResult = withContext(Dispatchers.IO) {
        if (config.apiKey.isBlank()) {
            return@withContext AiResult.Failure("Add your API key in Settings first.")
        }
        if (history.none { it.isUser }) {
            return@withContext AiResult.Failure("Send a message to start the conversation.")
        }

        runCatching {
            when (config.provider) {
                AiProvider.GEMINI -> sendGemini(config, history)
                AiProvider.OPENAI -> sendOpenAi(config, history)
            }
        }.fold(
            onSuccess = { AiResult.Success(it) },
            onFailure = { AiResult.Failure(it.userMessage()) },
        )
    }

    private fun sendGemini(config: AiConfig, history: List<ChatMessage>): String {
        val model = config.model.trim().removePrefix("models/").ifBlank { "gemini-2.5-flash" }
        val contents = JSONArray()
        history.filterNot { it.isError }.forEach { message ->
            contents.put(
                JSONObject()
                    .put("role", if (message.isUser) "user" else "model")
                    .put("parts", JSONArray().put(JSONObject().put("text", message.text))),
            )
        }

        val payload = JSONObject()
            .put(
                "systemInstruction",
                JSONObject().put("parts", JSONArray().put(JSONObject().put("text", config.systemPrompt))),
            )
            .put("contents", contents)
            .put("generationConfig", JSONObject().put("temperature", config.temperature))

        val body = post(
            url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent",
            headerName = "x-goog-api-key",
            headerValue = config.apiKey,
            payload = payload,
        )

        val root = JSONObject(body)
        root.optJSONObject("promptFeedback")?.optString("blockReason").takeUnless { it.isNullOrBlank() }?.let {
            throw ApiException("The request was blocked: $it")
        }
        val parts = root.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: throw ApiException("The model returned an empty response.")

        val text = buildString {
            for (i in 0 until parts.length()) {
                append(parts.optJSONObject(i)?.optString("text", "").orEmpty())
            }
        }.trim()
        return text.ifBlank { throw ApiException("The model returned an empty response.") }
    }

    private fun sendOpenAi(config: AiConfig, history: List<ChatMessage>): String {
        val base = config.baseUrl.trim().trimEnd('/')
        if (base.isBlank()) throw ApiException("Add a base URL in Settings.")

        val messages = JSONArray()
        messages.put(
            JSONObject()
                .put("role", "system")
                .put("content", config.systemPrompt),
        )
        history.filterNot { it.isError }.forEach { message ->
            messages.put(
                JSONObject()
                    .put("role", if (message.isUser) "user" else "assistant")
                    .put("content", message.text),
            )
        }

        val payload = JSONObject()
            .put("model", config.model.trim().ifBlank { "gpt-4o-mini" })
            .put("messages", messages)
            .put("temperature", config.temperature)

        val body = post(
            url = "$base/chat/completions",
            headerName = "Authorization",
            headerValue = "Bearer ${config.apiKey}",
            payload = payload,
        )

        val root = JSONObject(body)
        val text = root.optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("message")
            ?.optString("content", "")
            .orEmpty()
            .trim()
        return text.ifBlank { throw ApiException("The model returned an empty response.") }
    }

    private fun post(url: String, headerName: String, headerValue: String, payload: JSONObject): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.connectTimeout = 15_000
            connection.readTimeout = 60_000
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty(headerName, headerValue)
            connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }

            val code = connection.responseCode
            if (code !in 200..299) throw ApiException(errorMessage(code, connection))
            return connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    private fun errorMessage(code: Int, connection: HttpURLConnection): String {
        val raw = runCatching {
            (connection.errorStream ?: connection.inputStream).bufferedReader(Charsets.UTF_8).use { it.readText() }
        }.getOrDefault("").orEmpty()
        val detail = runCatching {
            JSONObject(raw).optJSONObject("error")?.optString("message").orEmpty()
        }.getOrDefault("").ifBlank { raw }.orEmpty().trim()

        return when (code) {
            401, 403 -> "API key was rejected (HTTP $code). Check the key in Settings."
            404 -> "Model or endpoint not found (HTTP 404). Check the model and base URL."
            429 -> "Rate limit reached. Wait a moment and try again."
            in 500..599 -> "The API had a server error (HTTP $code). Try again shortly."
            else -> "Request failed (HTTP $code). ${detail.take(240)}".trim()
        }
    }

    private fun Throwable.userMessage(): String = when (this) {
        is ApiException -> message.orEmpty().ifBlank { "The request failed." }
        is SocketTimeoutException -> "The request timed out. Check your connection and try again."
        is UnknownHostException -> "No internet connection. Check your connection and try again."
        is IOException -> "Could not reach the API: ${message.orEmpty()}".trim()
        else -> message.orEmpty().ifBlank { "Something went wrong." }
    }
}
