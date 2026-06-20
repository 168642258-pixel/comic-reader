package com.comicreader.app.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.comicreader.app.data.Comic
import com.comicreader.app.data.ComicRepository
import com.comicreader.app.ui.theme.OverlayColor
import com.comicreader.app.ui.theme.TextPrimary
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenComic: (Comic) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember { ComicRepository(context) }
    var comics by remember { mutableStateOf(repository.getAll()) }
    var showDeleteDialog by remember { mutableStateOf<Comic?>(null) }
    var isImporting by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val title = getFileName(context, uri) ?: "未知漫画"
            isImporting = true
            scope.launch {
                val comic = repository.importZip(uri, title)
                isImporting = false
                if (comic != null) {
                    comics = repository.getAll()
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("漫画阅读") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("+", style = MaterialTheme.typography.headlineMedium)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (comics.isEmpty() && !isImporting) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "还没有漫画",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "点击右下角 + 按钮导入 .zip 漫画",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(comics, key = { it.id }) { comic ->
                        ComicCard(
                            comic = comic,
                            onClick = { onOpenComic(comic) },
                            onLongClick = { showDeleteDialog = comic }
                        )
                    }
                }
            }

            if (isImporting) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OverlayColor),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("正在导入…", color = TextPrimary)
                    }
                }
            }
        }
    }

    showDeleteDialog?.let { comic ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text("删除漫画") },
            text = { Text("确定要删除「${comic.title}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    repository.delete(comic.id)
                    comics = repository.getAll()
                    showDeleteDialog = null
                }) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text("取消")
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ComicCard(
    comic: Comic,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.7f)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (comic.coverPath != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(File(comic.coverPath))
                        .crossfade(true)
                        .build(),
                    contentDescription = comic.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = comic.title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (comic.lastReadPage > 0) {
            Text(
                text = "${comic.lastReadPage + 1} / ${comic.pageCount}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex)
                }
            }
        }
    }
    if (name == null) {
        name = uri.lastPathSegment
    }
    return name?.removeSuffix(".zip")?.removeSuffix(".cbz")?.trim()
}
