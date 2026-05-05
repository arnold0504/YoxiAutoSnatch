package com.arnold.yoxiautosnatch;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class YoxiAccessibilityService extends AccessibilityService {
    private static final String TAG = "YoxiSnatch";
    private long lastClickTime = 0;
    private static final long CLICK_COOLDOWN = 3000;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) return;
        int type = event.getEventType();
        if (type != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        long now = System.currentTimeMillis();
        if (now - lastClickTime < CLICK_COOLDOWN) return;
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            StringBuilder sb = new StringBuilder();
            collectText(root, sb);
            String fullText = sb.toString();
            if (fullText.isEmpty()) return;
            int fare = parseFare(fullText);
            if (fare <= 0) return;
            SharedPreferences prefs = getSharedPreferences("yoxi", MODE_PRIVATE);
            int minFare = prefs.getInt("min_fare", 300);
            Log.d(TAG, "車資：$" + fare + " 門檻：$" + minFare);
            if (fare <= minFare) return;
            Log.d(TAG, "符合！搶單中...");
            if (clickNodeContaining(root, "搶單任務")) { lastClickTime = now; return; }
            if (clickNodeContaining(root, "搶單")) { lastClickTime = now; return; }
            AccessibilityNodeInfo node = findNodeContaining(root, "搶單");
            if (node != null) {
                Rect bounds = new Rect();
                node.getBoundsInScreen(bounds);
                performClick(bounds.centerX(), bounds.centerY());
                lastClickTime = now;
            }
        } catch (Exception e) {
            Log.e(TAG, "錯誤：" + e.getMessage());
        } finally {
            root.recycle();
        }
    }

    private void collectText(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;
        CharSequence text = node.getText();
        if (text != null && text.length() > 0) sb.append(text).append("\n");
        for (int i = 0; i < node.getChildCount(); i++) collectText(node.getChild(i), sb);
    }

    private int parseFare(String text) {
        Pattern p = Pattern.compile("\\$\\s*(\\d+)");
        Matcher m = p.matcher(text);
        int max = 0;
        while (m.find()) { int v = Integer.parseInt(m.group(1)); if (v > max) max = v; }
        if (max == 0) {
            p = Pattern.compile("車資[^\\d]*(\\d+)");
            m = p.matcher(text);
            if (m.find()) max = Integer.parseInt(m.group(1));
        }
        return max;
    }

    private boolean clickNodeContaining(AccessibilityNodeInfo root, String keyword) {
        AccessibilityNodeInfo node = findNodeContaining(root, keyword);
        if (node == null) return false;
        if (node.isClickable()) { node.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true; }
        AccessibilityNodeInfo parent = node.getParent();
        while (parent != null) {
            if (parent.isClickable()) { parent.performAction(AccessibilityNodeInfo.ACTION_CLICK); return true; }
            parent = parent.getParent();
        }
        return false;
    }

    private AccessibilityNodeInfo findNodeContaining(AccessibilityNodeInfo root, String keyword) {
        if (root == null) return null;
        CharSequence text = root.getText();
        if (text != null && text.toString().contains(keyword)) return root;
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo r = findNodeContaining(root.getChild(i), keyword);
            if (r != null) return r;
        }
        return null;
    }

    private void performClick(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0, 50);
        dispatchGesture(new GestureDescription.Builder().addStroke(stroke).build(), null, null);
    }

    @Override
    public void onInterrupt() { Log.d(TAG, "服務中斷"); }
}
