package com.example.ritsu.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

suspend fun saveUriToFile(context: Context, uri: Uri, fileName: String): File = withContext(Dispatchers.IO) {
    val file = File(context.filesDir, fileName)
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(file).use { output ->
            input.copyTo(output)
        }
    }
    file
}

suspend fun saveDrawableToFile(context: Context, drawable: Drawable, fileName: String): File = withContext(Dispatchers.IO) {
    val bitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        val b = Bitmap.createBitmap(
            drawable.intrinsicWidth.coerceAtLeast(1),
            drawable.intrinsicHeight.coerceAtLeast(1),
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(b)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        b
    }
    val file = File(context.filesDir, fileName)
    FileOutputStream(file).use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    file
}
