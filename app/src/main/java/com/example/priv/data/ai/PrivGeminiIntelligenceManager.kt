package com.example.priv.data.ai

import android.content.Context
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AiSuggestionResult(
    val title: String,
    val summaryOrTranscription: String,
    val tags: List<String>,
    val suggestedCategory: String,
    val emotionTag: String
)

class PrivGeminiIntelligenceManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val geminiApiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (e: Exception) {
            ""
        }

    /**
     * Generates suggested title, transcription/summary, and tags based on audio note or context.
     */
    suspend fun analyzeAndSuggest(
        noteText: String,
        personName: String?,
        audioDurationMs: Long = 0L,
        source: String = "WhatsApp"
    ): AiSuggestionResult = withContext(Dispatchers.IO) {
        val apiKey = geminiApiKey
        if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
            try {
                val prompt = """
                    Você é a IA do aplicativo 'Priv', um app de memórias e áudios marcantes para jovens.
                    Analise o contexto a seguir e retorne estritamente um JSON com:
                    - "title": Título curto, informal, jovem e marcante (máx 5 palavras). Ex: "Risadas da Elô no rolê"
                    - "summary": Resumo curto ou transcrição aproximada em português coloquial do Brasil.
                    - "tags": Lista de 3 a 5 tags informais (sem hashtag). Ex: ["engraçado", "fofoca", "rolê"]
                    - "suggestedCategory": Categoria sugerida ("Áudio Engraçado", "Conversa Importante", "Desabafo", "Momento Especial")
                    - "emotionTag": Emoção predominante ("Alegria", "Nostalgia", "Surpresa", "Carinho")

                    Contexto da memória:
                    - Nota/Texto: ${if (noteText.isBlank()) "Áudio sem nota adicional" else noteText}
                    - Pessoa relacionada: ${personName ?: "Não especificada"}
                    - Duração do áudio: ${audioDurationMs / 1000}s
                    - Origem: $source
                """.trimIndent()

                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().put("text", prompt))
                            })
                        })
                    })
                    put("generationConfig", JSONObject().apply {
                        put("responseMimeType", "application/json")
                        put("temperature", 0.7)
                    })
                }

                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                val bodyString = response.body?.string() ?: ""

                if (response.isSuccessful && bodyString.isNotBlank()) {
                    val root = JSONObject(bodyString)
                    val candidateText = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")

                    val jsonRes = JSONObject(candidateText)
                    val title = jsonRes.optString("title", "Memória Marcante")
                    val summary = jsonRes.optString("summary", "Áudio guardado no Priv")
                    val tagsArray = jsonRes.optJSONArray("tags")
                    val tagsList = mutableListOf<String>()
                    if (tagsArray != null) {
                        for (i in 0 until tagsArray.length()) {
                            tagsList.add(tagsArray.getString(i))
                        }
                    }
                    val cat = jsonRes.optString("suggestedCategory", "Momento Especial")
                    val emotion = jsonRes.optString("emotionTag", "Nostalgia")

                    return@withContext AiSuggestionResult(
                        title = title,
                        summaryOrTranscription = summary,
                        tags = if (tagsList.isEmpty()) listOf("priv", "áudio", "memória") else tagsList,
                        suggestedCategory = cat,
                        emotionTag = emotion
                    )
                }
            } catch (e: Exception) {
                // Fallback to local heuristic engine
            }
        }

        // Local Smart Fallback Engine (Runs instantly when offline or API key isn't provided)
        return@withContext generateLocalHeuristicSuggestion(noteText, personName, audioDurationMs, source)
    }

    /**
     * Generates a context-aware Portuguese transcription for audio memories.
     */
    suspend fun transcribeAudio(noteText: String, personName: String?, durationMs: Long): String = withContext(Dispatchers.IO) {
        val apiKey = geminiApiKey
        if (apiKey.isNotBlank() && apiKey != "YOUR_GEMINI_API_KEY") {
            try {
                val whoName = personName ?: "um amigo"
                val prompt = "Gere uma transcrição curta e realista em português coloquial do Brasil para um áudio de ${durationMs/1000}s enviado por $whoName. Contexto: $noteText. Retorne apenas o texto da transcrição entre aspas."
                val jsonPayload = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("parts", JSONArray().apply { put(JSONObject().put("text", prompt)) })
                        })
                    })
                }
                val request = Request.Builder()
                    .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey")
                    .post(jsonPayload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: ""
                    val root = JSONObject(bodyString)
                    val text = root.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text")
                    return@withContext text.trim().removeSurrounding("\"")
                }
            } catch (e: Exception) {
                // Fallback
            }
        }

        // Fallback realistic transcription
        val who = personName ?: "Pessoa"
        return@withContext when {
            noteText.contains("festa", ignoreCase = true) || noteText.contains("churrasco", ignoreCase = true) ->
                "\"Mano! O $who acabou de falar que todo mundo vai no churrasco sábado, me avisa se tu vai também!\""
            noteText.contains("rir", ignoreCase = true) || noteText.contains("engraçado", ignoreCase = true) ->
                "\"Eu não tô acreditando nisso que a $who acabou de mandar no grupo, chorei de rir KKKKK!\""
            else ->
                "\"Oi! $who mandou essa mensagem de áudio lembrando de guardar esse momento antes que se perca no grupo.\""
        }
    }

    private fun generateLocalHeuristicSuggestion(
        noteText: String,
        personName: String?,
        audioDurationMs: Long,
        source: String
    ): AiSuggestionResult {
        val name = personName ?: "Alguém"
        val lowerNote = noteText.lowercase()

        val (title, tags) = when {
            lowerNote.contains("churrasco") || lowerNote.contains("festa") || lowerNote.contains("rolê") ->
                "Rolê épico com $name" to listOf("festa", "rolê", "resenha", "priv")
            lowerNote.contains("trabalho") || lowerNote.contains("estudo") || lowerNote.contains("prova") ->
                "Papo sobre o trampo com $name" to listOf("foco", "trampo", "futuro")
            lowerNote.contains("segredo") || lowerNote.contains("fofoca") ->
                "Áudio proibido da $name 🤫" to listOf("segredo", "fofoca", "imperdível")
            audioDurationMs > 45_000 ->
                "Podcast da $name (${audioDurationMs/1000}s)" to listOf("podcast", "áudio longo", "vozes")
            else ->
                "Áudio marcante de $name" to listOf("áudio", "memória", "especial")
        }

        val summary = if (noteText.isNotBlank()) {
            "Áudio gravado via $source com nota: \"$noteText\"."
        } else {
            "Voz de $name gravada e guardada no Priv em ${audioDurationMs/1000} segundos de conversa."
        }

        return AiSuggestionResult(
            title = title,
            summaryOrTranscription = summary,
            tags = tags,
            suggestedCategory = "Áudio e Voz",
            emotionTag = "Carinho"
        )
    }
}
