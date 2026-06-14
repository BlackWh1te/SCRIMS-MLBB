package com.scrimslegends.app.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scrimslegends.app.ui.theme.iOSBlue
import com.scrimslegends.app.ui.utils.HapticFeedback
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PullToRefreshContainer(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val state = rememberPullToRefreshState()
    val context = LocalContext.current

    val currentOnRefresh by rememberUpdatedState(onRefresh)
    var refreshInProgress by remember { mutableStateOf(false) }
    val showIndicator = refreshInProgress || state.isRefreshing

    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            refreshInProgress = true
            HapticFeedback.performClick(context)
            currentOnRefresh()
        }
    }

    LaunchedEffect(isRefreshing, refreshInProgress) {
        if (refreshInProgress) {
            if (!isRefreshing) {
                if (state.isRefreshing) {
                    state.endRefresh()
                }
                refreshInProgress = false
            } else {
                delay(10_000)
                if (state.isRefreshing) {
                    state.endRefresh()
                }
                refreshInProgress = false
            }
        }
    }

    Box(
        modifier = modifier.nestedScroll(state.nestedScrollConnection)
    ) {
        content()

        AnimatedVisibility(
            visible = showIndicator,
            enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(100)),
            exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(100))
        ) {
            PullToRefreshContainer(
                state = state,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 8.dp),
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = iOSBlue
            )
        }
    }
}
