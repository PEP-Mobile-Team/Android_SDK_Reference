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
package com.foxit.uiextensions.modules.print;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.pdf.PdfDocument;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.pdf.PrintedPdfDocument;
import android.text.TextUtils;
import android.util.Log;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.Renderer;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;

import java.io.FileOutputStream;
import java.io.IOException;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class PDFPrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = PDFPrintAdapter.class.getSimpleName();

    private Context mContext;

    private PrintedPdfDocument mPdfDocument;
    private PDFDoc mPDFDoc;
    private String mFileName;
    private IPrintResultCallback resultCallback;
    private PrintDocumentInfo printDocumentInfo;

    private boolean mIsPrintAnnot = true;

    public PDFPrintAdapter(Context context, PDFDoc pdfDoc, String fileName,IPrintResultCallback callback) {
        this(context, pdfDoc, fileName, true, callback);
    }

    public PDFPrintAdapter(Context context, PDFDoc pdfDoc, String fileName, boolean isPrintAnnot, IPrintResultCallback callback) {
        this.mContext = context;
        this.mPDFDoc = pdfDoc;
        this.mFileName = fileName;
        this.mIsPrintAnnot = isPrintAnnot;
        this.resultCallback = callback;
    }

    @Override
    public void onLayout(PrintAttributes oldAttributes,
                         final PrintAttributes newAttributes,
                         final CancellationSignal cancellationSignal,
                         final LayoutResultCallback callback,
                         Bundle metadata) {

        if (cancellationSignal.isCanceled()) {
            callback.onLayoutCancelled();
            if (resultCallback != null) {
                resultCallback.printCancelled();
            }
            return;
        }

        new AsyncTask<Void, Void, PrintDocumentInfo>() {

            @Override
            protected void onPreExecute() {

                cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        cancel(true);
                    }
                });

                PrintAttributes mPrintAttributes = new PrintAttributes.Builder()
                        .setResolution(newAttributes.getResolution())
                        .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
                        .setMediaSize(newAttributes.getMediaSize())
                        .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
                        .build();

                mPdfDocument = new PrintedPdfDocument(mContext, mPrintAttributes);
            }

            @Override
            protected PrintDocumentInfo doInBackground(Void... voids) {

                try {
                    if (TextUtils.isEmpty(mFileName)) {
                        mFileName = PrintController.DEFAULT_OUTFILE_NAME;
                    }

                    printDocumentInfo = new PrintDocumentInfo
                            .Builder(mFileName)
                            .setPageCount(mPDFDoc.getPageCount())
                            .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build();

                    callback.onLayoutFinished(printDocumentInfo, true);
                    return printDocumentInfo;
                } catch (Exception e) {
                    callback.onLayoutFailed(null);
                    if (resultCallback != null) {
                        resultCallback.printFailed();
                    }
                    Log.e(TAG, "Exception - msg:" + e.getMessage());
                }
                return null;
            }

            @Override
            protected void onCancelled(PrintDocumentInfo result) {
                callback.onLayoutCancelled();
                if (resultCallback != null) {
                    resultCallback.printCancelled();
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    @Override
    public void onWrite(final PageRange[] pageRanges,
                        final ParcelFileDescriptor destination,
                        final CancellationSignal cancellationSignal,
                        final WriteResultCallback callback) {

        if (cancellationSignal.isCanceled()) {
            callback.onWriteCancelled();
            if (resultCallback != null) {
                resultCallback.printCancelled();
            }
            return;
        }

        new AsyncTask<Void, Void, Void>() {

            @Override
            protected void onPreExecute() {
                cancellationSignal.setOnCancelListener(new CancellationSignal.OnCancelListener() {
                    @Override
                    public void onCancel() {
                        cancel(true);
                    }
                });
            }

            @Override
            protected Void doInBackground(Void... voids) {
                try {
                    PdfDocument.Page page = null;
                    int pageCount = mPDFDoc.getPageCount();
                    for (int i = 0; i < pageCount; i++) {
                        if (isCancelled()) {
                            return null;
                        }
                        if (mPdfDocument == null){
                            return null;
                        }

                        page = mPdfDocument.startPage(i);

                        PDFPage pdfPage = mPDFDoc.getPage(i);
                        if (!pdfPage.isParsed()) {
                            Progressive progressive = pdfPage.startParse(PDFPage.e_ParsePageNormal, null, false);
                            int state = Progressive.e_ToBeContinued;

                            while (state == Progressive.e_ToBeContinued) {
                                state = progressive.resume();
                            }
                        }

                        Rect bmpArea = new Rect(0, 0, page.getCanvas().getWidth(), page.getCanvas().getHeight());
                        Bitmap bitmap = Bitmap.createBitmap(bmpArea.width(), bmpArea.height(), Bitmap.Config.RGB_565);

                        bitmap.eraseColor(Color.WHITE);

                        Renderer renderer = new Renderer(bitmap, true);
                        if (renderer == null) {
                            continue;
                        }
                        renderer.setColorMode(Renderer.e_ColorModeNormal);

                        int contentFlags = Renderer.e_RenderPage;
                        if (mIsPrintAnnot) {
                            contentFlags |= Renderer.e_RenderAnnot;
                        }
                        renderer.enableForPrint(true);
                        renderer.setRenderContentFlags(contentFlags);

                        Matrix2D matrix = pdfPage.getDisplayMatrix(-bmpArea.left, -bmpArea.top, bmpArea.width(), bmpArea.height(), 0);
                        Progressive progressive = renderer.startRender(pdfPage, matrix, null);
                        int state = Progressive.e_ToBeContinued;
                        while (state == Progressive.e_ToBeContinued) {
                            state = progressive.resume();
                        }

                        page.getCanvas().drawBitmap(bitmap, 0, 0, new Paint());
                        mPdfDocument.finishPage(page);
                        mPDFDoc.clearRenderCache();

                        bitmap.recycle();
                        bitmap = null;
                    }

                    mPdfDocument.writeTo(new FileOutputStream(
                            destination.getFileDescriptor()));
                    callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
                    if (resultCallback != null) {
                        resultCallback.printFinished();
                    }
                } catch (PDFException e) {
                    callback.onWriteFailed("An error occurred while trying to print the document: on write failed");
                    if (resultCallback != null) {
                        resultCallback.printFailed();
                    }
                    Log.e(TAG, "PDFException - code: " + e.getLastError() + "   msg: " + e.getMessage());
                } catch (IOException e) {
                    Log.e(TAG, "Exception - msg:" + e.getMessage());
                    callback.onWriteFailed(e.toString());
                    if (resultCallback != null) {
                        resultCallback.printFailed();
                    }
                } finally {
                    if (mPdfDocument != null) {
                        mPdfDocument.close();
                        mPdfDocument = null;
                    }
                }
                return null;
            }

            @Override
            protected void onCancelled(Void result) {
                callback.onWriteCancelled();
                if (resultCallback != null) {
                    resultCallback.printCancelled();
                }
                if (mPdfDocument != null) {
                    mPdfDocument.close();
                    mPdfDocument = null;
                }
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
    }

    @Override
    public void onFinish() {
        super.onFinish();
        if (mPdfDocument != null) {
            mPdfDocument.close();
            mPdfDocument = null;
        }
    }

}
