package com.driftdetector.app.data.remote

import retrofit2.Response
import retrofit2.http.*

/**
 * API service for optional encrypted, privacy-preserving backend sync
 */
interface DriftApiService {

    @POST("api/v1/drift/sync")
    suspend fun syncDriftMetadata(
        @Body metadata: DriftMetadataDto
    ): Response<SyncResponse>

    @GET("api/v1/models/{modelId}/drift-stats")
    suspend fun getDriftStatistics(
        @Path("modelId") modelId: String,
        @Query("start_time") startTime: Long,
        @Query("end_time") endTime: Long
    ): Response<DriftStatisticsDto>

    @POST("api/v1/patches/report")
    suspend fun reportPatchApplication(
        @Body report: PatchReportDto
    ): Response<SyncResponse>

    @GET("api/v1/health")
    suspend fun healthCheck(): Response<HealthResponse>
}

// DTOs for API communication

data class DriftMetadataDto(
    val modelId: String,
    val timestamp: Long,
    val driftScore: Double,
    val driftType: String,
    val featureCount: Int,
    val isPrivatized: Boolean = true
)

data class DriftStatisticsDto(
    val modelId: String,
    val totalDrifts: Int,
    val avgDriftScore: Double,
    val driftsByType: Map<String, Int>
)

data class PatchReportDto(
    val patchId: String,
    val modelId: String,
    val patchType: String,
    val status: String,
    val validationMetrics: ValidationMetricsDto?,
    val timestamp: Long
)

data class ValidationMetricsDto(
    val accuracy: Double,
    val safetyScore: Double,
    val driftReduction: Double
)

data class SyncResponse(
    val success: Boolean,
    val message: String,
    val timestamp: Long
)

data class HealthResponse(
    val status: String,
    val version: String
)
