package com.example.ui.home

import android.net.Uri
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.PdfProject
import com.example.ui.SortOrder
import com.example.ui.theme.FmBackground
import com.example.ui.theme.FmBorder
import com.example.ui.theme.FmPureWhite
import com.example.ui.theme.FmRed
import com.example.ui.theme.FmRedLight
import com.example.ui.theme.FmSlate
import com.example.ui.theme.FmSlateDark
import com.example.ui.theme.FmSlateLight
import com.example.ui.theme.FmSlateMuted
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    projects: List<PdfProject>,
    searchQuery: String,
    sortOrder: SortOrder,
    onSearchChanged: (String) -> Unit,
    onSortChanged: (SortOrder) -> Unit,
    onStartScan: () -> Unit,
    onImportGallery: (List<Uri>) -> Unit,
    onOpenViewer: (String) -> Unit,
    onEditProject: (PdfProject) -> Unit,
    onDownloadPdf: (PdfProject) -> Unit,
    onSharePdf: (PdfProject) -> Unit,
    onDuplicateProject: (String) -> Unit,
    onRenameProject: (String, String) -> Unit,
    onDeleteProject: (String) -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current

    val galleryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportGallery(uris)
        }
    }

    var projectToRename by remember { mutableStateOf<PdfProject?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var projectToDelete by remember { mutableStateOf<PdfProject?>(null) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Filter and sort projects
    val filteredProjects = projects.filter {
        searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true)
    }.let { list ->
        when (sortOrder) {
            SortOrder.NEWEST -> list.sortedByDescending { it.modifiedAt }
            SortOrder.OLDEST -> list.sortedBy { it.modifiedAt }
            SortOrder.NAME -> list.sortedBy { it.title.lowercase() }
            SortOrder.SIZE -> list.sortedByDescending { it.fileSizeBytes }
        }
    }

    val totalPages = projects.sumOf { it.pageCount }
    val totalBytes = projects.sumOf { it.fileSizeBytes }
    val formattedTotalStorage = when {
        totalBytes < 1024 -> "$totalBytes B"
        totalBytes < 1024 * 1024 -> String.format("%.1f KB", totalBytes / 1024.0)
        else -> String.format("%.2f MB", totalBytes / (1024.0 * 1024.0))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.fmpdf_logo),
                            contentDescription = "FmPDF Logo",
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "FmPDF",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 20.sp,
                                    color = FmSlateDark,
                                    letterSpacing = (-0.5).sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = FmRedLight,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        color = FmRed,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Premium PDF Maker & Scanner",
                                fontSize = 11.sp,
                                color = FmSlateMuted
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = FmSlateDark
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = FmPureWhite)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onStartScan,
                icon = { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White) },
                text = { Text("Scan Document", fontWeight = FontWeight.Bold, color = Color.White) },
                containerColor = FmRed,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.shadow(8.dp, RoundedCornerShape(20.dp))
            )
        },
        containerColor = FmBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // Quick Creation Action Cards
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Camera Scan Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onStartScan() },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFEE2E2))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(FmRedLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null, tint = FmRed, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Camera Scan", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FmSlateDark)
                            Text("Direct capture to PDF", fontSize = 12.sp, color = FmSlateMuted)
                        }
                    }

                    // Gallery Import Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                galleryPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = FmPureWhite),
                        border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Collections, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("Import Photos", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = FmSlateDark)
                            Text("From device gallery", fontSize = 12.sp, color = FmSlateMuted)
                        }
                    }
                }
            }

            // Stats Bar
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    color = FmPureWhite,
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${projects.size}", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FmSlateDark)
                            Text("Documents", fontSize = 11.sp, color = FmSlateMuted)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(FmBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalPages", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FmRed)
                            Text("Total Pages", fontSize = 11.sp, color = FmSlateMuted)
                        }
                        Box(modifier = Modifier.width(1.dp).height(24.dp).background(FmBorder))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(formattedTotalStorage, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = FmSlateDark)
                            Text("Stored Size", fontSize = 11.sp, color = FmSlateMuted)
                        }
                    }
                }
            }

            // Search and Filter Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChanged,
                        placeholder = { Text("Search PDF projects...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = FmSlateMuted) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChanged("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = FmSlateMuted)
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = FmPureWhite,
                            unfocusedContainerColor = FmPureWhite,
                            focusedBorderColor = FmRed,
                            unfocusedBorderColor = FmBorder
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(FmPureWhite)
                                .border(1.dp, FmBorder, RoundedCornerShape(16.dp))
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Sort", tint = FmSlateDark)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(order.displayName, fontWeight = if (sortOrder == order) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        onSortChanged(order)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Projects Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Saved Projects (${filteredProjects.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = FmSlateDark
                    )
                    Text(
                        text = "Sorted by ${sortOrder.displayName}",
                        fontSize = 12.sp,
                        color = FmSlateMuted
                    )
                }
            }

            // Projects List or Empty State
            if (filteredProjects.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No documents match '$searchQuery'" else "No PDF projects yet",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = FmSlateDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap 'Scan Document' below to create your first PDF!",
                            fontSize = 13.sp,
                            color = FmSlateMuted
                        )
                    }
                }
            } else {
                items(filteredProjects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onOpenViewer = { onOpenViewer(project.id) },
                        onEdit = { onEditProject(project) },
                        onDownload = { onDownloadPdf(project) },
                        onShare = { onSharePdf(project) },
                        onRename = {
                            projectToRename = project
                            renameInput = project.title
                        },
                        onDuplicate = { onDuplicateProject(project.id) },
                        onDelete = { projectToDelete = project }
                    )
                }
            }
        }
    }

    // Rename Dialog
    if (projectToRename != null) {
        AlertDialog(
            onDismissRequest = { projectToRename = null },
            title = { Text("Rename Project", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Document Title") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val proj = projectToRename
                        if (proj != null && renameInput.isNotBlank()) {
                            onRenameProject(proj.id, renameInput.trim())
                        }
                        projectToRename = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = FmRed)
                ) {
                    Text("Rename")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToRename = null }) {
                    Text("Cancel", color = FmSlate)
                }
            },
            containerColor = FmPureWhite
        )
    }

    // Delete Confirmation Dialog
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            title = { Text("Delete Document?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete '${projectToDelete?.title}'? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        projectToDelete?.let { onDeleteProject(it.id) }
                        projectToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = FmSlate)
                }
            },
            containerColor = FmPureWhite
        )
    }
}

