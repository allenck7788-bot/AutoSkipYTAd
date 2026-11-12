package com.example.autoskip

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 建立簡潔的畫面版面（無需額外 XML 布局檔，相容 Android 7.0）
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(60, 60, 60, 60)
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }

        val titleText = TextView(this).apply {
            text = "YouTube 廣告自動略過"
            textSize = 24f
            setTextColor(Color.parseColor("#222222"))
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 30)
        }

        statusText = TextView(this).apply {
            textSize = 18f
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 60)
        }

        val jumpButton = Button(this).apply {
            text = "前往「無障礙 / 協助工具」設定"
            textSize = 18f
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1976D2"))
            setPadding(50, 35, 50, 35)
            setOnClickListener {
                // 一鍵直接跳轉至系統的無障礙設定頁面
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                startActivity(intent)
            }
        }

        layout.addView(titleText)
        layout.addView(statusText)
        layout.addView(jumpButton)

        setContentView(layout)
    }

    override fun onResume() {
        super.onResume()
        // 每次回到畫面時，自動檢查無障礙服務是否已經開啟
        if (isAccessibilityServiceEnabled()) {
            statusText.text = "目前狀態：✅ 服務已開啟運作中"
            statusText.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            statusText.text = "目前狀態：❌ 服務尚未開啟"
            statusText.setTextColor(Color.parseColor("#C62828"))
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "$packageName/${SkipAdService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }
}
