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


import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Rect;
import android.hardware.Camera;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaMetadataRetriever;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.MediaStore;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.fragment.app.FragmentActivity;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.DocumentManager;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.multimedia.screen.MultimediaSupport;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.utils.UIToast;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MultimediaUtil {
    private static final String MULTI_MEDIA_BASE_PATH = Environment.getExternalStorageDirectory().getPath() + "/FoxitSDK/AttaTmp/multimedia/";

    private static final int SAMPLE_RATE_IN_HZ = 44100;
    private static final int AUDIO_CHANNEL = AudioFormat.CHANNEL_IN_STEREO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ, AUDIO_CHANNEL, AUDIO_FORMAT);

    private static final int VIDEO_FRAME_WIDTH = 640;
    private static final int VIDEO_FRAME_HEIGHT = 480;
    private static final int VIDEO_BIT_RATE = 1024 * 1024;
    private static final int VIDEO_FRAME_RATE = 30;

    private Context mContext;
    private AudioRecord mAudioRecord;
    private MediaRecorder mMediaRecorder;

    private File mRawAudioFile;
    private File mNewAudioFile;
    private File mVideoFile;

    private List<String> mAudioSupportList;
    private List<String> mVideoSuooprList;

    private SurfaceHolder mSurfaceHolder;
    private SurfaceView mSurfaceView;

    private Camera mCamera;

    private boolean mIsRecordAudio;
    private boolean mIsRecordVideo;
    private int mCameraPosition = 0;

    public MultimediaUtil(Context context) {
        this.mContext = context;
    }

    public void showPickDialog(UIExtensionsManager uiExtensionsManager, String intent, MultimediaSupport.IPickResultListener pictureResultlistener) {
        DocumentManager documentManager = uiExtensionsManager.getDocumentManager();
        if (documentManager.getCurrentAnnot() != null) {
            documentManager.setCurrentAnnot(null);
        }

        Activity activity = uiExtensionsManager.getAttachedActivity();
        if (activity == null) {
            return;
        }

        if (!(activity instanceof FragmentActivity)) {
            UIToast.getInstance(mContext).show(mContext.getApplicationContext().getString(R.string.the_attached_activity_is_not_fragmentActivity));
            return;
        }

        FragmentActivity act = (FragmentActivity) activity;
        MultimediaSupport support = (MultimediaSupport) act.getSupportFragmentManager().findFragmentByTag("MultimediaSupport");
        if (support == null) {
            support = new MultimediaSupport();
            support.setIntent(intent);
            support.setPDFViewCtrl(uiExtensionsManager.getPDFViewCtrl());
            support.setOnPickPicListener(pictureResultlistener);
        }

        AppDialogManager.getInstance().showAllowManager(support, act.getSupportFragmentManager(),
                "MultimediaSupport", null);
    }

    public String getAbsolutePath(Context context, String intent, Uri uri) {
        if (null == uri) return null;
        final String scheme = uri.getScheme();
        String data = null;
        if (scheme == null)
            data = uri.getPath();
        else if (ContentResolver.SCHEME_FILE.equals(scheme)) {
            data = uri.getPath();
        } else if (ContentResolver.SCHEME_CONTENT.equals(scheme)) {

            String projection;
            if (ToolHandler.TH_TYPE_PDFIMAGE.equals(intent)) {
                projection = MediaStore.Images.ImageColumns.DATA;
            } else if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(intent)) {
                projection = MediaStore.Audio.AudioColumns.DATA;
            } else {
                projection = MediaStore.Video.VideoColumns.DATA;
            }

            Cursor cursor = context.getContentResolver().query(uri,
                    new String[]{projection}, null, null, null);
            if (null != cursor) {
                if (cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(projection);
                    if (index > -1) {
                        data = cursor.getString(index);
                    }
                }
                cursor.close();
            }
        }
        return data;
    }

    public String getAbsolutePath(Context context, Uri uri, String selection, String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    public File getOutputMediaFile(String intent, String suffix) {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(mContext, mContext.getApplicationContext().getString(R.string.the_sdcard_not_exist), Toast.LENGTH_LONG).show();
            return null;
        }

        File mediaStorageDir = new File(MULTI_MEDIA_BASE_PATH);
        if (!mediaStorageDir.exists()) {
            mediaStorageDir.mkdirs();
        }
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String path;
        if (ToolHandler.TH_TYPE_PDFIMAGE.equals(intent)) {
            path = mediaStorageDir.getPath() + File.separator + "Image_" + timeStamp + suffix;
        } else if (ToolHandler.TH_TYPE_SCREEN_VIDEO.equals(intent)) {
            path = mediaStorageDir.getPath() + File.separator + "Video_" + timeStamp + suffix;
        } else {
            path = mediaStorageDir.getPath() + File.separator + "Audio_" + timeStamp + suffix;
        }
        return new File(path);
    }

    public String getMimeType(String filePath) {
        if (filePath == null) {
            return null;
        }
        int lastIndex = filePath.lastIndexOf('.');
        String suffix = lastIndex >= 0 ? filePath.substring(lastIndex + 1).toLowerCase() : "";
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(suffix);
    }

    public List<String> getAudioSupportMimeList() {
        if (mAudioSupportList == null) {
            mAudioSupportList = new ArrayList<>();
            mAudioSupportList.add("audio/aiff");
            mAudioSupportList.add("audio/basic");
            mAudioSupportList.add("audio/mid");
            mAudioSupportList.add("audio/midi");
            mAudioSupportList.add("audio/mp3");
            mAudioSupportList.add("audio/mpeg");
            mAudioSupportList.add("audio/mpeg3");
            mAudioSupportList.add("audio/mpegurl");
            mAudioSupportList.add("audio/wav");
            mAudioSupportList.add("audio/x-aiff");
            mAudioSupportList.add("audio/x-midi");
            mAudioSupportList.add("audio/x-mp3");
            mAudioSupportList.add("audio/x-mpeg");
            mAudioSupportList.add("audio/x-mpeg3");
            mAudioSupportList.add("audio/x-mpegurl");
            mAudioSupportList.add("audio/x-wav");
            mAudioSupportList.add("audio/x-ms-wax");
            mAudioSupportList.add("audio/x-ms-wma");
        }
        return mAudioSupportList;
    }

    public List<String> getVideoSupportMimeList() {
        if (mVideoSuooprList == null) {
            mVideoSuooprList = new ArrayList<>();
            mVideoSuooprList.add("application/x-shockwave-flash");
            mVideoSuooprList.add("video/avi");
            mVideoSuooprList.add("video/mpeg");
            mVideoSuooprList.add("video/msvideo");
            mVideoSuooprList.add("video/x-ivf");
            mVideoSuooprList.add("video/x-mpeg");
            mVideoSuooprList.add("video/x-ms-asf");
            mVideoSuooprList.add("video/x-ms-asx");
            mVideoSuooprList.add("video/x-ms-wm");
            mVideoSuooprList.add("video/x-ms-wmp");
            mVideoSuooprList.add("video/x-ms-wmv");
            mVideoSuooprList.add("video/x-ms-wmx");
            mVideoSuooprList.add("video/x-ms-wvx");
            mVideoSuooprList.add("video/x-msvideo");
            mVideoSuooprList.add("video/x-mpg");
            mVideoSuooprList.add("video/mpg");
            mVideoSuooprList.add("video/quicktime");
            mVideoSuooprList.add("video/mp4");
        }
        return mVideoSuooprList;
    }

    private float getImageScale(PDFViewCtrl pdfViewCtrl, int picWidth, int picHeight, int pageIndex) {
        int pageWidth = pdfViewCtrl.getPageViewWidth(pageIndex);
        int pageHeight = pdfViewCtrl.getPageViewHeight(pageIndex);

        float widthScale = (float) picWidth / pageWidth;
        float heightScale = (float) picHeight / pageHeight;
        float scale = widthScale > heightScale ? 1 / (5 * widthScale) : 1 / (5 * heightScale);
        scale = (float) (Math.round(scale * 100)) / 100;
        return scale;
    }

    private Bitmap getThumbnail(String filePath) {
        Bitmap thumbnail = null;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(filePath);
            thumbnail = retriever.getFrameAtTime();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                retriever.release();
            } catch (RuntimeException e) {
                e.printStackTrace();
            }
        }
        return thumbnail;
    }

    public Bitmap getVideoThumbnail(PDFViewCtrl pdfViewCtrl, String filePath) {
        Bitmap thumbnail = getThumbnail(filePath);
        if (thumbnail == null) {
            thumbnail = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.video_default_icon);
        }
        float scale = getImageScale(pdfViewCtrl, thumbnail.getWidth(), thumbnail.getHeight(), pdfViewCtrl.getCurrentPage());
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        Bitmap background = Bitmap.createBitmap(thumbnail, 0, 0, thumbnail.getWidth(), thumbnail.getHeight(), matrix, true);
        Canvas canvas = new Canvas(background);
        canvas.setDrawFilter(new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG));
        canvas.drawBitmap(background, 0, 0, null);

        Bitmap foreground = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.video_play);
        int fgWidth = foreground.getWidth();
        int fgHeight = foreground.getHeight();

        if (PDFViewCtrl.PAGELAYOUTMODE_FACING == pdfViewCtrl.getPageLayoutMode()
                || PDFViewCtrl.PAGELAYOUTMODE_COVER == pdfViewCtrl.getPageLayoutMode()) {
            fgWidth = fgWidth / 2;
            fgHeight = fgHeight / 2;
        }
        Rect rect = new Rect((background.getWidth() - fgWidth) / 2,
                (background.getHeight() - fgHeight) / 2,
                (background.getWidth() + fgWidth) / 2,
                (background.getHeight() + fgHeight) / 2);

        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setFilterBitmap(true);
        canvas.drawBitmap(foreground, null, rect, paint);

        thumbnail.recycle();
        foreground.recycle();
        thumbnail = null;
        foreground = null;
        return background;
    }

    public void startRecordAudio() {
        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            Toast.makeText(mContext, mContext.getApplicationContext().getString(R.string.the_sdcard_not_exist), Toast.LENGTH_LONG).show();
            return;
        }

        if (!mIsRecordAudio) {
            if (mAudioRecord == null) {
                mRawAudioFile = getOutputMediaFile(ToolHandler.TH_TYPE_SCREEN_AUDIO, ".raw");
                mNewAudioFile = getOutputMediaFile(ToolHandler.TH_TYPE_SCREEN_AUDIO, ".wav");

                mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ,
                        AUDIO_CHANNEL, AUDIO_FORMAT, BUFFER_SIZE);

                if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_STOPPED) {
                    if (mRecordFinishCallback != null) {
                        mRecordFinishCallback.onFailed();
                    }
                    return;
                }
            }
            mIsRecordAudio = true;
            mAudioRecord.startRecording();

            if (mAudioRecord.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                releaseAudioRecord();
                if (mRecordFinishCallback != null) {
                    mRecordFinishCallback.onFailed();
                }
                return;
            }
            AppThreadManager.getInstance().startThread(new AudioRecordThread());
        }
    }

    public void stopRecordAudio() {
        if (mIsRecordAudio) {
            AppThreadManager.getInstance().startThread(new AudioConvertThread());
        }
    }

    public void startRecordVideo() {
        if (!mIsRecordAudio && prepareRecord()) {
            try {
                mMediaRecorder.start();
                mIsRecordVideo = true;
            } catch (RuntimeException r) {
                releaseMediaRecorder();
                releaseCamera();
                if (mRecordFinishCallback != null) {
                    mRecordFinishCallback.onFailed();
                }
            }
        }
    }

    public void stopRecordVideo() {
        if (mIsRecordVideo) {
            mIsRecordVideo = false;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mMediaRecorder.stop();
                    } catch (RuntimeException r) {
                        releaseMediaRecorder();
                        releaseCamera();
                        if (mRecordFinishCallback != null) {
                            mRecordFinishCallback.onFailed();
                        }
                    } finally {
                        releaseMediaRecorder();
                        releaseCamera();

                        if (mRecordFinishCallback != null) {
                            mRecordFinishCallback.onSuccessed(mVideoFile);
                        }
                    }
                }
            }).start();
        }
    }

    public void setSurfaceView(SurfaceView view) {
        this.mSurfaceView = view;
        mSurfaceHolder = mSurfaceView.getHolder();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                mSurfaceHolder = holder;
                startPreView(holder);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
                if (mIsRecordVideo) {
                    stopRecordVideo();
                    if (mVideoFile.exists()) {
                        mVideoFile.delete();
                    }
                }
            }
        });
    }

    public void switchCamera() {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        int cameraCount = Camera.getNumberOfCameras();

        for (int i = 0; i < cameraCount; i++) {
            // back to faing
            Camera.getCameraInfo(i, cameraInfo);
            if (mCameraPosition == 0) {
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    mCamera = Camera.open(i);
                    startPreView(mSurfaceHolder);
                    mCameraPosition = 1;
                    break;
                }
            } else {
                //facing 2 back
                if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                    mCamera.stopPreview();
                    mCamera.release();
                    mCamera = null;
                    mCamera = Camera.open(i);
                    startPreView(mSurfaceHolder);
                    mCameraPosition = 0;
                    break;
                }
            }
        }
    }

    private boolean prepareRecord() {
        try {
            if (mCamera == null) {
                startPreView(mSurfaceHolder);
            }

            mMediaRecorder = new MediaRecorder();

            mCamera.unlock();
            mMediaRecorder.setCamera(mCamera);

            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
            mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

            CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
            profile.videoFrameWidth = VIDEO_FRAME_WIDTH;
            profile.videoFrameHeight = VIDEO_FRAME_HEIGHT;
            profile.videoBitRate = VIDEO_BIT_RATE;
            profile.videoFrameRate = VIDEO_FRAME_RATE;

            mMediaRecorder.setProfile(profile);
            if (mCameraPosition == 0) {
                mMediaRecorder.setOrientationHint(90);
            } else {
                mMediaRecorder.setOrientationHint(270);
            }

            mVideoFile = getOutputMediaFile(ToolHandler.TH_TYPE_SCREEN_VIDEO, ".mp4");
            mMediaRecorder.setOutputFile(mVideoFile.getAbsolutePath());

            mMediaRecorder.prepare();
        } catch (Exception e) {
            e.printStackTrace();
            releaseMediaRecorder();
            if (mRecordFinishCallback != null) {
                mRecordFinishCallback.onFailed();
            }
            return false;
        }
        return true;
    }

    public void startPreView(SurfaceHolder holder) {
        if (mCamera == null) {
            mCameraPosition = 0;
            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
        }

        if (mCamera != null) {
            try {
                Camera.Parameters parameters = mCamera.getParameters();
                List<Camera.Size> supportedVideoSizes = parameters.getSupportedVideoSizes();
                List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
                Camera.Size optimalSize = getOptimalVideoSize(supportedVideoSizes,
                        supportedPreviewSizes, mSurfaceView.getWidth(), mSurfaceView.getHeight());
                parameters.setPreviewSize(optimalSize.width, optimalSize.height);

                List<String> focusModes = parameters.getSupportedFocusModes();
                if (focusModes != null && !focusModes.isEmpty()) {

                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                    } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
                        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    }
                }
                mCamera.setParameters(parameters);
                mCamera.setDisplayOrientation(90);
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            } catch (IOException e) {
                releaseCamera();
                releaseMediaRecorder();
                if (mRecordFinishCallback != null) {
                    mRecordFinishCallback.onFailed();
                }
            }
        }
    }

    private Camera.Size getOptimalVideoSize(List<Camera.Size> supportedVideoSizes,
                                            List<Camera.Size> previewSizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio = (double) w / h;
        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;
        int targetHeight = h;

        List<Camera.Size> videoSizes = supportedVideoSizes != null ? supportedVideoSizes : previewSizes;
        for (Camera.Size size : videoSizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
                continue;
            if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : videoSizes) {
                if (Math.abs(size.height - targetHeight) < minDiff && previewSizes.contains(size)) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }

    private void releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera.lock();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            mCamera.release();
            mCamera = null;
        }
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }
    }

    private class AudioRecordThread implements Runnable {
        @Override
        public void run() {
            writeDateTOFile();
        }
    }

    private class AudioConvertThread implements Runnable {
        @Override
        public void run() {
            convertRaw2Wave(mRawAudioFile, mNewAudioFile);
        }
    }

    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            //close AudioRecord
            switch (msg.what) {
                case 0:
                    releaseAudioRecord();
                    if (mRecordFinishCallback != null) {
                        mRecordFinishCallback.onSuccessed(mNewAudioFile);
                    }
                    break;
                case 1:
                    releaseAudioRecord();
                    if (mRecordFinishCallback != null) {
                        mRecordFinishCallback.onFailed();
                    }
                    break;
                default:
            }
        }

    };

    public void releaseAudioRecord() {
        if (mIsRecordAudio){
            if (mRawAudioFile != null && mRawAudioFile.exists()) {
                mRawAudioFile.delete();
            }

            if (mAudioRecord != null) {
                mIsRecordAudio = false;
                mAudioRecord.stop();
                mAudioRecord.release();
                mAudioRecord = null;
            }
        }
    }

    private void writeDateTOFile() {
        byte[] audiodata = new byte[BUFFER_SIZE];
        FileOutputStream fos = null;
        int readsize = 0;
        try {
            if (mRawAudioFile.exists()) {
                mRawAudioFile.delete();
            }
            fos = new FileOutputStream(mRawAudioFile);

            while (mIsRecordAudio == true) {
                readsize = mAudioRecord.read(audiodata, 0, BUFFER_SIZE);
                if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
                    try {
                        fos.write(audiodata);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void convertRaw2Wave(File inFile, File outFile) {
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen;
        long longSampleRate = SAMPLE_RATE_IN_HZ;
        int channels = 2;
        long byteRate = 16 * SAMPLE_RATE_IN_HZ * channels / 8;
        byte[] data = new byte[BUFFER_SIZE];
        try {
            in = new FileInputStream(inFile);
            out = new FileOutputStream(outFile);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            writeWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);
            while (in.read(data) != -1) {
                out.write(data);
            }
            in.close();
            out.close();

            handler.sendEmptyMessage(0);
        } catch (FileNotFoundException e) {
            handler.sendEmptyMessage(1);
        } catch (IOException e) {
            handler.sendEmptyMessage(1);
        }
    }

    private void writeWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    public boolean canPlaySimpleAudio(String filePath) {
        int index = filePath.lastIndexOf('.');
        if (index < 0) return false;

        String ext = filePath.substring(index + 1);
        if (support(ext) && isAudio(ext)) {
            return true;
        }

        return false;
    }

    //call support(String) first, because {android os Version check};
    private boolean isAudio(String ext) {
        if (ext == null || ext.isEmpty()) return false;

        if (ext.equals("m4a")
                || ext.equals("mp3")
                || ext.equals("wav")
                || ext.equals("mid")
                || ext.equals("aac")
                || ext.equals("flac")) {
            return true;
        }
        return false;
    }

    private boolean support(String ext) {
        if (ext == null || ext.isEmpty()) return false;

        if (ext.equals("3gp")
                || ext.equals("mp4")
                || ext.equals("m4a")
                || ext.equals("mp3")
                || ext.equals("mid")
                || ext.equals("xmf")
                || ext.equals("mxmf")
                || ext.equals("rtttl")
                || ext.equals("rtx")
                || ext.equals("ota")
                || ext.equals("imy")
                || ext.equals("ogg")
                || ext.equals("wav")
                || ext.equals("jpg")
                || ext.equals("gif")
                || ext.equals("png")
                || ext.equals("bmp")
                ) {
            return true;
        } else if (ext.equals("aac")
                || ext.equals("flac")) {
            //3.1+
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1)
                return true;
        } else if (ext.equals("ts")) {
            //3.0+
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                return true;
        } else if (ext.equals("mkv")
                || ext.equals("webp")) {
            //4.0+
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH)
                return true;
        }

        return false;
    }

    private IRecordFinishCallback mRecordFinishCallback;

    public void setRecordFinishCallback(IRecordFinishCallback callback) {
        this.mRecordFinishCallback = callback;
    }

    public interface IRecordFinishCallback {
        void onSuccessed(File file);

        void onFailed();
    }

}
