package com.scrimslegends.app.data.model

// NOTE: ScrimRuleset was removed — the app uses BestOf + GameMode enums instead.
// If draft-pick / ban-count rules are needed in future, re-introduce here.
// Kept as empty file to prevent import breakage in case external code references it.

@Suppress("UNUSED")
enum class PickMode {
    BLIND, DRAFT
}
