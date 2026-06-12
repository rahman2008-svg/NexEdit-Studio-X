package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.database.AppDatabase
import com.example.database.ProjectRepository
import com.example.ui.ImageEditorApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PhotoEditorViewModel
import com.example.viewmodel.PhotoEditorViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create the Room offline persistence database and repository
        val database = AppDatabase.getDatabase(this)
        val repository = ProjectRepository(database.savedProjectDao())

        // Create ViewModel through Factory
        val viewModel: PhotoEditorViewModel by viewModels {
            PhotoEditorViewModelFactory(repository)
        }

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.ui.graphics.Color(0xFF0E1012)
                ) {
                    ImageEditorApp(viewModel = viewModel)
                }
            }
        }
    }
}
