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

    private boolean cursorMode = false;
    private float cursorX, cursorY;
    private float density;

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
        if (cursorMode && cursorX == 0 && cursorY == 0) {
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

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            long upTime = SystemClock.uptimeMillis();
            MotionEvent up = MotionEvent.obtain(downTime, upTime,
                    MotionEvent.ACTION_UP, cursorX, cursorY, 0);
            webView.dispatchTouchEvent(up);
            up.recycle();
        }, 60);
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) {
            toggleCursorMode();
            return true;
        }
        return super.onKeyLongPress(keyCode, event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
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
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    if (event.getRepeatCount() == 0) {
                        dispatchTapAtCursor();
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    toggleCursorMode();
                    return true;
                default:
                    return super.onKeyDown(keyCode, event);
            }
        }

        String js = null;
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                js = "window.__tvMove && window.__tvMove('left');";
                break;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                js = "window.__tvMove && window.__tvMove('right');";
                break;
            case KeyEvent.KEYCODE_DPAD_UP:
                js = "window.__tvScroll ? window.__tvScroll(-300) : window.scrollBy(0,-300);";
                break;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                js = "window.__tvScroll ? window.__tvScroll(300) : window.scrollBy(0,300);";
                break;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                js = "window.__tvClick && window.__tvClick();";
                break;
            case KeyEvent.KEYCODE_BACK:
                if (webView.canGoBack()) {
                    webView.goBack();
                    return true;
                }
                break;
            default:
                break;
        }
        if (js != null) {
            webView.evaluateJavascript(js, null);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (cursorMode) {
            toggleCursorMode();
            return;
        }
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
