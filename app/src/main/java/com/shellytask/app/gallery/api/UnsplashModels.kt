package com.shellytask.app.gallery.api

data class UnsplashPhoto(
    val id              : String,
    val description     : String?,
    val altDescription  : String?,
    val urls            : UnsplashUrls,
    val user            : UnsplashUser
)

data class UnsplashUrls(
    val thumb   : String,
    val small   : String,
    val regular : String,
    val full    : String
)

data class UnsplashUser(
    val name: String
)

data class PhotoItem(
    val id              : String,
    val description     : String,
    val photographer    : String,
    val thumbUrl        : String,
    val fullUrl         : String
)