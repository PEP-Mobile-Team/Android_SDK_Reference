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
package com.foxit.uiextensions.modules.print;


import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.text.TextUtils;

import com.foxit.uiextensions.utils.AppFileUtil;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class DefaultPrintAdapter extends PrintDocumentAdapter {

    private PrintController mPrintController;

    public DefaultPrintAdapter(Context context, PrintController controller) {
        this.mPrintController = controller;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         PrintAttributes newAttributes,
                         CancellationSignal cancellationSignal,
                         LayoutResultCallback callback,
                         Bundle metadata) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            return;
        }

        String outputfileName = mPrintController.getOutputFileName();
        if (TextUtils.isEmpty(outputfileName)) {
            if (PrintController.PRINT_FROM_PATH == mPrintController.getPrintFrom()) {
                outputfileName = AppFileUtil.getFileNameWithoutExt(mPrintController.getInputPath());
            } else {
                outputfileName = PrintController.DEFAULT_OUTFILE_NAME;
            }
        }

        PrintDocumentInfo.Builder builder = new PrintDocumentInfo
                .Builder(outputfileName)
                .setPageCount(mPrintController.getPageCount())
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT);

        PrintDocumentInfo info = builder.build();
        callback.onLayoutFinished(info, true);
    }

    @Override
    public void onWrite(final PageRange[] pageRanges,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal cancellationSignal,
                        final WriteResultCallback callback) {

        InputStream input = null;
        OutputStream output = null;
        try {
            switch (mPrintController.getPrintFrom()) {
                case PrintController.PRINT_FROM_PATH:
                    input = new FileInputStream(mPrintController.getInputPath());
                    break;
                case PrintController.PRINT_FROM_STREAM:
                    input = mPrintController.getInputStream();
                    break;
                default:
                    break;
            }
            output = new FileOutputStream(destination.getFileDescriptor());

            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) > 0) {
                output.write(buf, 0, bytesRead);
            }
            callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (input != null)
                    input.close();
                if (output != null)
                    output.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFinish() {
        super.onFinish();
        if (mPrintController != null) {
            mPrintController = null;
        }
    }


}
