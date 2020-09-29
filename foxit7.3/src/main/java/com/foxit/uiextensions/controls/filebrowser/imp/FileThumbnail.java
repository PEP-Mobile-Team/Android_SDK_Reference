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
package com.foxit.uiextensions.controls.filebrowser.imp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.Renderer;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppFileUtil;
import com.foxit.uiextensions.utils.AppStorageManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;

public class FileThumbnail {

    public interface ThumbnailCallback {
        void result(boolean succeed, String filePath);
    }

    private static final int MSG_EXECUTE = 0x0001;

    private Map<String, WeakReference<Bitmap>> mThumbnailMap;
    private Vector<ThumbnailTask> mThumbnailTasks;
    private Cache mThumbnailCache;
    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_EXECUTE) {
                mRunning = false;
                executeTask();
            }
        }
    };
    private boolean mRunning;
    private Set<String> mErrorSet;
    private Context mContext;

    private FileThumbnail(Context context) {
        mThumbnailCache = new Cache();
        mThumbnailMap = new ConcurrentHashMap<String, WeakReference<Bitmap>>();
        mThumbnailTasks = new Vector<ThumbnailTask>();
        mErrorSet = new HashSet<String>(10);
        mContext = context;
    }

    public static boolean isSupportThumbnail(String pathname) {
        if (pathname.toLowerCase().endsWith("pdf"))
            return true;

        return false;
    }

    public synchronized Bitmap getThumbnail(String filePath, ThumbnailCallback callback) {
        if (filePath == null || filePath.length() == 0 || !isSupportThumbnail(filePath))
            return null;
        if (mErrorSet.contains(filePath)) {
            return null;
        }
        WeakReference<Bitmap> reference = mThumbnailMap.get(filePath);
        Bitmap bitmap = null;
        if (reference != null) {
            bitmap = reference.get();
        }
        if (bitmap != null) {
            return bitmap;
        }
        bitmap = mThumbnailCache.getThumbnail(filePath);
        if (bitmap != null) {
            reference = new WeakReference<Bitmap>(bitmap);
            mThumbnailMap.put(filePath, reference);
            return bitmap;
        }
        addTask(filePath, callback);
        executeTask();
        return bitmap;
    }

    private void addTask(String filePath, ThumbnailCallback callback) {
        ThumbnailTask task = new ThumbnailTask(filePath, callback);
        if (!mThumbnailTasks.contains(task)) {
            mThumbnailTasks.add(task);
        }
    }

    public synchronized void updateThumbnail(String filePath, ThumbnailCallback callback) {
        mErrorSet.remove(filePath);
        addTask(filePath, callback);
        executeTask();
    }

    public synchronized void executeTask() {
        if (mRunning) return;
        if (mThumbnailTasks.size() > 0) {
            mRunning = true;
            ThumbnailTask task = mThumbnailTasks.remove(0);
            if (task != null) {
                new Thread(task).start();
            }
        }
    }

    private int dip2px(int dip) {
        return AppDisplay.getInstance(mContext).dp2px(dip);
    }

    class ThumbnailTask implements Runnable {
        private ThumbnailCallback mCallback;
        private String mFilePath;

        public ThumbnailTask(String filePath, ThumbnailCallback callback) {
            mFilePath = filePath;
            mCallback = callback;
        }

        @Override
        public void run() {
            Bitmap bitmap = null;
            try {
                bitmap = drawPage(mFilePath, 0, dip2px(38), dip2px(44), 0);
            } catch (OutOfMemoryError error) {
                error.printStackTrace();
            }
            if (bitmap != null) {
                mThumbnailCache.saveThumbnail(bitmap, mFilePath);
                mThumbnailMap.put(mFilePath, new WeakReference<Bitmap>(bitmap));
            } else {
                mErrorSet.add(mFilePath);
                mThumbnailCache.removeFile(mFilePath);
                mThumbnailMap.remove(mFilePath);
            }
            if (mCallback != null) {
                mCallback.result(bitmap != null, mFilePath);
            }
            mHandler.sendEmptyMessage(MSG_EXECUTE);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ThumbnailTask)) return false;
            if (((ThumbnailTask) o).mFilePath == null || ((ThumbnailTask) o).mCallback == null)
                return false;
            if (((ThumbnailTask) o).mCallback != mCallback) return false;
            return ((ThumbnailTask) o).mFilePath.equalsIgnoreCase(mFilePath);
        }

        private synchronized Bitmap drawPage(String filePath, int pageIndex, int bmpWidth, int bmpHeight, int rotate) {
            synchronized (AppFileUtil.getInstance().isOOMHappened) {
                if (AppFileUtil.getInstance().isOOMHappened) {
                    return null;
                }

                try {
                    PDFDoc pdfDoc = new PDFDoc(filePath);
                    pdfDoc.load(null);
                    if (pageIndex >= 0 && pageIndex < pdfDoc.getPageCount()) {
                        PDFPage page = pdfDoc.getPage(pageIndex);
                        if (!page.isParsed()) {
                            Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                            int state = Progressive.e_ToBeContinued;
                            while (state == Progressive.e_ToBeContinued) {
                                state = progressive.resume();
                            }
                        }

                        Matrix2D matrix = page.getDisplayMatrix(0, 0, bmpWidth, bmpHeight, rotate);
                        Bitmap bitmap = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.RGB_565);
                        bitmap.eraseColor(Color.WHITE);

                        Renderer renderer = new Renderer(bitmap, true);
                        renderer.setRenderAnnotsForThumbnail(true);
                        Progressive progressive = renderer.startRender(page, matrix, null);
                        int state = Progressive.e_ToBeContinued;
                        while (state == Progressive.e_ToBeContinued) {
                            state = progressive.resume();
                        }
                        return bitmap;
                    }
                    return null;
                } catch (PDFException ignored) {
                }
            }

            return null;
        }
    }

    public static synchronized FileThumbnail getInstance(Context context) {
        if (mFileThumbnail == null) {
            mFileThumbnail = new FileThumbnail(context);
        }
        return mFileThumbnail;
    }


    private static FileThumbnail mFileThumbnail = null;


    class Cache {
        private static final String CACHE_DIR = "FMThumbnailCache";
        private static final String SUFFIX = ".cache";

        //the max cache size 2M
        private static final int CACHE_SIZE = 2 << 20;

        public Bitmap getThumbnail(String filePath) {
            if (filePath == null || filePath.equals("")) return null;
            File cacheFile = new File(getCacheDir(), convertPathToFileName(filePath));
            if (cacheFile.exists()) {
                Bitmap bmp = BitmapFactory.decodeFile(cacheFile.getPath());
                if (bmp == null) {
                    cacheFile.delete();
                } else {
                    updateFileTime(cacheFile);
                    return bmp;
                }
            }
            return null;
        }

        public void saveThumbnail(Bitmap bitmap, String filePath) {
            if (bitmap == null) return;
            if (filePath == null || filePath.equals("")) return;
            File dir = getCacheDir();
            if (!dir.exists()) dir.mkdirs();
            long fileSize = AppFileUtil.getFolderSize(dir.getPath());
            if (fileSize > CACHE_SIZE) {
                removeCache(dir);
            }
            File file = new File(dir, convertPathToFileName(filePath));
            try {
                if (file.exists()) {
                    file.delete();
                }
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //if file size > 1M will clear 40% files
        private boolean removeCache(File dir) {
            File[] files = dir.listFiles();
            if (files == null) return true;
            long fileSize = AppFileUtil.getFolderSize(dir.getPath());
            if (fileSize > CACHE_SIZE) {
                int factor = (int) (0.4 * files.length + 1);
                Arrays.sort(files, new Comparator<File>() {

                    @Override
                    public int compare(File lhs, File rhs) {
                        if (lhs.lastModified() > rhs.lastModified()) {
                            return 1;
                        } else if (lhs.lastModified() < rhs.lastModified()) {
                            return -1;
                        }
                        return 0;
                    }
                });
                for (int i = 0; i < factor; i++) {
                    if (files[i].getName().contains(SUFFIX)) {
                        files[i].delete();
                    }
                }
            }
            return true;
        }

        private boolean updateFileTime(File file) {
            return file.setLastModified(System.currentTimeMillis());
        }

        private String convertPathToFileName(String filePath) {
            return filePath.replace(File.separator, "") + SUFFIX;
        }

        private File getCacheDir() {
            return new File(AppStorageManager.getInstance(mContext).getCacheDir(), CACHE_DIR);
        }

        public boolean removeFile(String filePath) {
            File delFile = new File(getCacheDir(), convertPathToFileName(filePath));
            if (delFile.exists()) {
                return delFile.delete();
            }
            return true;
        }
    }
}
