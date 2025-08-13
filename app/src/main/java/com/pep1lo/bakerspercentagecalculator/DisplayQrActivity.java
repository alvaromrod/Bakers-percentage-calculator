package com.pep1lo.bakerspercentagecalculator;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.pep1lo.bakerspercentagecalculator.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class DisplayQrActivity extends AppCompatActivity {

    private ImageView imageViewQrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_qr);

        imageViewQrCode = findViewById(R.id.imageViewQrCode);
        ImageButton buttonShareQr = findViewById(R.id.buttonShareQr);
        String recipeJson = getIntent().getStringExtra("RECIPE_JSON");

        if (recipeJson != null) {
            try {
                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
                Bitmap bitmap = barcodeEncoder.encodeBitmap(recipeJson, BarcodeFormat.QR_CODE, 400, 400);
                imageViewQrCode.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        buttonShareQr.setOnClickListener(v -> shareQrCode());
    }

    private void shareQrCode() {
        BitmapDrawable drawable = (BitmapDrawable) imageViewQrCode.getDrawable();
        if (drawable == null) {
            return;
        }
        Bitmap bitmap = drawable.getBitmap();
        try {
            File cachePath = new File(getCacheDir(), "images");
            cachePath.mkdirs();
            FileOutputStream stream = new FileOutputStream(cachePath + "/qr_code.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            stream.close();

            File imagePath = new File(getCacheDir(), "images");
            File newFile = new File(imagePath, "qr_code.png");
            Uri contentUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", newFile);

            if (contentUri != null) {
                Intent shareIntent = new Intent();
                shareIntent.setAction(Intent.ACTION_SEND);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                shareIntent.setDataAndType(contentUri, getContentResolver().getType(contentUri));
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                startActivity(Intent.createChooser(shareIntent, "Share QR Code via"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
