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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;

import java.io.IOException;

import androidx.annotation.Nullable;

public class AudioPlayService extends Service {

    private MediaPlayer mMediaPlayer;
    private AudioManager mAudioManager;

    private boolean isPauseByAudioFocusLoss = false;

    @Override
    public void onCreate() {
        mMediaPlayer = new MediaPlayer();
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
            mMediaPlayer.release();
            mAudioStatusChangeListener = null;
            mAudioFocusChangeListener = null;
            mMediaPlayer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new AudioPlayBinder();
    }

    public class AudioPlayBinder extends Binder {
        public AudioPlayService getService() {
            return AudioPlayService.this;
        }
    }

    public void prepare(String filepath, final MediaPlayer.OnPreparedListener listener) {
        try {
            mMediaPlayer.setDataSource(filepath);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (listener != null) {
                        listener.onPrepared(mp);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mMediaPlayer.start();
        }
    }

    public void pause() {
        mMediaPlayer.pause();
    }

    public void stop() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
        } else {
            mMediaPlayer.pause();
            mMediaPlayer.reset();
        }
    }

    public void seekTo(int pos) {
        mMediaPlayer.seekTo(pos);
    }

    public int getCurrentPosition() {
        return mMediaPlayer.getCurrentPosition();
    }

    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }

    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    if (isPauseByAudioFocusLoss && !mMediaPlayer.isPlaying()) {
                        isPauseByAudioFocusLoss = false;
                        start();
                        if (mAudioStatusChangeListener != null) {
                            mAudioStatusChangeListener.replay();
                        }
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    if (mMediaPlayer.isPlaying()) {
                        isPauseByAudioFocusLoss = true;
                        pause();
                        if (mAudioStatusChangeListener != null) {
                            mAudioStatusChangeListener.pause();
                        }
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    if (mMediaPlayer.isPlaying()) {
                        isPauseByAudioFocusLoss = true;
                        pause();
                        if (mAudioStatusChangeListener != null) {
                            mAudioStatusChangeListener.pause();
                        }
                    }
                    break;
                default:
                    isPauseByAudioFocusLoss = false;
                    break;
            }
        }
    };

    private IAudioStatusChangeListener mAudioStatusChangeListener;

    public void setAudioStatusChangeListener(IAudioStatusChangeListener listener) {
        mAudioStatusChangeListener = listener;
    }

    public interface IAudioStatusChangeListener {
        void replay();

        void pause();
    }

}
