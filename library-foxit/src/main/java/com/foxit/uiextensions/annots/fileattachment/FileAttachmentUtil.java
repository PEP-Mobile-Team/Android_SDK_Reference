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
package com.foxit.uiextensions.annots.fileattachment;


import android.content.Context;

import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.fxcrt.FileReaderCallback;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.FileAttachment;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.utils.Event;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class FileAttachmentUtil {

    public static String[] getIconNames(){
        return new String[]{
                "Graph", "Paperclip", "PushPin", "Tag"
        };
    }

    public static String getIconName(int type) {
        String[] iconNames = getIconNames();
        return iconNames[type];
    }

    public static int getIconType(String name) {
        String[] iconNames = getIconNames();
        for(int i = 0; i < iconNames.length; i++) {
            if(iconNames[i].contentEquals(name))
                return i;
        }
        return 0;
    }

    public static void saveAttachment(PDFViewCtrl pdfViewCtrl, final String newFile, final Annot annot, final Event.Callback callback) {

        Task.CallBack callBack = new Task.CallBack() {
            @Override
            public void result(Task task) {
                if (callback != null) {
                    callback.result(null, true);
                }
            }
        };
        Task task = new Task(callBack) {
            @Override
            protected void execute() {
                try {
                    FileSpec fileSpec = ((FileAttachment) annot).getFileSpec();
                    if (fileSpec == null)
                        return;
                    FileReaderCallback fileRead = fileSpec.getFileData();
                    if (fileRead == null)
                        return;
                    FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                    int offset = 0;
                    int bufSize = 4 * 1024;
                    long fileSize = fileRead.getSize();
                    byte[] buf;

                    while (true) {
                        if(fileSize < bufSize + offset) {
                            buf = new byte[(int)(fileSize - offset)];
                            fileRead.readBlock(buf, offset, fileSize - offset);
                        } else {
                            buf = new byte[bufSize];
                            fileRead.readBlock(buf, offset, bufSize);
                        }
                        if (buf.length != bufSize) {
                            bufferedOutputStream.write(buf, 0, buf.length);
                            break;
                        } else
                            bufferedOutputStream.write(buf, 0, bufSize);
                        offset += bufSize;

                    }
                    bufferedOutputStream.flush();

                    bufferedOutputStream.close();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        pdfViewCtrl.addTask(task);
    }

    public static void saveAttachment(PDFViewCtrl pdfViewCtrl, final String newFile, final FileSpec fileSpec, final Event.Callback callback) {

        Task.CallBack callBack = new Task.CallBack() {
            @Override
            public void result(Task task) {
                if (callback != null) {
                    callback.result(null, true);
                }
            }
        };
        Task task = new Task(callBack) {
            @Override
            protected void execute() {
                try {
                    if (fileSpec == null)
                        return;
                    FileReaderCallback fileRead = fileSpec.getFileData();
                    if (fileRead == null)
                        return;
                    File file = new File(newFile);
                    if (!file.exists()){
                        file.getParentFile().mkdirs();
                    }
                    FileOutputStream fileOutputStream = new FileOutputStream(file);
                    BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);

                    int offset = 0;
                    int bufSize = 4 * 1024;
                    long fileSize = fileRead.getSize();
                    byte[] buf;

                    while (true) {
                        if(fileSize < bufSize + offset) {
                            buf = new byte[(int)(fileSize - offset)];
                            fileRead.readBlock(buf, offset, fileSize - offset);
                        } else {
                            buf = new byte[bufSize];
                            fileRead.readBlock(buf, offset, bufSize);
                        }

                        if (buf.length != bufSize) {
                            bufferedOutputStream.write(buf, 0, buf.length);
                            break;
                        } else
                            bufferedOutputStream.write(buf, 0, bufSize);
                        offset += bufSize;

                    }
                    bufferedOutputStream.flush();

                    bufferedOutputStream.close();
                    fileOutputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        pdfViewCtrl.addTask(task);
    }

    public static void delete(String path){
        File file = new File(path);
        if (file.exists()){
            file.delete();
        }
    }

}
