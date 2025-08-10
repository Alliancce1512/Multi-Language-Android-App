package com.shellytask.app.gallery

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shellytask.app.R
import com.shellytask.app.gallery.ui.AppTopBar

class DetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val url     = intent.getStringExtra(EXTRA_URL).orEmpty()
        val author  = intent.getStringExtra(EXTRA_AUTHOR).orEmpty()
        val desc    = intent.getStringExtra(EXTRA_DESC).orEmpty()

        setContent {
            MaterialTheme {
                Scaffold(
                    topBar = {
                        AppTopBar(
                            title   = stringResource(id = R.string.image_details),
                            onBack  = { finish() }
                        )
                    }
                ) { padding ->
                    DetailScreen(
                        modifier    = Modifier
                            .padding(padding)
                            .statusBarsPadding()
                            .navigationBarsPadding(),
                        url         = url,
                        author      = author,
                        description = desc,
                    )
                }
            }
        }
    }

    companion object {
        private const val EXTRA_URL     = "extra_url"
        private const val EXTRA_AUTHOR  = "extra_author"
        private const val EXTRA_DESC    = "extra_desc"

        fun start(context: Context, url: String, author: String, description: String) {
            val intent = Intent(context, DetailActivity::class.java)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_AUTHOR, author)
                .putExtra(EXTRA_DESC, description)
            context.startActivity(intent)
        }
    }
}

@Composable
private fun DetailScreen(
    modifier    : Modifier = Modifier,
    url         : String,
    author      : String,
    description : String
) {
    Column(
        modifier            = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AsyncImage(
            model               = url,
            contentDescription  = description
        )

        Text(
            text    = author,
            style   = typography.titleMedium,
            color   = colorScheme.onBackground
        )

        if (description.isNotBlank()) {
            Text(
                text    = description,
                style   = typography.bodyMedium,
                color   = colorScheme.onBackground
            )
        }
    }
}