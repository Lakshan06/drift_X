package com.driftdetector.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.driftdetector.app.core.ai.AIAnalysisEngine
import com.driftdetector.app.data.repository.DriftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.Instant

/**
 * ViewModel for PatchBot Chat feature with contextual data access
 */
class AIAssistantViewModel(
    private val aiEngine: AIAnalysisEngine,
    private val driftRepository: DriftRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AIAssistantUiState())
    val uiState: StateFlow<AIAssistantUiState> = _uiState.asStateFlow()

    // Conversation history for context-aware responses
    private val conversationHistory = mutableListOf<String>()

    // Cache for contextual data
    private var currentModelId: String? = null
    private var cachedModelContext: String = ""

    init {
        Timber.d("🔍 AIAssistantViewModel init - PatchBot with contextual data access")
        checkAIAvailability()
        loadContextualData()
    }

    private fun loadContextualData() {
        viewModelScope.launch {
            try {
                // Load active model context
                val models = driftRepository.getActiveModels().firstOrNull()
                if (!models.isNullOrEmpty()) {
                    currentModelId = models.first().id
                    val model = models.first()
                    val drifts = driftRepository.getRecentDrifts(10).firstOrNull() ?: emptyList()
                    val patches = currentModelId?.let {
                        driftRepository.getPatchesByModel(it).firstOrNull()
                    } ?: emptyList()

                    cachedModelContext = buildContextString(model, drifts, patches)
                    Timber.d("📊 Loaded contextual data: ${drifts.size} drifts, ${patches.size} patches")
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load contextual data")
            }
        }
    }

    private fun buildContextString(
        model: com.driftdetector.app.domain.model.MLModel,
        drifts: List<com.driftdetector.app.domain.model.DriftResult>,
        patches: List<com.driftdetector.app.domain.model.Patch>
    ): String {
        val latestDrift = drifts.firstOrNull()
        val appliedPatches =
            patches.filter { it.status == com.driftdetector.app.domain.model.PatchStatus.APPLIED }

        return """
        Current Model: ${model.name} (v${model.version})
        Latest Drift Score: ${latestDrift?.driftScore ?: "N/A"}
        Total Drift Events: ${drifts.size}
        Applied Patches: ${appliedPatches.size}
        Total Patches Available: ${patches.size}
        """.trimIndent()
    }

    private fun checkAIAvailability() {
        viewModelScope.launch {
            Timber.d("🔍 PatchBot is always available using instant fallback responses")

            // Always mark as available since we use fallback responses
            _uiState.value = _uiState.value.copy(isAIAvailable = true)

            // Add welcome message with contextual info
            val welcomeMessage = buildWelcomeMessage()
            addMessage(
                ChatMessage(
                    id = "welcome",
                    content = welcomeMessage,
                    isUser = false,
                    timestamp = Instant.now()
                )
            )
        }
    }

    private suspend fun buildWelcomeMessage(): String {
        // Try to get real-time status
        val models = driftRepository.getActiveModels().firstOrNull()
        val recentDrifts = driftRepository.getRecentDrifts(5).firstOrNull()

        val statusInfo = if (!models.isNullOrEmpty() && !recentDrifts.isNullOrEmpty()) {
            val latestDrift = recentDrifts.first()
            val driftStatus = when {
                latestDrift.driftScore > 0.5 -> "⚠️ High drift detected"
                latestDrift.driftScore > 0.2 -> "📊 Moderate drift"
                else -> "✅ Low drift"
            }
            "\n\n**📊 Quick Status:**\n• Model: ${models.first().name}\n• $driftStatus (Score: ${
                String.format(
                    "%.3f",
                    latestDrift.driftScore
                )
            })\n• Recent events: ${recentDrifts.size}"
        } else {
            ""
        }

        return """👋 **Welcome to PatchBot!**

I'm your expert guide for model drift detection and monitoring. I can answer any questions instantly!$statusInfo

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
• "Show my current status"
• "PSI vs KS test"
• "How do I apply a patch?"
• "Best practices for monitoring"
• "When should I retrain?"

**Go ahead and ask me anything!** I'm here to help. 😊"""
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

        // Generate AI response with contextual data
        viewModelScope.launch {
            try {
                val assistantMessageId = "assistant_${System.currentTimeMillis()}"

                Timber.d("📥 Getting AI response with contextual data...")

                // Get response with context
                val response = aiEngine.answerQuestion(trimmedContent, cachedModelContext)

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

        // Reload contextual data and welcome message
        viewModelScope.launch {
            loadContextualData()
            checkAIAvailability()
        }
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
 * UI State for PatchBot
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
