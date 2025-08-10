package com.shellytask.app.gallery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shellytask.app.gallery.api.PhotoItem
import com.shellytask.app.gallery.data.UnsplashRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface GalleryUiState {
    data object Loading : GalleryUiState
    data class Success(
        val photos      : List<PhotoItem>,
        val page        : Int,
        val canLoadMore : Boolean
    ) : GalleryUiState
    data class Error(val message: String) : GalleryUiState
}

class GalleryViewModel(
    private val repository: UnsplashRepository = UnsplashRepository()
) : ViewModel() {
    private val _uiState: MutableStateFlow<GalleryUiState> = MutableStateFlow(GalleryUiState.Loading)
    val uiState: StateFlow<GalleryUiState> = _uiState.asStateFlow()

    private var isLoading   : Boolean   = false
    private val pageSize    : Int       = 30

    fun refresh() {
        if (isLoading) return

        _uiState.value = GalleryUiState.Loading
        load(
            page    = 1,
            append  = false
        )
    }

    fun loadNextPage() {
        val current = _uiState.value

        if (current is GalleryUiState.Success && current.canLoadMore && !isLoading) {
            load(
                page    = current.page + 1,
                append  = true
            )
        }
    }

    private fun load(page: Int, append: Boolean) {
        isLoading = true

        viewModelScope.launch {
            val result = repository.fetchPage(page, pageSize)

            result.fold(
                onSuccess = { photos ->
                    val canLoadMore = photos.size >= pageSize

                    _uiState.value = if (append && _uiState.value is GalleryUiState.Success) {
                        val prev = _uiState.value as GalleryUiState.Success

                        GalleryUiState.Success(
                            photos      = prev.photos + photos,
                            page        = page,
                            canLoadMore = canLoadMore
                        )
                    } else {
                        GalleryUiState.Success(
                            photos      = photos,
                            page        = page,
                            canLoadMore = canLoadMore
                        )
                    }
                },
                onFailure = { t ->
                    _uiState.value = GalleryUiState.Error(message = t.message ?: "Unknown error")
                }
            )
            isLoading = false
        }
    }
}