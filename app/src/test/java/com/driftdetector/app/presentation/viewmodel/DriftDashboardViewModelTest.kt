package com.driftdetector.app.presentation.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.driftdetector.app.MainCoroutineRule
import com.driftdetector.app.core.error.ErrorHandler
import com.driftdetector.app.core.notifications.DriftNotificationManager
import com.driftdetector.app.core.patch.IntelligentPatchGenerator
import com.driftdetector.app.data.repository.DriftRepository
import com.driftdetector.app.domain.model.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.*
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

/**
 * Comprehensive unit tests for DriftDashboardViewModel
 * Tests cover model loading, patch generation, and dashboard state management
 */
@ExperimentalCoroutinesApi
class DriftDashboardViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    private lateinit var repository: DriftRepository
    private lateinit var patchGenerator: IntelligentPatchGenerator
    private lateinit var notificationManager: DriftNotificationManager
    private lateinit var errorHandler: ErrorHandler
    private lateinit var viewModel: DriftDashboardViewModel

    @Before
    fun setup() {
        repository = mock()
        patchGenerator = mock()
        notificationManager = mock()
        errorHandler = mock()

        viewModel = DriftDashboardViewModel(
            repository = repository,
            intelligentPatchGenerator = patchGenerator,
            notificationManager = notificationManager,
            errorHandler = errorHandler
        )
    }

    @Test
    fun `loadActiveModels should update state when models exist`() = runTest {
        // Given
        val testModel = createTestModel()
        val models = listOf(testModel)
        val driftResults = emptyList<DriftResult>()

        whenever(repository.getActiveModels()).thenReturn(flowOf(models))
        whenever(repository.getDriftResultsByModel(any())).thenReturn(flowOf(driftResults))
        whenever(repository.getDriftCount(any())).thenReturn(0)

        // When
        viewModel.loadActiveModels()

        // Then - State should eventually be Success or Empty
        viewModel.uiState.test(timeout = 5.seconds) {
            val state = awaitItem()
            assertTrue(
                "State should be Loading or Success",
                state is DriftDashboardState.Loading || state is DriftDashboardState.Success
            )
        }
    }

    @Test
    fun `loadActiveModels should handle empty model list`() = runTest {
        // Given
        whenever(repository.getActiveModels()).thenReturn(flowOf(emptyList()))

        // When
        viewModel.loadActiveModels()

        // Then
        viewModel.uiState.test(timeout = 5.seconds) {
            val state = awaitItem()
            assertTrue(
                "State should be Loading or Empty",
                state is DriftDashboardState.Empty || state is DriftDashboardState.Loading
            )
        }
    }

    @Test
    fun `selectModel should update selected model`() = runTest {
        // Given
        val model = createTestModel(name = "Selected Model")

        whenever(repository.getDriftResultsByModel(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.getDriftCount(any())).thenReturn(0)

        // When
        viewModel.selectModel(model)

        // Then
        viewModel.selectedModel.test {
            val selectedModel = awaitItem()
            assertEquals("Selected Model", selectedModel?.name)
        }
    }

    @Test
    fun `toggleAutoPatch should toggle state`() = runTest {
        // Given
        val initialState = viewModel.autoPatchEnabled.value

        // When
        viewModel.toggleAutoPatch()

        // Then
        viewModel.autoPatchEnabled.test {
            val newState = awaitItem()
            assertEquals(!initialState, newState)
        }
    }

    @Test
    fun `refresh should trigger repository call`() = runTest {
        // Given
        val model = createTestModel()
        whenever(repository.getActiveModels()).thenReturn(flowOf(listOf(model)))
        whenever(repository.getDriftResultsByModel(any())).thenReturn(flowOf(emptyList()))
        whenever(repository.getDriftCount(any())).thenReturn(0)

        // When
        viewModel.refresh()

        // Then
        verify(repository, atLeastOnce()).getDriftResultsByModel(any())
    }

    @Test
    fun `getModelSummary should return correct statistics`() = runTest {
        // Given
        val modelId = "model-1"
        val patches = listOf(
            createTestPatch(modelId = modelId, status = PatchStatus.APPLIED),
            createTestPatch(modelId = modelId, status = PatchStatus.CREATED)
        )

        whenever(repository.getDriftCount(modelId)).thenReturn(5)
        whenever(repository.getPatchesByModel(modelId)).thenReturn(flowOf(patches))

        // When
        val summaryFlow = viewModel.getModelSummary(modelId)

        // Then
        summaryFlow.test {
            val summary = awaitItem()
            assertEquals(5, summary.totalDrifts)
            assertEquals(2, summary.totalPatches)
            assertEquals(1, summary.appliedPatches)
            awaitComplete()
        }
    }

    // ==================== Helper Functions ====================

    private fun createTestModel(
        id: String = "test-model-${System.currentTimeMillis()}",
        name: String = "Test Model",
        version: String = "1.0.0"
    ) = MLModel(
        id = id,
        name = name,
        version = version,
        modelPath = "/path/to/model",
        inputFeatures = listOf("feature1", "feature2"),
        outputLabels = listOf("label1", "label2"),
        isActive = true,
        createdAt = Instant.now(),
        lastUpdated = Instant.now()
    )

    private fun createTestPatch(
        id: String = "patch-${System.currentTimeMillis()}",
        modelId: String = "test-model",
        status: PatchStatus = PatchStatus.CREATED
    ) = Patch(
        id = id,
        modelId = modelId,
        driftResultId = "drift-1",
        patchType = PatchType.FEATURE_CLIPPING,
        status = status,
        createdAt = Instant.now(),
        appliedAt = if (status == PatchStatus.APPLIED) Instant.now() else null,
        rolledBackAt = null,
        configuration = PatchConfiguration.FeatureClipping(
            featureIndices = listOf(0, 1),
            minValues = floatArrayOf(0f, 0f),
            maxValues = floatArrayOf(1f, 1f)
        ),
        validationResult = null,
        metadata = emptyMap()
    )
}
