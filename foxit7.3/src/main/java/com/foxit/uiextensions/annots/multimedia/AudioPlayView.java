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
package com.foxit.uiextensions.annots.multimedia;


import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppDisplay;

import androidx.annotation.NonNull;

public class AudioPlayView extends LinearLayout {

    private SeekBar mediaPlayer_seekbar;
    private TextView mediaPlayer_pastTime;
    private TextView mediaPlayer_totalTime;
    private ImageView mediaPlayer_playbtn;
    private ImageView mediaPlayer_slowbtn;
    private ImageView mediaPlayer_speedbtn;
    private ImageView mediaPlayer_stopbtn;
    private View mediaPlayer_view;

    private PhoneStateBroadCastReceiver mPhoneStateBroadCastReceiver;
    private AudioPlayService mAudioPlayer;
    private boolean isBindService = false;

    private Context mContext;

    public AudioPlayView(@NonNull Context context) {
        super(context);
        mContext = context;
        init();
    }

    private void init() {
        mediaPlayer_view = View.inflate(mContext, R.layout.audio_play_layout, this);
        mediaPlayer_view.setVisibility(View.GONE);

        //init View
        LinearLayout container = (LinearLayout) mediaPlayer_view.findViewById(R.id.ll_audio_play_container);
        int width = AppDisplay.getInstance(mContext).dp2px(260);
        int height = AppDisplay.getInstance(mContext).dp2px(80);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, height);
        container.setLayoutParams(lp);

