@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
package com.comicreader.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comicreader.app.data.Comic
import com.comicreader.app.data.ComicRepository
import com.comicreader.app.reader.ReadingMode
import com.comicreader.app.reader.ZipExtractor
import com.comicreader.app.ui.theme.OverlayColor
import com.comicreader.app.ui.theme.PageIndicatorBg
import com.comicreader.app.ui.theme.TextPrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    comic: Comic,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val repository = remember { ComicRepository(context) }

    val imageFiles = remember(comic.id) {
        ZipExtractor.getImageFiles(File(comic.directoryPath))
    }

    var readingMode by remember { mutableStateOf(ReadingMode.LEFT_RIGHT) }
    var currentPage by remember { mutableIntStateOf(comic.lastReadPage.coerceIn(0, (imageFiles.size - 1).coerceAtLeast(0))) }
    var showControls by remember { mutableStateOf(true) }
    var showModeMenu by remember { mutableStateOf(false) }

    val totalPages = imageFiles.size

    // 保存阅读进度
    LaunchedEffect(currentPage) {
        if (totalPages > 0) {
            repository.updateProgress(comic.id, currentPage)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            repository.updateProgress(comic.id, currentPage)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (totalPages == 0) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("没有找到图片", color = Color.Gray)
            }
        } else {
            // 内容区域
            when (readingMode) {
                ReadingMode.LEFT_RIGHT -> {
                    val pagerState = rememberPagerState(
                        initialPage = currentPage,
                        pageCount = { totalPages }
                    )
                    LaunchedEffect(pagerState.currentPage) {
                        currentPage = pagerState.currentPage
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(imageFiles[page]))
                                .crossfade(true)
                                .build(),
                            contentDescription = "第${page + 1}页",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { showControls = !showControls }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                ReadingMode.UP_DOWN -> {
                    val pagerState = rememberPagerState(
                        initialPage = currentPage,
                        pageCount = { totalPages }
                    )
                    LaunchedEffect(pagerState.currentPage) {
                        currentPage = pagerState.currentPage
                    }

                    VerticalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(imageFiles[page]))
                                .crossfade(true)
                                .build(),
                            contentDescription = "第${page + 1}页",
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(Unit) {
                                    detectTapGestures { showControls = !showControls }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                ReadingMode.LEFT_RIGHT_CONTINUOUS -> {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { showControls = !showControls }
                            }
                    ) {
                        itemsIndexed(imageFiles) { index, path ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(path))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "第${index + 1}页",
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(min = 1.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }

                ReadingMode.UP_DOWN_CONTINUOUS -> {
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = currentPage
                    )
                    LaunchedEffect(listState.firstVisibleItemIndex) {
                        currentPage = listState.firstVisibleItemIndex
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures { showControls = !showControls }
                            }
                    ) {
                        itemsIndexed(imageFiles) { index, path ->
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(File(path))
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "第${index + 1}页",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 1.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
        }

        // 控制层遮罩
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(OverlayColor)
            ) {
                // 顶部栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xCC000000)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                            .systemBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack) {
                            Text(
                                text = "<",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = comic.title,
                            modifier = Modifier.weight(1f),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )

                        // 模式选择
                        Box {
                            TextButton(onClick = { showModeMenu = true }) {
                                Text(
                                    text = readingMode.displayName,
                                    color = Color.White
                                )
                            }
                            DropdownMenu(
                                expanded = showModeMenu,
                                onDismissRequest = { showModeMenu = false }
                            ) {
                                ReadingMode.entries.forEach { mode ->
                                    DropdownMenuItem(
                                        text = { Text(mode.displayName) },
                                        onClick = {
                                            readingMode = mode
                                            showModeMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // 页码指示器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .systemBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = PageIndicatorBg,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = "${currentPage + 1} / $totalPages",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
