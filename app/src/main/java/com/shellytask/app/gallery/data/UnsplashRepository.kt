package com.shellytask.app.gallery.data

import com.shellytask.app.gallery.api.PhotoItem
import com.shellytask.app.gallery.api.UnsplashApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UnsplashRepository(
    private val api: UnsplashApi = UnsplashApi.create()
) {
    suspend fun fetchPage(page: Int, pageSize: Int): Result<List<PhotoItem>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = api.listPhotos(page = page, perPage = pageSize)

            if (response.isSuccessful) {
                val body    = response.body().orEmpty()
                val mapped  = body.map { p ->
                    PhotoItem(
                        id              = p.id,
                        description     = p.description ?: p.altDescription ?: "",
                        photographer    = p.user.name,
                        thumbUrl        = p.urls.small,
                        fullUrl         = p.urls.full
                    )
                }
                Result.success(mapped)
            } else {
                Result.failure(IllegalStateException("HTTP ${response.code()}"))
            }
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }
}