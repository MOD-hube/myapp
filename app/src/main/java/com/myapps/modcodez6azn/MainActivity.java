package com.myapps.modcodez6azn;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView web;
    private long lastBack = 0;
    private ValueCallback<Uri[]> filePathCallback;
    private static final int FILE_CHOOSER_REQ = 4321;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#ffffff"));
        getWindow().setNavigationBarColor(Color.parseColor("#ffffff"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#fff9eb"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(true);

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, WebChromeClient.FileChooserParams params) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                if (params.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                    intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
                }
                ArrayList<String> mimes = new ArrayList<String>();
                String[] accept = params.getAcceptTypes();
                if (accept != null) {
                    for (int i = 0; i < accept.length; i++) {
                        if (accept[i] == null) continue;
                        String[] parts = accept[i].split(",");
                        for (int j = 0; j < parts.length; j++) {
                            String p = parts[j].trim();
                            if (p.length() == 0) continue;
                            if (p.startsWith(".")) {
                                String m = MimeTypeMap.getSingleton().getMimeTypeFromExtension(p.substring(1).toLowerCase());
                                if (m != null) mimes.add(m);
                            } else if (p.indexOf("/") >= 0) {
                                mimes.add(p);
                            }
                        }
                    }
                }
                if (!mimes.isEmpty()) {
                    intent.putExtra(Intent.EXTRA_MIME_TYPES, mimes.toArray(new String[0]));
                    if (mimes.size() == 1) intent.setType(mimes.get(0));
                }
                try {
                    startActivityForResult(Intent.createChooser(intent, "Select file"), FILE_CHOOSER_REQ);
                } catch (Exception e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

        });
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r) {
                String u = r.getUrl().toString();
                if (u.startsWith("file:")) return false;
                if (u.startsWith("http")) {
                    try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                    return true;
                }
                try { startActivity(new Intent(Intent.ACTION_VIEW, r.getUrl())); } catch (Exception e) { }
                return true;
            }
        });
        web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == FILE_CHOOSER_REQ) {
            if (filePathCallback == null) return;
            Uri[] results = null;
            if (resultCode == Activity.RESULT_OK && data != null) {
                if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    results = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = data.getClipData().getItemAt(i).getUri();
                    }
                } else if (data.getData() != null) {
                    results = new Uri[]{ data.getData() };
                }
            }
            filePathCallback.onReceiveValue(results);
            filePathCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) {
            web.goBack();
            return;
        }
        if (System.currentTimeMillis() - lastBack < 2000) {
            super.onBackPressed();
        } else {
            lastBack = System.currentTimeMillis();
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show();
        }
    }
}
