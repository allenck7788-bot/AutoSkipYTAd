package com.example.autoskip

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        // 取得目前畫面的根節點（空值防護）
        val rootNode = rootInActiveWindow ?: return

        try {
            findAndClickSkipButton(rootNode)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                @Suppress("DEPRECATION")
                rootNode.recycle()
            } catch (e: Exception) {
                // 忽略回收例外
            }
        }
    }

    private fun findAndClickSkipButton(node: AccessibilityNodeInfo?) {
        if (node == null) return

        // 讀取文字與描述
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        // 判斷常見的中英文略過廣告關鍵字
        if (nodeText.contains("略過廣告") || nodeText.contains("Skip Ad") ||
            contentDesc.contains("略過廣告") || contentDesc.contains("Skip Ad")) {
            
            val clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            
            // 若節點本身不可點擊，嘗試對其父層點擊
            if (!clicked && node.parent != null) {
                node.parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            return
        }

        // 遞迴搜尋所有子節點
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

    override fun onInterrupt() {}
}
