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
package com.foxit.uiextensions.modules;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;

/** The module provide auto-brightness of screen control..*/
public class BrightnessModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;

    private IMultiLineBar mSettingBar;
    private boolean mLinkToSystem = true;
    private int mBrightnessSeekValue = 3;//0-255
    private boolean mNightMode = false;
    private PDFViewCtrl.UIExtensionsManager mUiExtensionsManager;

    public BrightnessModule(Context context, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_BRIGHTNESS;
    }

    @Override
    public boolean loadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).registerLifecycleListener(mLifecycleEventListener);
            ((UIExtensionsManager) mUiExtensionsManager).registerModule(this);
        }

        return true;
    }

    @Override
    public boolean unloadModule() {
        if (mUiExtensionsManager != null && mUiExtensionsManager instanceof UIExtensionsManager) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterLifecycleListener(mLifecycleEventListener);
        }
        return true;
    }

    private void initValue() {
        mNightMode = false;
        //colorMode
        String colorMode = ((UIExtensionsManager) mUiExtensionsManager).getConfig().uiSettings.colorMode;
        if ("Night".equals(colorMode)){
            mNightMode = true;
            mPdfViewCtrl.setBackgroundResource(R.color.ux_bg_color_docviewer_night);
            mPdfViewCtrl.setNightMode(mNightMode);
        }
        // set value with the value of automatic brightness setting from system.
        mLinkToSystem = true;//true;
        mBrightnessSeekValue = getSavedBrightSeekValue();
    }

    private void initMLBarValue() {
        mSettingBar = ((UIExtensionsManager)mUiExtensionsManager).getMainFrame().getSettingBar();
        if (mSettingBar == null) return;
        if (!mNightMode) {
            mSettingBar.setProperty(IMultiLineBar.TYPE_DAYNIGHT, true);
        } else {
            mSettingBar.setProperty(IMultiLineBar.TYPE_DAYNIGHT, false);
        }
        mSettingBar.setProperty(IMultiLineBar.TYPE_SYSLIGHT, mLinkToSystem);
        mSettingBar.setProperty(IMultiLineBar.TYPE_LIGHT, mBrightnessSeekValue);
    }

    private void applyValue() {
        if (mLinkToSystem) {
            setSystemBrightness();
        } else {
            setManualBrightness();
        }
    }

    private void registerMLListener() {
        if (mSettingBar == null) return;
        mSettingBar.registerListener(mDayNightModeChangeListener);
        mSettingBar.registerListener(mLinkToSystemChangeListener);
        mSettingBar.registerListener(mBrightnessSeekValueChangeListener);
    }

    private void unRegisterMLListener() {
        if (mSettingBar == null) return;
        mSettingBar.unRegisterListener(mDayNightModeChangeListener);
        mSettingBar.unRegisterListener(mLinkToSystemChangeListener);
        mSettingBar.unRegisterListener(mBrightnessSeekValueChangeListener);
    }

    private int getSavedBrightSeekValue() {
        int progress;
        progress = getSysBrightnessProgress();
        if (progress <= 0 || progress > 255) {
            progress = (int) (0.4 * 255);
        }
        return progress;
    }

    private int getSysBrightnessProgress() {
        int progress = 3;
        // remove check isAutoBrightness;
        // so the value of "progress" will be same as the value of system screen brightness
        try {
            progress = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
            progress = (int) (0.4 * 255);
        }

        return progress;
    }


    private void setSystemBrightness() {
        Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        activity.getWindow().setAttributes(params);
    }

    private void setManualBrightness() {
        if (mBrightnessSeekValue <= 0 || mBrightnessSeekValue > 255) {
            mBrightnessSeekValue = getSysBrightnessProgress();
        }
        Activity activity = ((UIExtensionsManager)mPdfViewCtrl.getUIExtensionsManager()).getAttachedActivity();
        WindowManager.LayoutParams params = activity.getWindow().getAttributes();
        if (mBrightnessSeekValue < 3) {
            params.screenBrightness = 0.01f;
        } else {
            params.screenBrightness = mBrightnessSeekValue / 255.0f;
        }
        activity.getWindow().setAttributes(params);
        if (mBrightnessSeekValue < 1) {
            mBrightnessSeekValue = 1;
        }
        if (mBrightnessSeekValue > 255) {
            mBrightnessSeekValue = 255;
        }
    }

    IMultiLineBar.IValueChangeListener mLinkToSystemChangeListener = new IMultiLineBar.IValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_SYSLIGHT) {
                mLinkToSystem = (Boolean) value;
                if (mLinkToSystem) {
                    setSystemBrightness();
                } else {
                    setManualBrightness();
                }
            }
        }

        @Override
        public void onDismiss() {

        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_SYSLIGHT;
        }
    };

    IMultiLineBar.IValueChangeListener mBrightnessSeekValueChangeListener = new IMultiLineBar.IValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_LIGHT) {
                mBrightnessSeekValue = (Integer) value;
                if (mLinkToSystem) {
                } else {
                    if (mBrightnessSeekValue <= 1) {
                        mBrightnessSeekValue = 1;
                    }
                    setManualBrightness();
                }
            }
        }

        @Override
        public void onDismiss() {
            if (mBrightnessSeekValue < 1) {
                mBrightnessSeekValue = 1;
            }
            if (mBrightnessSeekValue > 255) {
                mBrightnessSeekValue = 255;
            }
        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_LIGHT;
        }
    };

    IMultiLineBar.IValueChangeListener mDayNightModeChangeListener = new IMultiLineBar.IValueChangeListener() {
        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_DAYNIGHT) {
                if ((Boolean) value) {
                    mNightMode = false;
                    mPdfViewCtrl.setBackgroundResource(R.color.ux_bg_color_docviewer);
                } else {
                    mNightMode = true;
                    mPdfViewCtrl.setBackgroundResource(R.color.ux_bg_color_docviewer_night);
                }
                mPdfViewCtrl.setNightMode(mNightMode);
            }
        }

        @Override
        public void onDismiss() {
        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_DAYNIGHT;
        }
    };

    IStateChangeListener mWindowDismissListener = new IStateChangeListener() {
        public void onStateChanged(int oldState, int newState) {
            if (newState != oldState && oldState == ReadStateConfig.STATE_EDIT) {
                if (mBrightnessSeekValue < 1) {
                    mBrightnessSeekValue = 1;
                }
                if (mBrightnessSeekValue > 255) {
                    mBrightnessSeekValue = 255;
                }
            }
        }
    };

    private ILifecycleEventListener mLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onCreate(Activity act, Bundle savedInstanceState) {
            super.onCreate(act, savedInstanceState);
            initValue();
        }

        @Override
        public void onStart(Activity act) {
            initMLBarValue();
            applyValue();
            registerMLListener();
            ((UIExtensionsManager) mUiExtensionsManager).registerStateChangeListener(mWindowDismissListener);
        }

        @Override
        public void onStop(Activity act) {
            ((UIExtensionsManager) mUiExtensionsManager).unregisterStateChangeListener(mWindowDismissListener);
        }

        @Override
        public void onDestroy(Activity act) {
            unRegisterMLListener();
        }
    };
}
