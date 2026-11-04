package com.example.ui.viewer

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.PdfProject
import com.example.ui.theme.FmBackground
import com.example.ui.theme.FmBorder
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmSlateDark
import com.example.ui.theme.FmSlateMuted
import com.example.util.PdfEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(
    project: PdfProject?,
    onBack: () -> Unit,
    onDownload: (PdfProject) -> Unit,
    onShare: (PdfProject) -> Unit
) {
    BackHandler { onBack() }

    var renderedPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val listState = rememberLazyListState()

    LaunchedEffect(project?.pdfFilePath) {
        val path = project?.pdfFilePath
        if (path != null) {
            isLoading = true
            val pages = PdfEngine.renderPdfPages(path)
            renderedPages = pages
            isLoading = false
        } else {
            isLoading = false
        }
    }

    val currentPage by remember {
        derivedStateOf {
            if (renderedPages.isEmpty()) 1 else (listState.firstVisibleItemIndex + 1).coerceAtMost(renderedPages.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.title ?: "PDF Document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FmSlateDark
                        )
                        Text(
                            text = "${project?.pageCount ?: 0} Pages • ${project?.formattedSize ?: ""}",
                            fontSize = 12.sp,
                            color = FmSlateMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = FmSlateDark
                        )
                    }
                },
                actions = {
                    if (project != null) {
                        IconButton(onClick = { onDownload(project) }) {
                            Icon(Icons.Default.Download, contentDescription = "Download PDF", tint = FmRed)
                        }
                        IconButton(onClick = { onShare(project) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share PDF", tint = FmSlateDark)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FmPureWhite)
            )
        },
        bottomBar = {
            if (renderedPages.isNotEmpty()) {
                Surface(
                    color = FmPureWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Page $currentPage of ${renderedPages.size}",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = FmSlateDark
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFFFEBEE),
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = null,
                                        tint = FmRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = project?.pageSize?.name ?: "A4",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = FmRed
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF1F5F9)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = FmRed)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Rendering PDF pages...",
                            fontSize = 14.sp,
                            color = FmSlateMuted
                        )
                    }
                }
                renderedPages.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "No PDF pages to display",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = FmSlateDark
                        )
                        Text(
                            "Please re-generate or check document pages.",
                            fontSize = 13.sp,
                            color = FmSlateMuted
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(renderedPages) { index, pageBitmap ->
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .shadow(6.dp, RoundedCornerShape(12.dp)),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White
                                ) {
                                    Image(
                                        bitmap = pageBitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                }
                                Text(
                                    text = "- Page ${index + 1} -",
                                    fontSize = 12.sp,
                                    color = FmSlateMuted,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
