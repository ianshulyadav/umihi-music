package ca.ilianokokoro.umihi.music.ui.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.core.graphics.createBitmap
import coil3.imageLoader
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.bitmapConfig
import coil3.asDrawable
import coil3.size.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ColorSchemeProcessor private constructor(private val context: Context) {
    private val memoryCache = LruCache<String, ColorSchemePair>(20)

    suspend fun getOrGenerateColorScheme(
        albumArtUri: String,
        paletteStyle: AlbumArtPaletteStyle = AlbumArtPaletteStyle.TONAL_SPOT,
        colorAccuracyLevel: Int = AlbumArtColorAccuracy.DEFAULT,
        forceRefresh: Boolean = false
    ): ColorSchemePair? {
        val resolvedAccuracyLevel = AlbumArtColorAccuracy.clamp(colorAccuracyLevel)
        val cacheKey = "$albumArtUri|$paletteStyle|$resolvedAccuracyLevel"
        
        if (!forceRefresh) {
            memoryCache.get(cacheKey)?.let { return it }
        }

        return generateAndCacheColorScheme(
            albumArtUri = albumArtUri,
            paletteStyle = paletteStyle,
            colorAccuracyLevel = resolvedAccuracyLevel,
            forceRefresh = forceRefresh
        )
    }

    private suspend fun generateAndCacheColorScheme(
        albumArtUri: String,
        paletteStyle: AlbumArtPaletteStyle,
        colorAccuracyLevel: Int,
        forceRefresh: Boolean = false
    ): ColorSchemePair? {
        return try {
            val bitmap = withContext(Dispatchers.IO) {
                loadBitmapForColorExtraction(albumArtUri, forceRefresh)
            } ?: return null

            val schemePair = withContext(Dispatchers.Default) {
                val seed = extractSeedColor(
                    bitmap = bitmap,
                    config = ColorExtractionConfig(
                        accuracyLevel = colorAccuracyLevel
                    )
                )
                bitmap.recycle()
                generateColorSchemeFromSeed(
                    seedColor = seed,
                    paletteStyle = paletteStyle
                )
            }

            val cacheKey = "$albumArtUri|$paletteStyle|$colorAccuracyLevel"
            memoryCache.put(cacheKey, schemePair)
            schemePair
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun loadBitmapForColorExtraction(uri: String, skipCache: Boolean): Bitmap? {
        return try {
            val cachePolicy = if (skipCache) CachePolicy.DISABLED else CachePolicy.ENABLED
            
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(Size(128, 128)) // Small size for fast processing
                .bitmapConfig(Bitmap.Config.ARGB_8888)
                .memoryCachePolicy(cachePolicy)
                .build()
            
            val result = context.imageLoader.execute(request)
            if (result !is SuccessResult) return null
            val drawable = result.image.asDrawable(context.resources)
            
            createBitmap(
                drawable.intrinsicWidth.coerceAtLeast(1),
                drawable.intrinsicHeight.coerceAtLeast(1)
            ).also { bmp ->
                Canvas(bmp).let { canvas ->
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ColorSchemeProcessor? = null

        fun getInstance(context: Context): ColorSchemeProcessor {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ColorSchemeProcessor(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
