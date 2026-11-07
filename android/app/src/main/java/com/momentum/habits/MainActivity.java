package com.momentum.habits;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.io.IOException;
import java.io.InputStream;

/** Offline app shell: HTML assets are served under a secure, app-only local origin. */
public class MainActivity extends Activity {
    private static final String APP_HOST = "habits.local";
    private static final int PICK_BACKUP_FILE = 102;
    private WebView webView;
    private ValueCallback<Uri[]> pendingFilePicker;
    private boolean webReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(247, 248, 252));
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(247, 248, 252));
        setContentView(webView);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true); // needed for the JSON backup file picker
        settings.setJavaScriptCanOpenWindowsAutomatically(false);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);

        webView.addJavascriptInterface(new BackupBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView view, String url) {
                if (url.startsWith("https://" + APP_HOST + "/")) {
                    webReady = true;
                    syncWebView();
                    openHabitFromWidget();
                }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return !"habits.local".equals(request.getUrl().getHost());
            }
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri url = request.getUrl();
                if (!"https".equals(url.getScheme()) || !APP_HOST.equals(url.getHost())) {
                    return null;
                }
                String path = url.getPath();
                if (path == null || "/".equals(path)) path = "/index.html";
                if (path.contains("..")) return missing();
                path = path.startsWith("/") ? path.substring(1) : path;
                try {
                    InputStream stream = getAssets().open(path);
                    String mime;
                    if (path.endsWith(".html")) mime = "text/html";
                    else if (path.endsWith(".js")) mime = "application/javascript";
                    else if (path.endsWith(".webmanifest")) mime = "application/manifest+json";
                    else if (path.endsWith(".svg")) mime = "image/svg+xml";
                    else if (path.endsWith(".png")) mime = "image/png";
                    else mime = "application/octet-stream";
                    return new WebResourceResponse(mime, "UTF-8", stream);
                } catch (IOException e) {
                    return missing();
                }
            }

            private WebResourceResponse missing() {
                return new WebResourceResponse("text/plain", "UTF-8", 404, "Not Found", null,
                        new java.io.ByteArrayInputStream(new byte[0]));
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback,
                                             FileChooserParams fileChooserParams) {
                if (pendingFilePicker != null) pendingFilePicker.onReceiveValue(null);
                pendingFilePicker = filePathCallback;
                Intent select = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                select.setType("application/json");
                select.addCategory(Intent.CATEGORY_OPENABLE);
                try {
                    startActivityForResult(select, PICK_BACKUP_FILE);
                    return true;
                } catch (Exception e) {
                    pendingFilePicker = null;
                    return false;
                }
            }
        });
        webView.loadUrl("https://" + APP_HOST + "/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_BACKUP_FILE && pendingFilePicker != null) {
            Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[]{data.getData()} : null;
            pendingFilePicker.onReceiveValue(result);
            pendingFilePicker = null;
        }
    }

    @Override protected void onResume() {
        super.onResume();
        syncWebView();
        ReminderReceiver.scheduleNext(this);
    }
    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        syncWebView();
        openHabitFromWidget();
    }
    private void syncWebView() {
        if (webReady && webView != null) webView.evaluateJavascript(
                "if(window.momentumSyncFromNative)window.momentumSyncFromNative();", null);
    }
    private void openHabitFromWidget() {
        if (!webReady || webView == null) return;
        Intent intent = getIntent();
        String id = intent == null ? null : intent.getStringExtra(HabitWidgetProvider.EXTRA_HABIT_ID);
        if (id == null || id.isEmpty()) return;
        intent.removeExtra(HabitWidgetProvider.EXTRA_HABIT_ID);
        webView.evaluateJavascript("if(window.momentumOpenLogger)window.momentumOpenLogger("+
                org.json.JSONObject.quote(id)+");", null);
    }
    private class BackupBridge {
        @JavascriptInterface public String getState() {
            return WidgetStore.getState(MainActivity.this);
        }
        @JavascriptInterface public boolean saveState(String json) {
            boolean ok = WidgetStore.saveState(MainActivity.this, json);
            if (ok) { HabitWidgetProvider.refreshAll(MainActivity.this); ReminderReceiver.scheduleNext(MainActivity.this); }
            return ok;
        }
        @JavascriptInterface
        public void requestNotifications() {
            if(android.os.Build.VERSION.SDK_INT>=33 &&
                checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
                runOnUiThread(()->requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},107));
        }
        @JavascriptInterface
        public void shareBackup(String json) {
            runOnUiThread(() -> {
                Intent send = new Intent(Intent.ACTION_SEND);
                send.setType("text/plain");
                send.putExtra(Intent.EXTRA_SUBJECT, "Momentum habit tracker backup");
                send.putExtra(Intent.EXTRA_TEXT, json);
                startActivity(Intent.createChooser(send, "Save or share your habit backup"));
            });
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) webView.destroy();
        super.onDestroy();
    }
}
