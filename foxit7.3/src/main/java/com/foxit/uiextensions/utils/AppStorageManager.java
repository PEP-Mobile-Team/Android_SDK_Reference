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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build.VERSION;
import android.os.Environment;
import android.os.storage.StorageManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

public class AppStorageManager {
    private Context mContext;
    private Map<String, Boolean> mCacheMap;

    private static AppStorageManager mAppStorageManager = null;

    public static AppStorageManager getInstance(Context context) {
        if (mAppStorageManager == null) {
            mAppStorageManager = new AppStorageManager(context);
        }
        return mAppStorageManager;
    }

    public AppStorageManager(Context context) {
        mContext = context;
        if (VERSION.SDK_INT >= 19) {
            mCacheMap = new HashMap<String, Boolean>(5);
        }
    }


    public File getCacheDir() {
        return mContext.getCacheDir();
    }

    public boolean checkStorageCanWrite(String filePath) {
        if (VERSION.SDK_INT < 19) {
            return true;
        }
        if (filePath.startsWith(Environment.getExternalStorageDirectory().getPath())) {
            return true;
        }
        List<String> list = getVolumePaths();
        boolean result = false;
        for (String path : list) {
            if (filePath.startsWith(path)) {
                if (mCacheMap != null && mCacheMap.containsKey(path)) {
                    result = mCacheMap.get(path);
                    break;
                }
                File file = new File(path, ".foxit-" + UUID.randomUUID());
                if (file.exists()) {
                    file.delete();
                }
                result = file.mkdir();
                if (result) {
                    file.delete();
                }
                if (mCacheMap != null) {
                    mCacheMap.put(path, result);
                }
                break;
            }
        }
        return result;
    }

    @TargetApi(14)
    private List<String> getVolumePathsAboveVersion14() {
        List<String> result = null;
        try {
            StorageManager storageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
            Method getPathsMethod = storageManager.getClass().getMethod("getVolumePaths");
            Method getVolumeStateMethod = storageManager.getClass().getMethod("getVolumeState", String.class);
            String[] paths = (String[]) getPathsMethod.invoke(storageManager);
            result = new ArrayList<String>();
            for (String path : paths) {
                String state = (String) getVolumeStateMethod.invoke(storageManager, path);
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    result.add(path);
                }
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }

    public List<String> getVolumePaths() {
        if (VERSION.SDK_INT >= 14) {
            List<String> volumList = getVolumePathsAboveVersion14();
            if (volumList != null && volumList.size() > 0) {
                return volumList;
            }
        }
        List<String> volumList = new ArrayList<String>();
        String sdCard = Environment.getExternalStorageDirectory().getPath();
        if (sdCard != null) {
            volumList.add(sdCard);
        }
        File mountsFile = new File("/proc/mounts");
        if (mountsFile.exists()) {
            Scanner scanner;
            try {
                scanner = new Scanner(mountsFile);
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[1];
                        if (!volumList.contains(element)) {
                            File f = new File(element);
                            if (f.exists() && f.isDirectory()) {
                                volumList.add(element);
                            }
                        }
                    }
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        File voldFile = new File("/system/etc/vold.fstab");
        if (voldFile.exists()) {
            Scanner scanner;
            try {
                scanner = new Scanner(mountsFile, "UTF-8");
                while (scanner.hasNext()) {
                    String line = scanner.nextLine();
                    if (line.startsWith("/dev/block/vold/")) {
                        String[] lineElements = line.split(" ");
                        String element = lineElements[1];
                        if (!volumList.contains(element)) {
                            File f = new File(element);
                            if (f.exists() && f.isDirectory()) {
                                volumList.add(element);
                            }
                        }
                    }
                }
                scanner.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return volumList;
    }
}
