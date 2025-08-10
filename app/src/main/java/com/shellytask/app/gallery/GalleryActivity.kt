package com.shellytask.app.gallery

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.shellytask.app.R
import com.shellytask.app.gallery.ui.AppTopBar

class GalleryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                GalleryScreen()
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun GalleryScreen(viewModel: GalleryViewModel = viewModel()) {
    val uiState            by viewModel.uiState.collectAsState()
    val snackbarHostState   = remember { SnackbarHostState() }
    val context             = LocalContext.current

    Scaffold(
        topBar          = {
            AppTopBar(
                title   = stringResource(id = R.string.image_gallery),
                onBack  = { (context as? android.app.Activity)?.finish() }
            )
        },
        snackbarHost    = { SnackbarHost(snackbarHostState) }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            when (val state = uiState) {
                is GalleryUiState.Loading   -> Loading()
                is GalleryUiState.Error     -> {
                    LaunchedEffect(state.message) {
                        snackbarHostState.showSnackbar(state.message)
                    }
                    Loading()
                }
                is GalleryUiState.Success   -> PhotoGrid(state) {
                    viewModel.loadNextPage()
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refresh()
    }
}

@Composable
private fun Loading() {
    Box(
        modifier            = Modifier.fillMaxSize(),
        contentAlignment    = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PhotoGrid(
    state           : GalleryUiState.Success,
    onLoadMore      : () -> Unit
) {
    val context = LocalContext.current

    LazyVerticalGrid(
        modifier                = Modifier.fillMaxSize(),
        columns                 = GridCells.Adaptive(minSize = 128.dp),
        contentPadding          = PaddingValues(8.dp),
        verticalArrangement     = Arrangement.spacedBy(8.dp),
        horizontalArrangement   = Arrangement.spacedBy(8.dp)
    ) {
        items(
            items       = state.photos,
            key         = { it.id },
            contentType = { "photo" }
        ) { photo ->
            val request = remember(key1 = photo.thumbUrl) {
                ImageRequest.Builder(context)
                    .data(data = photo.thumbUrl)
                    .placeholder(drawableResId = R.drawable.img_placeholder)
                    .error(drawableResId = R.drawable.img_error)
                    .crossfade(enable = false)
                    .build()
            }

            AsyncImage(
                modifier            = Modifier
                    .aspectRatio(1f)
                    .clickable {
                        DetailActivity.start(
                            context     = context,
                            url         = photo.fullUrl,
                            author      = photo.photographer,
                            description = photo.description
                        )
                    },
                model               = request,
                contentDescription  = photo.description,
                contentScale        = ContentScale.Crop
            )
        }

        if (state.canLoadMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = stringResource(R.string.network_loading))
                }

                LaunchedEffect(state.photos.size) { onLoadMore() }
            }
        }
    }
}