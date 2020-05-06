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
package com.foxit.uiextensions.modules.snapshot;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.edmodo.cropper.CropImageView;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.pdfreader.impl.MainFrame;
import com.foxit.uiextensions.utils.AppUtil;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * the snapshot view
 */
public class SnapshotDialogFragment extends DialogFragment implements SnapshotContract.View, View.OnClickListener {

    public final static String TAG = SnapshotDialogFragment.class.getSimpleName();

    private SnapshotContract.Presenter presenter;

    private CropImageView snapshotImage;

    private ImageView cancel;

    private ImageView save;

    private Bitmap bitmap;

    private PDFViewCtrl pdfViewCtrl;

    private Context context;

    private int orientation;

    public void setPdfViewCtrl(@NonNull PDFViewCtrl pdfViewCtrl) {
        this.pdfViewCtrl = AppUtil.requireNonNull(pdfViewCtrl);
    }

    public void setContext(Context context){
        this.context = context;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        orientation = getActivity().getRequestedOrientation();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }

    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_snapshot, container, false);
        snapshotImage = (CropImageView) rootView.findViewById(R.id.snapshot_cropimage);
        snapshotImage.setGuidelines(1);
        pdfViewCtrl.setDrawingCacheEnabled(true);
        pdfViewCtrl.buildDrawingCache();
        bitmap = pdfViewCtrl.getDrawingCache();

        snapshotImage.setFixedAspectRatio(false);
        snapshotImage.setImageBitmap(bitmap);
        cancel = (ImageView) rootView.findViewById(R.id.snapshot_cancel);
        save = (ImageView) rootView.findViewById(R.id.snapshot_save);
        cancel.setOnClickListener(this);
        save.setOnClickListener(this);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onStart() {
        super.onStart();

        showSystemUI();
        View rootView = ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getRootView();
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        int width = rootView.getWidth();
        int height = rootView.getHeight();

        Window window = getDialog().getWindow();
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.gravity = Gravity.LEFT | Gravity.TOP;
        windowParams.windowAnimations = R.style.View_Animation_RtoL;
        windowParams.dimAmount = 0.0f;
        windowParams.x = x;
        windowParams.y = y;
        windowParams.width = width;
        windowParams.height = height;
        window.setAttributes(windowParams);
        getDialog().setCanceledOnTouchOutside(true);
    }

    private void showToolbars(){
        MainFrame mainFrame = (MainFrame) ((UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager()).getMainFrame();
        mainFrame.setHideSystemUI(true);
        mainFrame.showToolbars();
    }

    private void showSystemUI(){
        UIExtensionsManager uiExtensionsManager = (UIExtensionsManager)pdfViewCtrl.getUIExtensionsManager();
        MainFrame mainFrame = (MainFrame)uiExtensionsManager.getMainFrame();
        if (mainFrame.isToolbarsVisible()){
            mainFrame.setHideSystemUI(false);
        } else {
            AppUtil.showSystemUI(uiExtensionsManager.getAttachedActivity());
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        pdfViewCtrl.destroyDrawingCache();
        showToolbars();
        getActivity().setRequestedOrientation(orientation);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        ViewGroup viewGroup = (ViewGroup) getView();
        viewGroup.removeAllViewsInLayout();
        View view = onCreateView(getActivity().getLayoutInflater(), viewGroup, null);
        viewGroup.addView(view);
    }

    @Override
    public void setPresenter(SnapshotContract.Presenter presenter) {
        this.presenter = AppUtil.requireNonNull(presenter);
    }

    @Override
    public void showToast(String content) {
        Toast.makeText(context,content, Toast.LENGTH_LONG).show();
    }

    @Override
    public Bitmap getBitmap() {
        return snapshotImage.getCroppedImage();
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.snapshot_cancel)
            dismiss();

        if (id == R.id.snapshot_save){
            try{
                presenter.save();
            }catch (RuntimeException re){
                re.printStackTrace();
            }
        }
    }

}
