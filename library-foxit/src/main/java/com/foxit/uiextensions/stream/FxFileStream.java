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
package com.foxit.uiextensions.stream;

import com.foxit.sdk.common.fxcrt.FileReaderCallback;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class FxFileStream extends FileReaderCallback {

    private RandomAccessFile mRaFile;
    private long mFileSize;
    public FxFileStream(String path, long fileSize) {
        try {
            mRaFile = new RandomAccessFile(path, "r");
            mFileSize = fileSize;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void release() {
        if (mRaFile != null) {
            try {
                mRaFile.close();
                mRaFile = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean readBlock(byte[] buffer, int offset, long size) {
        if (offset < 0 || offset >= mFileSize || size <= 0
                || size > mFileSize || offset + size > mFileSize
                || mRaFile == null) return false;
        try {
            mRaFile.seek(offset);
            mRaFile.read(buffer, 0, (int) size);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public int getSize() {
        return (int) mFileSize;
    }
}
