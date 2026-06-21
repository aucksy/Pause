package com.pause.app.data.system

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.pause.app.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

/**
 * Turns a user-picked image (any size, any orientation) into a small, app-private PNG suitable for
 * the overlay — so the user never has to resize anything themselves. The work is downsampled while
 * decoding (so we never load a huge bitmap into memory), EXIF-rotated, scaled to a sane maximum,
 * and written as PNG to preserve any transparency (the "floating cutout" look).
 *
 * Each save writes a NEW uniquely-named file and removes the previous one. A fresh path means the
 * overlay and the preview reliably re-decode the new image (and we never read a file mid-write).
 */
@Singleton
class CustomImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {

    /** Copy + resize the picked image into app storage. Returns the saved absolute path, or null on failure. */
    suspend fun save(uri: Uri): String? = withContext(io) {
        runCatching {
            val resolver = context.contentResolver

            // 1) Read just the dimensions so we can pick a downsample factor.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) return@runCatching null

            // 2) Decode, downsampled, keeping an alpha channel.
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = computeInSampleSize(srcW, srcH, MAX_DIMENSION)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val decoded = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOpts)
            } ?: return@runCatching null

            // 3) Apply EXIF orientation, then scale down to the maximum edge.
            val oriented = applyExifRotation(uri, decoded)
            val scaled = scaleToMax(oriented, MAX_DIMENSION)

            // 4) Write a new uniquely-named PNG (atomically via a temp file).
            val outFile = File(context.filesDir, PREFIX + System.currentTimeMillis() + ".png")
            val tmp = File(context.filesDir, outFile.name + ".tmp")
            FileOutputStream(tmp).use { out -> scaled.compress(Bitmap.CompressFormat.PNG, 100, out) }
            if (!tmp.renameTo(outFile)) {
                tmp.copyTo(outFile, overwrite = true)
                tmp.delete()
            }

            // Recycle every distinct intermediate bitmap.
            setOf(decoded, oriented, scaled).forEach { it.recycle() }

            // Clean up any earlier custom image(s).
            context.filesDir.listFiles { f ->
                f.name.startsWith(PREFIX) && f.absolutePath != outFile.absolutePath
            }?.forEach { it.delete() }

            outFile.absolutePath
        }.getOrNull()
    }

    /** Remove every stored custom image. */
    suspend fun delete() {
        withContext(io) {
            runCatching {
                context.filesDir.listFiles { f -> f.name.startsWith(PREFIX) }?.forEach { it.delete() }
            }
        }
    }

    /** Whether the file at [path] still exists (a stored path can go stale after a restore). */
    suspend fun exists(path: String): Boolean = withContext(io) {
        runCatching { File(path).exists() }.getOrDefault(false)
    }

    // Downsample keyed on the LARGER edge so panoramic / extreme-aspect images don't decode at
    // full resolution (which could OOM). scaleToMax then does the precise final downscale.
    private fun computeInSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        var w = width
        var h = height
        while (maxOf(w, h) / 2 >= maxEdge) {
            w /= 2
            h /= 2
            sample *= 2
        }
        return sample
    }

    private fun applyExifRotation(uri: Uri, bitmap: Bitmap): Bitmap {
        val orientation = runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL
        val matrix = matrixForOrientation(orientation) ?: return bitmap
        return runCatching {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }.getOrDefault(bitmap)
    }

    /** A transform matrix for the full set of EXIF orientations, or null for "no change needed". */
    private fun matrixForOrientation(orientation: Int): Matrix? {
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return null
        }
        return matrix
    }

    private fun scaleToMax(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val largest = maxOf(bitmap.width, bitmap.height)
        if (largest <= maxEdge) return bitmap
        val scale = maxEdge.toFloat() / largest
        val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private companion object {
        const val PREFIX = "custom_overlay_"
        const val MAX_DIMENSION = 720
    }
}
