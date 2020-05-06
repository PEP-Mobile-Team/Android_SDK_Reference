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
package com.foxit.uiextensions.annots.screen;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.annots.screen.multimedia.AudioRecordFragment;
import com.foxit.uiextensions.annots.screen.multimedia.MultimediaUtil;
import com.foxit.uiextensions.annots.screen.multimedia.ViedeoRecordFragment;
import com.foxit.uiextensions.controls.dialog.AppDialogManager;
import com.foxit.uiextensions.controls.dialog.MatchDialog;
import com.foxit.uiextensions.controls.dialog.fileselect.UIFileSelectDialog;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.UIToast;

import java.io.File;
import java.io.FileFilter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

public class MultimediaSupport extends DialogFragment {
    private final static int SYSTEM_BROWSER_REQUEST_CODE = 111;
    private final static int CAMERA_REQUEST_CODE = 222;
    private final static int RECORD_REQUEST_CODE = 333;

    private PDFViewCtrl mPDFViewCtrl;
    private Context mContext;
    private Activity mAttachActivity;
    private AlertDialog alertDialog;
    private UIFileSelectDialog mFileSelectDialog = null;
    private MultimediaUtil mMmultimediaUtil;

    private boolean mIsItemSelected = false;
    private String mCameraPath;
    private String mIntent;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAttachActivity = this.getActivity();
        mContext = mAttachActivity.getApplicationContext();
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

