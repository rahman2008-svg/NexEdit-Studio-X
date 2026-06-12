package com.example.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.database.ProjectRepository
import com.example.database.SavedProject
import com.example.model.DrawStroke
import com.example.model.EditState
import com.example.model.FilterType
import com.example.util.ImageProcessor
import com.example.util.StockGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface SaveState {
    object Idle : SaveState
    object Loading : SaveState
    data class Success(val path: String) : SaveState
    data class Error(val message: String) : SaveState
}

class PhotoEditorViewModel(private val repository: ProjectRepository) : ViewModel() {

    // Database flow
    val savedProjects: StateFlow<List<SavedProject>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Image flows
    private val _originalBitmap = MutableStateFlow<Bitmap?>(null)
    val originalBitmap: StateFlow<Bitmap?> = _originalBitmap.asStateFlow()

    private val _processedBitmap = MutableStateFlow<Bitmap?>(null)
    val processedBitmap: StateFlow<Bitmap?> = _processedBitmap.asStateFlow()

    // Editor state
    private val _currentEditState = MutableStateFlow(EditState())
    val currentEditState: StateFlow<EditState> = _currentEditState.asStateFlow()

    // Separate blur radius to keep state manipulation simple
    private val _blurRadius = MutableStateFlow(0)
    val blurRadius: StateFlow<Int> = _blurRadius.asStateFlow()

    // Undo/Redo historical stacks
    private val undoStack = mutableListOf<EditState>()
    private val redoStack = mutableListOf<EditState>()
    
    // Track blur in undo/redo too
    private val undoBlurStack = mutableListOf<Int>()
    private val redoBlurStack = mutableListOf<Int>()

    // Save status
    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    // Loading status
    private val _isLoadingImage = MutableStateFlow(false)
    val isLoadingImage: StateFlow<Boolean> = _isLoadingImage.asStateFlow()

    private var renderJob: Job? = null

