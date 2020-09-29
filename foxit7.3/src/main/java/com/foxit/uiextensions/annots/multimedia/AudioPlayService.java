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
    private int volume;

    @Override
    public void onCreate() {
        mMediaPlayer = new MediaPlayer();
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        try {
            if (mMediaPlayer.isPlaying()) {
                mMediaPlayer.stop();
            }
            mMediaPlayer.release();
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

    public void prepare(String filepath, final AudioPlayView.OnPreparedListener listener) {
        try {
            mMediaPlayer.setDataSource(filepath);
            mMediaPlayer.prepareAsync();
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
                    if (listener != null) {
                        listener.onPrepared(false, mp);
                    }
                    return true;
                }
            });
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    if (listener != null) {
                        listener.onPrepared(true, mp);
                    }
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        mMediaPlayer.start();
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

    public void mute(boolean act) {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            AudioManager mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (act) {
                int tmp = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                if (tmp > 0) {
                    volume = tmp;
                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
                }
            } else {
                mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0);
            }
        }
    }

}
