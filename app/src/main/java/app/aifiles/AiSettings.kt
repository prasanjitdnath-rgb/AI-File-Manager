package app.aifiles

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

data class AiConfig(
    val name: String = "Nova",
    val provider: AiProvider = AiProvider.GEMINI,
    val apiKey: String = "",
    val model: String = "gemini-2.5-flash",
    val baseUrl: String = "https://api.openai.com/v1",
    val systemPrompt: String =
        "You are a helpful assistant inside a file manager. Answer clearly and concisely. " +
            "When useful, suggest how the user can find or organize files with the app's AI tools.",
    val temperature: Float = 0.7f,
    val cloudOcrAuto: Boolean = false,
)

/** Local-only configuration and chat history. Uses the same private prefs file as Cloud OCR. */
object AiSettings {
    private const val PREFS = "cfg"
    private const val K_NAME = "ai_name"
    private const val K_PROVIDER = "ai_provider"
    private const val K_API_KEY = "ai_api_key"
    private const val K_MODEL = "ai_model"
    private const val K_BASE_URL = "ai_base_url"
    private const val K_PROMPT = "ai_prompt"
    private const val K_TEMPERATURE = "ai_temperature"
    private const val K_CLOUD_AUTO = "auto"
    private const val K_CHAT = "ai_chat"
    private const val MAX_HISTORY = 100

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        if (!::prefs.isInitialized) {
            prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        }
    }

    fun load(): AiConfig {
        if (!::prefs.isInitialized) return AiConfig()
        val provider = runCatching {
            AiProvider.valueOf(prefs.getString(K_PROVIDER, AiProvider.GEMINI.name).orEmpty())
        }.getOrDefault(AiProvider.GEMINI)

        return AiConfig(
            name = prefs.getString(K_NAME, null)?.ifBlank { "Nova" } ?: "Nova",
            provider = provider,
            // Fall back to the legacy keys written by the Cloud OCR card.
            apiKey = prefs.getString(K_API_KEY, null) ?: prefs.getString("key", "").orEmpty(),
            model = prefs.getString(K_MODEL, null)
                ?: prefs.getString("model", "gemini-2.5-flash").orEmpty().ifBlank { "gemini-2.5-flash" },
            baseUrl = prefs.getString(K_BASE_URL, null)?.ifBlank { "https://api.openai.com/v1" }
                ?: "https://api.openai.com/v1",
            systemPrompt = prefs.getString(K_PROMPT, null)?.ifBlank { AiConfig().systemPrompt }
                ?: AiConfig().systemPrompt,
            temperature = runCatching { prefs.getFloat(K_TEMPERATURE, 0.7f) }.getOrDefault(0.7f)
                .coerceIn(0f, 2f),
            cloudOcrAuto = prefs.getBoolean(K_CLOUD_AUTO, false),
        )
    }

    fun save(config: AiConfig) {
        if (!::prefs.isInitialized) return
        prefs.edit()
            .putString(K_NAME, config.name.trim().ifBlank { "Nova" })
            .putString(K_PROVIDER, config.provider.name)
            .putString(K_API_KEY, config.apiKey.trim())
            .putString(K_MODEL, config.model.trim())
            .putString(K_BASE_URL, config.baseUrl.trim())
            .putString(K_PROMPT, config.systemPrompt.trim())
            .putFloat(K_TEMPERATURE, config.temperature.coerceIn(0f, 2f))
            .putBoolean(K_CLOUD_AUTO, config.cloudOcrAuto)
            // Keep the legacy OCR keys in sync for Gemini users.
            .putString("key", if (config.provider == AiProvider.GEMINI) config.apiKey.trim() else "")
            .putString("model", config.model.trim())
            .apply()
    }

    fun loadChat(): List<ChatMessage> {
        if (!::prefs.isInitialized) return emptyList()
        val raw = prefs.getString(K_CHAT, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val obj = array.optJSONObject(i) ?: return@mapNotNull null
                val text = obj.optString("text", "")
                if (text.isBlank()) return@mapNotNull null
                ChatMessage(
                    id = obj.optString("id").ifBlank { UUID.randomUUID().toString() },
                    role = runCatching { ChatRole.valueOf(obj.optString("role")) }
                        .getOrDefault(ChatRole.ASSISTANT),
                    text = text,
                    timeMs = obj.optLong("time", System.currentTimeMillis()),
                )
            }.takeLast(MAX_HISTORY)
        }.getOrDefault(emptyList())
    }

    fun saveChat(messages: List<ChatMessage>) {
        if (!::prefs.isInitialized) return
        val array = JSONArray()
        messages.takeLast(MAX_HISTORY).forEach { message ->
            array.put(
                JSONObject()
                    .put("id", message.id)
                    .put("role", message.role.name)
                    .put("text", message.text)
                    .put("time", message.timeMs),
            )
        }
        prefs.edit().putString(K_CHAT, array.toString()).apply()
    }
}
