package com.mlbb.scrim.domain.usecases

import com.mlbb.scrim.data.repository.TeamRepositoryInterface
import kotlinx.coroutines.flow.Flow

/**
 * Example use case demonstrating the domain layer pattern.
 *
 * Use cases encapsulate a single business operation and may coordinate
 * multiple repositories. They live between ViewModels and repositories.
 *
 * Long-term: migrate ownership validation and complex orchestration
 * out of repositories into use cases for better testability.
 */
class DeleteTeamUseCase(
    private val teamRepository: TeamRepositoryInterface
) {
    /**
     * Deletes a team after verifying the current user is the team leader.
     * The authorization check is currently in the repository (defense-in-depth).
     * Future refactor: move leader verification here and keep repository as data-only.
     */
    suspend operator fun invoke(teamId: String): Flow<Result<Unit>> {
        return teamRepository.deleteTeam(teamId)
    }
}
