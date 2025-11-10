package com.driftdetector.app.core.offline

import com.driftdetector.app.domain.model.MLModel
import com.driftdetector.app.domain.model.Patch
import com.driftdetector.app.domain.model.DriftResult
import timber.log.Timber

/**
 * Resolves conflicts when syncing data between local and remote storage
 */
class ConflictResolver {

    /**
     * Resolve conflict between local and remote model
     * Strategy: Last-write-wins based on lastUpdated timestamp
     */
    fun resolveModelConflict(local: MLModel, remote: MLModel): MLModel {
        Timber.d("🔀 Resolving model conflict: local=${local.lastUpdated}, remote=${remote.lastUpdated}")

        return if (local.lastUpdated.isAfter(remote.lastUpdated)) {
            Timber.d("✅ Local version is newer")
            local
        } else {
            Timber.d("✅ Remote version is newer")
            remote
        }
    }

    /**
     * Resolve conflict between local and remote patch
     * Strategy: Prefer patch with higher safety score
     */
    fun resolvePatchConflict(local: Patch, remote: Patch): Patch {
        Timber.d("🔀 Resolving patch conflict")

        val localSafetyScore = local.validationResult?.metrics?.safetyScore ?: 0.0
        val remoteSafetyScore = remote.validationResult?.metrics?.safetyScore ?: 0.0

        return if (localSafetyScore > remoteSafetyScore) {
            Timber.d("✅ Local patch has higher safety score")
            local
        } else {
            Timber.d("✅ Remote patch has higher safety score")
            remote
        }
    }

    /**
     * Resolve conflict between local and remote drift result
     * Strategy: Keep the result with most recent timestamp
     */
    fun resolveDriftConflict(local: DriftResult, remote: DriftResult): DriftResult {
        Timber.d("🔀 Resolving drift conflict")

        return if (local.timestamp.isAfter(remote.timestamp)) {
            Timber.d("✅ Local drift result is newer")
            local
        } else {
            Timber.d("✅ Remote drift result is newer")
            remote
        }
    }

    /**
     * Merge lists of items, removing duplicates and resolving conflicts
     */
    fun <T> mergeWithConflictResolution(
        local: List<T>,
        remote: List<T>,
        getId: (T) -> String,
        resolve: (T, T) -> T
    ): List<T> {
        val localById = local.associateBy(getId)
        val remoteById = remote.associateBy(getId)

        val allIds = (localById.keys + remoteById.keys).distinct()

        return allIds.mapNotNull { id ->
            val localItem = localById[id]
            val remoteItem = remoteById[id]

            when {
                localItem != null && remoteItem != null -> {
                    // Conflict - resolve it
                    Timber.d("🔀 Conflict for id=$id, resolving...")
                    resolve(localItem, remoteItem)
                }

                localItem != null -> localItem
                remoteItem != null -> remoteItem
                else -> null
            }
        }
    }
}
