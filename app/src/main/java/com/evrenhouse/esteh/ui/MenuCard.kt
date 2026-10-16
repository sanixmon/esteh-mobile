package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.ImageRequest
import com.evrenhouse.esteh.EstehApp

/** Kartu menu vertikal: foto atas, nama, harga, tombol tambah. */
@Composable
fun MenuCard(
    name: String,
    price: Int,
    imageUrl: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    EstehBezel(modifier = modifier.clickable(onClick = onTap)) {
        Column {
            MenuPhoto(imageUrl = imageUrl, name = name)
            Column(modifier = Modifier.padding(EstehSpace.Sm)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    minLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(EstehSpace.Xxs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = price.toRp(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalIconButton(
                        onClick = onTap,
                        modifier = Modifier.size(48.dp),
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Tambah $name")
                    }
                }
            }
        }
    }
}

/** Foto katalog dari web; gagal/offline → inisial netral (bukan gambar dummy sama). */
@Composable
fun MenuPhoto(imageUrl: String, name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val loader = (context.applicationContext as EstehApp).container.imageLoader
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
        imageLoader = loader,
        contentDescription = name,
        contentScale = ContentScale.Crop,
        modifier = modifier.fillMaxWidth().aspectRatio(4f / 3f)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        loading = { PhotoFallback(name) },
        error = { PhotoFallback(name) },
        success = { SubcomposeAsyncImageContent() },
    )
}

@Composable
private fun PhotoFallback(name: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "?",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
