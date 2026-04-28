package com.example.ritsu.ui.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import android.content.Intent
import androidx.core.view.drawToBitmap
import java.io.OutputStream

object CaptureUtils {
    /**
     * Captures a Composable to a Bitmap.
     * Note: This is a simplified version. Real-world implementation might need a more robust approach
     * like using a hidden ComposeView in the Activity or a dedicated CaptureActivity.
     */
    fun captureComposable(view: View): Bitmap {
        return view.drawToBitmap()
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, fileName: String): Uri? {
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ritsu")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(imageCollection, contentValues) ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                context.contentResolver.update(uri, contentValues, null, null)
            }
            Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show()
            return uri
        } catch (e: Exception) {
            e.printStackTrace()
            context.contentResolver.delete(uri, null, null)
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
            return null
        }
    }

    fun shareBitmap(context: Context, bitmap: Bitmap, fileName: String) {
        try {
            val cachePath = File(context.cacheDir, "shared_images")
            cachePath.mkdirs()
            val file = File(cachePath, "$fileName.png")
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val contentUri = FileProvider.getUriForFile(context, "com.example.ritsu.fileprovider", file)

            if (contentUri != null) {
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    setDataAndType(contentUri, context.contentResolver.getType(contentUri))
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Data Summary"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }
}
