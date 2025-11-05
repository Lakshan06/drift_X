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
        Timber.d("🔍 AIAssistantViewModel init - AI always available (fallback mode)")
        checkAIAvailability()
    }

    private fun checkAIAvailability() {
        viewModelScope.launch {
            Timber.d("🔍 AI is always available using instant fallback responses")

            // Always mark as available since we use fallback responses
            _uiState.value = _uiState.value.copy(isAIAvailable = true)

            // Add welcome message
            addMessage(
                ChatMessage(
                    id = "welcome",
                    content = """👋 **Welcome to AI Assistant!**

I'm your expert guide for model drift detection and monitoring. I can answer any questions instantly!

**I can help you with:**

📊 **Understanding Drift**
• What is model drift? (concept, covariate, prior)
• PSI vs KS statistical tests
• Feature-level drift analysis

🔧 **Managing Patches**
• How to apply and rollback patches
• Understanding safety scores
• Patch types and their effects

📈 **Best Practices**
• Setting up drift monitoring
• When to retrain vs patch
• Alert thresholds and strategies

🔍 **Troubleshooting**
• Interpreting drift scores
• Investigating high drift
• Validating model performance

**Try asking:**
• "What is drift?"
• "PSI vs KS test"
• "How do I apply a patch?"
• "Best practices for monitoring"
• "When should I retrain?"

**Go ahead and ask me anything!** I'm here to help. 😊""",
                    isUser = false,
                    timestamp = Instant.now()
                )
            )
        }
    }

    fun sendMessage(content: String) {
        if (content.isBlank()) return

        val trimmedContent = content.trim()

        Timber.d("📤 Sending message: $trimmedContent")

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

        // Generate AI response (instant fallback)
        viewModelScope.launch {
            try {
                val assistantMessageId = "assistant_${System.currentTimeMillis()}"

                Timber.d("📥 Getting instant AI response...")

                // Get instant response from fallback
                val response = aiEngine.answerQuestion(trimmedContent)

                // Add complete message
                addMessage(
                    ChatMessage(
                        id = assistantMessageId,
                        content = response,
                        isUser = false,
                        timestamp = Instant.now()
                    )
                )

                Timber.d("✅ Response delivered: ${response.length} characters")

                // Add AI response to conversation history
                conversationHistory.add("AI: $response")

                // Keep only last 10 exchanges for context (20 messages total)
                if (conversationHistory.size > 20) {
                    conversationHistory.removeAt(0)
                }

                _uiState.value = _uiState.value.copy(isLoading = false)

            } catch (e: Exception) {
                Timber.e(e, "❌ Error generating AI response")

                val errorMessage = """❌ Oops! Something went wrong.

**Don't worry** - This is unusual. Let me try to help anyway!

**Your question was:** "$trimmedContent"

**Common topics I can help with:**
• Model drift concepts
• PSI and KS tests
• Applying patches
• Monitoring strategies

**Try:**
• Rephrase your question
• Ask about a specific topic
• Be more specific

**Error details:** ${e.message ?: "Unknown error"}"""

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
        Timber.d("🗑️ Clearing chat history")
        conversationHistory.clear()
        _uiState.value = AIAssistantUiState(isAIAvailable = true) // Always available
        checkAIAvailability() // Re-add welcome message
    }

    private fun addMessage(message: ChatMessage) {
        val currentMessages = _uiState.value.messages.toMutableList()
        currentMessages.add(message)
        _uiState.value = _uiState.value.copy(messages = currentMessages)
    }

    override fun onCleared() {
        super.onCleared()
        Timber.d("🧹 AIAssistantViewModel cleared")
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
