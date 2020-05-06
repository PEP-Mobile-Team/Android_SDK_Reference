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
import android.graphics.Bitmap;

import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import androidx.annotation.NonNull;

/**  Take snapshot on the view control..*/
public class SnapshotPresenter implements SnapshotContract.Presenter {

    private SnapshotContract.View view;
    private Context mContext;

    private final static String PATH = "/mnt/sdcard/FoxitSDK/Images/";

    public SnapshotPresenter(Context context, @NonNull SnapshotContract.View view) {
        this.view = AppUtil.requireNonNull(view);
        this.view.setPresenter(this);
        this.mContext = context;
    }

    @Override
    public void save() {
        Bitmap result = view.getBitmap();
        FileOutputStream fo = null;
        String path = generateFilePath();
        try {
            mkdir();
            File file = new File(path);
            file.createNewFile();
            fo = new FileOutputStream(file);
            result.compress(Bitmap.CompressFormat.PNG, 100, fo);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            view.showToast(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));
            view.dismiss();
            throw new RuntimeException(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));
        } catch (IOException ioe) {
            ioe.printStackTrace();
            view.showToast(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));
            view.dismiss();
            throw new RuntimeException(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));
        } catch (Exception e){
            e.printStackTrace();
            view.showToast(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));
            view.dismiss();
            throw new RuntimeException(mContext.getApplicationContext().getString(R.string.failed_to_save_snapshot));

        } finally {
            if (fo != null)
                try {
                    fo.flush();
                    fo.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
        }
        view.showToast(mContext.getApplicationContext().getString(R.string.the_snapshot_save_path, path));
        view.dismiss();
    }


    private String generateFilePath(){
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyMMddHHmmss");

        StringBuffer sb = new StringBuffer();
        sb.append(PATH+"snapshot-");
        sb.append(simpleDateFormat.format(calendar.getTime()));
        sb.append(".png");

        return sb.toString();
    }

    private boolean mkdir(){
        File file = new File(PATH);
        if (!file.exists()){
            return file.mkdirs();
        }
        return false;
    }
}

