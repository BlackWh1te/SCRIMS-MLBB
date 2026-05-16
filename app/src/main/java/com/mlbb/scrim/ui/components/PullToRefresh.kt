package com.mlbb.scrim.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshContainer
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import com.mlbb.scrim.ui.theme.iOSBlue
import com.mlbb.scrim.ui.theme.SurfaceElevated
import com.mlbb.scrim.ui.utils.HapticFeedback

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

    LaunchedEffect(state.isRefreshing) {
        if (state.isRefreshing) {
            HapticFeedback.performClick(context)
            onRefresh()
        }
    }

    LaunchedEffect(isRefreshing) {
        if (!isRefreshing) {
            state.endRefresh()
        }
    }

    Box(
        modifier = modifier.nestedScroll(state.nestedScrollConnection)
    ) {
        content()

        PullToRefreshContainer(
            state = state,
            modifier = Modifier.align(Alignment.TopCenter),
            containerColor = SurfaceElevated,
            contentColor = iOSBlue
        )
    }
}
