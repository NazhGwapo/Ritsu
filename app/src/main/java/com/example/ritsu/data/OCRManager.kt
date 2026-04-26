package com.example.ritsu.data

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.get
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
        processImageWithResult(result, config, bitmap)
    }

    fun processImageWithResult(result: Text, config: GameConfigData, bitmap: Bitmap): Map<String, String> {
        val extractedData = mutableMapOf<String, String>()
        val width = bitmap.width
        val height = bitmap.height

        // Extract generic fields if they have rects defined
        config.titleRect?.let { extractedData["songTitle"] = extractTextFromRect(result, it, width, height) }
        config.scoreRect?.let { extractedData["totalScore"] = extractTextFromRect(result, it, width, height, isNumeric = true) }
        config.comboRect?.let { extractedData["maxCombo"] = extractTextFromRect(result, it, width, height, isNumeric = true) }
        config.accuracyRect?.let { extractedData["accuracy"] = extractTextFromRect(result, it, width, height, isNumeric = true) }
        config.difficultyNameRect?.let { extractedData["difficultyName"] = extractTextFromRect(result, it, width, height) }
        config.difficultyValRect?.let { extractedData["difficultyVal"] = extractTextFromRect(result, it, width, height) }
        config.rankRect?.let { 
            if (config.useRankOcr) {
                extractedData["playRank"] = extractTextFromRect(result, it, width, height)
            }
        }

        // Extract custom fields
        config.allFieldsWithCategory.forEach { (field, _) ->
            field.ocrRect?.let { rect ->
                if ((field.type == "boolean") && (field.targetColor != null)) {
                    extractedData[field.key] = detectColor(bitmap, rect, field.targetColor, field.threshold).toString()
                } else {
                    extractedData[field.key] = extractTextFromRect(result, rect, width, height, isNumeric = field.type == "number")
                }
            }
        }

        return extractedData
    }

    /**
     * Checks if the [targetColor] exists within the [ocrRect] of the [bitmap].
     * Returns true if the color is found with enough density, false otherwise.
     */
    private fun detectColor(bitmap: Bitmap, ocrRect: OcrRect, targetColor: Int, threshold: Float): Boolean {
        val left = (ocrRect.x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
        val top = (ocrRect.y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
        val right = ((ocrRect.x + ocrRect.w) * bitmap.width).toInt().coerceIn(0, bitmap.width)
        val bottom = ((ocrRect.y + ocrRect.h) * bitmap.height).toInt().coerceIn(0, bitmap.height)

        if (left >= right || top >= bottom) return false

        val targetR = (targetColor shr 16) and 0xFF
        val targetG = (targetColor shr 8) and 0xFF
        val targetB = targetColor and 0xFF

        var matchCount = 0
        val totalPixels = (right - left) * (bottom - top)

        for (y in top until bottom) {
            for (x in left until right) {
                val pixel = bitmap[x, y]
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF

                // Euclidean distance in RGB space (ignoring alpha for now as bitmaps are usually opaque)
                val distance = kotlin.math.sqrt(
                    ((r - targetR) * (r - targetR) +
                            (g - targetG) * (g - targetG) +
                            (b - targetB) * (b - targetB)).toDouble(),
                ) / 441.67 // Max distance is sqrt(255^2 * 3) ≈ 441.67

                if (distance < threshold) {
                    matchCount++
                }
            }
        }

        // If more than 10% of pixels match, we consider it "activated"
        // This is a simple heuristic, might need adjustment
        return (matchCount.toFloat() / totalPixels) > 0.1f
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

    private fun extractTextFromRect(text: Text, ocrRect: OcrRect, imgW: Int, imgH: Int, isNumeric: Boolean = false): String {
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
                        val filteredText = symbols.asSequence().filter { symbol ->
                            val sBox = symbol.boundingBox ?: return@filter false
                            targetRect.contains(sBox.centerX(), sBox.centerY())
                        }.joinToString("") { it.text }

                        if (filteredText.isNotBlank()) {
                            // Find the horizontal position of the first symbol included
                            val firstSymbolLeft = symbols.asSequence().firstOrNull { symbol ->
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
        var resultText = items.sortedWith(compareBy({ it.second }, { it.third }))
            .joinToString(" ") { it.first }
            .trim()

        if (isNumeric) {
            // Replace 'O' or 'o' with '0' as OCR often confuses them
            resultText = resultText.replace('O', '0').replace('o', '0')
            // Filter only digits, dots, or signs if needed, but here we focus on simple cleanup
            val digitsOnly = resultText.filter { it.isDigit() || it == '.' }
            if (digitsOnly.isNotEmpty()) {
                // Remove leading zeros, but keep at least one digit if it's all zeros
                val trimmed = digitsOnly.trimStart('0')
                resultText = if (trimmed.isEmpty()) "0" else if (trimmed.startsWith(".")) "0$trimmed" else trimmed
            }
        }

        return resultText
    }
}
