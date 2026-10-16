package com.example.priv.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

private const val TAG = "AiAssistantViewModel"

sealed interface AiAssistantUiState {
    data object Idle : AiAssistantUiState
    data object Loading : AiAssistantUiState
    data class Streaming(val text: String) : AiAssistantUiState
    data class Success(val text: String) : AiAssistantUiState
    data class Error(val message: String) : AiAssistantUiState
}

data class SuggestedInterviewQuestion(
    val category: String,
    val question: String,
    val contextReason: String
)

sealed interface InterviewQuestionsUiState {
    data object Idle : InterviewQuestionsUiState
    data object Loading : InterviewQuestionsUiState
    data class Success(val questions: List<SuggestedInterviewQuestion>) : InterviewQuestionsUiState
    data class Error(val message: String) : InterviewQuestionsUiState
}

class AiAssistantViewModel(application: Application) : AndroidViewModel(application) {

    // Lazy initialization of standard generative model to avoid startup overhead
    private val geminiFlashModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash"
        )
    }

    // Lazy initialization of structured model for interview prompts
    private val interviewSchema = Schema.obj(
        properties = mapOf(
            "questions" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "category" to Schema.string(description = "Tema da pergunta, ex: Infância, Lições de Vida, Amor"),
                        "question" to Schema.string(description = "Pergunta profunda para fazer ao ente querido gravar em áudio"),
                        "contextReason" to Schema.string(description = "Por que essa memória tem alto valor de preservação")
                    )
                ),
                description = "Lista de perguntas sugeridas para gravação de áudio e preservação de histórias"
            )
        )
    )

    private val structuredInterviewModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = interviewSchema
            }
        )
    }

    private val _uiState = MutableStateFlow<AiAssistantUiState>(AiAssistantUiState.Idle)
    val uiState: StateFlow<AiAssistantUiState> = _uiState.asStateFlow()

    private val _interviewState = MutableStateFlow<InterviewQuestionsUiState>(InterviewQuestionsUiState.Idle)
    val interviewState: StateFlow<InterviewQuestionsUiState> = _interviewState.asStateFlow()

    /**
     * Gera uma reflexão narrativa e poética com streaming para minimizar latência (TTFT).
     */
    fun generateMemoryReflectionStream(memoryTitle: String, memoryContent: String, personName: String?) {
        viewModelScope.launch {
            _uiState.value = AiAssistantUiState.Loading
            val prompt = buildString {
                appendLine("Você é o Guardião de Memórias do GuardDay, um assistente empático e sensível dedicado a eternizar momentos humanos, vozes e laços de afeto.")
                if (!personName.isNullOrBlank()) {
                    appendLine("Pessoa homenageada / autor(a): $personName")
                }
                appendLine("Título da Memória: $memoryTitle")
                appendLine("Conteúdo / Relato gravado: $memoryContent")
                appendLine()
                appendLine("Escreva uma reflexão calorosa, de 2 a 3 parágrafos em português do Brasil, destacando a preciosidade dessa lembrança, o significado emocional que ela carrega para o futuro e uma mensagem de carinho e preservação.")
            }

            val accumulatedText = StringBuilder()
            try {
                // Realistic coroutine timeout window of 60 seconds
                withTimeout(60_000L) {
                    geminiFlashModel.generateContentStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _uiState.value = AiAssistantUiState.Streaming(accumulatedText.toString())
                    }
                }

                if (accumulatedText.isNotBlank()) {
                    _uiState.value = AiAssistantUiState.Success(accumulatedText.toString())
                } else {
                    _uiState.value = AiAssistantUiState.Error("O modelo retornou uma reflexão vazia.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Geração de reflexão excedeu o tempo limite", e)
                _uiState.value = AiAssistantUiState.Error("Tempo limite de geração esgotado (60 segundos). Tente novamente.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Falha na geração com Gemini API", e)
                _uiState.value = AiAssistantUiState.Error(e.localizedMessage ?: "Erro ao gerar reflexão com o Gemini.")
            }
        }
    }

    /**
     * Pergunta ao Cofre com Gemini API sobre o conjunto de memórias do usuário.
     */
    fun askMemoryVault(question: String, memorySummaries: String) {
        viewModelScope.launch {
            _uiState.value = AiAssistantUiState.Loading
            val prompt = buildString {
                appendLine("Você é o Guardião do Cofre de Histórias do GuardDay. Você ajuda a família a reviver histórias e encontrar lembranças preciosas.")
                appendLine("Contexto do Cofre de Memórias disponíveis:")
                appendLine(memorySummaries.ifBlank { "Nenhuma memória detalhada encontrada no cofre até o momento." })
                appendLine()
                appendLine("Pergunta do usuário: $question")
                appendLine()
                appendLine("Responda de forma clara, afetuosa e inspiradora em português do Brasil, conectando com as memórias acima sempre que possível.")
            }

            val accumulatedText = StringBuilder()
            try {
                withTimeout(60_000L) {
                    geminiFlashModel.generateContentStream(prompt).collect { chunk ->
                        val chunkText = chunk.text.orEmpty()
                        accumulatedText.append(chunkText)
                        _uiState.value = AiAssistantUiState.Streaming(accumulatedText.toString())
                    }
                }

                if (accumulatedText.isNotBlank()) {
                    _uiState.value = AiAssistantUiState.Success(accumulatedText.toString())
                } else {
                    _uiState.value = AiAssistantUiState.Error("Resposta vazia retornada pelo modelo.")
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Consulta ao cofre excedeu o tempo", e)
                _uiState.value = AiAssistantUiState.Error("A consulta com o Gemini excedeu 60 segundos.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erro na consulta ao cofre com Gemini", e)
                _uiState.value = AiAssistantUiState.Error(e.localizedMessage ?: "Falha ao consultar memórias com Gemini.")
            }
        }
    }

    /**
     * Gera perguntas guiadas de entrevista para gravar vozes de familiares antes que o tempo passe.
     */
    fun generateInterviewQuestions(personName: String, relation: String) {
        viewModelScope.launch {
            _interviewState.value = InterviewQuestionsUiState.Loading
            val prompt = "Gere 4 perguntas essenciais e profundas para gravar em áudio e preservar a história de vida de $personName, cuja relação afetiva é: $relation. As perguntas devem resgatar histórias marcantes de infância, sabedoria, superação e amor."

            try {
                val response = withTimeout(60_000L) {
                    structuredInterviewModel.generateContent(prompt)
                }

                val jsonText = response.text ?: throw IllegalStateException("Modelo retornou texto vazio.")
                val rootJson = org.json.JSONObject(jsonText)
                val array = rootJson.getJSONArray("questions")
                val questionsList = mutableListOf<SuggestedInterviewQuestion>()

                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    questionsList.add(
                        SuggestedInterviewQuestion(
                            category = item.optString("category", "Memória"),
                            question = item.optString("question", ""),
                            contextReason = item.optString("contextReason", "")
                        )
                    )
                }

                _interviewState.value = InterviewQuestionsUiState.Success(questionsList)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "Geração de perguntas de entrevista expirou", e)
                _interviewState.value = InterviewQuestionsUiState.Error("Tempo limite para sugerir perguntas esgotado.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao gerar perguntas com Gemini", e)
                _interviewState.value = InterviewQuestionsUiState.Error(e.localizedMessage ?: "Erro na geração com Gemini API.")
            }
        }
    }

    fun resetState() {
        _uiState.value = AiAssistantUiState.Idle
    }
}
