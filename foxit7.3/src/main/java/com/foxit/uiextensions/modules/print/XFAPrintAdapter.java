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
import com.foxit.sdk.addon.xfa.XFADoc;
import com.foxit.sdk.addon.xfa.XFAPage;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.Renderer;
import com.foxit.sdk.common.fxcrt.Matrix2D;

import java.io.FileOutputStream;
import java.io.IOException;

@TargetApi(Build.VERSION_CODES.KITKAT)
public class XFAPrintAdapter extends PrintDocumentAdapter {
    private static final String TAG = XFAPrintAdapter.class.getSimpleName();

    private Context mContext;

    private PrintedPdfDocument mPdfDocument;
    private XFADoc mXFADoc;
    private String mFileName;
    private IPrintResultCallback resultCallback;
    private PrintDocumentInfo printDocumentInfo;

    private boolean mIsPrintAnnot = true;

    public XFAPrintAdapter(Context context, XFADoc xfaDoc, String name, IPrintResultCallback callback) {
        this(context, xfaDoc, name, true, callback);
    }

    public XFAPrintAdapter(Context context, XFADoc xfaDoc, String name, boolean isPrintAnnot, IPrintResultCallback callback) {
        this.mContext = context;
        this.mXFADoc = xfaDoc;
        this.mFileName = name;
        this.resultCallback = callback;
        this.mIsPrintAnnot = isPrintAnnot;
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
                            .setPageCount(mXFADoc.getPageCount())
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
                    int pageCount = mXFADoc.getPageCount();
                    for (int i = 0; i < pageCount; i++) {
                        if (isCancelled()) {
                            return null;
                        }
                        if (mPdfDocument == null) {
                            return null;
                        }

                        page = mPdfDocument.startPage(i);
                        Rect bmpArea = new Rect(0, 0, page.getCanvas().getWidth(), page.getCanvas().getHeight());
                        Bitmap bitmap = Bitmap.createBitmap(bmpArea.width(), bmpArea.height(), Bitmap.Config.RGB_565);
                        bitmap.eraseColor(Color.WHITE);

                        Renderer renderer = new Renderer(bitmap, true);
                        renderer.setColorMode(Renderer.e_ColorModeNormal);
                        renderer.setMappingModeColors(Color.WHITE, Color.BLACK);
                        int contentFlags = Renderer.e_RenderPage;
                        if (mIsPrintAnnot) {
                            contentFlags |= Renderer.e_RenderAnnot;
                        }
                        renderer.enableForPrint(true);
                        renderer.setRenderContentFlags(contentFlags);

                        XFAPage xfaPage = mXFADoc.getPage(i);
                        Matrix2D matrix = xfaPage.getDisplayMatrix(-bmpArea.left, -bmpArea.top, bmpArea.width(), bmpArea.height(), 0);
                        Progressive progressive = renderer.startRenderXFAPage(xfaPage, matrix, false, null);
                        int state = Progressive.e_ToBeContinued;
                        while (state == Progressive.e_ToBeContinued) {
                            state = progressive.resume();
                        }

                        xfaPage = null;
                        renderer = null;
                        matrix = null;
                        progressive = null;

                        if (state != Progressive.e_Finished) {
                            continue;
                        }

                        page.getCanvas().drawBitmap(bitmap, 0, 0, new Paint());
                        mPdfDocument.finishPage(page);

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
