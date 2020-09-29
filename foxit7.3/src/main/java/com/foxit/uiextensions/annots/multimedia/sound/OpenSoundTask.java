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
package com.foxit.uiextensions.annots.multimedia.sound;


import com.foxit.sdk.PDFException;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.fxcrt.FileReaderCallback;
import com.foxit.sdk.pdf.FileSpec;
import com.foxit.sdk.pdf.annots.Annot;
import com.foxit.sdk.pdf.annots.Sound;
import com.foxit.sdk.pdf.objects.PDFStream;
import com.foxit.uiextensions.utils.Event;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

public class OpenSoundTask extends Task {

    private Annot mAnnot;
    private String mSavePath;
    private boolean mRet = false;

    public OpenSoundTask(Annot annot, String savePath, final Event.Callback callback) {
        super(new CallBack() {
            @Override
            public void result(Task task) {
                callback.result(null, ((OpenSoundTask) task).mRet);
            }
        });

        mSavePath = savePath;
        mAnnot = annot;
    }

    @Override
    protected void execute() {
        try {
            if (mAnnot == null || mAnnot.isEmpty() || mAnnot.getType() != Annot.e_Sound) {
                mRet = false;
                return;
            }

            Sound sound = new Sound(mAnnot);
            FileSpec fileSpec = sound.getFileSpec();
            if (fileSpec == null || fileSpec.isEmpty()) {
                mRet = saveSoundToFile(sound, mSavePath);
            } else {
                FileReaderCallback fileRead = fileSpec.getFileData();
                if (fileRead == null || fileRead.getSize() == 0) {
                    mRet = false;
                    return;
                }

                FileOutputStream fileOutputStream = new FileOutputStream(mSavePath);
                BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
                int offset = 0;
                int bufSize = 4 * 1024;
                long fileSize = fileRead.getSize();
                byte[] buf;
                while (true) {
                    if (fileSize < bufSize + offset) {
                        buf = new byte[(int) (fileSize - offset)];
                        fileRead.readBlock(buf, offset, fileSize - offset);
                    } else {
                        buf = new byte[bufSize];
                        fileRead.readBlock(buf, offset, bufSize);
                    }
                    if (buf.length != bufSize) {
                        bufferedOutputStream.write(buf, 0, buf.length);
                        break;
                    } else {
                        bufferedOutputStream.write(buf, 0, bufSize);
                    }
                    offset += bufSize;
                }
                bufferedOutputStream.flush();

                bufferedOutputStream.close();
                fileOutputStream.close();
                mRet = true;
            }
        } catch (PDFException e) {
            e.printStackTrace();
            mRet = false;
        } catch (Exception e) {
            e.printStackTrace();
            mRet = false;
        }
    }

    private boolean saveSoundToFile(Sound sound, String path) {
        try {
            RandomAccessFile randomAccessFile = new RandomAccessFile(path, "rw");
            PDFStream soundStream = sound.getSoundStream();
            int streamSize = soundStream.getDataSize(false);
            byte[] data = new byte[streamSize];
            soundStream.getData(false, streamSize, data);

            short bit = (short) sound.getBits();

            int riffSize = streamSize + 36;
            randomAccessFile.write("RIFF".getBytes());
            randomAccessFile.write(intToByteArray(riffSize));
            randomAccessFile.write("WAVEfmt ".getBytes());

            int chunkSize = 16;
            randomAccessFile.write(intToByteArray(chunkSize));

            short format = 1;
            randomAccessFile.write(shortToByteArray(format));

            int channelCount = sound.getChannelCount();
            randomAccessFile.write(shortToByteArray((short) channelCount));

            int rate = (int) sound.getSamplingRate();
            randomAccessFile.write(intToByteArray(rate));

            int bytePerSec = rate * channelCount * bit / 8;
            randomAccessFile.write(intToByteArray(bytePerSec));

            short blockAlign = (short) (bit * channelCount / 8);
            randomAccessFile.write(shortToByteArray(blockAlign));
            randomAccessFile.write(shortToByteArray(bit));
            randomAccessFile.write("data".getBytes());
            randomAccessFile.write(intToByteArray(streamSize));

            boolean ret = false;
            int encodingFormat = sound.getSampleEncodingFormat();
            switch (encodingFormat) {
                case Sound.e_SampleEncodingFormatALaw:
                    break;
                case Sound.e_SampleEncodingFormatMuLaw:
                    break;
                case Sound.e_SampleEncodingFormatSigned:
                    byte[] buffer = new byte[streamSize + 1];
                    int j = 0, k = 0;
                    for (int i = 0; i < streamSize; i += 2) {
                        byte low = data[j++];
                        byte high;
                        if (j == streamSize) {
                            high = 0;
                        } else {
                            high = data[j++];
                        }

                        buffer[k++] = high;
                        buffer[k++] = low;
                    }
                    randomAccessFile.write(buffer, 0, streamSize);
                    ret = true;
                    break;
                case Sound.e_SampleEncodingFormatRaw:
                default:
                    randomAccessFile.write(data, 0, streamSize);
                    ret = true;
                    break;
            }

            randomAccessFile.close();
            return ret;
        } catch (PDFException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF)
        };
    }

    private static byte[] shortToByteArray(short value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF)
        };
    }

}
