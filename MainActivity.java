package com.rafiqhifz.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import java.util.ArrayList;

public class MainActivity extends Activity {
    private WebView webView;
    private SpeechRecognizer recognizer;
    private static final int AUDIO_PERMISSION = 44;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient());
        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    private class Bridge {
        @JavascriptInterface public void startListening() {
            runOnUiThread(() -> {
                if (!SpeechRecognizer.isRecognitionAvailable(MainActivity.this)) {
                    sendResult("تعذر بدء التعرف على الصوت في هذا الجهاز."); return;
                }
                if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION); return;
                }
                beginRecognition();
            });
        }
        @JavascriptInterface public void stopListening() { runOnUiThread(() -> { if (recognizer != null) recognizer.stopListening(); }); }
    }
    private void beginRecognition() {
        if (recognizer != null) recognizer.destroy();
        recognizer = SpeechRecognizer.createSpeechRecognizer(this);
        recognizer.setRecognitionListener(new RecognitionListener() {
            public void onReadyForSpeech(Bundle b) { sendStatus("اسمعك الآن…"); }
            public void onBeginningOfSpeech() { }
            public void onRmsChanged(float v) { }
            public void onBufferReceived(byte[] b) { }
            public void onEndOfSpeech() { sendStatus("جارٍ تحليل ما سمعته…"); }
            public void onError(int e) { sendStatus("لم أتمكن من فهم الصوت. جرّب مرة أخرى."); }
            public void onResults(Bundle b) {
                ArrayList<String> r = b.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                sendResult(r != null && !r.isEmpty() ? r.get(0) : "لم يتم التعرف على كلام واضح.");
            }
            public void onPartialResults(Bundle b) { }
            public void onEvent(int t, Bundle b) { }
        });
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-EG");
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ar");
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        recognizer.startListening(i);
    }
    private void sendResult(String text) {
        String safe = org.json.JSONObject.quote(text);
        webView.post(() -> webView.evaluateJavascript("window.onSpeechResult && window.onSpeechResult(" + safe + ")", null));
    }
    private void sendStatus(String text) {
        String safe = org.json.JSONObject.quote(text);
        webView.post(() -> webView.evaluateJavascript("window.onSpeechStatus && window.onSpeechStatus(" + safe + ")", null));
    }
    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) beginRecognition();
            else Toast.makeText(this, "يلزم السماح باستخدام الميكروفون للتسميع", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onDestroy() { if (recognizer != null) recognizer.destroy(); if (webView != null) webView.destroy(); super.onDestroy(); }
    @Override public void onBackPressed() { if (webView != null && webView.canGoBack()) webView.goBack(); else super.onBackPressed(); }
}
