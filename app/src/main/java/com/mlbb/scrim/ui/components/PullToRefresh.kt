package com.mlbb.scrim.ui.components

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
import com.mlbb.scrim.ui.theme.iOSBlue
import com.mlbb.scrim.ui.theme.SurfaceElevated
import com.mlbb.scrim.ui.utils.HapticFeedback
import kotlinx.coroutines.delay

/**
 * Pull-to-refresh wrapper that properly syncs the Material3 refresh indicator
 * with an external [isRefreshing] state from a ViewModel.
 *
 * Fixes for phone devices:
 * 1. Uses [rememberUpdatedState] so the [onRefresh] callback is always current
 * 2. Tracks whether a user-initiated refresh is in progress, and only dismisses
 *    the indicator when the external [isRefreshing] transitions true→false
 * 3. Safety timeout of 10s prevents the stuck-forever bug
 * 4. Guards [state.endRefresh] with [state.isRefreshing] check to avoid
 *    calling it on a non-refreshing state (which causes visual glitches)
 */
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

    // Always hold the latest onRefresh callback so we don't capture a stale lambda
    val currentOnRefresh by rememberUpdatedState(onRefresh)

    // Track whether we're in a user-initiated refresh cycle
    var refreshInProgress by remember { mutableStateOf(false) }

    // ── Trigger refresh when user pulls down ──────────────────────
    // state.isRefreshing becomes true when the user drags past the threshold
    // and releases. We then invoke onRefresh() to start the data load.
    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            refreshInProgress = true
            HapticFeedback.performClick(context)
            currentOnRefresh()
        }
    }

    // ── Dismiss indicator when external refresh completes ──────────
    // We watch isRefreshing and dismiss the indicator when:
    // - A refresh was in progress AND isRefreshing becomes false (normal path)
    // - OR 10 seconds have passed (safety timeout for stuck states)
    LaunchedEffect(isRefreshing, refreshInProgress) {
        if (refreshInProgress) {
            if (!isRefreshing) {
                // Normal path: data finished loading, dismiss the indicator
                if (state.isRefreshing) {
                    state.endRefresh()
                }
                refreshInProgress = false
            } else {
                // Safety timeout — force-end after 10 seconds
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

        PullToRefreshContainer(
            state = state,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp),
            containerColor = SurfaceElevated,
            contentColor = iOSBlue
        )
    }
}
