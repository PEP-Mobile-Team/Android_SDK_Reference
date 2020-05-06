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
package com.foxit.uiextensions.annots.screen.multimedia;


import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.annots.screen.MultimediaSupport;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class ViedeoRecordFragment extends DialogFragment implements View.OnClickListener, MultimediaUtil.IRecordFinishCallback {
    private static final int RECORD_READY = 111;
    private static final int RECORD_START = 222;
    private static final int RECORD_STOP = 333;
    private static final int RECORD_CANEL = 444;
    private static final int RECORD_RESUME = 555;

    private static final int SWITCH_CAMERE = 666;
    private static final int SAVE_VIDEO = 777;
    private static final int PREVIEW_VIDEO = 888;

    private Context mContext;
    private MultimediaUtil mMultimediaUtil;
    private MultimediaSupport.IPickResultListener mPickListener;

    private SurfaceView mSurfaceView;
    private Chronometer mChronometer;

    private TextView mTvStartRecord;
    private ImageView mIvPreviewRecord;
    private ImageView mIvCancel;
    private ImageView mIvSwitchCamera;
    private ImageView mIvSaveVideo;
    private RelativeLayout mRLVideoBar;

    private MediaPlayer mMediaPlayer;

    private File mVideoFile;

    private int mState = RECORD_READY;
    private int mLastState = RECORD_READY;

    private int mLastScreenOrientation;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mLastScreenOrientation = getActivity().getRequestedOrientation();
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        mContext = getActivity().getApplicationContext();
        int theme;
        if (Build.VERSION.SDK_INT >= 21) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 14) {
            theme = android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen;
        } else if (Build.VERSION.SDK_INT >= 13) {
            theme = android.R.style.Theme_Holo_Light_NoActionBar_Fullscreen;
        } else {
            theme = android.R.style.Theme_Light_NoTitleBar_Fullscreen;
        }
        setStyle(STYLE_NO_TITLE, theme);

        mMultimediaUtil = new MultimediaUtil(mContext);
        mMultimediaUtil.setRecordFinishCallback(this);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = getActivity().getLayoutInflater().inflate(R.layout.fragment_video_record, container, false);

        initView(view);
        initListener();
        return view;
    }

    private void initView(View view) {
        mSurfaceView = view.findViewById(R.id.surface_view);
        mMultimediaUtil.setSurfaceView(mSurfaceView);

        mTvStartRecord = view.findViewById(R.id.tv_start_record);
        mIvPreviewRecord = view.findViewById(R.id.image_preview_video);
        mIvCancel = view.findViewById(R.id.iv_cancel_record);
        mIvSwitchCamera = view.findViewById(R.id.iv_switch_camera);
        mIvSaveVideo = view.findViewById(R.id.iv_save_video);
        mChronometer = view.findViewById(R.id.video_time_display);
        mRLVideoBar = view.findViewById(R.id.rela_bar_video);
    }

    private void initListener() {
        mIvCancel.setOnClickListener(this);
        mTvStartRecord.setOnClickListener(this);
        mIvSwitchCamera.setOnClickListener(this);
        mIvSaveVideo.setOnClickListener(this);
        mIvPreviewRecord.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int i = v.getId();

        if (AppUtil.isFastDoubleClick()){
            return;
        }

        if (i == R.id.iv_cancel_record) {

            mLastState = mState;
            mState = RECORD_CANEL;
            handler.sendEmptyMessage(mState);
        } else if (i == R.id.tv_start_record) {

            mLastState = mState;
            if (RECORD_READY == mState) {
                mState = RECORD_START;
            } else if (RECORD_START == mState) {
                mState = RECORD_STOP;
            } else if (RECORD_STOP == mState) {
                mState = RECORD_RESUME;
            }

            handler.sendEmptyMessage(mState);
        } else if (i == R.id.iv_switch_camera) {
            handler.sendEmptyMessage(SWITCH_CAMERE);
        } else if (i == R.id.iv_save_video) {
            handler.sendEmptyMessage(SAVE_VIDEO);
        } else if (i == R.id.image_preview_video) {
            handler.sendEmptyMessage(PREVIEW_VIDEO);
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case RECORD_READY:
                    mChronometer.stop();
                    mChronometer.setVisibility(View.GONE);
                    mIvSaveVideo.setVisibility(View.GONE);
                    mIvPreviewRecord.setVisibility(View.GONE);

                    mIvSwitchCamera.setVisibility(View.VISIBLE);
                    mIvCancel.setVisibility(View.VISIBLE);

                    mTvStartRecord.setText("");
                    mTvStartRecord.setBackgroundResource(R.drawable.video_start_selector);
                    break;
                case RECORD_START:
                    mChronometer.setVisibility(View.VISIBLE);
                    mIvSwitchCamera.setVisibility(View.GONE);
                    mIvSaveVideo.setVisibility(View.GONE);
                    mIvPreviewRecord.setVisibility(View.GONE);
                    mIvCancel.setVisibility(View.GONE);
                    mTvStartRecord.setText("");
                    mTvStartRecord.setBackgroundResource(R.drawable.video_stop_seletror);

                    mChronometer.setBase(SystemClock.elapsedRealtime());
                    mChronometer.setFormat("%S");
                    mChronometer.start();
                    mMultimediaUtil.startRecordVideo();
                    break;
                case RECORD_STOP:
                    mChronometer.stop();
                    mMultimediaUtil.stopRecordVideo();
                    break;
                case RECORD_CANEL:
                    if (RECORD_STOP == mLastState) {
                        if (mVideoFile != null && mVideoFile.exists()) {
                            mVideoFile.delete();
                        }
                        dismiss();
                    } else if (RECORD_READY == mLastState){
                        dismiss();
                    } else {
                        mMultimediaUtil.stopRecordVideo();
                    }
                    break;
                case RECORD_RESUME:
                    stopPlay();
                    mMultimediaUtil.startPreView(mSurfaceView.getHolder());

                    mTvStartRecord.setText("");
                    mTvStartRecord.setBackgroundResource(R.drawable.video_start_selector);
                    mLastState = mState;
                    mState = RECORD_READY;
                    handler.sendEmptyMessage(mState);
                    break;
                case SWITCH_CAMERE:
                    mMultimediaUtil.switchCamera();
                    break;
                case PREVIEW_VIDEO:
                    try {
                        mRLVideoBar.setVisibility(View.GONE);
                        mIvPreviewRecord.setVisibility(View.GONE);

                        if (mVideoFile != null && mVideoFile.exists()) {
                            if (mMediaPlayer == null) {
                                mMediaPlayer = new MediaPlayer();
                            }

                            mMediaPlayer.setDataSource(mVideoFile.getAbsolutePath());
                            mMediaPlayer.setDisplay(mSurfaceView.getHolder());

                            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                @Override
                                public void onCompletion(MediaPlayer mp) {
                                    mIvPreviewRecord.setVisibility(View.VISIBLE);
                                    mRLVideoBar.setVisibility(View.VISIBLE);
                                    stopPlay();
                                }
                            });

                            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                @Override
                                public void onPrepared(MediaPlayer mp) {
                                    mMediaPlayer.start();
                                }
                            });

                            mMediaPlayer.prepareAsync();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case SAVE_VIDEO:
                    if (mVideoFile != null && mVideoFile.exists()) {
                        //pdf is not support .mp4 ,so change the format of video
                        String oldPath = mVideoFile.getAbsolutePath();
                        int suffexIndex = oldPath.lastIndexOf('.');
                        String newPath = oldPath.substring(0, suffexIndex) + ".avi";
                        mVideoFile.renameTo(new File(newPath));
                        mPickListener.onResult(true, newPath);
                    } else {
                        mPickListener.onResult(false, null);
                    }
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    };


    private void stopPlay() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        mLastState = mState;
        mState = RECORD_CANEL;
        handler.sendEmptyMessage(mState);
        super.onCancel(dialog);
    }

    @Override
    public void onResume() {
        mSurfaceView.setBackgroundColor(Color.BLACK);
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mSurfaceView.setBackgroundColor(Color.TRANSPARENT);
            }
        },500);

        if (RECORD_READY != mState) {
            mLastState = mState;
            mState = RECORD_READY;
            handler.sendEmptyMessage(mState);
        }
        super.onResume();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (getActivity() != null) {
            getActivity().setRequestedOrientation(mLastScreenOrientation);
        }
    }


    @Override
    public void onSuccessed(File file) {
        if (mPickListener != null) {

            switch (mState) {
                case RECORD_STOP:
                    mVideoFile = file;

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mChronometer.setVisibility(View.GONE);
                            mIvCancel.setVisibility(View.VISIBLE);
                            mIvSaveVideo.setVisibility(View.VISIBLE);
                            mIvPreviewRecord.setVisibility(View.VISIBLE);
                            mTvStartRecord.setText(mContext.getString(R.string.remake_video));
                            mTvStartRecord.setBackgroundResource(R.drawable.video_resume_selector);
                        }
                    });
                    break;
                case RECORD_CANEL:
                    if (file != null && file.exists()) {
                        file.delete();
                    }
                    dismiss();
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onFailed() {
        UIToast.getInstance(mContext).show(mContext.getString(R.string.record_failed));
        dismiss();
    }

    public void setOnPickPicListener(MultimediaSupport.IPickResultListener listener) {
        this.mPickListener = listener;
    }

}
