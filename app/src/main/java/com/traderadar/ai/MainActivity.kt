package com.traderadar.ai

import android.app.Activity
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONArray
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var status: TextView
    private lateinit var results: LinearLayout
    private lateinit var scanButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(20, 25, 20, 20)
        root.setBackgroundColor(Color.rgb(10, 18, 32))

        val title = TextView(this)
        title.text = "TradeRadar AI"
        title.textSize = 28f
        title.gravity = Gravity.CENTER
        title.setTextColor(Color.WHITE)

        status = TextView(this)
        status.text = "آماده اسکن بازار"
        status.setTextColor(Color.WHITE)
        status.textSize = 16f

        scanButton = Button(this)
        scanButton.text = "شروع اسکن بازار"
        scanButton.setOnClickListener { scan() }

        results = LinearLayout(this)
        results.orientation = LinearLayout.VERTICAL

        val scroll = ScrollView(this)
        scroll.addView(results)

        root.addView(title)
        root.addView(status)
        root.addView(scanButton)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        setContentView(root)
    }

    private fun scan() {
        scanButton.isEnabled = false
        status.text = "در حال دریافت اطلاعات بازار..."

        executor.execute {
            try {
                val url = URL(
                    "https://api.binance.com/api/v3/ticker/24hr"
                )
                val connection =
                    url.openConnection() as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 15000

                val data = connection.inputStream
                    .bufferedReader().use { it.readText() }

                val tickers = JSONArray(data)
                val found = mutableListOf<String>()

                for (i in 0 until tickers.length()) {
                    val item = tickers.getJSONObject(i)
                    val symbol = item.getString("symbol")

                    if (!symbol.endsWith("USDT")) continue

                    val change = item.optDouble("priceChangePercent")
                    val volume = item.optDouble("quoteVolume")

                    if (volume > 10000000 && kotlin.math.abs(change) >= 2) {
                        val direction =
                            if (change > 0) "BUY / خرید" else "SELL / فروش"

                        found.add(
                            "$symbol\n" +
                            "جهت: $direction\n" +
                            "تغییر قیمت: ${"%.2f".format(change)}٪\n" +
                            "حجم معاملات: ${"%.0f".format(volume)} USDT"
                        )
                    }
                }

                connection.disconnect()

                runOnUiThread {
                    results.removeAllViews()

                    if (found.isEmpty()) {
                        show("در حال حاضر سیگنالی پیدا نشد.")
                    } else {
                        found.take(30).forEach { show(it) }
                    }

                    status.text = "اسکن تمام شد"
                    scanButton.isEnabled = true
                }

            } catch (e: Exception) {
                runOnUiThread {
                    status.text = "خطا در اتصال؛ اینترنت را بررسی کن."
                    scanButton.isEnabled = true
                }
            }
        }
    }

    private fun show(message: String) {
        val card = TextView(this)
        card.text = message
        card.textSize = 15f
        card.setTextColor(Color.WHITE)
        card.setPadding(15, 15, 15, 15)
        card.setBackgroundColor(Color.rgb(30, 48, 70))

        val params = LinearLayout.LayoutParams(-1, -2)
        params.setMargins(0, 8, 0, 8)
        results.addView(card, params)
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }
}