        mediaPlayer_pastTime = (TextView) mediaPlayer_view.findViewById(R.id.audio_play_pasttime);
        mediaPlayer_totalTime = (TextView) mediaPlayer_view.findViewById(R.id.audio_play_totaltime);
        mediaPlayer_playbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_pause);
        mediaPlayer_slowbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_slow);
        mediaPlayer_speedbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_speed);
        mediaPlayer_stopbtn = (ImageView) mediaPlayer_view.findViewById(R.id.audio_play_stop);

        //init play status
        mediaPlayer_seekbar = (SeekBar) mediaPlayer_view.findViewById(R.id.audio_play_seekbar);
        mediaPlayer_seekbar.setOnSeekBarChangeListener(mSeekbarChangedListener);

        mediaPlayer_playbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mAudioPlayer.isPlaying()) {
                    mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_play_selector);
                    mAudioPlayer.pause();
                } else {
                    mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_pause_selector);
                    mAudioPlayer.seekTo(mAudioPlayer.getCurrentPosition());
                    try {
                        mAudioPlayer.start();
                        mHandler.sendEmptyMessage(CHANGE_UI_STATE);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });

        mediaPlayer_stopbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                release();
            }
        });

        mediaPlayer_slowbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int pos = mAudioPlayer.getCurrentPosition();
                pos -= 5000;

                if (pos < 0) {
                    pos = 0;
                }
                mAudioPlayer.seekTo(pos);
                mediaPlayer_seekbar.setProgress(pos);
                mediaPlayer_pastTime.setText(timeParse(pos));
            }
        });

        mediaPlayer_speedbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                int pos = mAudioPlayer.getCurrentPosition();
                pos += 5000;

                if (pos > mAudioPlayer.getDuration()) {
                    pos = mAudioPlayer.getDuration();
                }
                mAudioPlayer.seekTo(pos);
                mediaPlayer_seekbar.setProgress(pos);
                mediaPlayer_pastTime.setText(timeParse(pos));
            }
        });
    }

    private boolean fromUser;
    private SeekBar.OnSeekBarChangeListener mSeekbarChangedListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            if (fromUser) {
                mAudioPlayer.seekTo(mediaPlayer_seekbar.getProgress());
                mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getCurrentPosition()));
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            fromUser = true;
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            fromUser = false;
        }
    };

    private String timeParse(long duration) {
        String time = "";
        long minute = duration / 60000;
        long seconds = duration % 60000;
        long second = Math.round((float) seconds / 1000);
        if (minute < 10) {
            time += "0";
        }
        time += minute + ":";
        if (second < 10) {
            time += "0";
        }
        time += second;
        return time;
    }

    private void startAudioService() {
        if (isBindService) {
            stopAudioService();
        }
        Intent intent = new Intent(mContext, AudioPlayService.class);
        mContext.startService(intent);
        isBindService = true;
        mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void stopAudioService() {
        Intent intent = new Intent(mContext, AudioPlayService.class);
        if (isBindService) {
            mContext.unbindService(mServiceConnection);
            isBindService = false;
        }
        mContext.stopService(intent);
        mAudioPlayer = null;
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mAudioPlayer = ((AudioPlayService.AudioPlayBinder) service).getService();
            mPhoneStateBroadCastReceiver = new PhoneStateBroadCastReceiver();
            mPhoneStateBroadCastReceiver.setAudioPlay(mAudioPlayer);
            mHandler.sendEmptyMessage(SERVICE_CONNECTED);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mHandler.sendEmptyMessage(SERVICE_DISCONNECTED);
        }
    };

    private static final int START_SERVICE = 111;
    private static final int STOP_SERVICE = 222;
    private static final int SERVICE_CONNECTED = 333;
    private static final int SERVICE_DISCONNECTED = 444;
    private static final int CHANGE_UI_STATE = 555;
    private String mPlayFilePath;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case START_SERVICE:
                    startAudioService();
                    break;
                case STOP_SERVICE:
                    stopAudioService();
                    break;
                case SERVICE_CONNECTED:
                    preparePlayer();
                    break;
                case SERVICE_DISCONNECTED:
                    break;
                case CHANGE_UI_STATE:
                    changeUIState();
                    break;
                default:
                    break;
            }
        }
    };

    private OnPreparedListener mPreparedListener;

    public void startPlayAudio(String path, OnPreparedListener preparedListener) {
        mPreparedListener = preparedListener;
        mPlayFilePath = path;
        if (isBindService) {
            mAudioPlayer.stop();
            mediaPlayer_view.setVisibility(View.GONE);
            preparePlayer();
        } else {
            mHandler.sendEmptyMessage(START_SERVICE);
        }
    }

    public void release() {
        if (mediaPlayer_view == null) return;
        mediaPlayer_view.setVisibility(View.GONE);
        if (mAudioPlayer != null) {
            mAudioPlayer.stop();
            mHandler.sendEmptyMessage(STOP_SERVICE);
        }
    }

    private void preparePlayer() {
        if (mediaPlayer_view == null) return;
        try {
            mAudioPlayer.prepare(mPlayFilePath, new AudioPlayView.OnPreparedListener() {
                @Override
                public void onPrepared(boolean success, MediaPlayer mp) {
                    if (mPreparedListener != null) {
                        mPreparedListener.onPrepared(success, mp);
                    }
                    if (success){
                        mediaPlayer_view.setVisibility(View.VISIBLE);
                        mediaPlayer_seekbar.setMax(mAudioPlayer.getDuration());
                        mediaPlayer_seekbar.setProgress(0);
                        mediaPlayer_pastTime.setText(timeParse(0));

                        //set totalTime
                        mediaPlayer_totalTime.setText(timeParse(mAudioPlayer.getDuration()));
                        //play file
                        mAudioPlayer.start();
                        mHandler.sendEmptyMessage(CHANGE_UI_STATE);
                    } else {
                        mediaPlayer_view.setVisibility(View.GONE);
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            mediaPlayer_view.setVisibility(View.GONE);
        }
    }

    public void changeUIState() {
        if (mAudioPlayer == null || (mediaPlayer_view != null && !mediaPlayer_view.isShown()))
            return;

        if (!mAudioPlayer.isPlaying()) {
            mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_play_selector);
            if (mAudioPlayer.getCurrentPosition() + 1000 > mAudioPlayer.getDuration()) {
                //end
                mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getDuration()));
                try {
                    mAudioPlayer.pause();
                    mAudioPlayer.seekTo(0);
                    mediaPlayer_pastTime.setText(timeParse(0));
                    mediaPlayer_seekbar.setProgress(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            mediaPlayer_playbtn.setImageResource(R.drawable.audio_player_pause_selector);
            mediaPlayer_pastTime.setText(timeParse(mAudioPlayer.getCurrentPosition()));
            mediaPlayer_seekbar.setProgress(mAudioPlayer.getCurrentPosition());

            mHandler.sendEmptyMessageDelayed(CHANGE_UI_STATE, 100);
        }
    }

    public View getContentView() {
        return mediaPlayer_view;
    }

    public interface OnPreparedListener {
        void onPrepared(boolean success, MediaPlayer mp);
    }

}
