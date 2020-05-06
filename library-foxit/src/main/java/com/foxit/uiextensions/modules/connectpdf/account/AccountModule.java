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
package com.foxit.uiextensions.modules.connectpdf.account;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;

import com.foxit.sdk.Localization;
import com.foxit.uiextensions.BuildConfig;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig;
import com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.modules.connectpdf.WebUtil;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.config.AppBuildConfig;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppSystemUtil;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.Locale;

import androidx.fragment.app.FragmentActivity;
/**  Login module for foxit account.*/
public class AccountModule implements Module, ILifecycleEventListener {
    private static final String CPDF_URL_SIGNIN = "https://cws-fz09.connectedpdf.com/site/plugin-sign-in";
    private static final String CPDF_URL_SIGNOUT = "https://cas-fz09.connectedpdf.com/cas/logout";


    private BaseItemImpl mModuleTitleItem;
    private Dialog mLoginDialog;
    private RelativeLayout mLoginContentView;
    private RelativeLayout mLoginDialogRootView;

    private RelativeLayout mLoginView;
    private BaseBarImpl mLoginDialogTopToolBar;

    private boolean mRememberUser = true;
    private String mToken;
    private String mUserID;
    private String mEmail;
    private String mUserName;
    private String mFirstName;
    private String mLastName;
    private String mAvatar;
    private String mFullName;

    private FragmentActivity mActivity;
    private Context mContext = null;
    private static AccountModule mAccount;
    public static AccountModule getInstance() {
        if (mAccount == null) {
            mAccount = new AccountModule();
        }
        return mAccount;
    }

    private AccountModule() {
    }


    @Override
    public String getName() {
        return Module.MODULE_NAME_ACCOUNT;
    }

