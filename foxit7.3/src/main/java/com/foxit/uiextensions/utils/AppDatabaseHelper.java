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

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.foxit.uiextensions.modules.signature.SignatureConstants;


class AppDatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "Foxit_Rdk.db";

    private static final int DATABASE_VERSION = 3;

    public AppDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //	Called when the database is created for the first time
    @Override
    public void onCreate(SQLiteDatabase db) {
    }

    //	Called when the database needs to be upgraded
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.beginTransaction();

        // add sg dsgPath field
        if (tableIsExist(db, SignatureConstants.getModelTableName())) {
            if (!isExistColumn(db, SignatureConstants.getModelTableName(), SignatureConstants.SG_DSG_PATH_FIELD)) {
                db.execSQL("ALTER TABLE " + SignatureConstants.getModelTableName() + " ADD COLUMN " + SignatureConstants.SG_DSG_PATH_FIELD + " VARCHAR");
            }
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    /**
     * is the table exist
     */
    synchronized public boolean tableIsExist(SQLiteDatabase db, String tableName) {
        if (db == null) return false;

        boolean result = false;
        if (tableName == null) {
            return false;
        }
        Cursor cursor = null;
        try {
            String sql = "select count(*) as CNT from sqlite_master where type ='table' and name ='" + tableName.trim() + "'";
            cursor = db.rawQuery(sql, null);
            if (cursor.moveToNext()) {
                int count = cursor.getInt(0);
                if (count > 0) {
                    result = true;
                }
            }
            if (cursor != null)
                cursor.close();
        } catch (Exception e) {
        }
        return result;
    }

    //is the column exist
    synchronized public boolean isExistColumn(SQLiteDatabase db, String tableName, String columnName) {
        if (db == null) return false;

        boolean isExist = true;
        String sql = "select * from " + tableName;
        Cursor cursor = db.rawQuery(sql, null);
        int columnId = cursor.getColumnIndex(columnName);
        if (columnId == -1) {
            isExist = false;
        }

        cursor.close();
        return isExist;
    }
}
