package com.example.autoskip

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class SkipAdService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private var isLoopRunning = false

    // 每 5 秒執行一次偵測的定時任務
    private val detectRunnable = object : Runnable {
        override fun run() {
            try {
                // 1. 顯示「偵測中」的訊息提示
                showDetectingToast()

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
        // 當無障礙服務開啟時，啟動 5 秒循環偵測
        isLoopRunning = true
        handler.post(detectRunnable)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 事件驅動交給 Handler 定時處理，此處保持留空即可
    }

    private fun showDetectingToast() {
        val toast = Toast.makeText(applicationContext, "偵測中...", Toast.LENGTH_SHORT)
        // 設定位置在畫面左上角（Gravity.TOP or Gravity.START）
        // xOffset: 30 (距左邊界 30 像素)
        // yOffset: 150 (距頂部 150 像素，避開最頂部狀態列與返回按鈕)
        toast.setGravity(
            android.view.Gravity.TOP or android.view.Gravity.START,
            30,
            150
        )
        toast.show()
    }

    private fun findAndClickSkipButton(node: AccessibilityNodeInfo?) {
        if (node == null) return

        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        // 判斷中英文略過廣告關鍵字
        if (nodeText.contains("略過廣告") || nodeText.contains("Skip Ad") ||
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

    override fun onInterrupt() {
        // 服務中斷時停止定時器
        isLoopRunning = false
        handler.removeCallbacks(detectRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        isLoopRunning = false
        handler.removeCallbacks(detectRunnable)
    }
}
