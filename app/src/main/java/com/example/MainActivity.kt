package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MainViewModel
import com.example.ui.Screen
import com.example.ui.camera.CameraScanScreen
import com.example.ui.easteregg.Android11EasterEgg
import com.example.ui.editor.PageEditorScreen
import com.example.ui.home.HomeScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.FmPdfTheme
import com.example.ui.viewer.PdfViewerScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FmPdfTheme {
                FmPdfApp()
            }
        }
    }
}

@Composable
fun FmPdfApp(viewModel: MainViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val projects by viewModel.projects.collectAsState()
    val activeProject by viewModel.activeProject.collectAsState()
    val selectedPageIndex by viewModel.selectedPageIndex.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val generationProgress by viewModel.generationProgress.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarEvent.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        AnimatedContent(
            targetState = currentScreen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "ScreenTransition",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { screen ->
            when (screen) {
                is Screen.Home -> {
                    HomeScreen(
                        projects = projects,
                        searchQuery = searchQuery,
                        sortOrder = sortOrder,
                        onSearchChanged = { viewModel.setSearchQuery(it) },
                        onSortChanged = { viewModel.setSortOrder(it) },
                        onStartScan = { viewModel.startNewProject() },
                        onImportGallery = { uris -> viewModel.addPagesFromUris(uris) },
                        onOpenViewer = { projId -> viewModel.openViewer(projId) },
                        onEditProject = { proj -> viewModel.editProject(proj) },
                        onDownloadPdf = { proj -> viewModel.exportPdfToDownloads(proj) },
                        onSharePdf = { proj -> viewModel.sharePdf(proj) },
                        onDuplicateProject = { id -> viewModel.duplicateProject(id) },
                        onRenameProject = { id, title -> viewModel.renameProject(id, title) },
                        onDeleteProject = { id -> viewModel.deleteProject(id) },
                        onOpenSettings = { viewModel.navigateTo(Screen.Settings) }
                    )
                }

                is Screen.CameraScan -> {
                    CameraScanScreen(
                        currentPagesCount = activeProject?.pageCount ?: 0,
                        lastCapturedBitmap = null,
                        onPageCaptured = { bmp -> viewModel.addPageFromBitmap(bmp) },
                        onImportUris = { uris -> viewModel.addPagesFromUris(uris) },
                        onDoneScanning = { viewModel.navigateTo(Screen.PageEditor) },
                        onBack = {
                            if ((activeProject?.pageCount ?: 0) > 0) {
                                viewModel.navigateTo(Screen.PageEditor)
                            } else {
                                viewModel.navigateTo(Screen.Home)
                            }
                        }
                    )
                }

                is Screen.PageEditor -> {
                    PageEditorScreen(
                        project = activeProject,
                        selectedPageIndex = selectedPageIndex,
                        isGenerating = isGenerating,
                        generationProgress = generationProgress,
                        onSelectPage = { idx -> viewModel.setSelectedPageIndex(idx) },
                        onFilterChanged = { filter -> viewModel.updateSelectedPageFilter(filter) },
                        onApplyFilterToAll = { filter -> viewModel.applyFilterToAllPages(filter) },
                        onRotatePage = { viewModel.rotateSelectedPage() },
                        onDeletePage = { viewModel.deleteSelectedPage() },
                        onMovePage = { step -> viewModel.moveSelectedPage(step) },
                        onAddFromCamera = { viewModel.navigateTo(Screen.CameraScan) },
                        onAddFromGallery = { uris -> viewModel.addPagesFromUris(uris) },
                        onSaveAndGeneratePdf = { title, pageSize, quality ->
                            viewModel.saveAndGeneratePdf(title, pageSize, quality)
                        },
                        onBack = { viewModel.navigateTo(Screen.Home) }
                    )
                }

                is Screen.PdfViewer -> {
                    val proj = projects.find { it.id == screen.projectId } ?: activeProject
                    PdfViewerScreen(
                        project = proj,
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onDownload = { p -> viewModel.exportPdfToDownloads(p) },
                        onShare = { p -> viewModel.sharePdf(p) }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        onBack = { viewModel.navigateTo(Screen.Home) },
                        onOpenEasterEgg = { viewModel.navigateTo(Screen.EasterEgg) },
                        onShowMessage = { msg ->
                            scope.launch { snackbarHostState.showSnackbar(msg) }
                        }
                    )
                }

                is Screen.EasterEgg -> {
                    Android11EasterEgg(
                        onBack = { viewModel.navigateTo(Screen.Settings) }
                    )
                }
            }
        }
    }
}
