package com.example.recipeapp.ml

import android.graphics.Bitmap
import android.net.Uri
import android.content.Context
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.regex.Pattern

data class ReceiptLine(
    val text: String,
    val isAmount: Boolean
)

object MlKitHelper {

    // Regex to match currency amounts, totals, prices or quantities (e.g., $12.99, 4.50, 500g, 2kg, etc.)
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?i)(\$|€|£|₹|\bUSD|\bEUR|\bTOTAL|\bPRICE|\bSUBTOTAL|\bAMOUNT)?\s*\d+([.,]\d{1,2})?\s*(\$|€|£|₹|\bkg|\bg|\blb|\boz|\bml|\bl)?\b"""
    )

    fun processImage(
        context: Context,
        uri: Uri,
        onSuccess: (labels: List<String>, receiptLines: List<ReceiptLine>, rawText: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            processInputImage(image, onSuccess, onError)
        } catch (e: Exception) {
            onError(e)
        }
    }

    fun processBitmap(
        bitmap: Bitmap,
        onSuccess: (labels: List<String>, receiptLines: List<ReceiptLine>, rawText: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            processInputImage(image, onSuccess, onError)
        } catch (e: Exception) {
            onError(e)
        }
    }

    private fun processInputImage(
        image: InputImage,
        onSuccess: (labels: List<String>, receiptLines: List<ReceiptLine>, rawText: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
        // Uses Google Play Services ML Kit text recognition
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        labeler.process(image)
            .addOnSuccessListener { labels ->
                val topLabels = labels
                    .sortedByDescending { it.confidence }
                    .map { it.text }
                    .take(3)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val lines = mutableListOf<ReceiptLine>()
                        for (block in visionText.textBlocks) {
                            for (line in block.lines) {
                                val lineText = line.text.trim()
                                if (lineText.isNotBlank()) {
                                    val isAmount = AMOUNT_PATTERN.matcher(lineText).find()
                                    lines.add(ReceiptLine(lineText, isAmount))
                                }
                            }
                        }
                        onSuccess(topLabels, lines, visionText.text)
                    }
                    .addOnFailureListener { textError ->
                        // If text fails, still return image labels
                        onSuccess(topLabels, emptyList(), "")
                    }
            }
            .addOnFailureListener { labelError ->
                // Try text recognition anyway if labeling fails
                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val lines = visionText.textBlocks.flatMap { it.lines }.map {
                            ReceiptLine(it.text.trim(), AMOUNT_PATTERN.matcher(it.text).find())
                        }
                        onSuccess(emptyList(), lines, visionText.text)
                    }
                    .addOnFailureListener {
                        onError(it)
                    }
            }
    }
}
