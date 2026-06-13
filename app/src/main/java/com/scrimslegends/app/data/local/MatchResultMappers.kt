package com.scrimslegends.app.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.scrimslegends.app.data.local.MatchResultEntity
import com.scrimslegends.app.data.model.MatchResult
import com.scrimslegends.app.data.model.TeamReport
import com.scrimslegends.app.data.model.VerificationStatus
import com.scrimslegends.app.data.model.AdminVerdict
import com.scrimslegends.app.data.model.MatchType
import com.scrimslegends.app.data.model.RosterPlayerInfo

fun entityToModel(entity: MatchResultEntity): MatchResult {
    val gson = Gson()
    val teamAReportType = object : TypeToken<TeamReport?>() {}.type
    val rosterType = object : TypeToken<List<RosterPlayerInfo>>() {}.type

    return MatchResult(
        id = entity.id,
        scrimId = entity.scrimId,
        teamAId = entity.teamAId,
        teamAName = entity.teamAName,
        teamBId = entity.teamBId,
        teamBName = entity.teamBName,
        teamAReport = gson.fromJson(entity.teamAReportJson, teamAReportType),
        teamBReport = gson.fromJson(entity.teamBReportJson, teamAReportType),
        screenshotUrl = entity.screenshotUrl,
        verificationStatus = VerificationStatus.valueOf(entity.verificationStatus),
        confirmedWinnerId = entity.confirmedWinnerId,
        adminNotes = entity.adminNotes,
        createdAt = entity.createdAt,
        resolvedAt = entity.resolvedAt,
        teamARoster = gson.fromJson(entity.teamARosterJson, rosterType) ?: emptyList(),
        teamBRoster = gson.fromJson(entity.teamBRosterJson, rosterType) ?: emptyList(),
        adminVerdict = entity.adminVerdict?.let { AdminVerdict.valueOf(it) },
        punishedTeamId = entity.punishedTeamId,
        punishmentDurationHours = entity.punishmentDurationHours,
        reviewedByAdminId = entity.reviewedByAdminId,
        reviewedAt = entity.reviewedAt,
        noShowTeamId = entity.noShowTeamId,
        matchActuallyPlayed = entity.matchActuallyPlayed,
        matchType = MatchType.valueOf(entity.matchType),
        tournamentTitle = entity.tournamentTitle,
        roundNumber = entity.roundNumber
    )
}

fun modelToEntity(model: MatchResult): MatchResultEntity {
    val gson = Gson()
    return MatchResultEntity(
        id = model.id,
        scrimId = model.scrimId,
        teamAId = model.teamAId,
        teamAName = model.teamAName,
        teamBId = model.teamBId,
        teamBName = model.teamBName,
        teamAReportJson = gson.toJson(model.teamAReport),
        teamBReportJson = gson.toJson(model.teamBReport),
        screenshotUrl = model.screenshotUrl,
        verificationStatus = model.verificationStatus.name,
        confirmedWinnerId = model.confirmedWinnerId,
        adminNotes = model.adminNotes,
        createdAt = model.createdAt,
        resolvedAt = model.resolvedAt,
        teamARosterJson = gson.toJson(model.teamARoster),
        teamBRosterJson = gson.toJson(model.teamBRoster),
        adminVerdict = model.adminVerdict?.name,
        punishedTeamId = model.punishedTeamId,
        punishmentDurationHours = model.punishmentDurationHours,
        reviewedByAdminId = model.reviewedByAdminId,
        reviewedAt = model.reviewedAt,
        noShowTeamId = model.noShowTeamId,
        matchActuallyPlayed = model.matchActuallyPlayed,
        matchType = model.matchType.name,
        tournamentTitle = model.tournamentTitle,
        roundNumber = model.roundNumber
    )
}
