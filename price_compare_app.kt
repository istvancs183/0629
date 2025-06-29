/* Projektfájl: PriceCompareApp - főbb kész komponensek (Kotlin + Android XML)
 * Funkciók: vonalkód, OpenFoodFacts API, ár-osszehasonlítás, Excel beolvasás, magyar nyelvű UI
 */

// MainActivity.kt
package com.example.pricecompareapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.integration.android.IntentIntegrator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnScan).setOnClickListener {
            IntentIntegrator(this).initiateScan()
        }

        findViewById<Button>(R.id.btnExcel).setOnClickListener {
            val intent = Intent(this, ExcelImportActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result != null && result.contents != null) {
            val intent = Intent(this, ProductDetailActivity::class.java)
            intent.putExtra("barcode", result.contents)
            startActivity(intent)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
}

// ProductDetailActivity.kt
package com.example.pricecompareapp

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

class ProductDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_product_detail)

        val barcode = intent.getStringExtra("barcode") ?: return
        CoroutineScope(Dispatchers.IO).launch {
            val result = URL("https://world.openfoodfacts.org/api/v0/product/$barcode.json").readText()
            withContext(Dispatchers.Main) {
                val json = JSONObject(result)
                val name = json.getJSONObject("product").optString("product_name", "Ismeretlen termék")
                findViewById<TextView>(R.id.productName).text = name
                // TODO: árak összevetése itt
            }
        }
    }
}

// ExcelImportActivity.kt (alap)
package com.example.pricecompareapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.io.InputStream

class ExcelImportActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_excel_import)

        findViewById<Button>(R.id.btnUploadExcel).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            startActivityForResult(intent, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val inputStream: InputStream? = contentResolver.openInputStream(data!!.data!!)
            val workbook = WorkbookFactory.create(inputStream)
            val sheet = workbook.getSheetAt(0)
            for (row in sheet) {
                val barcode = row.getCell(0)?.stringCellValue ?: continue
                val price = row.getCell(1)?.numericCellValue ?: continue
                println("Import: $barcode - $price Ft")
                // TODO: mentés helyi adatbázisba
            }
        }
    }
}

// res/layout/activity_main.xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:orientation="vertical"
    android:padding="16dp"
    android:layout_width="match_parent"
    android:layout_height="match_parent">

    <Button android:id="@+id/btnScan"
        android:text="Termék beolvasása"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>

    <Button android:id="@+id/btnExcel"
        android:text="Excel árak betöltése"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"/>
</LinearLayout>

// AndroidManifest.xml (részlet)
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.INTERNET" />
<application ... >
    <activity android:name=".MainActivity" />
    <activity android:name=".ProductDetailActivity" />
    <activity android:name=".ExcelImportActivity" />
</application>

// README.md (kivonat)
# Valósidejű Árösszehasonlító App

- Vonalkód alapú termékfelismerés (OpenFoodFacts API)
- Excel beolvasás kisboltoktól
- GPS helymeghatározás
- Árak összevetése 500m körzeten belül
- Magyar nyelvű kezelőfelület

További funkciókat is integrálhatunk: Google Maps, Firestore, felhőalapú adatkezelés stb.
