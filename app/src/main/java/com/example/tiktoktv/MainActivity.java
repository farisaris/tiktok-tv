package com.example.tiktoktv;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private WebView webView;
    private FrameLayout root;
    private android.view.View cursor;

    private static final String START_URL = "https://www.tiktok.com/login";
    private static final int CURSOR_SIZE_DP = 28;
    private static final int STEP_DP = 40;
    private static final int LONG_PRESS_MS = 450;

    private boolean cursorMode = false;
    private float cursorX, cursorY;
    private float density;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable longPressRunnable;
    private boolean longPressFired = false;

    private static final String NAV_JS =
        "(function(){" +
        "if(window.__tvNavInstalled) return; window.__tvNavInstalled = true;" +
        "var idx=-1; var els=[];" +
        "var style=document.createElement('style');" +
        "style.innerHTML='.__tv_focus{outline:4px solid #FE2C55 !important; outline-offset:2px !important;}';" +
        "document.head.appendChild(style);" +
        "function isVisible(el){var r=el.getBoundingClientRect(); return r.width>4&&r.height>4&&r.top<window.innerHeight&&r.bottom>0;}" +
        "function collect(){var nodes=document.querySelectorAll('a,button,[role=button],[role=tab],input,textarea,[tabindex]'); els=[]; for(var i=0;i<nodes.length;i++){if(isVisible(nodes[i]))els.push(nodes[i]);}}" +
        "function clearFocus(){for(var i=0;i<els.length;i++){els[i].classList.remove('__tv_focus');}}" +
        "function setFocus(i){clearFocus(); if(els.length===0)return; idx=((i%els.length)+els.length)%els.length; var el=els[idx]; el.classList.add('__tv_focus'); el.scrollIntoView({block:'center',inline:'center',behavior:'smooth'});}" +
        "window.__tvMove=function(dir){collect(); if(els.length===0)return; if(idx===-1){setFocus(0);return;} var cur=els[idx]?els[idx].getBoundingClientRect():{left:0,top:0}; var best=-1,bestDist=Infinity; for(var i=0;i<els.length;i++){if(i===idx)continue; var r=els[i].getBoundingClientRect(); var dx=r.left-cur.left,dy=r.top-cur.top; var ok=false; if(dir==='left')ok=dx<-5; if(dir==='right')ok=dx>5; if(dir==='up')ok=dy<-5; if(dir==='down')ok=dy>5; if(!ok)continue; var dist=Math.sqrt(dx*dx+dy*dy); if(dist<bestDist){bestDist=dist; best=i;}} if(best>=0)setFocus(best); else setFocus(idx);};" +
        "window.__tvClick=function(){if(idx>=0&&els[idx]){els[idx].click();}};" +
        "window.__tvScroll=function(dy){window.scrollBy({top:dy,behavior:'smooth'});};" +
        "})();";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        density = getResources().getDisplayMetrics().density;

        root = new FrameLayout(this);
        webView = new WebView(this);
        root.addView(webView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cursor = new android.view.View(this);
        GradientDrawable dot = new GradientDrawable();
        dot.setShape(GradientDrawable.OVAL);
        dot.setColor(Color.parseColor("#FE2C55"));
        dot.setStroke((int) (2 * density), Color.WHITE);
        cursor.setBackground(dot);
        int sizePx = (int) (CURSOR_SIZE_DP * density);
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(sizePx, sizePx);
        cp.gravity = Gravity.TOP | Gravity.START;
        root.addView(cursor, cp);
        cursor.setVisibility(android.view.View.GONE);

        setContentView(root);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setUserAgentString(settings.getUserAgentString() + " TVStick");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    return false; // biar WebView load macam biasa
                }
                // Scheme bukan http/https (contoh: snssdk1180://) - abaikan,
                // supaya app tak "terkeluar" ke error page bila cuba buka
                // TikTok app asli.
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.evaluateJavascript(NAV_JS, null);
            }
        });

        webView.loadUrl(START_URL);

        root.post(() -> {
            cursorX = root.getWidth() / 2f;
            cursorY = root.getHeight() / 2f;
            updateCursorPosition();
        });
    }

    private void updateCursorPosition() {
        cursor.setX(cursorX - cursor.getWidth() / 2f);
        cursor.setY(cursorY - cursor.getHeight() / 2f);
    }

    private void toggleCursorMode() {
        cursorMode = !cursorMode;
        cursor.setVisibility(cursorMode ? android.view.View.VISIBLE : android.view.View.GONE);
        if (cursorX == 0 && cursorY == 0) {
            cursorX = root.getWidth() / 2f;
            cursorY = root.getHeight() / 2f;
        }
        updateCursorPosition();
    }

    private void dispatchTapAtCursor() {
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime,
                MotionEvent.ACTION_DOWN, cursorX, cursorY, 0);
        webView.dispatchTouchEvent(down);
        down.recycle();

        handler.postDelayed(() -> {
            long upTime = SystemClock.uptimeMillis();
            MotionEvent up = MotionEvent.obtain(downTime, upTime,
                    MotionEvent.ACTION_UP, cursorX, cursorY, 0);
            webView.dispatchTouchEvent(up);
            up.recycle();
        }, 60);
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        boolean isCenter = event.getKeyCode() == KeyEvent.KEYCODE_DPAD_CENTER
                || event.getKeyCode() == KeyEvent.KEYCODE_ENTER;

        if (isCenter) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    longPressFired = false;
                    longPressRunnable = () -> {
                        longPressFired = true;
                        toggleCursorMode();
                    };
                    handler.postDelayed(longPressRunnable, LONG_PRESS_MS);
                }
                return true;
            } else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (longPressRunnable != null) {
                    handler.removeCallbacks(longPressRunnable);
                }
                if (!longPressFired) {
                    if (cursorMode) {
                        dispatchTapAtCursor();
                    } else {
                        webView.evaluateJavascript("window.__tvClick && window.__tvClick();", null);
                    }
                }
                return true;
            }
            return true;
        }

        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
            if (handleDirectionalOrBack(event.getKeyCode())) {
                return true;
            }
        }

        return super.dispatchKeyEvent(event);
    }

    private boolean handleDirectionalOrBack(int keyCode) {
        if (cursorMode) {
            float step = STEP_DP * density;
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    cursorX = Math.max(0, cursorX - step);
                    updateCursorPosition();
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    cursorX = Math.min(root.getWidth(), cursorX + step);
                    updateCursorPosition();
                    return true;
                case KeyEvent.KEYCODE_DPAD_UP:
                    cursorY = Math.max(0, cursorY - step);
                    updateCursorPosition();
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    cursorY = Math.min(root.getHeight(), cursorY + step);
                    updateCursorPosition();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    toggleCursorMode();
                    return true;
                default:
                    return false;
            }
        }

        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                webView.evaluateJavascript("window.__tvMove && window.__tvMove('left');", null);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                webView.evaluateJavascript("window.__tvMove && window.__tvMove('right');", null);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                webView.evaluateJavascript(
                        "window.__tvScroll ? window.__tvScroll(-300) : window.scrollBy(0,-300);", null);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                webView.evaluateJavascript(
                        "window.__tvScroll ? window.__tvScroll(300) : window.scrollBy(0,300);", null);
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                return false;
            default:
                return false;
        }
    }
}
