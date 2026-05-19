package ca.ilianokokoro.umihi.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade

@Composable
fun SmartImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    contentScale: ContentScale = ContentScale.Crop,
    alpha: Float = 1f,
    placeholderModel: Any? = null,
    placeHolderBackgroundColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh
) {
    val clippedModifier = modifier.clip(shape)

    if (model == null) {
        Placeholder(
            modifier = clippedModifier,
            contentDescription = contentDescription,
            containerColor = placeHolderBackgroundColor,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            alpha = alpha
        )
        return
    }

    val request = ImageRequest.Builder(LocalContext.current)
        .data(model)
        .crossfade(true)
        .build()

    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        modifier = clippedModifier,
        contentScale = contentScale,
        alpha = alpha,
        placeholder = null,
        error = null
    )
}

@Composable
private fun Placeholder(
    modifier: Modifier,
    contentDescription: String?,
    containerColor: Color,
    iconColor: Color,
    alpha: Float,
) {
    Box(
        modifier = modifier
            .background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Image(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = contentDescription ?: "Music placeholder",
            colorFilter = ColorFilter.tint(iconColor),
            modifier = Modifier.size(32.dp),
            contentScale = ContentScale.Fit,
            alpha = alpha
        )
    }
}
