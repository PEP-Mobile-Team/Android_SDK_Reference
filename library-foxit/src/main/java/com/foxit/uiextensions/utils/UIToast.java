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
package com.foxit.uiextensions.utils;

import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.foxit.uiextensions.R;

public class UIToast {

    private static final int INIT = 0xF01;
    private static final int SHOW = 0xF02;
    private static final int HIDE = 0xF03;
    private static final int LONG = Toast.LENGTH_LONG;
    private static final int LAST = 0x2;
    private static final String MODE = "mode";
    private static final String MSG = "message";

    private Context mContext;
    private Toast mToast;
    private Toast mAnnotToast;
    private AppDisplay mAppDisplay;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {

        @Override
        public void handleMessage(Message msg) {
            if (msg.what == INIT) {
                mToast = Toast.makeText(mContext, "", Toast.LENGTH_LONG);
            } else if (msg.what == SHOW) {
                if (mToast == null) return;
                Bundle bundle = msg.getData();
                if (bundle == null) return;
                Object obj = bundle.get(MSG);
                if (obj == null) return;
                try {
                    if (obj instanceof Integer) {
                        mToast.setText(Integer.class.cast(obj));
                    } else if (obj instanceof CharSequence) {
                        mToast.setText(CharSequence.class.cast(obj));
                    }
                } catch (NotFoundException e) {
                    e.printStackTrace();
                }
                int mode = bundle.getInt(MODE, LONG);
                if (mode == LAST) {
                    mToast.setDuration(Toast.LENGTH_LONG);
                    mToast.show();
                    Message m = Message.obtain();
                    m.copyFrom(msg);
                    mHandler.sendMessageDelayed(m, 3000);
                } else {
                    mHandler.removeMessages(SHOW);
                    mToast.setDuration(mode);
                    mToast.show();
                }
            } else if (msg.what == HIDE) {
                mHandler.removeMessages(SHOW);
                if (mToast != null) {
                    mToast.cancel();
                }
            }
        }
    };

    private static UIToast mInstance = null;

    public static UIToast getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new UIToast(context);
        }
        return mInstance;
    }

    private UIToast(Context context) {
        mContext = context;
        mAppDisplay = AppDisplay.getInstance(context);
        mHandler.obtainMessage(INIT).sendToTarget();
        initAnnotToast();
    }

    public void show(int resId) {
        Message msg = mHandler.obtainMessage(SHOW);
        Bundle data = new Bundle();
        data.putInt(MSG, resId);
        data.putInt(MODE, LONG);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void show(CharSequence s) {
        Message msg = mHandler.obtainMessage(SHOW);
        Bundle data = new Bundle();
        data.putCharSequence(MSG, s);
        data.putInt(MODE, LONG);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void show(int resId, int duration) {
        Message msg = mHandler.obtainMessage(SHOW);
        Bundle data = new Bundle();
        data.putInt(MSG, resId);
        data.putInt(MODE, duration);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void show(CharSequence s, int duration) {
        Message msg = mHandler.obtainMessage(SHOW);
        Bundle data = new Bundle();
        data.putCharSequence(MSG, s);
        data.putInt(MODE, duration);
        msg.setData(data);
        msg.sendToTarget();
    }

    public void show(int resId, long timeout) {
        mHandler.removeMessages(SHOW);
        Message msg = Message.obtain();
        msg.what = SHOW;
        Bundle data = new Bundle();
        data.putInt(MSG, resId);
        data.putInt(MODE, LAST);
        msg.setData(data);
        msg.sendToTarget();
        mHandler.sendEmptyMessageDelayed(HIDE, timeout);
    }

    public void show(CharSequence s, long timeout) {
        mHandler.removeMessages(SHOW);
        Message msg = mHandler.obtainMessage(SHOW);
        Bundle data = new Bundle();
        data.putCharSequence(MSG, s);
        data.putInt(MODE, LAST);
        msg.setData(data);
        msg.sendToTarget();
        mHandler.sendEmptyMessageDelayed(HIDE, timeout);
    }

    private void initAnnotToast() {
        try {
            mAnnotToast = new Toast(mContext);
            LayoutInflater inflate = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View toastLayout = inflate.inflate(R.layout.annot_continue_create_tips, null);
            mAnnotToast.setView(toastLayout);
            mAnnotToast.setDuration(Toast.LENGTH_SHORT);
            int yOffset;
            if (mAppDisplay.isPad()) {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_pad) + mAppDisplay.dp2px(16) * (2 + 1);
            } else {
                yOffset = AppResource.getDimensionPixelSize(mContext, R.dimen.ux_toolbar_height_phone) + mAppDisplay.dp2px(16) * (2 + 1);
            }
            mAnnotToast.setGravity(Gravity.BOTTOM, 0, yOffset);
        } catch (Exception e) {
            mAnnotToast = null;
        }
    }
}