        mMmultimediaUtil = new MultimediaUtil(mContext);
    }

    public void setIntent(String intent) {
        this.mIntent = intent;
    }

    public void setPDFViewCtrl(PDFViewCtrl pdfViewCtrl) {
        this.mPDFViewCtrl = pdfViewCtrl;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mFileSelectDialog != null && mFileSelectDialog.isShowing()) {
            mFileSelectDialog.setHeight(mFileSelectDialog.getDialogHeight());
            mFileSelectDialog.showDialog();
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (ToolHandler.TH_TYPE_PDFIMAGE.equals(mIntent)) {
            initImageAlert();
        } else if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(mIntent)) {
            initAudioAlert();
        } else {
            initVideoAlert();
        }
        alertDialog.show();
    }

    private void initImageAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAttachActivity);
        String[] items = new String[]{
                AppResource.getString(mContext, R.string.fx_import_file),
                AppResource.getString(mContext, R.string.fx_import_dcim),
                AppResource.getString(mContext, R.string.fx_import_camera),
        };

        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                mIsItemSelected = true;
                switch (which) {
                    case 0: // from file
                        showSelectFileDialog();
                        break;
                    case 1: // from album
                        //19 == Build.VERSION_CODES.KITKAT
                        Intent intent = getFileIntent(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
                        startActivityForResult(intent, SYSTEM_BROWSER_REQUEST_CODE);
                        break;
                    case 2: // from camera
                        startCamera();
                        break;
                    default:
                        break;
                }

                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        createDialog(builder);
    }

    private void initAudioAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAttachActivity);
        String[] items = new String[]{
                AppResource.getString(mContext, R.string.fx_import_file),
                AppResource.getString(mContext, R.string.fx_import_dcim),
                AppResource.getString(mContext, R.string.recording_audio)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsItemSelected = true;
                switch (which) {
                    case 0:
                        showSelectFileDialog();
                        break;
                    case 1:
                        Intent intent = getFileIntent(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, "audio/*");
                        startActivityForResult(intent, SYSTEM_BROWSER_REQUEST_CODE);
                        break;
                    case 2:
                        startRecordAudio();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }
        });
        builder.setCancelable(true);
        createDialog(builder);
    }

    private void initVideoAlert() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mAttachActivity);
        String[] items = new String[]{
                AppResource.getString(mContext, R.string.fx_import_file),
                AppResource.getString(mContext, R.string.fx_import_dcim),
                AppResource.getString(mContext, R.string.recording_video)
        };
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mIsItemSelected = true;
                switch (which) {
                    case 0:
                        showSelectFileDialog();
                        break;
                    case 1:
                        Intent intent = getFileIntent(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video/*");
                        startActivityForResult(intent, SYSTEM_BROWSER_REQUEST_CODE);
                        break;
                    case 2:
                        startRecordVideo();
                        break;
                    default:
                        break;
                }
                dialog.dismiss();
            }

        });
        builder.setCancelable(true);
        createDialog(builder);
    }

    private void createDialog(AlertDialog.Builder builder) {
        alertDialog = builder.create();
        alertDialog.setCanceledOnTouchOutside(true);
        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!mIsItemSelected) {
                    mPickListener.onResult(false, null);
                    dismiss();
                }
            }
        });
    }

    private void showSelectFileDialog() {
        UIFileSelectDialog fileDialog = getFileSelectDialog();
        fileDialog.setFileClickedListener(new UIFileSelectDialog.OnFileClickedListener() {
            @Override
            public void onFileClicked(String filepath) {
                if (mPickListener != null) {
                    if (TextUtils.isEmpty(filepath)) {
                        mPickListener.onResult(false, null);
                    } else {
                        mPickListener.onResult(true, filepath);
                    }
                }
            }
        });
        fileDialog.setHeight(fileDialog.getDialogHeight());
        fileDialog.setOnDLDismissListener(new MatchDialog.DismissListener() {
            @Override
            public void onDismiss() {
                dismiss();
            }
        });
        fileDialog.showDialog();
    }


    private Intent getFileIntent(@Nullable Uri data, @Nullable String type) {
        //19 == Build.VERSION_CODES.KITKAT
        String action = Build.VERSION.SDK_INT >= 19 ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_GET_CONTENT;
        Intent intent = new Intent(action, null);
        intent.setDataAndType(data, type);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        return intent;
    }

    private UIFileSelectDialog getFileSelectDialog() {
        if (mFileSelectDialog == null) {
            mFileSelectDialog = new UIFileSelectDialog(mAttachActivity, null);
            mFileSelectDialog.init(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    boolean isAcceptType;
                    if (ToolHandler.TH_TYPE_PDFIMAGE.equals(mIntent)) {
                        isAcceptType = isPictures(pathname);
                    } else if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(mIntent)) {
                        isAcceptType = isAudio(pathname);
                    } else {
                        isAcceptType = isVideo(pathname);
                    }
                    return !(pathname.isHidden() || !pathname.canRead())
                            && !(pathname.isFile() && !isAcceptType);
                }
            }, true);
            mFileSelectDialog.setTitle(AppResource.getString(mContext, R.string.fx_string_import));
            mFileSelectDialog.setCanceledOnTouchOutside(true);
        }
        return mFileSelectDialog;
    }

    private boolean isPictures(File file) {
        String pathName = file.getName().toLowerCase();
        return pathName.endsWith(".bmp")
                || pathName.endsWith(".jpg")
                || pathName.endsWith(".png")
                || pathName.endsWith(".gif")
                || pathName.endsWith(".tif")
                || pathName.endsWith(".jpx")
                || pathName.endsWith(".jpeg");
    }

    private boolean isAudio(File file) {
        String pathName = file.getName().toLowerCase();
        return pathName.endsWith(".aiff") // audio/aiff
                || pathName.endsWith(".aif") // audio/aiff audio/x-aiff
                || pathName.endsWith(".aifc") // audio/x-aiff
                || pathName.endsWith(".au") // audio/basic
                || pathName.endsWith(".m3u") // audio/mpegurl
                || pathName.endsWith(".wav") // audio/wav audio/x-wav
                || pathName.endsWith(".wma") // audio/x-ms-wma
                || pathName.endsWith(".wax") // audio/x-ms-wax
                || pathName.endsWith(".mpa") // audio/mpeg
                || pathName.endsWith(".kar") // audio/midi
                || pathName.endsWith(".rmi") // audio/midi
                || pathName.endsWith(".midi") // application/x-midi audio/midi audio/x-mid audio/x-midi music/crescendo x-music/x-midi
                || pathName.endsWith(".mid")// audio/mid audio/midi application/x-midi audio/x-mid audio/x-midi music/crescendo x-music/x-midi
                || pathName.endsWith(".mp3");// audio/mp3 audio/mpeg audio/mpeg3 audio/mpg audio/x-mpeg audio/x-mpeg-3
    }

    private boolean isVideo(File file) {
        String pathName = file.getName().toLowerCase();
        return pathName.endsWith(".avi") // video/avi video/msvideo video/x-msvideo video/quicktime application/x-troff-msvideo
                || pathName.endsWith(".ivf") // video/x-ivf
                || pathName.endsWith(".wmp") // video/x-ms-wmp
                || pathName.endsWith(".wm") // video/x-ms-wm video/x-ms-asf
                || pathName.endsWith(".wvx") // video/x-ms-wvx
                || pathName.endsWith(".wmx") // video/x-ms-wmx
                || pathName.endsWith(".wmv") // video/x-ms-wmv
                || pathName.endsWith(".asf") // video/x-ms-asf
                || pathName.endsWith(".asx") // video/x-ms-asx
                || pathName.endsWith(".swf") // application/x-shockwave-flash
                || pathName.endsWith(".mp4")// video/mp4
                || pathName.endsWith(".mov")// video/quicktime
                || pathName.endsWith(".mpeg"); // video/mpeg
    }

    private void startCamera() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA}, CAMERA_REQUEST_CODE);
                return;
            }
        }

        File outFile = mMmultimediaUtil.getOutputMediaFile(mIntent, ".png");
        if (outFile == null) {
            return;
        }

        mCameraPath = outFile.getAbsolutePath();
        Uri uri;
        if (Build.VERSION.SDK_INT > 23) {//Build.VERSION_CODES.M
            uri = FileProvider.getUriForFile(mContext, getFileProviderName(mContext), outFile);
        } else {
            uri = Uri.fromFile(outFile);
        }
        Intent intent = new Intent();
        // set the action to open system camera.
        intent.setAction(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        startActivityForResult(intent, CAMERA_REQUEST_CODE);
    }

    private void startRecordAudio() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permission = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.RECORD_AUDIO);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.RECORD_AUDIO}, RECORD_REQUEST_CODE);
                return;
            }
        }

        FragmentActivity act = (FragmentActivity) mAttachActivity;
        AudioRecordFragment recordFragment = (AudioRecordFragment) act.getSupportFragmentManager().findFragmentByTag("AudioRecordFragment");
        if (recordFragment == null) {
            recordFragment = new AudioRecordFragment();
            recordFragment.init(mPDFViewCtrl);
            recordFragment.setOnPickPicListener(mPickListener);
        }
        AppDialogManager.getInstance().showAllowManager(recordFragment, act.getSupportFragmentManager(),
                "AudioRecordFragment", null);

        AppDialogManager.getInstance().dismiss(this);
    }

    private void startRecordVideo() {
        if (Build.VERSION.SDK_INT >= 23) {
            int permissionCamera = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.CAMERA);
            int permissionRecord = ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.RECORD_AUDIO);
            if (permissionCamera != PackageManager.PERMISSION_GRANTED || permissionRecord != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO}, RECORD_REQUEST_CODE);
                return;
            }
        }

        FragmentActivity act = (FragmentActivity) mAttachActivity;
        ViedeoRecordFragment recordFragment = (ViedeoRecordFragment) act.getSupportFragmentManager().findFragmentByTag("AudioRecordFragment");
        if (recordFragment == null) {
            recordFragment = new ViedeoRecordFragment();
            recordFragment.setOnPickPicListener(mPickListener);
        }
        AppDialogManager.getInstance().showAllowManager(recordFragment, act.getSupportFragmentManager(),
                "AudioRecordFragment", null);

        AppDialogManager.getInstance().dismiss(this);
    }

    private String getFileProviderName(Context context) {
        return context.getPackageName() + ".fileprovider";
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == SYSTEM_BROWSER_REQUEST_CODE) {
                Uri uri = data.getData();
                String path = mMmultimediaUtil.getAbsolutePath(mContext, mIntent, uri);
                //if cannot get path data when sometime. We should get the path use other way.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && AppUtil.isBlank(path)) {
                    if ("com.android.providers.media.documents".equals(uri.getAuthority())) {
                        final String docId = DocumentsContract.getDocumentId(uri);
                        final String[] split = docId.split(":");
                        final String type = split[0];

                        Uri contentUri = null;
                        if ("image".equals(type)) {
                            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                        } else if ("video".equals(type)) {
                            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                        } else if ("audio".equals(type)) {
                            contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                        }

                        final String selection = "_id=?";
                        final String[] selectionArgs = new String[]{
                                split[1]
                        };

                        path = mMmultimediaUtil.getAbsolutePath(mContext, contentUri, selection, selectionArgs);
                    } else if ("com.android.providers.downloads.documents".equals(uri.getAuthority())) {
                        Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"),
                                Long.parseLong(DocumentsContract.getDocumentId(uri)));
                        path = mMmultimediaUtil.getAbsolutePath(mContext, contentUri, null, null);
                    } else if ("com.android.externalstorage.documents".equals(uri.getAuthority())) {
                        String[] split = DocumentsContract.getDocumentId(uri).split(":");
                        if ("primary".equalsIgnoreCase(split[0])) {
                            path = Environment.getExternalStorageDirectory() + "/" + split[1];
                        }
                    }
                }

                if (mPickListener != null) {
                    if (TextUtils.isEmpty(path)) {
                        mPickListener.onResult(false, null);
                    } else {
                        mPickListener.onResult(true, path);
                    }
                }
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                if (mPickListener != null) {
                    if (new File(mCameraPath).exists()) {
                        mPickListener.onResult(true, mCameraPath);
                    } else {
                        mPickListener.onResult(false, null);
                    }
                }
            }
        }

        this.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (verifyPermissions(grantResults)) {
            switch (requestCode) {
                case CAMERA_REQUEST_CODE:
                    startCamera();
                    break;
                case RECORD_REQUEST_CODE:
                    if (ToolHandler.TH_TYPE_SCREEN_AUDIO.equals(mIntent)) {
                        startRecordAudio();
                    } else {
                        startRecordVideo();
                    }
                    break;
                default:
                    break;
            }
        } else {
            UIToast.getInstance(mContext.getApplicationContext()).show(mContext.getString(R.string.fx_permission_denied));
            dismiss();
        }
    }

    private boolean verifyPermissions(int[] grantResults) {
        for (int grantResult : grantResults) {
            if (grantResult != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private IPickResultListener mPickListener;

    public void setOnPickPicListener(IPickResultListener listener) {
        this.mPickListener = listener;
    }

    public interface IPickResultListener {
        void onResult(boolean isSuccess, String path);
    }

}