    // Load Uri Image
    fun loadSourceUri(context: Context, uri: Uri) {
        _isLoadingImage.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = ImageProcessor.loadBitmapFromUri(context, uri, 1200)
            _originalBitmap.value = bitmap
            // Reset state
            _currentEditState.value = EditState()
            _blurRadius.value = 0
            undoStack.clear()
            redoStack.clear()
            undoBlurStack.clear()
            redoBlurStack.clear()
            _isLoadingImage.value = false
            triggerRender()
        }
    }

    // Load Stock programmatically
    fun loadStockTheme(theme: StockGenerator.StockTheme) {
        _isLoadingImage.value = true
        viewModelScope.launch(Dispatchers.Default) {
            val bitmap = StockGenerator.generateStockBitmap(theme)
            _originalBitmap.value = bitmap
            // Reset states
            _currentEditState.value = EditState()
            _blurRadius.value = 0
            undoStack.clear()
            redoStack.clear()
            undoBlurStack.clear()
            redoBlurStack.clear()
            _isLoadingImage.value = false
            triggerRender()
        }
    }

    fun clearActiveImage() {
        _originalBitmap.value = null
        _processedBitmap.value = null
        _currentEditState.value = EditState()
        _blurRadius.value = 0
        undoStack.clear()
        redoStack.clear()
        undoBlurStack.clear()
        redoBlurStack.clear()
        _saveState.value = SaveState.Idle
    }

    // Slider adjustment updates (Temporary, rendered live)
    fun updateBrightness(brightness: Float) {
        _currentEditState.value = _currentEditState.value.copy(brightness = brightness)
        triggerRender()
    }

    fun updateContrast(contrast: Float) {
        _currentEditState.value = _currentEditState.value.copy(contrast = contrast)
        triggerRender()
    }

    fun updateSaturation(saturation: Float) {
        _currentEditState.value = _currentEditState.value.copy(saturation = saturation)
        triggerRender()
    }

    fun updateBlur(radius: Int) {
        _blurRadius.value = radius
        triggerRender()
    }

    fun applyFilter(filter: FilterType) {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(selectedFilter = filter)
        triggerRender()
    }

    // Rotate & Flip
    fun rotate90() {
        commitHistoryChange()
        var newRot = _currentEditState.value.rotation + 90f
        if (newRot >= 360f) newRot = 0f
        _currentEditState.value = _currentEditState.value.copy(rotation = newRot)
        triggerRender()
    }

    fun flipHorizontal() {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(
            isFlippedHorizontally = !_currentEditState.value.isFlippedHorizontally
        )
        triggerRender()
    }

    fun flipVertical() {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(
            isFlippedVertically = !_currentEditState.value.isFlippedVertically
        )
        triggerRender()
    }

    // Drawing Strokes
    fun addStroke(stroke: DrawStroke) {
        commitHistoryChange()
        val currentStrokes = _currentEditState.value.strokes.toMutableList()
        currentStrokes.add(stroke)
        _currentEditState.value = _currentEditState.value.copy(strokes = currentStrokes)
        triggerRender()
    }

    fun clearDrawings() {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(strokes = emptyList())
        triggerRender()
    }

    // Crop update (Applies non-destructively)
    fun applyCrop(left: Float, top: Float, right: Float, bottom: Float) {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(
            cropLeft = left.coerceIn(0f, 0.9f),
            cropTop = top.coerceIn(0f, 0.9f),
            cropRight = right.coerceIn(left + 0.1f, 1f),
            cropBottom = bottom.coerceIn(top + 0.1f, 1f)
        )
        triggerRender()
    }

    fun resetCrop() {
        commitHistoryChange()
        _currentEditState.value = _currentEditState.value.copy(
            cropLeft = 0f,
            cropTop = 0f,
            cropRight = 1f,
            cropBottom = 1f
        )
        triggerRender()
    }

    // Save state history stack before a destructive transformation
    fun commitHistoryChange() {
        undoStack.add(_currentEditState.value)
        undoBlurStack.add(_blurRadius.value)
        
        redoStack.clear()
        redoBlurStack.clear()
        
        if (undoStack.size > 15) {
            undoStack.removeAt(0)
            undoBlurStack.removeAt(0)
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prevEdit = undoStack.removeAt(undoStack.size - 1)
            val prevBlur = undoBlurStack.removeAt(undoBlurStack.size - 1)

            redoStack.add(_currentEditState.value)
            redoBlurStack.add(_blurRadius.value)

            _currentEditState.value = prevEdit
            _blurRadius.value = prevBlur
            triggerRender()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextEdit = redoStack.removeAt(redoStack.size - 1)
            val nextBlur = redoBlurStack.removeAt(redoBlurStack.size - 1)

            undoStack.add(_currentEditState.value)
            undoBlurStack.add(_blurRadius.value)

            _currentEditState.value = nextEdit
            _blurRadius.value = nextBlur
            triggerRender()
        }
    }

    val canUndo: Boolean get() = undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // Live Render Triggering (Debounced / Thread-Cancelled for exceptional speed)
    fun triggerRender() {
        renderJob?.cancel()
        renderJob = viewModelScope.launch(Dispatchers.Default) {
            val base = _originalBitmap.value ?: return@launch
            val rendered = ImageProcessor.processBitmap(base, _currentEditState.value, _blurRadius.value)
            _processedBitmap.value = rendered
        }
    }

    // Save To Gallery (Scoped Storage + Db insertion)
    fun saveImageToGallery(context: Context, customName: String = "") {
        val original = _originalBitmap.value ?: return
        _saveState.value = SaveState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Compile the pipeline on full original resource
                val finished = ImageProcessor.processBitmap(original, _currentEditState.value, _blurRadius.value)
                
                val userTitle = customName.trim().ifEmpty { "NexEdit_${System.currentTimeMillis()}" }
                val fullname = "$userTitle.jpg"
                
                val resolver = context.contentResolver
                val imageValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fullname)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/NexEditStudio")
                        put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                val targetUri = resolver.insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageValues)
                if (targetUri != null) {
                    resolver.openOutputStream(targetUri).use { os ->
                        if (os != null) {
                            finished.compress(Bitmap.CompressFormat.JPEG, 95, os)
                        }
                    }

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                        imageValues.clear()
                        imageValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                        resolver.update(targetUri, imageValues, null, null)
                    }

                    val savedLoc = targetUri.toString()
                    val pLog = SavedProject(name = userTitle, filePath = savedLoc)
                    repository.insert(pLog)

                    _saveState.value = SaveState.Success(savedLoc)
                } else {
                    // Fallback to cache directory writing for unit sandbox files
                    val picturesDirectory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES)
                    val outputfile = java.io.File(picturesDirectory, fullname)
                    java.io.FileOutputStream(outputfile).use { outStream ->
                        finished.compress(Bitmap.CompressFormat.JPEG, 95, outStream)
                    }
                    val pathLoc = outputfile.absolutePath
                    val pLog = SavedProject(name = userTitle, filePath = pathLoc)
                    repository.insert(pLog)

                    _saveState.value = SaveState.Success(pathLoc)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _saveState.value = SaveState.Error(e.localizedMessage ?: "Persistence Writing Failure")
            }
        }
    }

    fun deleteProject(project: SavedProject) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.delete(project)
        }
    }

    fun resetSaveState() {
        _saveState.value = SaveState.Idle
    }
}

class PhotoEditorViewModelFactory(private val repository: ProjectRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PhotoEditorViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PhotoEditorViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
