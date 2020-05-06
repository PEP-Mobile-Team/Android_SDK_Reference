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
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.foxit.uiextensions.R;

import java.io.File;
import java.io.InputStream;

@TargetApi(Build.VERSION_CODES.KITKAT)
class PrintController {

    protected static final int PRINT_BY_CUSTOM = 0x00000000;
    protected static final int PRINT_FROM_STREAM = 0x00000002;
    protected static final int PRINT_FROM_PATH = 0x00000004;
    protected static final String DEFAULT_JOB_NAME = "print_job";
    protected static final String DEFAULT_OUTFILE_NAME = "print_output";

    private Context mContext;

    private int mPrintFrom = PRINT_BY_CUSTOM;
    private int mPageCount = PrintDocumentInfo.PAGE_COUNT_UNKNOWN;
    private String mPrintJobName;
    private String mOutputFileName;

    private PrintDocumentAdapter mAdapter;
    private PrintAttributes mAttributes;

    private String mInputPath;
    private InputStream mInputStream;

    public PrintController(Context context) {
        this.mContext = context;
    }

    public void print() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            Toast.makeText(mContext, mContext.getApplicationContext().getString(R.string.fx_os_version_too_low_toast), Toast.LENGTH_SHORT).show();
            return;
        }

        if (!isFileExists()) {
            Toast.makeText(mContext, mContext.getApplicationContext().getString(R.string.file_not_exist), Toast.LENGTH_SHORT).show();
            return;
        }

        if (mPrintFrom == PRINT_FROM_PATH  && !getInputPath().toLowerCase().endsWith(".pdf")){
            Toast.makeText(mContext, mContext.getApplicationContext().getString(R.string.file_is_not_pdf), Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager printManager = (PrintManager) mContext.getSystemService(Context.PRINT_SERVICE);
        PrintDocumentAdapter adapter = getAdapter();
        if (adapter == null) {
            adapter = new DefaultPrintAdapter(mContext, this);
        }
        //Optionally include print attributes.
        PrintAttributes printAttributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.NA_LETTER)
                .build();

        String jobName = getPrintJobName();
        if (TextUtils.isEmpty(jobName)) {
            jobName = DEFAULT_JOB_NAME;
        }
        printManager.print(jobName, adapter, printAttributes);
    }

    public String getInputPath() {
        return mInputPath;
    }

    public void setInputPath(String inputPath) {
        this.mInputPath = inputPath;
    }

    public String getPrintJobName() {
        return mPrintJobName;
    }

    public void setPrintJobName(String printJobName) {
        this.mPrintJobName = printJobName;
    }

    public String getOutputFileName() {
        return mOutputFileName;
    }

    public void setOutputFileName(String outputFileName) {
        this.mOutputFileName = outputFileName;
    }

    public PrintDocumentAdapter getAdapter() {
        return mAdapter;
    }

    public void setAdapter(PrintDocumentAdapter adapter) {
        this.mAdapter = adapter;
    }

    public PrintAttributes getAttributes() {
        return mAttributes;
    }

    public void setAttributes(PrintAttributes attributes) {
        this.mAttributes = attributes;
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.mInputStream = inputStream;
    }

    public int getPrintFrom() {
        return mPrintFrom;
    }

    public void setPrintFrom(int printFrom) {
        this.mPrintFrom = printFrom;
    }

    public int getPageCount() {
        return mPageCount;
    }

    public void setPageCount(int pageCount) {
        this.mPageCount = pageCount;
    }

    public static class PrintParmas {

        public final Context mContext;

        public int mPageCount = PrintDocumentInfo.PAGE_COUNT_UNKNOWN;
        public String mPrintJobName;
        public String mInputPath;
        public String mOutputFileName;
        public PrintDocumentAdapter mAdapter;
        public PrintAttributes mAttributes;
        public InputStream mInputStream;

        public PrintParmas(Context context) {
            this.mContext = context;
        }

        public void apply(PrintController controller) {
            if (null != mPrintJobName)
                controller.setPrintJobName(mPrintJobName);
            if (mOutputFileName != null)
                controller.setOutputFileName(mOutputFileName);
            if (mAdapter != null)
                controller.setAdapter(mAdapter);
            if (mAttributes != null)
                controller.setAttributes(mAttributes);
            if (mPageCount != PrintDocumentInfo.PAGE_COUNT_UNKNOWN)
                controller.setPageCount(mPageCount);

            if (mInputPath != null) {
                controller.setInputPath(mInputPath);
                controller.setPrintFrom(PrintController.PRINT_FROM_PATH);
            }
            if (mInputStream != null) {
                controller.setInputStream(mInputStream);
                controller.setPrintFrom(PrintController.PRINT_FROM_STREAM);
            }
        }
    }

    private boolean isFileExists() {
        if (mPrintFrom == PRINT_BY_CUSTOM && getAdapter() == null) {
            return false;
        }
        if (mPrintFrom == PRINT_FROM_PATH) {
            File file = new File(getInputPath());
            if (!file.exists()) {
                return false;
            }
        }
        if (mPrintFrom == PRINT_FROM_STREAM && (null == getInputStream())) {
            return false;
        }
        return true;
    }
}
