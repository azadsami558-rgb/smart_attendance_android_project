package com.example.smartattendance;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.journeyapps.barcodescanner.ScanContract;
import com.journeyapps.barcodescanner.ScanOptions;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private ActivityResultLauncher<String> cameraPermission;
    private ActivityResultLauncher<ScanOptions> qrLauncher;
    private boolean waitingForPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(android.graphics.Color.rgb(11,19,38));
        getWindow().setNavigationBarColor(android.graphics.Color.rgb(11,19,38));

        cameraPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            granted -> {
                if (granted && waitingForPermission) launchScanner();
                else if (!granted) sendJs("alert('يلزم السماح باستخدام الكاميرا لمسح رمز QR.');");
                waitingForPermission = false;
            }
        );

        qrLauncher = registerForActivityResult(new ScanContract(), result -> {
            if (result.getContents() != null) {
                String value = result.getContents()
                    .replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "");
                sendJs("handleScannedQr('" + value + "');");
            }
        });

        webView = new WebView(this);
        webView.setBackgroundColor(android.graphics.Color.rgb(11,19,38));
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient());
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface
            public void scanQr() {
                runOnUiThread(() -> requestScanner());
            }
        }, "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    private void requestScanner() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            launchScanner();
        } else {
            waitingForPermission = true;
            cameraPermission.launch(Manifest.permission.CAMERA);
        }
    }

    private void launchScanner() {
        ScanOptions options = new ScanOptions();
        options.setPrompt("وجّه الكاميرا نحو رمز QR الخاص بالطالب");
        options.setBeepEnabled(true);
        options.setOrientationLocked(false);
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE);
        qrLauncher.launch(options);
    }

    private void sendJs(String js) {
        runOnUiThread(() -> {
            if (webView != null) webView.evaluateJavascript(js, null);
        });
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
