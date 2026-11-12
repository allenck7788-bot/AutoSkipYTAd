package com.example.autoskip

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.TextView

class SkipAdService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLoopRunning = false
    private var windowManager: WindowManager? = null
    private var statusTextView: TextView? = null

    // 每 5 秒執行一次偵測的定時任務
    private val detectRunnable = object : Runnable {
        override fun run() {
            try {
                // 1. 閃爍顯示「偵測中...」提示（顯示 1.2 秒後自動隱藏，確認循環有在跑）
                flashDetectingBadge()

                // 2. 執行廣告偵測與自動點擊
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    findAndClickSkipButton(rootNode)
                    try {
                        @Suppress("DEPRECATION")
                        rootNode.recycle()
                    } catch (e: Exception) {
                        // 忽略回收例外
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                // 3. 設定 5 秒後再次循環執行
                if (isLoopRunning) {
                    handler.postDelayed(this, 5000)
                }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        setupOverlayBadge()
        isLoopRunning = true
        handler.post(detectRunnable)
    }

    private fun setupOverlayBadge() {
        try {
            windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

            statusTextView = TextView(this).apply {
                text = "偵測中..."
                textSize = 13f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#B3000000")) // 半透明黑底
                setPadding(24, 12, 24, 12)
                visibility = android.view.View.GONE
            }

            // 使用 TYPE_ACCESSIBILITY_OVERLAY，無障礙服務開啟即可直接顯示，免額外權限
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 40   // 距離左邊界 40 像素
                y = 160  // 距離頂部 160 像素（避開狀態列與左上角返回/選單按鈕）
            }

            windowManager?.addView(statusTextView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun flashDetectingBadge() {
        statusTextView?.let { view ->
            view.visibility = android.view.View.VISIBLE
            // 顯示 1.2 秒後隱藏，這樣每 5 秒會閃出一次，既能確認運作又不影響看影片
            handler.postDelayed({
                if (isLoopRunning) {
                    view.visibility = android.view.View.GONE
                }
            }, 1200)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 交由 5 秒定時器處理
    }

    private fun findAndClickSkipButton(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        if (nodeText.contains("略過廣告") || nodeText.contains("Skip Ad") ||
            nodeText.contains("略過") ||
            contentDesc.contains("略過廣告") || contentDesc.contains("Skip Ad")) {

            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (!clicked && node.parent != null) {
                node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            return
        }

        val childCount = node.childCount
        for (i in 0 until childCount) {
            val child = node.getChild(i) ?: continue
            findAndClickSkipButton(child)
            try {
                @Suppress("DEPRECATION")
                child.recycle()
            } catch (e: Exception) {
                // 忽略回收例外
            }
        }
    }

    private fun cleanup() {
        isLoopRunning = false
        handler.removeCallbacks(detectRunnable)
        try {
            if (statusTextView != null && windowManager != null) {
                windowManager?.removeView(statusTextView)
                statusTextView = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onInterrupt() {
        cleanup()
    }

    override fun onDestroy() {
        super.onDestroy()
        cleanup()
    }
}
