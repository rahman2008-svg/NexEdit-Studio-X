package com.example.model

import androidx.compose.ui.geometry.Offset

data class DrawStroke(
    val points: List<Offset>,
    val color: Long, // Color stored as value bits
    val strokeWidth: Float
)
