package com.rang.wordpop.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rang.wordpop.ui.history.HistoryScreen
import com.rang.wordpop.ui.notebook.NotebookScreen
import com.rang.wordpop.ui.search.SearchResultsScreen
import com.rang.wordpop.ui.settings.SettingsScreen
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

private enum class HomeTab(val label: String, val emoji: String) {
    HISTORY("Lịch sử", "📋"),
    NOTEBOOK("Sổ từ vựng", "📚"),
    SETTINGS("Cài đặt", "⚙️")
}

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { HomeTab.entries.size })
    val pagerScope = rememberCoroutineScope()
    val selectedTab = HomeTab.entries[pagerState.currentPage]

    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }
    val expanded = searchActive || searchQuery.isNotBlank()

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    fun exitSearch() {
        searchQuery = ""
        searchActive = false
        focusManager.clearFocus()
    }

    BackHandler(enabled = expanded) { exitSearch() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(expanded) {
                if (expanded) {
                    detectTapGestures(onTap = { exitSearch() })
                }
            }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (selectedTab != HomeTab.SETTINGS) {
                    val barAlpha by animateFloatAsState(
                        targetValue = if (expanded) 1f else 0.5f,
                        label = "search_bar_alpha"
                    )

                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .height(40.dp)
                            .alpha(barAlpha)
                            .background(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .padding(horizontal = 12.dp)
                            .focusRequester(focusRequester)
                            .onFocusChanged { searchActive = it.isFocused },
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Tìm kiếm...",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    inner()
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { exitSearch() },
                                        modifier = Modifier.height(40.dp)
                                    ) {
                                        Text(
                                            text = "✕",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            },
            bottomBar = {
                NavigationBar {
                    HomeTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab,
                            onClick = {
                                exitSearch()
                                pagerScope.launch {
                                    pagerState.animateScrollToPage(
                                        page = tab.ordinal,
                                        // Animation ngắn hơn + easing mềm:
                                        // cảm giác dứt khoát, không "nảy" như mặc định.
                                        animationSpec = tween(
                                            durationMillis = 250,
                                            easing = FastOutSlowInEasing
                                        )
                                    )
                                }
                            },
                            icon = { Text(tab.emoji) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            if (searchQuery.isNotBlank()) {
                SearchResultsScreen(
                    query = searchQuery,
                    modifier = Modifier.padding(innerPadding)
                )
            } else {
                HorizontalPager(
                    state = pagerState,
                    // Giữ cả 3 tab luôn sống trong bộ nhớ, tránh việc
                    // dispose + recreate mỗi lần vuốt qua lại.
                    beyondViewportPageCount = HomeTab.entries.size - 1,
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize()
                ) { page ->
                    // Fade nhẹ cho tab không active:
                    //  - Tab đang active: alpha = 1.0 (rõ nét)
                    //  - Tab lân cận khi vuốt: alpha giảm dần xuống 0.6
                    // → mắt không phải "đuổi theo" chi tiết khi trượt → giảm hoa mắt.
                    val pageOffset = (
                            (pagerState.currentPage - page) +
                                    pagerState.currentPageOffsetFraction
                            ).absoluteValue.coerceIn(0f, 1f)
                    val contentAlpha = 1f - (pageOffset * 0.4f)

                    Box(modifier = Modifier.alpha(contentAlpha)) {
                        when (HomeTab.entries[page]) {
                            HomeTab.HISTORY -> HistoryScreen()
                            HomeTab.NOTEBOOK -> NotebookScreen()
                            HomeTab.SETTINGS -> SettingsScreen()
                        }
                    }
                }
            }
        }
    }
}