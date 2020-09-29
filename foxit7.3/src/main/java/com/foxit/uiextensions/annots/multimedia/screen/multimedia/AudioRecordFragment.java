/**
 * Copyright (C) 2003-2020, Foxit Software Inc..
 * All Rights Reserved.
 * <p>
 * http://www.foxitsoftware.com
 * <p>
 * The following code is copyrighted and is the proprietary of Foxit Software Inc.. It is not allowed to
 * distribute any parts of Foxit PDF SDK to third party or public without permission unless an agreement
 * is signed between Foxit Software Inc. and customers to explicitly grant customers permissions.
 * Review legal.txt for additional license and legal information.
 */
package com.foxit.uiextensions.annots.multimedia.screen.multimedia;


import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.multimedia.MultimediaUtil;
import com.foxit.uiextensions.annots.multimedia.screen.MultimediaSupport;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;

public class AudioRecordFragment extends DialogFragment implements MultimediaUtil.IRecordFinishCallback {

    private static final int RECORD_READY = 111;
    private static final int RECORD_START = 222;
    private static final int RECORD_STOP = 333;
    private static final int RECORD_CANEL = 444;

    private MultimediaSupport.IPickResultListener mPickListener;
    private MultimediaUtil mMultimediaUtil;
    private Context mContext;

    private PDFViewCtrl mPDFViewCtrl;
    private TextView mRecordText;
    private ImageView mRecordIcon;
    private Chronometer mChronometer;

    private int mState = RECORD_READY;
    private int mLastState = RECORD_READY;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getActivity().getApplicationContext();
    }

    public void init(PDFViewCtrl pdfViewCtrl){
        mPDFViewCtrl = pdfViewCtrl;
        ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).registerLayoutChangeListener(mLayoutChangeListener);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_audio_record, container, false);
        LinearLayout ll_record_container = view.findViewById(R.id.ll_record_container);

        mRecordText = view.findViewById(R.id.record_audio_text);
        mRecordIcon = view.findViewById(R.id.record_src);
        mChronometer = view.findViewById(R.id.audio_time_display);

        ll_record_container.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mLastState = mState;
                if (RECORD_READY == mState) {
                    mState = RECORD_START;
                } else if (RECORD_START == mState) {
                    mState = RECORD_STOP;
                }

                handler.sendEmptyMessage(mState);
            }
        });
        mMultimediaUtil = new MultimediaUtil(getActivity().getApplicationContext());
        mMultimediaUtil.setRecordFinishCallback(this);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                mLastState = mState;
                mState = RECORD_CANEL;
                handler.sendEmptyMessage(RECORD_CANEL);
                dismiss();
                return true;
            }
        });
        return view;
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECORD_START:
                    mRecordText.setVisibility(View.GONE);

                    mChronometer.setVisibility(View.VISIBLE);
                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.setFormat("%S");
                    mChronometer.start();

                    mRecordIcon.setImageResource(R.drawable.audio_stop_icon);
                    mMultimediaUtil.startRecordAudio();
                    break;
                case RECORD_STOP:
                    mChronometer.stop();
                    mMultimediaUtil.stopRecordAudio();
                    break;
                case RECORD_CANEL:
                    if (RECORD_START == mLastState) {
                        mChronometer.stop();
                    }
                    mMultimediaUtil.releaseAudioRecord();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    public void onCancel(DialogInterface dialog) {
        mLastState = mState;
        mState = RECORD_CANEL;
        handler.sendEmptyMessage(RECORD_CANEL);
        super.onCancel(dialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        refreshLayout();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).unregisterLayoutChangeListener(mLayoutChangeListener);
    }

    private void refreshLayout(){
        View rootView = ((UIExtensionsManager)mPDFViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = rootView.getWidth();
        int height = rootView.getHeight();

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowParams.dimAmount = 0.0f;
        windowParams.x = x;
        windowParams.y = y;
        windowParams.width = width;
        windowParams.height = height;
        window.setAttributes(windowParams);
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getDialog().setCanceledOnTouchOutside(true);
    }

    public void setOnPickPicListener(MultimediaSupport.IPickResultListener listener) {
        this.mPickListener = listener;
    }

    @Override
    public void onSuccessed(File file) {
        if (mPickListener != null) {

            switch (mState) {
                case RECORD_STOP:
                    if (file != null && file.exists()) {
                        mPickListener.onResult(true, file.getAbsolutePath());
                    } else {
                        mPickListener.onResult(false, null);
                    }
                    break;
                case RECORD_CANEL:
                    if (file != null && file.exists()) {
                        file.delete();
                    }
                    break;
                default:
                    break;

            }
        }
        dismiss();
    }

    @Override
    public void onFailed() {
        UIToast.getInstance(mContext).show(mContext.getString(R.string.record_failed));
        dismiss();
    }

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (null != getDialog() && getDialog().isShowing()){
                if (newWidth != oldWidth || newHeight != oldHeight){
                    refreshLayout();
                }
            }
        }
    };

}
