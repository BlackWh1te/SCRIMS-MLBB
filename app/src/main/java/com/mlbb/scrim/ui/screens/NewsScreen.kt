package com.mlbb.scrim.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mlbb.scrim.R
import com.mlbb.scrim.data.model.NewsArticle
import com.mlbb.scrim.ui.components.AnimatedEntrance
import com.mlbb.scrim.ui.components.GradientButton
import com.mlbb.scrim.ui.components.timeAgo
import com.mlbb.scrim.ui.theme.*
import com.mlbb.scrim.viewmodel.NewsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NewsScreen(
    viewModel: NewsViewModel,
    languageCode: String = "en"
) {
    val articles by viewModel.articles.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val selectedArticle by viewModel.selectedArticle.collectAsState()
    val dripInfo by viewModel.dripInfo.collectAsState()

    // Only translate on language change, don't force API refresh
    LaunchedEffect(languageCode) {
        viewModel.loadNews(languageCode, forceRefresh = false)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .background(brush = heroGradientBrush())
    ) {
        selectedArticle?.let { article ->
            NewsDetailScreen(
                article = article,
                onBack = { viewModel.clearSelectedArticle() }
            )
        } ?: run {
            NewsListContent(
                articles = articles,
                isLoading = isLoading,
                error = error,
                dripInfo = dripInfo,
                onRefresh = { viewModel.refresh(languageCode) },
                onArticleClick = { viewModel.selectArticle(it) },
                onClearError = { viewModel.clearError() }
            )
        }
    }
}

@Composable
private fun NewsListContent(
    articles: List<NewsArticle>,
    isLoading: Boolean,
    error: String?,
    dripInfo: com.mlbb.scrim.viewmodel.NewsViewModel.DripInfo,
    onRefresh: () -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onClearError: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Header with Quota
        AnimatedEntrance(delayMillis = 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.news),
                            style = iOSTitle1,
                            color = White
                        )
                        Text(
                            text = stringResource(R.string.news_subtitle),
                            style = iOSBody.copy(color = LightGray),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    // Drip progress badge
                    val dripPercent = if (dripInfo.total > 0) {
                        dripInfo.unlocked.toFloat() / dripInfo.total
                    } else 0f
                    val dripColor = when {
                        dripPercent >= 1f -> SuccessGreen
                        dripPercent >= 0.5f -> GoldPrimary
                        else -> BluePrimary
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    dripColor.copy(alpha = 0.15f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.NewReleases,
                                contentDescription = null,
                                tint = dripColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${dripInfo.unlocked}/${dripInfo.total}",
                                color = dripColor,
                                style = iOSFootnote.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                        // Unseen count pill
                        if (dripInfo.unseen > 0) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.locked_count, dripInfo.unseen),
                                color = MidGray,
                                style = iOSFootnote.copy(fontSize = 10.sp)
                            )
                        }
                    }
                }
            }
        }

        // Error banner
        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ErrorRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = error ?: "",
                        color = ErrorRed,
                        style = iOSFootnote,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClearError) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Content
        if (isLoading && articles.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = GoldPrimary,
                    modifier = Modifier.size(48.dp)
                )
            }
        } else if (articles.isEmpty()) {
            EmptyNewsState(onRefresh = onRefresh)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = articles,
                    key = { it.id }
                ) { article ->
                    NewsCard(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }

                item {
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = GoldPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    } else {
                        // Pull to refresh hint
                        TextButton(
                            onClick = onRefresh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MidGray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.refresh),
                                color = MidGray,
                                style = iOSFootnote
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsCard(
    article: NewsArticle,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .shadow(
                elevation = 6.dp,
                spotColor = Color.Black.copy(alpha = 0.15f),
                shape = RoundedCornerShape(16.dp)
            ),
        colors = CardDefaults.cardColors(containerColor = SurfaceGlass),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Image
            if (article.imageUrl.isNotBlank()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = article.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // Source and date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.source,
                        color = GoldPrimary,
                        style = iOSFootnote.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = timeAgo(article.publishedAt, useShort = true),
                        color = MidGray,
                        style = iOSFootnote
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title
                Text(
                    text = article.title,
                    style = iOSTitle3.copy(color = White),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Description
                Text(
                    text = article.description,
                    style = iOSBody.copy(color = LightGray),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Translated indicator
                if (article.isTranslated) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = SuccessGreen.copy(alpha = 0.8f),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.translated),
                            color = SuccessGreen.copy(alpha = 0.8f),
                            style = iOSFootnote.copy(fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsDetailScreen(
    article: NewsArticle,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(brush = heroGradientBrush())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = White
                )
            }
            Text(
                text = stringResource(R.string.news_detail),
                style = iOSTitle3.copy(color = White),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                    context.startActivity(intent)
                }
            ) {
                Icon(
                    imageVector = Icons.Default.OpenInBrowser,
                    contentDescription = stringResource(R.string.open_in_browser),
                    tint = BluePrimary
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            item {
                if (article.imageUrl.isNotBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .shadow(
                                elevation = 8.dp,
                                spotColor = Color.Black.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkNavy)
                    ) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = article.title,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // Source & Date
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = article.source,
                        color = GoldPrimary,
                        style = iOSCallout.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = dateFormat.format(Date(article.publishedAt)),
                            color = MidGray,
                            style = iOSFootnote
                        )
                        Text(
                            text = timeAgo(article.publishedAt),
                            color = GoldPrimary.copy(alpha = 0.7f),
                            style = iOSFootnote.copy(fontSize = 11.sp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Title
                Text(
                    text = article.title,
                    style = iOSTitle1.copy(color = White)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Translated badge
                if (article.isTranslated) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                SuccessGreen.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Translate,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.translated),
                            color = SuccessGreen,
                            style = iOSFootnote.copy(fontWeight = FontWeight.Medium)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Content
                Text(
                    text = article.content.ifBlank { article.description },
                    style = iOSBody.copy(color = LightGray.copy(alpha = 0.9f)),
                    lineHeight = 24.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Open in browser button
                GradientButton(
                    text = stringResource(R.string.read_full_article),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(article.url))
                        context.startActivity(intent)
                    },
                    gradient = BlueGradient
                )

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun EmptyNewsState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Newspaper,
            contentDescription = null,
            tint = MidGray.copy(alpha = 0.5f),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.no_news),
            style = iOSTitle3.copy(color = MidGray),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.no_news_subtitle),
            style = iOSBody.copy(color = MidGray.copy(alpha = 0.7f)),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            modifier = Modifier.padding(horizontal = 40.dp)
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick = onRefresh) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                tint = BluePrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.try_again),
                color = BluePrimary,
                style = iOSBody.copy(fontWeight = FontWeight.SemiBold)
            )
        }
    }
}
