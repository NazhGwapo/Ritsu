package com.example.ritsu.data

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.core.graphics.get
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class OCRManager {
    private val latinRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val japaneseRecognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val chineseRecognizer = TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())

    /**
     * Processes the [bitmap] and extracts text based on bounding boxes in [config].
     * Returns a map of keys to their extracted text values.
     */
    suspend fun processImage(bitmap: Bitmap, config: GameConfigData): Map<String, String> = withContext(Dispatchers.Default) {
        coroutineScope {
            val latinResultDeferred = async { recognizeText(latinRecognizer, bitmap) }
            val japaneseResultDeferred = async { recognizeText(japaneseRecognizer, bitmap) }
            val chineseResultDeferred = async { recognizeText(chineseRecognizer, bitmap) }

            val latinResult = latinResultDeferred.await()
            val japaneseResult = japaneseResultDeferred.await()
            val chineseResult = chineseResultDeferred.await()

            processImageWithResults(latinResult, japaneseResult, chineseResult, config, bitmap)
        }
    }

    private fun processImageWithResults(
        latin: Text?,
        japanese: Text?,
        chinese: Text?,
        config: GameConfigData,
        bitmap: Bitmap
    ): Map<String, String> {
        val extractedData = mutableMapOf<String, String>()
        val width = bitmap.width
        val height = bitmap.height

        fun extractBestText(ocrRect: OcrRect, isNumeric: Boolean): String {
            // For numeric fields, Latin is usually most accurate
            if (isNumeric && latin != null) {
                return extractTextFromRect(latin, ocrRect, width, height, isNumeric = true)
            }

            // For text fields, try Japanese/Chinese then Latin
            val jaText = japanese?.let { extractTextFromRect(it, ocrRect, width, height) } ?: ""
            val zhText = chinese?.let { extractTextFromRect(it, ocrRect, width, height) } ?: ""
            val laText = latin?.let { extractTextFromRect(it, ocrRect, width, height) } ?: ""

            // Heuristic: Prefer the result with CJK characters if present, otherwise longest/latin
            return when {
                hasCJK(jaText) -> jaText
                hasCJK(zhText) -> zhText
                jaText.length > laText.length -> jaText
                zhText.length > laText.length -> zhText
                else -> laText
            }
        }

        // Extract generic fields
        config.titleRect?.let { extractedData["songTitle"] = extractBestText(it, isNumeric = false) }
        config.scoreRect?.let { extractedData["totalScore"] = extractBestText(it, isNumeric = true) }
        config.comboRect?.let { extractedData["maxCombo"] = extractBestText(it, isNumeric = true) }
        config.accuracyRect?.let { extractedData["accuracy"] = extractBestText(it, isNumeric = true) }
        config.difficultyNameRect?.let { extractedData["difficultyName"] = extractBestText(it, isNumeric = false) }
        config.difficultyValRect?.let { extractedData["difficultyVal"] = extractBestText(it, isNumeric = false) }
        config.rankRect?.let { 
            if (config.useRankOcr) {
                extractedData["playRank"] = extractBestText(it, isNumeric = false)
            }
        }

        // Extract custom fields
        config.allFieldsWithCategory.forEach { (field, _) ->
            field.ocrRect?.let { rect ->
                if ((field.type == "boolean") && (field.targetColor != null)) {
                    extractedData[field.key] = detectColor(bitmap, rect, field.targetColor, field.threshold).toString()
                } else {
                    extractedData[field.key] = extractBestText(rect, isNumeric = field.type == "number")
                }
            }
        }

        return extractedData
    }

    private fun hasCJK(s: String): Boolean {
        return s.any { c ->
            Character.UnicodeBlock.of(c).let {
                it == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS ||
                it == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS ||
                it == Character.UnicodeBlock.HIRAGANA ||
                it == Character.UnicodeBlock.KATAKANA ||
                it == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION ||
                it == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
            }
        }
    }

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

                val distance = kotlin.math.sqrt(
                    ((r - targetR) * (r - targetR) +
                            (g - targetG) * (g - targetG) +
                            (b - targetB) * (b - targetB)).toDouble(),
                ) / 441.67

                if (distance < threshold) {
                    matchCount++
                }
            }
        }

        return (matchCount.toFloat() / totalPixels) > 0.1f
    }

    suspend fun recognizeText(recognizer: com.google.mlkit.vision.text.TextRecognizer, bitmap: Bitmap): Text? = withContext(Dispatchers.Default) {
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

                    val symbols = element.symbols
                    if (symbols.isNotEmpty()) {
                        val filteredText = symbols.asSequence().filter { symbol ->
                            val sBox = symbol.boundingBox ?: return@filter false
                            targetRect.contains(sBox.centerX(), sBox.centerY())
                        }.joinToString("") { it.text }

                        if (filteredText.isNotBlank()) {
                            val firstSymbolLeft = symbols.asSequence().firstOrNull { symbol ->
                                val sBox = symbol.boundingBox ?: return@firstOrNull false
                                targetRect.contains(sBox.centerX(), sBox.centerY())
                            }?.boundingBox?.left ?: elementBox.left
                            
                            items.add(Triple(filteredText, elementBox.top, firstSymbolLeft))
                        }
                    } else {
                        if (targetRect.contains(elementBox.centerX(), elementBox.centerY())) {
                            items.add(Triple(element.text, elementBox.top, elementBox.left))
                        }
                    }
                }
            }
        }

        var resultText = items.sortedWith(compareBy({ it.second }, { it.third }))
            .joinToString(" ") { it.first }
            .trim()

        if (isNumeric) {
            resultText = resultText.replace('O', '0').replace('o', '0')
            val digitsOnly = resultText.filter { it.isDigit() || it == '.' }
            if (digitsOnly.isNotEmpty()) {
                val trimmed = digitsOnly.trimStart('0')
                resultText = if (trimmed.isEmpty()) "0" else if (trimmed.startsWith(".")) "0$trimmed" else trimmed
            }
        }

        return resultText
    }
}
