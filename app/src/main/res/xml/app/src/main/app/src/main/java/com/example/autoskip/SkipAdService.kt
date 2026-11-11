package com.example.autoskip

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAdService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        val keywords = listOf("略過", "略過廣告", "Skip", "Skip Ad", "Skip Ads")

        for (keyword in keywords) {
            val nodes = rootNode.findAccessibilityNodeInfosByText(keyword)
            if (!nodes.isNullOrEmpty()) {
                for (node in nodes) {
                    if (!clickNodeOrParent(node)) {
                        clickByCoordinates(node)
                    }
                    break
                }
            }
        }
        rootNode.recycle()
    }

    private fun clickNodeOrParent(node: AccessibilityNodeInfo?): Boolean {
        var current = node
        while (current != null) {
            if (current.isClickable) {
                return current.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            current = current.parent
        }
        return false
    }

    private fun clickByCoordinates(node: AccessibilityNodeInfo) {
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val x = rect.exactCenterX()
        val y = rect.exactCenterY()

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        dispatchGesture(gesture, null, null)
    }

    override fun onInterrupt() {}
}
