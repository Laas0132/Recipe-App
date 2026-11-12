package com.example.recipeapp.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.recipeapp.databinding.ActivityScanIngredientBinding
import com.example.recipeapp.ml.MlKitHelper
import com.example.recipeapp.ml.ReceiptLine
import com.example.recipeapp.ui.adapter.ReceiptLineAdapter
import com.google.android.material.chip.Chip
import java.io.File

class ScanIngredientActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanIngredientBinding
    private lateinit var receiptAdapter: ReceiptLineAdapter
    private var tempPhotoUri: Uri? = null

    // Camera launcher
    private val takePhotoLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempPhotoUri?.let { uri ->
                displayAndProcessUri(uri)
            }
        }
    }

    // Gallery launcher
    private val pickGalleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            displayAndProcessUri(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityScanIngredientBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.scanRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
    }

    private fun setupUI() {
        binding.btnScanBack.setOnClickListener {
            finish()
        }

        binding.btnTakePhoto.setOnClickListener {
            launchCamera()
        }

        binding.btnPickGallery.setOnClickListener {
            pickGalleryLauncher.launch("image/*")
        }

        receiptAdapter = ReceiptLineAdapter(emptyList()) { lineText ->
            // Clean up text if it contains amounts/numbers to extract ingredient word
            val cleanedQuery = cleanQueryForSearch(lineText)
            selectQueryAndReturn(cleanedQuery)
        }
        binding.rvReceiptLines.layoutManager = LinearLayoutManager(this)
        binding.rvReceiptLines.adapter = receiptAdapter
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile("scan_photo_", ".jpg", cacheDir).apply {
                deleteOnExit()
            }
            tempPhotoUri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.fileprovider",
                photoFile
            )
            takePhotoLauncher.launch(tempPhotoUri!!)
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayAndProcessUri(uri: Uri) {
        binding.layoutPlaceholder.visibility = View.GONE
        binding.progressBarScan.visibility = View.VISIBLE
        binding.layoutResults.visibility = View.GONE
        binding.tvScanStatus.text = "Analysing image with ML Kit..."

        Glide.with(this)
            .load(uri)
            .into(binding.ivCapturedImage)

        MlKitHelper.processImage(
            context = this,
            uri = uri,
            onSuccess = { labels, receiptLines, rawText ->
                binding.progressBarScan.visibility = View.GONE
                handleMlKitResults(labels, receiptLines)
            },
            onError = { error ->
                binding.progressBarScan.visibility = View.GONE
                binding.tvScanStatus.text = "Recognition error: ${error.localizedMessage}"
                Toast.makeText(this, "Recognition failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun handleMlKitResults(labels: List<String>, lines: List<ReceiptLine>) {
        binding.layoutResults.visibility = View.VISIBLE
        binding.chipGroupLabels.removeAllViews()

        // 1. Top 3 Labels
        val top3 = labels.take(3)
        if (top3.isNotEmpty()) {
            binding.cardTopLabels.visibility = View.VISIBLE
            for (label in top3) {
                val chip = Chip(this).apply {
                    text = label
                    isClickable = true
                    isCheckable = false
                    setOnClickListener {
                        selectQueryAndReturn(label)
                    }
                }
                binding.chipGroupLabels.addView(chip)
            }
        } else {
            binding.cardTopLabels.visibility = View.GONE
        }

        // 2. Receipt / Text Recognition Lines
        if (lines.isNotEmpty()) {
            binding.cardReceiptText.visibility = View.VISIBLE
            receiptAdapter.updateList(lines)
            val amountCount = lines.count { it.isAmount }
            binding.tvScanStatus.text = "Recognition complete: Found ${top3.size} labels & ${lines.size} text lines ($amountCount amounts detected)."
        } else {
            binding.cardReceiptText.visibility = View.GONE
            if (top3.isNotEmpty()) {
                binding.tvScanStatus.text = "Recognised top ${top3.size} labels. Tap one to find recipes!"
            } else {
                binding.tvScanStatus.text = "No ingredients or text detected in this photo. Try another angle or clearer image."
            }
        }
    }

    private fun cleanQueryForSearch(raw: String): String {
        // Strip out currency, prices, digits, symbols like "$ 12.99" to get clean ingredient name
        val clean = raw.replace(Regex("""[\$€£₹\d\.,:;!#%&*()_+\-=\[\]{}|<>?]"""), " ").trim()
        val words = clean.split(Regex("""\s+""")).filter { it.length > 2 }
        return if (words.isNotEmpty()) words.first() else raw.trim()
    }

    private fun selectQueryAndReturn(query: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SEARCH_QUERY, query)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    companion object {
        const val EXTRA_SEARCH_QUERY = "extra_search_query"
    }
}
