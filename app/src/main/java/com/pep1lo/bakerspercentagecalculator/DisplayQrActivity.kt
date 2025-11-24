package com.pep1lo.bakerspercentagecalculator

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.pep1lo.bakerspercentagecalculator.databinding.ActivityDisplayQrBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class DisplayQrActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDisplayQrBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDisplayQrBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipeJson = intent.getStringExtra("RECIPE_JSON")

        if (recipeJson != null) {
            try {
                val barcodeEncoder = BarcodeEncoder()
                val bitmap = barcodeEncoder.encodeBitmap(recipeJson, BarcodeFormat.QR_CODE, 400, 400)
                binding.imageViewQrCode.setImageBitmap(bitmap)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        binding.buttonShareQr.setOnClickListener { shareQrCode() }
    }

    private fun shareQrCode() {
        val drawable = binding.imageViewQrCode.drawable as? BitmapDrawable ?: return
        val bitmap = drawable.bitmap
        try {
            val cachePath = File(cacheDir, "images")
            cachePath.mkdirs()
            val stream = FileOutputStream("$cachePath/qr_code.png")
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val imagePath = File(cacheDir, "images")
            val newFile = File(imagePath, "qr_code.png")
            val contentUri = FileProvider.getUriForFile(this, applicationContext.packageName + ".provider", newFile)

            if (contentUri != null) {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                shareIntent.setDataAndType(contentUri, contentResolver.getType(contentUri))
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                startActivity(Intent.createChooser(shareIntent, "Share QR Code via"))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