    @Override
    public boolean loadModule() {
        mLoginContentView = new RelativeLayout(mContext);
        mLoginContentView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mLoginContentView.setGravity(Gravity.CENTER_HORIZONTAL);

        mModuleTitleItem = new BaseItemImpl(mContext);
        mModuleTitleItem.setText(AppResource.getString(mContext, R.string.connected_pdf_login_title_text));
        mModuleTitleItem.setTextColor(Color.WHITE);
        mModuleTitleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));

        mLoginView = new RelativeLayout(mContext);
        return true;
    }

    @Override
    public boolean unloadModule() {
        return true;
    }

    WebView mLoginWebView = null;
    RelativeLayout getLoginView() {
        if (mLoginWebView == null) {
            Locale locale = Localization.getCurrentLanguage(mContext);
            mLoginWebView = WebUtil.createWebView(mContext);
            Localization.setCurrentLanguage(mContext, locale);
            mLoginWebView.setWebViewClient(new WebViewClient() {
                @Override
                public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                    super.onReceivedError(view, request, error);
                    if (view == mLoginWebView) {
                        mLoginWebView.setTag(R.id.login_webview_end_point_tag_key, null);
                    }
                }

                @Override
                public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                    super.onReceivedError(view, errorCode, description, failingUrl);
                    if (view == mLoginWebView) {
                        mLoginWebView.setTag(R.id.login_webview_end_point_tag_key, null);
                    }
                }
            });

            mLoginWebView.addJavascriptInterface(new LoginJavaScriptObject(), "external");  //TODO
            mLoginView.addView(mLoginWebView,
                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        } else {
            String curEndPoint = (String)mLoginWebView.getTag(R.id.login_webview_end_point_tag_key);
            String endPoint = AppBuildConfig.getEndPointServerAddr();
            if (mLoginWebView.getTag() != null || !AppUtil.isEqual(endPoint, curEndPoint)) {
                mLoginView.removeView(mLoginWebView);
                mLoginWebView = WebUtil.createWebView(mContext);
                mLoginWebView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                        super.onReceivedError(view, request, error);
                        if (view == mLoginWebView) {
                            mLoginWebView.setTag(R.id.login_webview_end_point_tag_key, null);
                        }
                    }

                    @Override
                    public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                        super.onReceivedError(view, errorCode, description, failingUrl);
                        if (view == mLoginWebView) {
                            mLoginWebView.setTag(R.id.login_webview_end_point_tag_key, null);
                        }
                    }
                });

                mLoginWebView.addJavascriptInterface(new LoginJavaScriptObject(), "external"); //TODO
                mLoginView.addView(mLoginWebView,
                        new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            }
        }

        String url = CPDF_URL_SIGNIN;
        StringBuilder reqUrl = new StringBuilder();
        reqUrl.append(url);
        reqUrl.append("?al=" + AppSystemUtil.getLanguageWithCountry(mContext));
        reqUrl.append("&an=" + WebUtil.URLEncode("Foxit RDK"));
        reqUrl.append("&av=" + BuildConfig.VERSION_NAME);

        String curUrl = mLoginWebView.getUrl();
        if (!AppUtil.isEqual(reqUrl.toString(), curUrl)) {
            mLoginWebView.loadUrl(reqUrl.toString());
        } else {
        }
        mLoginWebView.setTag(R.id.login_webview_end_point_tag_key, AppBuildConfig.getEndPointServerAddr());

        return mLoginView;
    }

    public void showLoginDialog(final IResult<Void, Void, Void> result) {
        showLoginDialog(mActivity, result);
    }

    public void showLoginDialog(final Activity act, final IResult<Void, Void, Void> result) {
        if (mLoginDialog != null) {
            if (mLoginDialog.isShowing()) {
                return;
            }
        }
        mLoginDialog = new Dialog(act, R.style.rd_dialog_fullscreen_style);
        mLoginDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        mLoginDialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        mLoginDialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        if (mLoginDialogRootView == null) {
            initLoginDialogRootView();
        } else {
            if (mLoginDialogRootView.getParent() != null)
                ((ViewGroup) mLoginDialogRootView.getParent()).removeView(mLoginDialogRootView);
        }
        mLoginDialog.setContentView(mLoginDialogRootView);

        if (mLoginContentView.getParent() != null)
            ((ViewGroup) mLoginContentView.getParent()).removeView(mLoginContentView);
        RelativeLayout loginDialogContent = (RelativeLayout) mLoginDialogRootView.findViewById(R.id.contentview);
        loginDialogContent.removeAllViews();
        loginDialogContent.addView(mLoginContentView);

        mLoginDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (result != null) {
                    result.onResult(!AppUtil.isEmpty(mToken), null, null, null);
                }
            }
        });
        mLoginDialog.setCanceledOnTouchOutside(false);
        mLoginDialog.show();
        mModuleTitleItem.setText(AppResource.getString(mContext, R.string.connected_pdf_login_title_text));
        mLoginContentView.removeAllViews();
        mLoginContentView.addView(getLoginView());
        mLoginDialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override
            public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_BACK) {
                    if (mLoginView.getParent() == null) {//if not show loginView in mLoginContentView,then remove other and add it to mLoginContentView.
                        mLoginContentView.removeAllViews();
                        mModuleTitleItem.setText(AppResource.getString(mContext, R.string.connected_pdf_login_title_text));
                        mLoginContentView.addView(getLoginView());
                        return true;
                    }
                }
                return false;
            }
        });
    }

    private void initLoginDialogRootView() {
        if (AppDisplay.getInstance(mContext).isPad())
            mLoginDialogRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_pad, null);
        else
            mLoginDialogRootView = (RelativeLayout) View.inflate(mContext, R.layout.hf_home_right_phone, null);

        RelativeLayout loginDialogTitle = ((RelativeLayout) mLoginDialogRootView.findViewById(R.id.toptoolbar));
        mLoginDialogTopToolBar = new TopBarImpl(mContext);
        mLoginDialogTopToolBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_color_blue_ff179cd8));
        BaseItemImpl closeItem = new BaseItemImpl(mContext);
        closeItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_BACK);
        closeItem.setImageResource(R.drawable.panel_topbar_close_selector);
        closeItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLoginDialog.dismiss();
            }
        });

        BaseItemImpl dialogTitleItem = new BaseItemImpl(mContext);
        dialogTitleItem.setText(AppResource.getString(mContext, R.string.connected_pdf_login_title_text));
        dialogTitleItem.setTextColor(Color.WHITE);
        dialogTitleItem.setTag(ToolbarItemConfig.ITEM_TOPBAR_BACK + 1);
        dialogTitleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimensionPixelOffset(R.dimen.ux_text_height_title)));

        mLoginDialogTopToolBar.addView(closeItem, BaseBar.TB_Position.Position_LT);
        mLoginDialogTopToolBar.addView(dialogTitleItem, BaseBar.TB_Position.Position_LT);
        loginDialogTitle.addView(mLoginDialogTopToolBar.getContentView());

    }

    public void showLogoutDialog() {
        showLogoutDialog(mActivity);
    }

    public void showLogoutDialog(Activity activity) {
        String message = mContext.getString(R.string.sign_out_tips);
        String title = "";
        Dialog dialog = new AlertDialog.Builder(activity).setCancelable(true).setTitle(title)
                .setMessage(message)
                .setPositiveButton(mContext.getString(R.string.fx_string_yes),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                signOut();
                                onSignOut(true);
                                dialog.dismiss();
                            }
                        }).setNegativeButton(mContext.getString(R.string.fx_string_no),
                        new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).create();
        dialog.show();
    }

    private void signOut() {
        if (mLoginWebView != null) {
            final String url = CPDF_URL_SIGNOUT;
            mLoginWebView.setTag(url);
            AppThreadManager.getInstance().runOnUiThreadAndWait(new Runnable() {
                @Override
                public void run() {
                    mLoginWebView.loadUrl(url);
                    mLoginWebView.setTag(url);
                    mToken = null;
                }
            });
        }
    }

    @Override
    public void onCreate(Activity act, Bundle savedInstanceState) {
        mActivity = (FragmentActivity) act;
        mContext = mActivity.getApplicationContext();
        loadModule();
    }

    @Override
    public void onStart(Activity act) {

    }

    @Override
    public void onPause(Activity act) {

    }

    @Override
    public void onResume(Activity act) {

    }

    @Override
    public void onStop(Activity act) {

    }

    @Override
    public void onDestroy(Activity act) {
        mLoginDialog = null;
        mActivity = null;
        mAccount = null;
    }

    @Override
    public void onSaveInstanceState(Activity act, Bundle bundle) {

    }

    @Override
    public void onHiddenChanged(boolean hidden) {
    }

    @Override
    public void onActivityResult(Activity act, int requestCode, int resultCode, Intent data) {

    }

    class LoginJavaScriptObject {
        @JavascriptInterface
        public String DispatchFun(String module, final String fun, String var) {

            if (module != null && module.length() > 0 && fun != null && fun.length() > 0) {
                switch (fun) {
                    case "IsRemember": {
                        break;
                    }
                    case "SetUserIdAndUserToken": {
                        try {
                            JSONTokener jsonparse = new JSONTokener(var);
                            JSONObject root = (JSONObject) jsonparse.nextValue();
                            if (!root.isNull("access_token")
                                    && !root.isNull("user_email")
                                    && !root.isNull("is_remember")
                                    && !root.isNull("login_host")
                                    ) {
                                final String endPoint = root.getString("login_host");
                                if (!AppUtil.isEqual(endPoint, AppBuildConfig.getEndPointServerAddr())) {
                                    AppBuildConfig.setEndPointServerAddr(endPoint, null);
                                }
                                AppBuildConfig.setLastEndPointServerAddr(endPoint);

                                mLoginDialog.dismiss();

                                mToken = root.getString("access_token");
                                mEmail = root.getString("user_email");
                                // default remember for mobile client
                                mRememberUser = true; //(root.getInt("is_remember") != 0);

                                onSignIn(true);
                            }
                        } catch (Exception e) {
                            onSignIn(false);
                            e.printStackTrace();
                        }
                        break;
                    }
                    case "openWebPage": {
                        final String url = var;
                        AppThreadManager.getInstance().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String title = " ";
                                WebUtil.openCpdfWebPage(mActivity, title, url, null);
                            }
                        });
                        break;
                    }
                    case "GetEnterpPageDefaultDisplayHost": {
                        if (AppBuildConfig.isEntEndPoint()) {
                            return AppBuildConfig.getEndPointServerAddr();
                        } else {
                            return AppBuildConfig.getLastEndPointServerAddr();
                        }
                    }
                }

            }
            return "";
        }
    }

    public String getUserToken() {
        return mToken;
    }

    public interface IAccountEventListener {
        void onSignIn(boolean success);
        void onSignOut(boolean success);
    }

    private ArrayList<IAccountEventListener> mAccountEventListeners = new ArrayList<IAccountEventListener>();
    public void registerAccountEventListener(IAccountEventListener listener) {
        mAccountEventListeners.add(listener);
    }

    public void unregisterAccountEventListener(IAccountEventListener listener) {
        mAccountEventListeners.remove(listener);
    }

    private void onSignIn(boolean success) {
        for (IAccountEventListener listener: mAccountEventListeners) {
            listener.onSignIn(success);
        }
    }

    private void onSignOut(boolean success) {
        for (IAccountEventListener listener: mAccountEventListeners) {
            listener.onSignOut(success);
        }
    }
}
