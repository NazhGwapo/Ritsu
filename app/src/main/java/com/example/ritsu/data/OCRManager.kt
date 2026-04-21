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
        val result = recognizeText(bitmap) ?: return@withContext emptyMap()
        processImageWithResult(result, config, bitmap.width, bitmap.height)
    }

    fun processImageWithResult(result: Text, config: GameConfigData, width: Int, height: Int): Map<String, String> {
        val extractedData = mutableMapOf<String, String>()

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

        return extractedData
    }

    suspend fun recognizeText(bitmap: Bitmap): Text? = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        try {
            Tasks.await(recognizer.process(image))
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractTextFromRect(text: Text, ocrRect: OcrRect, imgW: Int, imgH: Int): String {
        val left = (ocrRect.x * imgW).toInt()
        val top = (ocrRect.y * imgH).toInt()
        val right = ((ocrRect.x + ocrRect.w) * imgW).toInt()
        val bottom = ((ocrRect.y + ocrRect.h) * imgH).toInt()
        val targetRect = Rect(left, top, right, bottom)

        val items = mutableListOf<Triple<String, Int, Int>>()

        for (block in text.textBlocks) {
            for (line in block.lines) {
                for (element in line.elements) {
                    val elementBox = element.boundingBox ?: continue
                    if (!Rect.intersects(targetRect, elementBox)) continue

                    // Precision at symbol level if available
                    val symbols = element.symbols
                    if (symbols.isNotEmpty()) {
                        val filteredText = symbols.filter { symbol ->
                            val sBox = symbol.boundingBox ?: return@filter false
                            targetRect.contains(sBox.centerX(), sBox.centerY())
                        }.joinToString("") { it.text }

                        if (filteredText.isNotBlank()) {
                            // Find the horizontal position of the first symbol included
                            val firstSymbolLeft = symbols.firstOrNull { symbol ->
                                val sBox = symbol.boundingBox ?: return@firstOrNull false
                                targetRect.contains(sBox.centerX(), sBox.centerY())
                            }?.boundingBox?.left ?: elementBox.left
                            
                            items.add(Triple(filteredText, elementBox.top, firstSymbolLeft))
                        }
                    } else {
                        // Fallback to element level if symbols are not available
                        if (targetRect.contains(elementBox.centerX(), elementBox.centerY())) {
                            items.add(Triple(element.text, elementBox.top, elementBox.left))
                        }
                    }
                }
            }
        }

        // Sort by vertical then horizontal position to maintain reading order
        return items.sortedWith(compareBy({ it.second }, { it.third }))
            .joinToString(" ") { it.first }
            .trim()
    }
}
