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
package com.foxit.uiextensions.home.local;

import android.content.Context;
import android.os.Handler;

import com.foxit.uiextensions.controls.filebrowser.imp.FileItem;
import com.foxit.uiextensions.utils.AppFileUtil;


import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppStorageManager;
import com.foxit.uiextensions.utils.thread.AppThreadManager;

import java.io.File;
import java.io.FileFilter;
import java.util.List;

class LocalTask {

    static class AllPDFs implements Runnable {
        static AllPDFs sPDFTask = null;

        public static void start(Context context, Handler handler) {
            if (sPDFTask != null) {
                sPDFTask.stopped = true;
                sPDFTask = null;
            }
            sPDFTask = new AllPDFs(context, handler);
            AppThreadManager.getInstance().startThread(sPDFTask);
        }

        public static void stop() {
            if (sPDFTask != null) {
                sPDFTask.stopped = true;
            }
            sPDFTask = null;
        }

        private Handler handler;
        private boolean stopped;
        private Context mContext;
        public AllPDFs(Context context, Handler handler) {
            this.handler = handler;
            mContext = context;
        }

        @Override
        public void run() {
            if (stopped) return;
            Thread.currentThread().setPriority(3);
            List<String> roots = AppStorageManager.getInstance(mContext).getVolumePaths();
            if (roots.size() == 0) return;
            for (String path : roots) {
                if (stopped) return;
                try {
                    scanPDF(new File(path));
                } catch (StackOverflowError error) {
                    error.printStackTrace();
                }
            }
            handler.obtainMessage(LocalModule.MSG_PDFs_STOP, null).sendToTarget();
        }

        private void scanPDF(File file) {
            File[] files = file.listFiles(mPDFFilter);
            if (files != null && files.length > 0) {
                FileItem[] items = new FileItem[files.length + 1];
                int index = 0;
                items[index] = new FileItem();
                items[index].type = FileItem.TYPE_ALL_PDF_FOLDER;
                items[index].path = file.getPath();
                items[index].name = file.getPath();
                for (File f : files) {
                    index++;
                    items[index] = new FileItem();
                    items[index].type = FileItem.TYPE_ALL_PDF_FILE;
                    items[index].path = f.getPath();
                    items[index].parentPath = f.getParent();
                    items[index].name = f.getName();
                    items[index].date = AppDmUtil.getLocalDateString(AppDmUtil.javaDateToDocumentDate(f.lastModified()));
                    items[index].size = AppFileUtil.formatFileSize(f.length());
                }
                if (stopped) return;
                handler.obtainMessage(LocalModule.MSG_UPDATE_PDFs, items).sendToTarget();
            }
            File[] folders = file.listFiles(mFolderFilter);
            if (folders == null || folders.length == 0) return;
            for (File f : folders) {
                if (stopped) return;
                scanPDF(f);
            }
        }

        private FileFilter mPDFFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                if (pathname.isHidden() || !pathname.canRead() || !pathname.isFile()) return false;
//                String name = pathname.getName().toLowerCase();
//                return name.toLowerCase().endsWith(".pdf");
                return true;
            }
        };

        private FileFilter mFolderFilter = new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isHidden() && pathname.canRead() && pathname.isDirectory();
            }
        };
    }
}

