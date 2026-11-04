package com.example.ui.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.ArrowLeft
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FilterVintage
import androidx.compose.material.icons.filled.InvertColors
import androidx.compose.material.icons.filled.MonochromePhotos
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DocPage
import com.example.model.FilterType
import com.example.model.PdfPageSize
import com.example.model.PdfProject
import com.example.model.PdfQuality
import com.example.ui.theme.FmBackground
import com.example.ui.theme.FmBorder
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmRedLight
import com.example.ui.theme.FmSlate
import com.example.ui.theme.FmSlateDark
import com.example.ui.theme.FmSlateMuted
import com.example.util.ImageFilterUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageEditorScreen(
    project: PdfProject?,
    selectedPageIndex: Int,
    isGenerating: Boolean,
    generationProgress: Float,
    onSelectPage: (Int) -> Unit,
    onFilterChanged: (FilterType) -> Unit,
    onApplyFilterToAll: (FilterType) -> Unit,
    onRotatePage: () -> Unit,
    onDeletePage: () -> Unit,
    onMovePage: (Int) -> Unit,
    onAddFromCamera: () -> Unit,
    onAddFromGallery: (List<Uri>) -> Unit,
    onSaveAndGeneratePdf: (title: String, pageSize: PdfPageSize, quality: PdfQuality) -> Unit,
    onBack: () -> Unit
) {
    BackHandler { onBack() }

    val pages = project?.pages ?: emptyList()
    val currentPage = pages.getOrNull(selectedPageIndex)

    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPreview by remember { mutableStateOf(false) }
    var showSaveDialog by remember { mutableStateOf(false) }

    // Save dialog form states
    var docTitle by remember(project?.title) { mutableStateOf(project?.title ?: "Document") }
    var selectedPageSize by remember(project?.pageSize) { mutableStateOf(project?.pageSize ?: PdfPageSize.A4) }
    var selectedQuality by remember(project?.quality) { mutableStateOf(project?.quality ?: PdfQuality.HIGH) }

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onAddFromGallery(uris)
        }
    }

    // Refresh page preview bitmap with active filter and rotation
    LaunchedEffect(currentPage?.id, currentPage?.filterType, currentPage?.rotationDegrees) {
        val p = currentPage
        if (p != null) {
            isLoadingPreview = true
            val raw = ImageFilterUtil.loadBitmap(p.originalImagePath, maxDimension = 1200)
            if (raw != null) {
                previewBitmap = ImageFilterUtil.applyFilter(
                    source = raw,
                    filter = p.filterType,
                    brightness = p.brightness,
                    contrast = p.contrast,
                    rotationDegrees = p.rotationDegrees
                )
            }
            isLoadingPreview = false
        } else {
            previewBitmap = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = project?.title ?: "Edit Document",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FmSlateDark
                        )
                        Text(
                            text = "${pages.size} Page(s) • Tap filter to adjust",
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
                    Button(
                        onClick = { showSaveDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = FmRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save PDF", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FmPureWhite)
            )
        },
        containerColor = FmBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Document Page Viewport
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (currentPage != null && previewBitmap != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White
                    ) {
                        Image(
                            bitmap = previewBitmap!!.asImageBitmap(),
                            contentDescription = "Current page preview",
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Floating Page Actions (Rotate, Delete, Reorder)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xDD0F172A))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rotate
                        IconButton(
                            onClick = onRotatePage,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.RotateRight, contentDescription = "Rotate", tint = Color.White)
                        }

                        // Move Left
                        IconButton(
                            onClick = { onMovePage(-1) },
                            enabled = selectedPageIndex > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Move Left",
                                tint = if (selectedPageIndex > 0) Color.White else Color.Gray
                            )
                        }

                        // Move Right
                        IconButton(
                            onClick = { onMovePage(1) },
                            enabled = selectedPageIndex < pages.size - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Move Right",
                                tint = if (selectedPageIndex < pages.size - 1) Color.White else Color.Gray
                            )
                        }

                        // Delete
                        IconButton(
                            onClick = onDeletePage,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Page", tint = Color(0xFFEF4444))
                        }
                    }

                    // Page Badge at Bottom Center
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xDD0F172A)
                    ) {
                        Text(
                            text = "Page ${selectedPageIndex + 1} of ${pages.size} • ${currentPage.filterType.displayName}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                } else if (isLoadingPreview) {
                    CircularProgressIndicator(color = FmRed)
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FmSlateMuted, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No pages added yet", fontWeight = FontWeight.SemiBold, color = FmSlateDark)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onAddFromCamera,
                            colors = ButtonDefaults.buttonColors(containerColor = FmRed)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Camera to Add Page")
                        }
                    }
                }
            }

            // Bottom Filter Studio Panel
            Surface(
                color = FmPureWhite,
                shadowElevation = 12.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    // Filters Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoFixHigh, contentDescription = null, tint = FmRed, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Document Filters",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = FmSlateDark
                            )
                        }

                        if (currentPage != null) {
                            TextButton(
                                onClick = { onApplyFilterToAll(currentPage.filterType) },
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Text("Apply to all", fontSize = 12.sp, color = FmRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    // Filter Options Carousel
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                    ) {
                        itemsIndexed(FilterType.entries) { _, filter ->
                            val isSelected = currentPage?.filterType == filter
                            val icon = when (filter) {
                                FilterType.ORIGINAL -> Icons.Default.FilterVintage
                                FilterType.MAGIC_COLOR -> Icons.Default.AutoFixHigh
                                FilterType.BW_CLEAN -> Icons.Default.MonochromePhotos
                                FilterType.GRAYSCALE -> Icons.Default.ColorLens
                                FilterType.WARM -> Icons.Default.WbSunny
                                FilterType.VIBRANT -> Icons.Default.Tune
                                FilterType.INVERT -> Icons.Default.InvertColors
                            }

                            FilterChip(
                                selected = isSelected,
                                onClick = { onFilterChanged(filter) },
                                label = { Text(filter.displayName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) FmRed else FmSlateMuted
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FmRedLight,
                                    selectedLabelColor = FmRed,
                                    containerColor = Color(0xFFF1F5F9),
                                    labelColor = FmSlateDark
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Pages Filmstrip Bar with Direct Camera & Gallery Add Buttons!
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Action Buttons
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Direct Camera Add Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = FmRedLight,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onAddFromCamera() }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.AddAPhoto,
                                        contentDescription = "Camera Add",
                                        tint = FmRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Camera", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FmRed)
                                }
                            }

                            // Gallery Add Button
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        galleryPickerLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Default.Collections,
                                        contentDescription = "Gallery Add",
                                        tint = FmSlate,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text("Gallery", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = FmSlate)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Filmstrip Thumbnails
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            itemsIndexed(pages) { index, page ->
                                val isPageSelected = index == selectedPageIndex
                                Box(
                                    modifier = Modifier
                                        .size(width = 46.dp, height = 58.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            width = if (isPageSelected) 2.5.dp else 1.dp,
                                            color = if (isPageSelected) FmRed else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .background(Color(0xFFE2E8F0))
                                        .clickable { onSelectPage(index) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = if (isPageSelected) FmRed else FmSlateDark
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Save and Generate PDF Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { if (!isGenerating) showSaveDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null, tint = FmRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF Document", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OutlinedTextField(
                        value = docTitle,
                        onValueChange = { docTitle = it },
                        label = { Text("Document Name") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Text("Page Format:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FmSlateDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfPageSize.entries.forEach { size ->
                            FilterChip(
                                selected = selectedPageSize == size,
                                onClick = { selectedPageSize = size },
                                label = { Text(size.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FmRedLight,
                                    selectedLabelColor = FmRed
                                )
                            )
                        }
                    }

                    Text("Quality & Compression:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = FmSlateDark)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PdfQuality.entries.forEach { qual ->
                            FilterChip(
                                selected = selectedQuality == qual,
                                onClick = { selectedQuality = qual },
                                label = { Text(qual.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = FmRedLight,
                                    selectedLabelColor = FmRed
                                )
                            )
                        }
                    }

                    if (isGenerating) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            LinearProgressIndicator(
                                progress = { generationProgress },
                                color = FmRed,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Compiling PDF pages...", fontSize = 12.sp, color = FmSlateMuted)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveAndGeneratePdf(docTitle, selectedPageSize, selectedQuality)
                    },
                    enabled = !isGenerating && docTitle.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = FmRed),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isGenerating) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Create & Save")
                    }
                }
            },
            dismissButton = {
                if (!isGenerating) {
                    TextButton(onClick = { showSaveDialog = false }) {
                        Text("Cancel", color = FmSlate)
                    }
                }
            },
            containerColor = FmPureWhite,
            shape = RoundedCornerShape(20.dp)
        )
    }
}
