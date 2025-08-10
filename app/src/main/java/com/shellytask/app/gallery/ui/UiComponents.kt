package com.shellytask.app.gallery.ui

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title   : String,
    onBack  : (() -> Unit)?                     = null,
    actions : @Composable RowScope.() -> Unit   = { }
) {
    CenterAlignedTopAppBar(
        modifier        = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars),
        title           = {
            Text(
                text    = title,
                style   = typography.titleLarge
            )
        },
        navigationIcon  = {
            onBack?.let {
                IconButton(onClick = it) {
                    Icon(
                        imageVector         = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription  = "Back"
                    )
                }
            }
        },
        actions         = actions
    )
}