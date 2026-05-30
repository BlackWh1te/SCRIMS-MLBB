package com.scrimslegends.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scrimslegends.app.R
import com.scrimslegends.app.data.model.RankTier
import com.scrimslegends.app.data.model.RegionalRank
import com.scrimslegends.app.ui.theme.*

// ═══════════════════════════════════════════════════════════════════
// RANK BADGE — Rich visual badge for each of the 7 custom tiers
// Bronze → Solver → Gold → Grandmaster → Epic → Legend → Mythic
// ═══════════════════════════════════════════════════════════════════

@Composable
fun RankBadge(
    tier: RankTier,
    modifier: Modifier = Modifier,
    size: RankBadgeSize = RankBadgeSize.MEDIUM
) {
    when (tier) {
        RankTier.BRONZE -> BronzeBadge(modifier, size)
        RankTier.SOLVER -> SolverBadge(modifier, size)
        RankTier.GOLD -> GoldBadge(modifier, size)
        RankTier.GRANDMASTER -> GrandmasterBadge(modifier, size)
        RankTier.EPIC -> EpicBadge(modifier, size)
        RankTier.LEGEND -> LegendBadge(modifier, size)
        RankTier.MYTHIC -> MythicBadge(modifier, size)
    }
}

enum class RankBadgeSize(val dp: Int) {
    SMALL(24),
    MEDIUM(32),
    LARGE(48)
}

// ─── Bronze Badge ───
@Composable
private fun BronzeBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_bronze),
        contentDescription = "Bronze",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Solver (Silver) Badge ───
@Composable
private fun SolverBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_silver),
        contentDescription = "Solver",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Gold Badge ───
@Composable
private fun GoldBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_gold),
        contentDescription = "Gold",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Grandmaster Badge ───
@Composable
private fun GrandmasterBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_grandmaster),
        contentDescription = "Grandmaster",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Epic Badge ───
@Composable
private fun EpicBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_epic),
        contentDescription = "Epic",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Legend Badge ───
@Composable
private fun LegendBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_legend),
        contentDescription = "Legend",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ─── Mythic Badge ───
@Composable
private fun MythicBadge(modifier: Modifier, size: RankBadgeSize) {
    Image(
        painter = painterResource(id = R.drawable.tier_mythic),
        contentDescription = "Mythic",
        modifier = modifier.size(size.dp.dp),
        contentScale = androidx.compose.ui.layout.ContentScale.Fit
    )
}

// ═══════════════════════════════════════════════════════════════════
// REGIONAL TOP BADGE — TOP1/TOP2/TOP3 for RU scrims
// ═══════════════════════════════════════════════════════════════════

@Composable
fun RegionalTopBadge(
    rank: RegionalRank,
    modifier: Modifier = Modifier
) {
    when (rank) {
        RegionalRank.TOP1 -> Top1Badge(modifier)
        RegionalRank.TOP2 -> Top2Badge(modifier)
        RegionalRank.TOP3 -> Top3Badge(modifier)
    }
}

@Composable
private fun Top1Badge(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(6.dp, RoundedCornerShape(9999.dp), spotColor = Top1Gold.copy(alpha = 0.5f))
            .background(
                brush = Brush.horizontalGradient(
                    colors = GoldRankGradient
                ),
                shape = RoundedCornerShape(9999.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.EmojiEvents,
                contentDescription = null,
                tint = DarkBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "TOP 1 RU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = DarkBlue
            )
        }
    }
}

@Composable
private fun Top2Badge(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(4.dp, RoundedCornerShape(9999.dp), spotColor = Top2Silver.copy(alpha = 0.5f))
            .background(
                brush = Brush.horizontalGradient(
                    colors = SilverGradient
                ),
                shape = RoundedCornerShape(9999.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = DarkBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "TOP 2 RU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = DarkBlue
            )
        }
    }
}

@Composable
private fun Top3Badge(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(32.dp)
            .shadow(4.dp, RoundedCornerShape(9999.dp), spotColor = Top3Bronze.copy(alpha = 0.5f))
            .background(
                brush = Brush.horizontalGradient(
                    colors = BronzeGradient
                ),
                shape = RoundedCornerShape(9999.dp)
            )
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "TOP 3 RU",
                fontSize = 12.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// LEGACY: Simple tier badge for backwards compat in list views
// ═══════════════════════════════════════════════════════════════════

@Composable
fun TierBadge(
    tierName: String,
    modifier: Modifier = Modifier
) {
    val tier = try {
        RankTier.valueOf(tierName.uppercase())
    } catch (_: Exception) {
        RankTier.values().find { it.displayName.equals(tierName, ignoreCase = true) }
    }

    if (tier != null) {
        RankBadge(tier = tier, modifier = modifier, size = RankBadgeSize.SMALL)
    } else {
        // Fallback for unknown tiers
        val (gradient, textColor) = when (tierName.lowercase()) {
            "bronze" -> BronzeGradient to Color.White
            "silver" -> SilverGradient to Color.White
            "gold" -> GoldRankGradient to DarkBlue
            "platinum" -> PlatinumGradient to DarkBlue
            "diamond" -> DiamondGradient to DarkBlue
            "master" -> MasterGradient to Color.White
            "grandmaster" -> GrandmasterGradient to Color.White
            else -> PurpleGradient to Color.White
        }
        val brush = Brush.horizontalGradient(colors = gradient)
        Box(
            modifier = modifier
                .background(brush = brush, shape = RoundedCornerShape(9999.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = tierName.uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = 0.8.sp
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
// DRAW HELPERS — Custom badge decorations
// ═══════════════════════════════════════════════════════════════════

private fun DrawScope.drawCrownTop(w: Float, h: Float) {
    val crownColor = Gold
    val stroke = Stroke(width = 1.5f)
    drawArc(
        color = crownColor,
        startAngle = -30f,
        sweepAngle = 60f,
        useCenter = false,
        topLeft = Offset(w * 0.25f, -h * 0.15f),
        size = Size(w * 0.5f, h * 0.5f),
        style = stroke
    )
}

private fun DrawScope.drawFlameBorder(w: Float, h: Float) {
    val flameColor = WarningOrange.copy(alpha = 0.4f)
    drawRect(
        color = flameColor,
        topLeft = Offset(-1f, -1f),
        size = Size(w + 2, h + 2),
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawWingAccents(w: Float, h: Float) {
    val wingColor = Gold.copy(alpha = 0.3f)
    // Left wing hint
    drawRect(
        color = wingColor,
        topLeft = Offset(0f, h * 0.2f),
        size = Size(3f, h * 0.6f)
    )
    // Right wing hint
    drawRect(
        color = wingColor,
        topLeft = Offset(w - 3f, h * 0.2f),
        size = Size(3f, h * 0.6f)
    )
}

// Bronze hex background modifier
private fun Modifier.bronzeHexBackground() = this.then(
    Modifier.background(
        brush = Brush.linearGradient(
            colors = BronzeGradient
        ),
        shape = RoundedCornerShape(8.dp)
    )
)
