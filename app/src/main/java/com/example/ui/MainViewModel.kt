package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProjectRepository
import com.example.model.DocPage
import com.example.model.FilterType
import com.example.model.PdfPageSize
import com.example.model.PdfProject
import com.example.model.PdfQuality
import com.example.util.ImageFilterUtil
import com.example.util.PdfEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

sealed class Screen {
    object Home : Screen()
    object CameraScan : Screen()
    object PageEditor : Screen()
    data class PdfViewer(val projectId: String) : Screen()
    object Settings : Screen()
    object EasterEgg : Screen()
}

enum class SortOrder(val displayName: String) {
    NEWEST("Newest"),
    OLDEST("Oldest"),
    NAME("Name A-Z"),
    SIZE("File Size")
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProjectRepository(application)
    val projects: StateFlow<List<PdfProject>> = repository.projects

    private val _currentScreen = MutableStateFlow<Screen>(Screen.Home)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _activeProject = MutableStateFlow<PdfProject?>(null)
    val activeProject: StateFlow<PdfProject?> = _activeProject.asStateFlow()

    private val _selectedPageIndex = MutableStateFlow<Int>(0)
    val selectedPageIndex: StateFlow<Int> = _selectedPageIndex.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private val _generationProgress = MutableStateFlow(0f)
    val generationProgress: StateFlow<Float> = _generationProgress.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.NEWEST)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _snackbarEvent = MutableSharedFlow<String>()
    val snackbarEvent: SharedFlow<String> = _snackbarEvent.asSharedFlow()

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
    }

    fun setSelectedPageIndex(index: Int) {
        _selectedPageIndex.value = index
    }

    fun startNewProject() {
        val newProj = PdfProject(
            id = UUID.randomUUID().toString(),
            title = "Document ${System.currentTimeMillis() % 10000}",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            pages = emptyList()
        )
        _activeProject.value = newProj
        _selectedPageIndex.value = 0
        navigateTo(Screen.CameraScan)
    }

    fun editProject(project: PdfProject) {
        _activeProject.value = project
        _selectedPageIndex.value = 0
        navigateTo(Screen.PageEditor)
    }

    fun openViewer(projectId: String) {
        navigateTo(Screen.PdfViewer(projectId))
    }

    fun addPageFromBitmap(bitmap: Bitmap) {
        viewModelScope.launch {
            val imagePath = ImageFilterUtil.saveImageToFile(getApplication(), bitmap)
            val current = _activeProject.value ?: PdfProject(
                title = "Scan ${System.currentTimeMillis() % 10000}"
            )
            val newPage = DocPage(
                originalImagePath = imagePath,
                filterType = FilterType.ORIGINAL,
                pageNumber = current.pages.size + 1
            )
            val updatedPages = current.pages + newPage
            _activeProject.value = current.copy(pages = updatedPages)
            _selectedPageIndex.value = updatedPages.size - 1
        }
    }

    fun addPagesFromUris(uris: List<Uri>) {
        viewModelScope.launch {
            val context = getApplication<Application>()
            val newPages = mutableListOf<DocPage>()
            val current = _activeProject.value ?: PdfProject(
                title = "Document ${System.currentTimeMillis() % 10000}"
            )
            val startIndex = current.pages.size

            for ((i, uri) in uris.withIndex()) {
                try {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val bitmap = BitmapFactory.decodeStream(input)
                        if (bitmap != null) {
                            val path = ImageFilterUtil.saveImageToFile(context, bitmap)
                            newPages.add(
                                DocPage(
                                    originalImagePath = path,
                                    filterType = FilterType.ORIGINAL,
                                    pageNumber = startIndex + i + 1
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            if (newPages.isNotEmpty()) {
                val updatedPages = current.pages + newPages
                _activeProject.value = current.copy(pages = updatedPages)
                _selectedPageIndex.value = updatedPages.size - 1
                navigateTo(Screen.PageEditor)
                _snackbarEvent.emit("Added ${newPages.size} page(s)")
            }
        }
    }

    fun updateSelectedPageFilter(filter: FilterType) {
        val current = _activeProject.value ?: return
        val idx = _selectedPageIndex.value
        if (idx in current.pages.indices) {
            val updatedPages = current.pages.toMutableList()
            updatedPages[idx] = updatedPages[idx].copy(filterType = filter)
            _activeProject.value = current.copy(pages = updatedPages)
        }
    }

    fun applyFilterToAllPages(filter: FilterType) {
        val current = _activeProject.value ?: return
        val updatedPages = current.pages.map { it.copy(filterType = filter) }
        _activeProject.value = current.copy(pages = updatedPages)
        viewModelScope.launch {
            _snackbarEvent.emit("Applied ${filter.displayName} to all pages")
        }
    }

    fun rotateSelectedPage() {
        val current = _activeProject.value ?: return
        val idx = _selectedPageIndex.value
        if (idx in current.pages.indices) {
            val updatedPages = current.pages.toMutableList()
            val nextRotation = (updatedPages[idx].rotationDegrees + 90) % 360
            updatedPages[idx] = updatedPages[idx].copy(rotationDegrees = nextRotation)
            _activeProject.value = current.copy(pages = updatedPages)
        }
    }

    fun deleteSelectedPage() {
        val current = _activeProject.value ?: return
        val idx = _selectedPageIndex.value
        if (idx in current.pages.indices) {
            val updatedPages = current.pages.toMutableList()
            val removed = updatedPages.removeAt(idx)
            File(removed.originalImagePath).delete()
            // Re-number
            val renumbered = updatedPages.mapIndexed { index, page -> page.copy(pageNumber = index + 1) }
            _activeProject.value = current.copy(pages = renumbered)
            _selectedPageIndex.value = (idx - 1).coerceAtLeast(0)
            viewModelScope.launch {
                _snackbarEvent.emit("Page removed")
            }
        }
    }

    fun moveSelectedPage(step: Int) {
        val current = _activeProject.value ?: return
        val idx = _selectedPageIndex.value
        val newIdx = idx + step
        if (idx in current.pages.indices && newIdx in current.pages.indices) {
            val updatedPages = current.pages.toMutableList()
            val item = updatedPages.removeAt(idx)
            updatedPages.add(newIdx, item)
            val renumbered = updatedPages.mapIndexed { index, page -> page.copy(pageNumber = index + 1) }
            _activeProject.value = current.copy(pages = renumbered)
            _selectedPageIndex.value = newIdx
        }
    }

    fun updateProjectSettings(title: String, pageSize: PdfPageSize, quality: PdfQuality) {
        val current = _activeProject.value ?: return
        _activeProject.value = current.copy(
            title = title,
            pageSize = pageSize,
            quality = quality
        )
    }

    fun saveAndGeneratePdf(title: String, pageSize: PdfPageSize, quality: PdfQuality) {
        val current = _activeProject.value ?: return
        if (current.pages.isEmpty()) {
            viewModelScope.launch { _snackbarEvent.emit("Cannot save an empty document! Add at least 1 page.") }
            return
        }

        viewModelScope.launch {
            _isGenerating.value = true
            _generationProgress.value = 0.1f
            try {
                val projToSave = current.copy(
                    title = title.ifBlank { "Document ${System.currentTimeMillis() % 10000}" },
                    pageSize = pageSize,
                    quality = quality
                )

                val (pdfPath, size) = PdfEngine.generatePdf(
                    getApplication(),
                    projToSave
                ) { curr, tot ->
                    _generationProgress.value = curr.toFloat() / tot.toFloat()
                }

                val firstPage = projToSave.pages.firstOrNull()
                val thumbPath = firstPage?.let {
                    ImageFilterUtil.createThumbnail(
                        getApplication(),
                        it.originalImagePath,
                        it.filterType,
                        it.rotationDegrees
                    )
                }

                val finalProject = projToSave.copy(
                    pdfFilePath = pdfPath,
                    thumbnailPath = thumbPath,
                    fileSizeBytes = size,
                    modifiedAt = System.currentTimeMillis()
                )

                repository.saveProject(finalProject)
                _activeProject.value = finalProject
                _snackbarEvent.emit("PDF '${finalProject.title}' generated successfully!")
                navigateTo(Screen.Home)
            } catch (e: Exception) {
                e.printStackTrace()
                _snackbarEvent.emit("Failed to generate PDF: ${e.message}")
            } finally {
                _isGenerating.value = false
            }
        }
    }

    fun exportPdfToDownloads(project: PdfProject) {
        viewModelScope.launch {
            val result = PdfEngine.exportPdfToDownloads(getApplication(), project)
            result.onSuccess { msg ->
                _snackbarEvent.emit(msg)
            }.onFailure { err ->
                _snackbarEvent.emit("Download failed: ${err.message}")
            }
        }
    }

    fun sharePdf(project: PdfProject) {
        val success = PdfEngine.sharePdf(getApplication(), project)
        if (!success) {
            viewModelScope.launch {
                _snackbarEvent.emit("Could not open share dialog for this PDF.")
            }
        }
    }

    fun duplicateProject(projectId: String) {
        viewModelScope.launch {
            repository.duplicateProject(projectId)
            _snackbarEvent.emit("Project duplicated")
        }
    }

    fun renameProject(projectId: String, newTitle: String) {
        viewModelScope.launch {
            repository.renameProject(projectId, newTitle)
            _snackbarEvent.emit("Project renamed to '$newTitle'")
        }
    }

    fun deleteProject(projectId: String) {
        viewModelScope.launch {
            repository.deleteProject(projectId)
            _snackbarEvent.emit("Project deleted")
        }
    }
}
