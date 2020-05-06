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
package com.foxit.uiextensions.security.certificate;

import android.content.Context;
import android.os.Handler;
import android.os.Message;


import com.foxit.uiextensions.utils.AppStorageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class CertificateSearchRunnable implements Runnable {
	
	private boolean				mShouldStop;
	private boolean				mbOnlyPfx;
	private Handler				mHandler;
	private Context mContext;
	public CertificateSearchRunnable(Context context) {
		mContext = context;
	}

	public void init(Handler handler, boolean isOnlyPfxFile) {
		mHandler = handler;
		mShouldStop = false;
		mbOnlyPfx = isOnlyPfxFile;
	}

	public void stopSearch() {
		mShouldStop = true;
	}

	public boolean isStoped() {
		return mShouldStop;
	}

	private  boolean  filterFile(String filename) {
		if (filename.toLowerCase().endsWith(".pfx")) {
			return true;
		}
        if (filename.toLowerCase().endsWith(".p12")) {
            return true;
        }
		if (!mbOnlyPfx && filename.toLowerCase().endsWith(".cer")) {
			return true;
		}
		
		return false;
	}
	
	@Override
	public void run() {
		if (mHandler == null) return ;
		List<File> folderList = new ArrayList<File>();
		List<String> listFiles = AppStorageManager.getInstance(mContext).getVolumePaths();
		if (listFiles != null) {
			for (int i = 0 ; i < listFiles.size(); i ++) {
				if (mShouldStop) {
					return ;
				}
				File file = new File(listFiles.get(i));
				if (file.isDirectory() && !file.isHidden()) {
					folderList.add(file);
				} else if (file.isFile() && !file.isHidden()) {
					if (mShouldStop) {
						return ;
					}
					if (!filterFile(file.getName())) {
						continue;
					}
					Message msg = mHandler.obtainMessage(CertificateFragment.MESSAGE_UPDATE, file);
					msg.sendToTarget();
				}
			}
		}

		while (folderList.size() > 0) {
			if (mShouldStop) {
				return ;
			}
			File tempFile = (File) folderList.remove(0);
			File[] tempFolderFileList = tempFile.listFiles();
			if (tempFolderFileList != null) {
				for (int i = 0; i < tempFolderFileList.length; i ++) {
					if (mShouldStop) {
						return ;
					}
					if (tempFolderFileList[i].isDirectory() && !tempFolderFileList[i].isHidden()) {
						folderList.add(tempFolderFileList[i]);
					} else if (tempFolderFileList[i].isFile() && !tempFolderFileList[i].isHidden() ) {
						if (mShouldStop) {
							return ;
						}
						if (!filterFile(tempFolderFileList[i].getName())) {
							continue;
						}
						Message msg = mHandler.obtainMessage(CertificateViewSupport.MESSAGE_UPDATE, tempFolderFileList[i]);
						msg.sendToTarget();
					}
				}
			}
		}
		Message msg = mHandler.obtainMessage(CertificateViewSupport.MESSAGE_FINISH);
		msg.sendToTarget();
	}

}