@Composable
fun ProjectCard(
    project: PdfProject,
    onOpenViewer: () -> Unit,
    onEdit: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    val formattedDate = remember(project.modifiedAt) {
        val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
        sdf.format(Date(project.modifiedAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable { onOpenViewer() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = FmPureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, FmBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Document Thumbnail Preview
                Box(
                    modifier = Modifier
                        .size(width = 68.dp, height = 88.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F5F9))
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    val thumb = project.thumbnailPath
                    if (thumb != null && File(thumb).exists()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(File(thumb))
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = null,
                            tint = FmRed,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                // Document Metadata Info
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = project.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = FmSlateDark,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = FmRedLight
                        ) {
                            Text(
                                text = "${project.pageCount} Pages",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = FmRed,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = project.formattedSize,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = FmSlate,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9)
                        ) {
                            Text(
                                text = project.pageSize.name,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = FmSlateMuted,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = formattedDate,
                        fontSize = 11.sp,
                        color = FmSlateMuted
                    )
                }

                // More Menu Button
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = FmSlate)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Rename") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onRename()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color(0xFFEF4444)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFEF4444)) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row: View, Download, Share, Edit
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // View PDF
                TextButton(
                    onClick = onOpenViewer,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = FmSlateDark, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Read", color = FmSlateDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Download PDF
                TextButton(
                    onClick = onDownload,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, tint = FmRed, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download", color = FmRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Share PDF
                TextButton(
                    onClick = onShare,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", color = Color(0xFF2563EB), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                // Edit Pages / Add Camera Pages
                TextButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = FmSlate, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Edit", color = FmSlate, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
