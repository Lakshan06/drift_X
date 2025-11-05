package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.ai.AIAnalysisEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

/**
 * ViewModel for AI Assistant Chat feature
 */
class AIAssistantViewModel(
    private val aiEngine: AIAnalysisEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    // Conversation history for context-aware responses
    private val conversationHistory = mutableListOf<String>()

    init {
        checkAIAvailability()
    }

    private fun checkAIAvailability() {
        viewModelScope.launch {
            val isAvailable = aiEngine.isAvailable()
            _uiState.value = _uiState.value.copy(isAIAvailable = isAvailable)

            if (isAvailable) {
                // Add welcome message
                addMessage(
                    ChatMessage(
                        id = "welcome",
                        content = """👋 Hello! I'm your AI expert for drift detection and model monitoring.

I can help you with:
• Understanding drift detection results
• Explaining patches and recommendations
• Troubleshooting issues
• Best practices for monitoring
• Learning about drift concepts

Ask me anything about model drift!""",
                        isUser = false,
                        timestamp = Instant.now()
                    )
                )
            } else {
                addMessage(
                    ChatMessage(
                        id = "unavailable",
                        content = """⚠️ AI Assistant is currently initializing.

**First Time Setup:**
When you send your first message, I'll automatically download a lightweight AI model (119-374 MB).

**Models Available:**
• SmolLM2 360M (119 MB) - Fast responses
• Qwen 2.5 0.5B (374 MB) - Better quality

WiFi connection recommended for download.""",
                        isUser = false,
                        timestamp = Instant.now()
                    )
                )
            }
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val trimmedContent = content.trim()

        // Add user message
        val userMessage = ChatMessage(
            id = "user_${System.currentTimeMillis()}",
            content = trimmedContent,
            isUser = true,
            timestamp = Instant.now()
        )
        addMessage(userMessage)

        // Add to conversation history for context
        conversationHistory.add("User: $trimmedContent")

        // Set loading state
        _uiState.value = _uiState.value.copy(isLoading = true)

        // Generate AI response with streaming
        viewModelScope.launch {
            try {
                val assistantMessageId = "assistant_${System.currentTimeMillis()}"
                var fullResponse = ""

                // Add placeholder for assistant message
                addMessage(
                    ChatMessage(
                        id = assistantMessageId,
                        content = "",
                        isUser = false,
                        timestamp = Instant.now()
                    )
                )

                // Collect streaming response with intelligent prompting
                aiEngine.answerQuestionStream(trimmedContent).collect { token ->
                    fullResponse += token
                    updateMessageContent(assistantMessageId, fullResponse)
                }

                // Add AI response to conversation history
                conversationHistory.add("AI: $fullResponse")

                // Keep only last 10 exchanges for context (20 messages total)
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                }

                _uiState.value = _uiState.value.copy(isLoading = false)
                Timber.d("AI response generated successfully: ${fullResponse.take(100)}...")
            } catch (e: Exception) {
                Timber.e(e, "Error generating AI response")

                // Provide helpful error message based on error type
                val errorMessage = when {
                    e.message?.contains("model") == true ->
                        "❌ Model not ready. Please wait for the AI model to download, then try again."

                    e.message?.contains("network") == true ->
                        "❌ Network error. Please check your connection and try again."

                    else ->
                        "❌ Sorry, I encountered an error. Please try rephrasing your question."
                }

                addMessage(
                    ChatMessage(
                        id = "error_${System.currentTimeMillis()}",
                        content = errorMessage,
                        isUser = false,
                        timestamp = Instant.now()
                    )
                )
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun clearChat() {
        conversationHistory.clear()
        _uiState.value = AIAssistantUiState(isAIAvailable = _uiState.value.isAIAvailable)
        checkAIAvailability() // Re-add welcome message
    }

    private fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }

    private fun updateMessageContent(messageId: String, newContent: String) {
        val updatedMessages = _uiState.value.messages.map { message ->
            if (message.id == messageId) {
                message.copy(content = newContent)
            } else {
                message
            }
        }
        _uiState.value = _uiState.value.copy(messages = updatedMessages)
    }

    override fun onCleared() {
        super.onCleared()
        conversationHistory.clear()
    }
}

/**
 * UI State for AI Assistant
 */
data class AIAssistantUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isAIAvailable: Boolean = false
)

/**
 * Chat message data class
 */
data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Instant
)
