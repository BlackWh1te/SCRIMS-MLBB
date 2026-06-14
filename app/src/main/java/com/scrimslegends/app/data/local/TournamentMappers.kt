package com.scrimslegends.app.data.local

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scrimslegends.app.data.local.TournamentEntity
import com.scrimslegends.app.data.model.Tournament
import com.scrimslegends.app.data.model.TournamentRequirement
import com.scrimslegends.app.data.model.PrizeType
import com.scrimslegends.app.data.model.TournamentStatus

fun entityToModel(entity: TournamentEntity): Tournament {
    val gson = Gson()
    val requirementsType = object : TypeToken<List<TournamentRequirement>>() {}.type

    return Tournament(
        id = entity.id,
        hostUserId = entity.hostUserId,
        hostUsername = entity.hostUsername,
        title = entity.title,
        description = entity.description,
        logoUrl = entity.logoUrl,
        prizeType = PrizeType.fromValue(entity.prizeType),
        prizeDescription = entity.prizeDescription,
        maxTeams = entity.maxTeams,
        minTeamSize = entity.minTeamSize,
        bestOf = entity.bestOf,
        region = entity.region,
        skillLevel = entity.skillLevel,
        swissRounds = entity.swissRounds,
        currentRound = entity.currentRound,
        status = TournamentStatus.fromValue(entity.status),
        registrationDeadline = entity.registrationDeadline,
        checkInDeadline = entity.checkInDeadline,
        isLiveStreamEnabled = entity.isLiveStreamEnabled,
        isFlagged = entity.isFlagged,
        createdAt = entity.createdAt,
        updatedAt = entity.updatedAt,
        requirements = gson.fromJson(entity.requirementsJson, requirementsType) ?: emptyList(),
        teamCount = entity.teamCount,
        hostTrustScore = entity.hostTrustScore
    )
}

fun modelToEntity(model: Tournament): TournamentEntity {
    val gson = Gson()
    return TournamentEntity(
        id = model.id,
        hostUserId = model.hostUserId,
        hostUsername = model.hostUsername,
        title = model.title,
        description = model.description,
        logoUrl = model.logoUrl,
        prizeType = model.prizeType.value,
        prizeDescription = model.prizeDescription,
        maxTeams = model.maxTeams,
        minTeamSize = model.minTeamSize,
        bestOf = model.bestOf,
        region = model.region,
        skillLevel = model.skillLevel,
        swissRounds = model.swissRounds,
        currentRound = model.currentRound,
        status = model.status.value,
        registrationDeadline = model.registrationDeadline,
        checkInDeadline = model.checkInDeadline,
        isLiveStreamEnabled = model.isLiveStreamEnabled,
        isFlagged = model.isFlagged,
        createdAt = model.createdAt,
        updatedAt = model.updatedAt,
        requirementsJson = gson.toJson(model.requirements),
        teamCount = model.teamCount,
        hostTrustScore = model.hostTrustScore
    )
}
