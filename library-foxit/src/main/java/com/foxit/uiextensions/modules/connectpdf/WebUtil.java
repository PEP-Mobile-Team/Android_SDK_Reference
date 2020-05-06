/**
 * Copyright (C) 2003-2019, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.modules.connectpdf;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.UIMatchDialog;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class WebUtil {
    public static String URLEncode(String str) {
        try {
            str = URLEncoder.encode(str, "UTF-8");
            str = str.replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
        }
        return str;
    }

    public static WebView createWebView(Context context) {
        WebView webView = new WebView(context);
        webView.requestFocus();
        WebViewClient wvClient = new WebViewClient() {
            @Override
            public void onLoadResource(WebView view, String url) {
                super.onLoadResource(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
            }
        };
        webView.setWebViewClient(wvClient);
        webView.setWebChromeClient(new WebChromeClient());
        webView.getSettings().setSupportZoom(false);
        webView.getSettings().setBuiltInZoomControls(false);
        webView.getSettings().setJavaScriptEnabled(true);
        AppUtil.setWebViewZoomControlButtonGone(webView);

        boolean isPad = AppDisplay.getInstance(context).isPad();
        if (!isPad) {
            webView.setPadding(0, 0, 0, AppDisplay.getInstance(context).dp2px(56));
        }
        webView.setBackgroundColor(Color.WHITE);

        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);

        return webView;
    }

    public static UIMatchDialog openCpdfWebPage(final Activity activity, final String title, final String url, final Object jsInterface) {
        class Result {
            public Object			mResult;
        }
        final Result result = new Result();
        final Context mContext = activity.getApplicationContext();

        Runnable runnable = new Runnable() {
            UIMatchDialog   homePageDialog;
            @SuppressLint("JavascriptInterface")
            @Override
            public void run() {
                homePageDialog = new UIMatchDialog(
                        activity,
                        R.style.rd_dialog_fullscreen_style, false);
                homePageDialog.setFullScreenWithStatusBar();
                homePageDialog.setStyle(UIMatchDialog.DLG_TITLE_STYLE_BG_BLUE);
                homePageDialog.setTitleBlueLineVisible(false);

                final BaseItemImpl webBackItem = new BaseItemImpl(mContext);
                final BaseItemImpl webForwardItem = new BaseItemImpl(mContext);
                LinearLayout homePageTitleLayout = (LinearLayout) homePageDialog.getRootView().findViewById(R.id.dlg_top_title);
                RelativeLayout ll = (RelativeLayout) View.inflate(
                        activity,
                        R.layout.connectedpdf_homepage_webview, null);
                RelativeLayout homePageBottomBarLayout = (RelativeLayout) ll.findViewById(R.id.homepage_bottom_bar);

                final WebView webView = (WebView) ll.findViewById(R.id.homepage_webview);
                webView.requestFocus();
                WebViewClient wvc = new WebViewClient() {
                    @Override
                    public void onLoadResource(WebView view, String url) {
                        super.onLoadResource(view, url);
                        webBackItem.setEnable(webView.canGoBack());
                        webForwardItem.setEnable(webView.canGoForward());
                    }

                    //@Override
                    //public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError err) {
                    //    handler.proceed();
                    //}
                };

                webView.setWebViewClient(wvc);
                webView.setWebChromeClient(new WebChromeClient());

                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setSupportZoom(true);
                webView.getSettings().setBuiltInZoomControls(true);

                AppUtil.setWebViewZoomControlButtonGone(webView);

                boolean isPad = AppDisplay.getInstance(activity.getApplicationContext()).isPad();
                if (!isPad) {
                    webView.setPadding(0, 0, 0, AppDisplay.getInstance(activity.getApplicationContext()).dp2px(56));
                }

                if (jsInterface != null) {
                    webView.addJavascriptInterface(jsInterface, "external");
                }

                homePageDialog.setContentView(ll);

                webView.loadUrl(url);
                initBar(ll, homePageTitleLayout, homePageBottomBarLayout, webBackItem,
                        webForwardItem, webView, homePageDialog, isPad, title);
                homePageDialog.setOnDLDismissListener(new MatchDialog.DismissListener() {
                    @Override
                    public void onDismiss() {
                        webView.loadUrl("");
                        webView.destroy();
                    }
                });
                homePageDialog.showDialog();

                result.mResult = homePageDialog;
            }

            private void initBar(RelativeLayout rootLayout, LinearLayout topBarLayout, final RelativeLayout bottomBarLayout,
                                 final BaseItemImpl webBackItem, BaseItemImpl webForwardItem, final WebView webView,
                                 final UIMatchDialog dialog, boolean isPad, String title) {
                BaseBar topBar;
                if (isPad) {
                    topBar = new BaseBarImpl(mContext);
                } else {
                    topBar = new TopBarImpl(mContext);
                }
                topBar.setBackgroundResource(R.color.ux_color_blue_ff179cd8);

                BaseBar bottomBar = new BottomBarImpl(mContext);
                bottomBar.setBackgroundColor(
                        mContext.getResources().getColor(R.color.ux_color_grey_fffafafa));

                BaseItemImpl backItem = new BaseItemImpl(mContext);
                BaseItemImpl titleItem = new BaseItemImpl(mContext);
                titleItem.setText(title);
                titleItem.setTextSize(18.0f);
                backItem.setImageResource(R.drawable.cloud_back);
                titleItem.setTextColor(R.color.ux_color_white);
                topBar.addView(backItem, BaseBar.TB_Position.Position_LT);
                topBar.addView(titleItem, BaseBar.TB_Position.Position_LT);
                if (isPad) {
                    webBackItem.setImageResource(R.drawable.cpdf_homepage_back_pad_selector);
                    webForwardItem.setImageResource(R.drawable.cpdf_homepage_next_pad_selector);
                    webBackItem.setRelation(BaseItemImpl.RELATION_RIGNT);
                    webBackItem.setInterval(120);
                    topBar.addView(webBackItem, BaseBar.TB_Position.Position_RB);
                    topBar.addView(webForwardItem, BaseBar.TB_Position.Position_RB);
                } else {
                    webBackItem.setImageResource(R.drawable.cpdf_homepage_back_phone_selector);
                    webForwardItem.setImageResource(R.drawable.cpdf_homepage_next_phone_selector);
                    bottomBar.setItemInterval(AppDisplay.getInstance(activity.getApplicationContext()).dp2px(90));
                    bottomBar.addView(webBackItem, BaseBar.TB_Position.Position_CENTER);
                    bottomBar.addView(webForwardItem, BaseBar.TB_Position.Position_CENTER);
                }
                webBackItem.setEnable(false);
                webForwardItem.setEnable(false);
                backItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                webBackItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        webView.goBack();
                    }
                });
                webForwardItem.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        webView.goForward();
                    }
                });
                topBarLayout.removeAllViews();
                topBarLayout.addView(topBar.getContentView());
                bottomBarLayout.removeAllViews();
                if (!isPad) {
                    bottomBarLayout.addView(bottomBar.getContentView());
                }
                //toolbarShowBtn
                if (!isPad) {
                    final ImageView toolbarShowBtn = new ImageView(mContext);
                    toolbarShowBtn.setImageResource(R.drawable.cpdf_homepage_toolbar_show);
                    toolbarShowBtn.setVisibility(View.GONE);
                    toolbarShowBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            bottomBarLayout.setVisibility(View.VISIBLE);
                            toolbarShowBtn.setVisibility(View.GONE);
                        }
                    });
                    RelativeLayout.LayoutParams toolbarShowBtnParams = new RelativeLayout.LayoutParams(
                            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    toolbarShowBtnParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                    toolbarShowBtnParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
                    toolbarShowBtnParams.rightMargin = AppDisplay.getInstance(activity.getApplicationContext()).dp2px(10);
                    toolbarShowBtnParams.bottomMargin = AppDisplay.getInstance(activity.getApplicationContext()).dp2px(10);
                    rootLayout.addView(toolbarShowBtn, toolbarShowBtnParams);

                    webView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            if (event.getAction() == MotionEvent.ACTION_UP) {
                                bottomBarLayout.setVisibility(View.GONE);
                                toolbarShowBtn.setVisibility(View.VISIBLE);
                            }
                            return false;
                        }
                    });
                }
            }
        };

        runnable.run();
        return (UIMatchDialog) result.mResult;
    }
}
