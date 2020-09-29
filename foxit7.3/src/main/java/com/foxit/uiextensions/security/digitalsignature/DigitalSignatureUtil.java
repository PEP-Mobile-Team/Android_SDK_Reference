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
package com.foxit.uiextensions.security.digitalsignature;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.pdf.Signature;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.security.certificate.CertificateDataSupport;
import com.foxit.uiextensions.security.certificate.CertificateFileInfo;
import com.foxit.uiextensions.security.certificate.CertificateFragment;
import com.foxit.uiextensions.security.certificate.CertificateSupport;
import com.foxit.uiextensions.security.certificate.CertificateViewSupport;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.Event;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DigitalSignatureUtil implements IDigitalSignatureUtil {
    private static final String DB_TABLE_DSG_PFX = "_pfx_dsg_cert";
    private static final String PUBLISHER = "publisher";
    private static final String ISSUER = "issuer";
    private static final String SERIALNUMBER = "serial_number";
    private static final String FILEPATH = "file_path";
    private static final String CHANGEFILEPATH = "file_change_path";
    private static final String FILENAME = "file_name";
    private static final String PASSWORD = "password";
    public static final int FULLPERMCODE = 0xf3c;
    public DigitalSignatureSecurityHandler mSecurityHandler;
    public Context mContext;
    public PDFViewCtrl mPdfViewCtrl;
    public CertificateSupport mCertSupport;
    public CertificateViewSupport mViewSupport;
    public CertificateFileInfo mFileInfo;
    private ExecutorService mExecutor;

    public DigitalSignatureUtil(Context context, PDFViewCtrl pdfViewCtrl) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mCertSupport = new CertificateSupport(mContext);
        mViewSupport = new CertificateViewSupport(context, pdfViewCtrl, mCertSupport);
        mSecurityHandler = new DigitalSignatureSecurityHandler(mContext,mPdfViewCtrl, this);
        ThreadFactory namedThreadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(@NonNull Runnable r) {
                Thread thread = Executors.defaultThreadFactory().newThread(r);
                thread.setName("signature-pool");
                return thread;
            }
        };

        mExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public CertificateDataSupport getCertDataSupport(){
        return mViewSupport.getDataSupport();
    }

    @Override
    public void addCertSignature(final String docPath, final String certPath, final Bitmap bitmap, final RectF rectF, final int pageIndex, final IDigitalSignatureCreateCallBack callBack) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_DSG_PFX, null, null, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String newPath = certPath + "x";
                        if (newPath.equals(cursor.getString(cursor.getColumnIndex(CHANGEFILEPATH)))) {

                            copyFile(newPath, cursor.getString(cursor.getColumnIndex(FILEPATH)));
                            final CertificateFileInfo info = new CertificateFileInfo();
                            info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                            info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                            info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                            info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                            info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                            info.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                            info.isCertFile = false;
                            info.permCode = FULLPERMCODE;

                            AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    DigitalSignatureUtil.this.creatDSGSign(bitmap, docPath, pageIndex, rectF, callBack, info);
                                }
                            });
                            break;
                        }
                    }
                    cursor.close();
                }
            }
        });

    }

    @Override
    public void addCertSignature(final String docPath, final String certPath, final Signature signature, final RectF rectF, final int pageIndex, final boolean isCustom, final IDigitalSignatureCreateCallBack callBack) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_DSG_PFX, null, null, null, null, null, null);
                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        String newPath = certPath + "x";
                        if (newPath.equals(cursor.getString(cursor.getColumnIndex(CHANGEFILEPATH)))) {

                            copyFile(newPath, cursor.getString(cursor.getColumnIndex(FILEPATH)));
                            final CertificateFileInfo info = new CertificateFileInfo();
                            info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                            info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                            info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                            info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                            info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                            info.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                            info.isCertFile = false;
                            info.permCode = FULLPERMCODE;

                            AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                                @Override
                                public void run() {
                                    DigitalSignatureUtil.this.creatDSGSign(docPath, pageIndex, signature, rectF, isCustom, callBack, info);
                                }
                            });
                            break;
                        }
                    }
                    cursor.close();
                }
            }
        });

    }

    @Override
    public void addCertList(final IDigitalSignatureCallBack callBack) {
        AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
            @Override
            public void run() {
                mViewSupport.showAllPfxFileDialog(true, true, new CertificateFragment.ICertDialogCallback() {
                    @Override
                    public void result(boolean succeed, Object result, Bitmap forSign) {
                        if (succeed) {
                            if (result == null) return;
                            mFileInfo = (CertificateFileInfo) result;

                            String newCertPath = mContext.getFilesDir() + "/DSGCert/" + mFileInfo.fileName;
                            String changeCertPath = newCertPath + "x";
                            copyFile(mFileInfo.filePath, changeCertPath);
                            ContentValues values = new ContentValues();
                            values.put(ISSUER, mFileInfo.issuer);
                            values.put(PUBLISHER, mFileInfo.publisher);
                            values.put(SERIALNUMBER, mFileInfo.serialNumber);
                            values.put(FILEPATH, newCertPath);
                            values.put(CHANGEFILEPATH, changeCertPath);
                            values.put(FILENAME, mFileInfo.fileName);
                            values.put(PASSWORD, mFileInfo.password);
                            Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_DSG_PFX, null, null, null, null, null, null);

                            AppSQLite.getInstance(mContext).insert(DB_TABLE_DSG_PFX, values);
                            if (callBack != null) {
                                callBack.onCertSelect(newCertPath, mFileInfo.fileName);
                            }
                        } else {
                            mViewSupport.dismissPfxDialog();
                            if (callBack != null) {
                                callBack.onCertSelect(null, null);
                            }
                        }
                    }
                });
            }
        });
    }

    public void creatDSGSign(Bitmap bitmap, final String docPath, final int pageIndex, final RectF rectF, final IDigitalSignatureCreateCallBack callBack, final CertificateFileInfo info) {

        mSecurityHandler.addSignature(docPath, info, bitmap, pageIndex, rectF, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_succeed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_failed), Toast.LENGTH_SHORT).show();
                }
                callBack.onCreateFinish(success);

            }
        });

    }

    public void creatDSGSign(final String docPath, final int pageIndex, Signature signature, final RectF rectF, boolean isCustom, final IDigitalSignatureCreateCallBack callBack, final CertificateFileInfo info) {

        mSecurityHandler.addSignature(docPath, info, pageIndex, signature, isCustom, new Event.Callback() {
            @Override
            public void result(Event event, boolean success) {
                if (success) {
                    Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_succeed), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(mContext, AppResource.getString(mContext, R.string.dsg_sign_failed), Toast.LENGTH_SHORT).show();
                }
                callBack.onCreateFinish(success);

            }
        });

    }


    public void copyFile(String oldPath, String newPath) {
        InputStream inStream = null;
        FileOutputStream fs = null;
        try {
            int byteread = 0;
            File file = new File(newPath);
            if (!file.exists()) {
                inStream = new FileInputStream(oldPath);
                fs = new FileOutputStream(newPath);
                byte[] buffer = new byte[1444];
                while ((byteread = inStream.read(buffer)) != -1) {
                    fs.write(buffer, 0, byteread);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inStream != null){
                    inStream.close();
                }

                if (fs != null){
                    fs.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}