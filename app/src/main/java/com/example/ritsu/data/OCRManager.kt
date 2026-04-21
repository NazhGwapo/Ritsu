package com.example.ritsu.data

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class OCRManager {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /**
     * Processes the [bitmap] and extracts text based on bounding boxes in [config].
     * Returns a map of keys to their extracted text values.
     */
    suspend fun processImage(bitmap: Bitmap, config: GameConfigData): Map<String, String> = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = try {
            Tasks.await(recognizer.process(image))
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext emptyMap()
        }

        val extractedData = mutableMapOf<String, String>()
        val width = bitmap.width
        val height = bitmap.height

        // Extract generic fields if they have rects defined
        config.titleRect?.let { extractedData["songTitle"] = extractTextFromRect(result, it, width, height) }
        config.scoreRect?.let { extractedData["totalScore"] = extractTextFromRect(result, it, width, height) }
        config.comboRect?.let { extractedData["maxCombo"] = extractTextFromRect(result, it, width, height) }
        config.difficultyNameRect?.let { extractedData["difficultyName"] = extractTextFromRect(result, it, width, height) }
        config.difficultyValRect?.let { extractedData["difficultyVal"] = extractTextFromRect(result, it, width, height) }
        config.rankRect?.let { extractedData["playRank"] = extractTextFromRect(result, it, width, height) }

        // Extract custom fields
        config.fields.forEach { field ->
            field.ocrRect?.let { rect ->
                extractedData[field.key] = extractTextFromRect(result, rect, width, height)
            }
        }

        extractedData
    }

    private fun extractTextFromRect(text: Text, ocrRect: OcrRect, imgW: Int, imgH: Int): String {
        val left = (ocrRect.x * imgW).toInt()
        val top = (ocrRect.y * imgH).toInt()
        val right = ((ocrRect.x + ocrRect.w) * imgW).toInt()
        val bottom = ((ocrRect.y + ocrRect.h) * imgH).toInt()
        val targetRect = Rect(left, top, right, bottom)

        val elements = mutableListOf<Pair<Text.Element, Int>>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val box = element.boundingBox ?: continue
                    // Use center point for more robust inclusion check
                    if (targetRect.contains(box.centerX(), box.centerY())) {
                        elements.add(element to box.top)
                    }
                }
            }
        }

        // Sort by vertical then horizontal position to maintain reading order
        return elements.sortedWith(compareBy({ it.second }, { it.first.boundingBox?.left ?: 0 }))
            .joinToString(" ") { it.first.text }
            .trim()
    }
}
