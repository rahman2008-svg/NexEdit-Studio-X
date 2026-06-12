package com.example.ui

import android.Manifest
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.GetContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.database.SavedProject
import com.example.model.DrawStroke
import com.example.model.FilterType
import com.example.util.StockGenerator
import com.example.viewmodel.PhotoEditorViewModel
import com.example.viewmodel.SaveState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorApp(viewModel: PhotoEditorViewModel) {
    val context = LocalContext.current
    val originalBmp by viewModel.originalBitmap.collectAsStateWithLifecycle()

    // Setup permission launchers for legacy / old devices
    val readPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Storage permissions required for gallery selection on older devices", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            readPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0E1012)) // Clean Minimal Dark Background
    ) {
        if (originalBmp == null) {
            HomeScreen(viewModel = viewModel)
        } else {
            ActiveEditorScreen(viewModel = viewModel)
        }
    }
}

@Composable
fun HomeScreen(viewModel: PhotoEditorViewModel) {
    val context = LocalContext.current
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val isLoadingImage by viewModel.isLoadingImage.collectAsStateWithLifecycle()

    // File selection picker launcher
    val pickerLauncher = rememberLauncherForActivityResult(contract = GetContent()) { uri: Uri? ->
        if (uri != null) {
            viewModel.loadSourceUri(context, uri)
        }
    }

    var projectToDelete by remember { mutableStateOf<SavedProject?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Camera,
                contentDescription = "App Icon",
                tint = Color(0xFFD0E4FF), // Periwinkle minimal accent
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "NexEdit Studio X",
                    color = Color(0xFFE2E2E6),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "Offline Creative Canvas",
                    color = Color(0xFFC2C7CF),
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Pick Actions
        Text(
            text = "Create New Project",
            color = Color(0xFFE2E2E6),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Import Gallery Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .clickable { pickerLauncher.launch("image/*") }
                    .testTag("gallery_picker_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoLibrary,
                        contentDescription = "Gallery",
                        tint = Color(0xFFD0E4FF),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Open Gallery",
                        color = Color(0xFFE2E2E6),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Demo Templates Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
                    .testTag("demo_templates_card"),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                var expandedStockMenu by remember { mutableStateOf(false) }

                Box(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { expandedStockMenu = true }
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Presets",
                            tint = Color(0xFFD0E4FF),
                            modifier = Modifier.size(34.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Artistic Presets",
                            color = Color(0xFFE2E2E6),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    DropdownMenu(
                        expanded = expandedStockMenu,
                        onDismissRequest = { expandedStockMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1C1E))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Neon Cyber Sun", color = Color(0xFFE2E2E6)) },
                            leadingIcon = { Icon(Icons.Default.Waves, contentDescription = null, tint = Color(0xFFFF007F)) },
                            onClick = {
                                expandedStockMenu = false
                                viewModel.loadStockTheme(StockGenerator.StockTheme.NEON_CYBER)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Warm Mountain Pine", color = Color(0xFFE2E2E6)) },
                            leadingIcon = { Icon(Icons.Default.Terrain, contentDescription = null, tint = Color(0xFFFC5C7D)) },
                            onClick = {
                                expandedStockMenu = false
                                viewModel.loadStockTheme(StockGenerator.StockTheme.AUTUMN_MOUNTAIN)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Celestial Cosmic Aurora", color = Color(0xFFE2E2E6)) },
                            leadingIcon = { Icon(Icons.Default.Stars, contentDescription = null, tint = Color(0xFF00FF87)) },
                            onClick = {
                                expandedStockMenu = false
                                viewModel.loadStockTheme(StockGenerator.StockTheme.COSMIC_AURORA)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Modern Brutalist Abstract", color = Color(0xFFE2E2E6)) },
                            leadingIcon = { Icon(Icons.Default.Category, contentDescription = null, tint = Color(0xFFFFCC00)) },
                            onClick = {
                                expandedStockMenu = false
                                viewModel.loadStockTheme(StockGenerator.StockTheme.MODERN_BRUTALIST)
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Saved Creations List
        Text(
            text = "Studio Projects (${savedProjects.size})",
            color = Color(0xFFE2E2E6),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        if (isLoadingImage) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFD0E4FF))
            }
        } else if (savedProjects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1A1C1E))
                    .border(1.dp, Color(0xFF2C2E33), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = "Empty DB",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No saved creations yet",
                        color = Color(0xFFE2E2E6),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Your offline modifications and exports will safely display here for fast reference.",
                        color = Color(0xFFC2C7CF),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("saved_creations_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(savedProjects, key = { it.id }) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1C1E)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0E1012)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Image,
                                    contentDescription = "Photo Preview icon",
                                    tint = Color(0xFFD0E4FF)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    color = Color(0xFFE2E2E6),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Saved in NexEdit Folder",
                                    color = Color(0xFFC2C7CF),
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = { projectToDelete = item },
                                modifier = Modifier.testTag("delete_project_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete project",
                                    tint = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog Box
    if (projectToDelete != null) {
        AlertDialog(
            onDismissRequest = { projectToDelete = null },
            containerColor = Color(0xFF1A1C1E),
            title = { Text("Delete Studio Project?", color = Color(0xFFE2E2E6)) },
            text = { Text("This removes the edit record logs from your studio timeline list.", color = Color(0xFFC2C7CF)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        projectToDelete?.let { viewModel.deleteProject(it) }
                        projectToDelete = null
                        Toast.makeText(context, "Project removed", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { projectToDelete = null }) {
                    Text("Cancel", color = Color(0xFFE2E2E6))
                }
            }
        )
    }
}

// Editor Categories enum
enum class EditorTab {
    FILTERS,
    ADJUST,
    CROP,
    DRAW
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveEditorScreen(viewModel: PhotoEditorViewModel) {
    val context = LocalContext.current
    val processedBmp by viewModel.processedBitmap.collectAsStateWithLifecycle()
    val editState by viewModel.currentEditState.collectAsStateWithLifecycle()
    val blurVal by viewModel.blurRadius.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()

    var activeTab by remember { mutableStateOf(EditorTab.FILTERS) }
    var exportDialogOpen by remember { mutableStateOf(false) }
    var saveNameInput by remember { mutableStateOf("") }

    // Drawing parameters
    var activeBrushColor by remember { mutableStateOf(0xFFFF007F) } // Hex value Long
    var activeBrushSize by remember { mutableStateOf(16f) }
    val localDrawPoints = remember { mutableStateListOf<Offset>() }
    var drawCanvasSize by remember { mutableStateOf(IntSize.Zero) }

    // Direct local alerts of save state changes
    LaunchedEffect(saveState) {
        when (saveState) {
            is SaveState.Success -> {
                exportDialogOpen = false
                Toast.makeText(context, "Successfully exported to Gallery!", Toast.LENGTH_LONG).show()
                viewModel.resetSaveState()
            }
            is SaveState.Error -> {
                Toast.makeText(context, "Export Error: ${(saveState as SaveState.Error).message}", Toast.LENGTH_SHORT).show()
                viewModel.resetSaveState()
            }
            else -> {}
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFF000000), // Black central image backdrop
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF0E1012),
                        titleContentColor = Color(0xFFE2E2E6)
                    ),
                    title = {
                        Text(
                            text = "NEXEDIT STUDIO X",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.5.sp,
                            color = Color(0xFFE2E2E6)
                        )
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.clearActiveImage() },
                            modifier = Modifier.testTag("editor_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Leave Editor",
                                tint = Color(0xFFE2E2E6)
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = viewModel.canUndo,
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo step",
                                tint = if (viewModel.canUndo) Color(0xFFE2E2E6) else Color(0xFF2C2E33)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = viewModel.canRedo,
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Redo,
                                contentDescription = "Redo step",
                                tint = if (viewModel.canRedo) Color(0xFFE2E2E6) else Color(0xFF2C2E33)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFD0E4FF),
                                contentColor = Color(0xFF003258)
                            ),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .testTag("export_trigger_btn"),
                            onClick = {
                                saveNameInput = "NexEdit_${System.currentTimeMillis() / 1000}"
                                exportDialogOpen = true
                            }
                        ) {
                            Text(
                                text = "SAVE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                )
                HorizontalDivider(color = Color(0xFF2C2E33), thickness = 1.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = Color(0xFF2C2E33), thickness = 1.dp)
                Column(modifier = Modifier.background(Color(0xFF1A1C1E))) {
                    // Secondary visual editor controller card based on active main tab selection
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .heightIn(min = 100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        when (activeTab) {
                            EditorTab.FILTERS -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Creative Color Filters", color = Color(0xFFC2C7CF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val filtersMap = listOf(
                                            "Original" to FilterType.NONE,
                                            "Vintage Ink" to FilterType.VINTAGE,
                                            "Classic B&W" to FilterType.MONOCHROME,
                                            "Cozy Sun" to FilterType.WARM_TONE,
                                            "Breezy Frost" to FilterType.COOL_TONE,
                                            "Glow Dream" to FilterType.GRAIN_DREAM,
                                            "Sharp Boost" to FilterType.SHARPNESS_BOOST
                                        )
                                        items(filtersMap) { (name, itemType) ->
                                            val isActive = editState.selectedFilter == itemType
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isActive) Color(0xFFD0E4FF) else Color(0xFF0E1012)
                                                ),
                                                border = if (isActive) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier
                                                    .clickable { viewModel.applyFilter(itemType) }
                                                    .testTag("filter_$name")
                                            ) {
                                                Text(
                                                    text = name,
                                                    color = if (isActive) Color(0xFF003258) else Color(0xFFE2E2E6),
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            EditorTab.ADJUST -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    var selectedAdjustIndex by remember { mutableStateOf(0) } // 0: Bright, 1: Contrast, 2: Saturation, 3: Blur
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        val adjustTabs = listOf("Lightness", "Contrast", "Saturation", "Blur Focus")
                                        adjustTabs.forEachIndexed { idx, label ->
                                            val isSel = selectedAdjustIndex == idx
                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isSel) Color(0xFFD0E4FF) else Color(0xFF0E1012)
                                                ),
                                                border = if (isSel) null else androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable { selectedAdjustIndex = idx },
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = if (isSel) Color(0xFF003258) else Color(0xFFE2E2E6),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(vertical = 8.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    when (selectedAdjustIndex) {
                                        0 -> { // Brightness
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.LightMode, contentDescription = null, tint = Color(0xFFE2E2E6))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Slider(
                                                    value = editState.brightness,
                                                    onValueChange = { viewModel.updateBrightness(it) },
                                                    valueRange = -100f..100f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color(0xFFD0E4FF),
                                                        activeTrackColor = Color(0xFFD0E4FF),
                                                        inactiveTrackColor = Color(0xFF2C2E33)
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("${editState.brightness.toInt()}", color = Color(0xFFE2E2E6), fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                            }
                                        }
                                        1 -> { // Contrast
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Contrast, contentDescription = null, tint = Color(0xFFE2E2E6))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Slider(
                                                    value = editState.contrast,
                                                    onValueChange = { viewModel.updateContrast(it) },
                                                    valueRange = 0.3f..2.0f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color(0xFFD0E4FF),
                                                        activeTrackColor = Color(0xFFD0E4FF),
                                                        inactiveTrackColor = Color(0xFF2C2E33)
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(String.format("%.2f", editState.contrast), color = Color(0xFFE2E2E6), fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                            }
                                        }
                                        2 -> { // Saturation
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Palette, contentDescription = null, tint = Color(0xFFE2E2E6))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Slider(
                                                    value = editState.saturation,
                                                    onValueChange = { viewModel.updateSaturation(it) },
                                                    valueRange = 0.0f..2.0f,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color(0xFFD0E4FF),
                                                        activeTrackColor = Color(0xFFD0E4FF),
                                                        inactiveTrackColor = Color(0xFF2C2E33)
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(String.format("%.2f", editState.saturation), color = Color(0xFFE2E2E6), fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                            }
                                        }
                                        3 -> { // Blur
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.BlurOn, contentDescription = null, tint = Color(0xFFE2E2E6))
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Slider(
                                                    value = blurVal.toFloat(),
                                                    onValueChange = { viewModel.updateBlur(it.toInt()) },
                                                    valueRange = 0f..15f,
                                                    steps = 15,
                                                    colors = SliderDefaults.colors(
                                                        thumbColor = Color(0xFFD0E4FF),
                                                        activeTrackColor = Color(0xFFD0E4FF),
                                                        inactiveTrackColor = Color(0xFF2C2E33)
                                                    ),
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text("$blurVal px", color = Color(0xFFE2E2E6), fontSize = 12.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.End)
                                            }
                                        }
                                    }
                                }
                            }

                            EditorTab.CROP -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Non-destructive Image Cropping Margins", color = Color(0xFFC2C7CF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Bounding Box Sliders
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Left Margin: ${(editState.cropLeft * 100).toInt()}%", color = Color(0xFFE2E2E6), fontSize = 10.sp)
                                            Slider(
                                                value = editState.cropLeft,
                                                onValueChange = { l -> viewModel.applyCrop(l, editState.cropTop, editState.cropRight, editState.cropBottom) },
                                                valueRange = 0f..0.8f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFFD0E4FF),
                                                    activeTrackColor = Color(0xFFD0E4FF),
                                                    inactiveTrackColor = Color(0xFF2C2E33)
                                                )
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Right Margin: ${(editState.cropRight * 100).toInt()}%", color = Color(0xFFE2E2E6), fontSize = 10.sp)
                                            Slider(
                                                value = editState.cropRight,
                                                onValueChange = { r -> viewModel.applyCrop(editState.cropLeft, editState.cropTop, r, editState.cropBottom) },
                                                valueRange = 0.2f..1.0f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFFD0E4FF),
                                                    activeTrackColor = Color(0xFFD0E4FF),
                                                    inactiveTrackColor = Color(0xFF2C2E33)
                                                )
                                            )
                                        }
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Button(
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                                            shape = RoundedCornerShape(8.dp),
                                            onClick = { viewModel.resetCrop() }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset Boundary", fontSize = 11.sp, color = Color.White)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Quick Presets
                                            Button(
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1012)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                                                shape = RoundedCornerShape(8.dp),
                                                onClick = { viewModel.applyCrop(0.1f, 0.1f, 0.9f, 0.9f) }
                                            ) {
                                                Text("Symmetric Zoom", fontSize = 10.sp, color = Color(0xFFE2E2E6))
                                            }
                                            Button(
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0E1012)),
                                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2C2E33)),
                                                shape = RoundedCornerShape(8.dp),
                                                onClick = { viewModel.applyCrop(0.25f, 0f, 0.75f, 1f) }
                                            ) {
                                                Text("Vertical 1:2", fontSize = 10.sp, color = Color(0xFFE2E2E6))
                                            }
                                        }
                                    }
                                }
                            }

                            EditorTab.DRAW -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Freehand Paint Overlay", color = Color(0xFFC2C7CF), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                        TextButton(onClick = { viewModel.clearDrawings() }) {
                                            Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFEF4444))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Clear Paint", color = Color(0xFFEF4444), fontSize = 11.sp)
                                        }
                                    }

                                    // Color Palette list
                                    val colorsList = listOf(
                                        0xFFFF007F to Color(0xFFFF007F), // Neon Pink
                                        0xFF38BDF8 to Color(0xFF38BDF8), // Light Blue
                                        0xFF4ADE80 to Color(0xFF4ADE80), // Pastel Green
                                        0xFFFACC15 to Color(0xFFFACC15), // Yellow
                                        0xFFFFFFFF to Color.White,
                                        0xFF000000 to Color.Black
                                    )

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Brush size
                                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                            Text("Brush", color = Color(0xFFE2E2E6), fontSize = 10.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Slider(
                                                value = activeBrushSize,
                                                onValueChange = { activeBrushSize = it },
                                                valueRange = 4f..50f,
                                                colors = SliderDefaults.colors(
                                                    thumbColor = Color(0xFFD0E4FF),
                                                    activeTrackColor = Color(0xFFD0E4FF),
                                                    inactiveTrackColor = Color(0xFF2C2E33)
                                                )
                                            )
                                        }

                                        // Color Circles
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            colorsList.forEach { (colorHex, colorValue) ->
                                                val isSel = activeBrushColor == colorHex
                                                Box(
                                                    modifier = Modifier
                                                        .size(26.dp)
                                                        .aspectRatio(1f)
                                                        .clip(CircleShape)
                                                        .background(colorValue)
                                                        .border(
                                                            width = if (isSel) 2.dp else 1.dp,
                                                            color = if (isSel) Color(0xFFD0E4FF) else Color(0xFF2C2E33),
                                                            shape = CircleShape
                                                        )
                                                        .clickable { activeBrushColor = colorHex }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFF2C2E33), thickness = 1.dp)

                    // Primary Bottom Navigation Rail Bar
                    NavigationBar(
                        containerColor = Color(0xFF1A1C1E),
                        tonalElevation = 0.dp,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        NavigationBarItem(
                            selected = activeTab == EditorTab.FILTERS,
                            onClick = { activeTab = EditorTab.FILTERS },
                            icon = { Icon(Icons.Default.FilterBAndW, contentDescription = "Tab Filters") },
                            label = { Text("Filters") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF003258),
                                selectedTextColor = Color(0xFFD0E4FF),
                                indicatorColor = Color(0xFFD0E4FF),
                                unselectedIconColor = Color(0xFFC2C7CF),
                                unselectedTextColor = Color(0xFFC2C7CF)
                            ),
                            modifier = Modifier.testTag("filter_navigation_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == EditorTab.ADJUST,
                            onClick = { activeTab = EditorTab.ADJUST },
                            icon = { Icon(Icons.Default.Tune, contentDescription = "Tab Adjust") },
                            label = { Text("Adjust") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF003258),
                                selectedTextColor = Color(0xFFD0E4FF),
                                indicatorColor = Color(0xFFD0E4FF),
                                unselectedIconColor = Color(0xFFC2C7CF),
                                unselectedTextColor = Color(0xFFC2C7CF)
                            ),
                            modifier = Modifier.testTag("adjust_navigation_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == EditorTab.CROP,
                            onClick = { activeTab = EditorTab.CROP },
                            icon = { Icon(Icons.Default.Crop, contentDescription = "Tab Crop") },
                            label = { Text("Crop") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF003258),
                                selectedTextColor = Color(0xFFD0E4FF),
                                indicatorColor = Color(0xFFD0E4FF),
                                unselectedIconColor = Color(0xFFC2C7CF),
                                unselectedTextColor = Color(0xFFC2C7CF)
                            ),
                            modifier = Modifier.testTag("crop_navigation_tab")
                        )

                        NavigationBarItem(
                            selected = activeTab == EditorTab.DRAW,
                            onClick = { activeTab = EditorTab.DRAW },
                            icon = { Icon(Icons.Default.Brush, contentDescription = "Tab Paint") },
                            label = { Text("Paint") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color(0xFF003258),
                                selectedTextColor = Color(0xFFD0E4FF),
                                indicatorColor = Color(0xFFD0E4FF),
                                unselectedIconColor = Color(0xFFC2C7CF),
                                unselectedTextColor = Color(0xFFC2C7CF)
                            ),
                            modifier = Modifier.testTag("paint_navigation_tab")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF000000)),
            contentAlignment = Alignment.Center
        ) {
            if (processedBmp != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .fillMaxHeight(0.92f)
                        .clip(RoundedCornerShape(12.dp))
                        .shadow(12.dp)
                        .onSizeChanged { drawCanvasSize = it }
                        .pointerInput(activeTab == EditorTab.DRAW) {
                            if (activeTab != EditorTab.DRAW) return@pointerInput
                            detectDragGestures(
                                onDragStart = { offset ->
                                    localDrawPoints.add(offset)
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    localDrawPoints.add(change.position)
                                },
                                onDragEnd = {
                                    if (localDrawPoints.isNotEmpty() && drawCanvasSize.width > 0 && drawCanvasSize.height > 0) {
                                        // Scale native stroke points relative to viewport size (0f..1f)
                                        val relStrokes = localDrawPoints.map { pt ->
                                            Offset(
                                                pt.x / drawCanvasSize.width,
                                                pt.y / drawCanvasSize.height
                                            )
                                        }
                                        viewModel.addStroke(
                                            DrawStroke(
                                                points = relStrokes,
                                                color = activeBrushColor.toLong(),
                                                strokeWidth = activeBrushSize
                                            )
                                        )
                                    }
                                    localDrawPoints.clear()
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Image layer
                    Image(
                        bitmap = processedBmp!!.asImageBitmap(),
                        contentDescription = "Active Filtered Creation Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("editor_active_image"),
                        contentScale = ContentScale.Fit
                    )

                    // Draw Live overlay lines when dragging finger
                    if (activeTab == EditorTab.DRAW && localDrawPoints.isNotEmpty()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (localDrawPoints.size > 1) {
                                val path = androidx.compose.ui.graphics.Path()
                                var isFirst = true
                                for (point in localDrawPoints) {
                                    if (isFirst) {
                                        path.moveTo(point.x, point.y)
                                        isFirst = false
                                    } else {
                                        path.lineTo(point.x, point.y)
                                    }
                                }
                                drawPath(
                                    path = path,
                                    color = Color(activeBrushColor),
                                    style = Stroke(
                                        width = activeBrushSize,
                                        cap = StrokeCap.Round
                                    )
                                )
                            }
                        }
                    }

                    // Floating visual paintbrush preview size
                    if (activeTab == EditorTab.DRAW) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(activeBrushSize.dp.coerceIn(12.dp, 60.dp))
                                .shadow(4.dp, CircleShape)
                                .background(Color(activeBrushColor), CircleShape)
                                .border(2.dp, Color.White, CircleShape)
                        )
                    }

                    // Render temporary yellow bounding outline represent cropped margin boundaries
                    if (activeTab == EditorTab.CROP) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawRect(
                                color = Color(0xFFFACC15),
                                topLeft = Offset(size.width * editState.cropLeft, size.height * editState.cropTop),
                                size = androidx.compose.ui.geometry.Size(
                                    width = size.width * (editState.cropRight - editState.cropLeft),
                                    height = size.height * (editState.cropBottom - editState.cropTop)
                                ),
                                style = Stroke(
                                    width = 4f,
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(15f, 15f), 0f
                                    )
                                )
                            )
                        }
                    }
                }
            } else {
                CircularProgressIndicator(color = Color(0xFF6366F1))
            }
        }
    }

    // Export & Name File confirmation Dialog Box
    if (exportDialogOpen) {
        AlertDialog(
            onDismissRequest = { exportDialogOpen = false },
            containerColor = Color(0xFF1E293B),
            title = { Text("Export Creation", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = "Specify a name for your master modified image. It will save fully offline to Pictures directory.",
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = saveNameInput,
                        onValueChange = { saveNameInput = it },
                        label = { Text("File Name") },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF0F172A),
                            unfocusedContainerColor = Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("save_name_input_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    onClick = {
                        viewModel.saveImageToGallery(context, saveNameInput)
                    },
                    modifier = Modifier.testTag("dialog_save_confirm_btn")
                ) {
                    if (saveState is SaveState.Loading) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Export Jpeg")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { exportDialogOpen = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }
}
