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
package com.foxit.uiextensions.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class AppFileUtil {
    public Boolean isOOMHappened = false;


    //Check whether the SD is available.
    public static boolean isSDAvailable() {
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }

    public static String getAppCacheDir(Context context) {
        return context.getApplicationContext().getCacheDir().getAbsolutePath();
    }

    public static String getSDPath() {
        return Environment.getExternalStorageDirectory().getPath();
    }

    public static String getFilePath(Context context, Intent intent, String stringExtra) {
        String filePath = null;
        String action = intent.getAction();
        if (Intent.ACTION_VIEW.equals(action)) {
            Uri uri = intent.getData();
            if (uri.getScheme().equals("file")) {
                filePath = uri.getPath();
            } else {
                try {
                    ContentResolver resolver = context.getContentResolver();

                    String[][] projections = {new String[]{"_display_name"},
                            new String[]{"filename"},
                    };
                    String filename = null;
                    for (String[] projection : projections) {
                        try {
                            Cursor cursor = resolver.query(uri, projection, null, null, null);
                            if (cursor == null) continue;
                            cursor.moveToFirst();
                            int column_index = cursor.getColumnIndex(projection[0]);
                            if (column_index >= 0) {
                                filename = cursor.getString(column_index);
                                cursor.close();
                                break;
                            }
                            cursor.close();
                        } catch (Exception ignored) {
                        }
                    }
                    if (filename == null) {
                        filename = AppDmUtil.randomUUID(null) + ".pdf";
                    }
                    ParcelFileDescriptor parcelFileDescriptor = resolver.openFileDescriptor(uri, "r");
                    filePath = cacheContentPdfFile(context, parcelFileDescriptor != null ? parcelFileDescriptor.getFileDescriptor() : null, filename);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        if (filePath == null && stringExtra != null) {
            filePath = intent.getStringExtra(stringExtra);
        }
        return filePath;
    }

    private static String cacheContentPdfFile(Context context, FileDescriptor fileDesp, String filename) {
        String cacheDir;
        File contentDirFile;
        File contentFile = null;

        cacheDir = context.getCacheDir().getPath() + "/contentfile";

        contentDirFile = new File(cacheDir);
        if (contentDirFile.exists()) {
            AppFileUtil.deleteFolder(contentDirFile, false);
        }
        contentDirFile.mkdirs();
        if (contentDirFile.exists() && contentDirFile.isDirectory()) {
            String filePath = cacheDir + "/" + filename;
            contentFile = new File(filePath);
        }

        if (contentFile != null) {
            try {
                if (contentFile.exists()) {
                    contentFile.delete();
                }
                contentFile.createNewFile();
                FileInputStream fis = new FileInputStream(fileDesp);
                FileOutputStream fos = new FileOutputStream(contentFile);

                byte[] read = new byte[8 * 1024];
                int byteCount;
                while ((byteCount = fis.read(read)) > 0) {
                    fos.write(read, 0, byteCount);
                }
                fis.close();
                fos.flush();
                fos.close();
                return contentFile.getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        return null;
    }

    public static long getFileSize(String filePath) {
        if (AppUtil.isEmpty(filePath))
            return 0;
        long size = 0;
        File file = new File(filePath);
        if (!file.exists()) return size;
        size = file.length();
        return size;
    }

    //Format file size to string.
    public static String formatFileSize(long fileSize) {
        String sizeStr = null;
        float sizeFloat = 0;
        if (fileSize < 1024) {
            sizeStr = Long.toString(fileSize);
            sizeStr += "B";
        } else if (fileSize < (1 << 20)) {
            sizeFloat = (float) fileSize / (1 << 10);
            sizeFloat = (float) (Math.round(sizeFloat * 100)) / 100;
            sizeStr = Float.toString(sizeFloat) + "KB";
        } else if (fileSize < (1 << 30)) {
            sizeFloat = (float) fileSize / (1 << 20);
            sizeFloat = (float) (Math.round(sizeFloat * 100)) / 100;
            sizeStr = Float.toString(sizeFloat) + "MB";
        } else {
            sizeFloat = (float) fileSize / (1 << 30);
            sizeFloat = (float) (Math.round(sizeFloat * 100)) / 100;
            sizeStr = Float.toString(sizeFloat) + "GB";
        }
        return sizeStr;
    }

    public static long getFolderSize(String filePath) {
        long size = 0;
        File file = new File(filePath);
        if (!file.exists()) return size;
        File[] fileList = file.listFiles();
        if (fileList != null) {
            for (File subFile : fileList) {
                if (subFile.isDirectory()) {
                    size += getFolderSize(subFile.getPath());
                } else {
                    size += subFile.length();
                }
            }
        }
        return size;
    }

    public static String getFileFolder(String filePath) {
        int index = filePath.lastIndexOf('/');
        if (index < 0) return "";
        return filePath.substring(0, index);
    }

    public static String getFileName(String filePath) {
        int index = filePath.lastIndexOf('/');
        return (index < 0) ? filePath : filePath.substring(index + 1, filePath.length());
    }

    public static String getFileExt(String filePath) {
        int index = filePath.lastIndexOf('.');
        return (index < 0) ? filePath : filePath.substring(index + 1);
    }

    public static String getFileNameWithoutExt(String filePath) {
        int index = filePath.lastIndexOf('/');
        String name = filePath.substring(index + 1);
        index = name.lastIndexOf('.');
        if (index > 0) {
            name = name.substring(0, index);
        }
        return name;
    }

    //Rename the file path as "xxxxx(num).xxx".
    public static String getFileDuplicateName(String filePath) {
        String newPath = filePath;
        while (true) {
            File file2 = new File(newPath);
            if (file2.exists()) {
                String ext = newPath.substring(newPath.lastIndexOf('.'));
                newPath = newPath.substring(0, newPath.lastIndexOf('.'));
                int begin = 0;
                int end = newPath.length() - 1;
                if (newPath.charAt(end) == ')') {
                    for (int i = end - 1; i >= 0; i--) {
                        char c = newPath.charAt(i);
                        if (c == '(') {
                            begin = i;
                            break;
                        }
                        if (c < '0' || c > '9')
                            break;
                    }
                }
                if (begin > 0 && end - begin < 32) {
                    String num = newPath.substring(begin + 1, end);
                    int index = Integer.parseInt(num, 10) + 1;
                    newPath = newPath.substring(0, begin) + "(" + index + ")" + ext;
                    continue;
                }
                newPath = newPath + "(" + 1 + ")" + ext;
                continue;
            }
            break;
        }
        return newPath;
    }

    public static boolean isFileExist(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    public static boolean renameFile(String path, String newPath) {
        File file = new File(path);
        return file.renameTo(new File(newPath));
    }

    public static boolean deleteFile(String path) {
        File file = new File(path);
        return file.delete();
    }

    public static boolean deleteFolder(File dirPath, boolean deleteHistory) {
        boolean flag = false;
        if (!dirPath.isDirectory()) {
            return flag;
        }
        File[] fileList = dirPath.listFiles();
        if (fileList != null) {
            for (File file : fileList) {
                if (file.isFile()) {
                    flag = file.delete();
                } else if (file.isDirectory()) {
                    flag = deleteFolder(file, deleteHistory);
                }
                if (!flag) {
                    break;
                }
            }
        }
        flag = dirPath.delete();
        return flag;
    }
    public static AppFileUtil getInstance() {
        return INSTANCE;
    }

    private AppFileUtil() {
    }

    private static AppFileUtil INSTANCE = new AppFileUtil();

    public static String getDiskCachePath(Context context) {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            return context.getExternalCacheDir().getPath();
        } else {
            return context.getCacheDir().getPath();
        }
    }

    public static String getFileExtension(String filePath) {
        if (filePath == null) return "";
        int index = filePath.lastIndexOf(".");
        if (index != -1) {
            return filePath.substring(index + 1);
        } else {
            return "";
        }
    }

    public static String replaceFileExtension(String filePath, String ext) {
        if (AppUtil.isEmpty(filePath) || AppUtil.isEmpty(ext)) return "";
        int index = filePath.lastIndexOf(".");
        if (index != -1) {
            return filePath.substring(0, index) + ext;
        }
        return "";
    }
}
