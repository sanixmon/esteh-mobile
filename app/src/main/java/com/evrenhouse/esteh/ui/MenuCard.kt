package com.evrenhouse.esteh.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.evrenhouse.esteh.EstehApp

/** Kartu menu bezel-ganda: foto web + nama + harga mono + pulau "+". */
@Composable
fun MenuCard(
    name: String,
    price: Int,
    imageUrl: String,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val loader = (context.applicationContext as EstehApp).container.imageLoader
    EstehBezel(modifier = modifier.fillMaxWidth().clickable(onClick = onTap)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(imageUrl).crossfade(true).build(),
                imageLoader = loader,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(76.dp).clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            )
            Column(modifier = Modifier.weight(1f)) {
                MicroLabel("Menu")
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = price.toRp(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Box(
                modifier = Modifier.clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "+",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}
