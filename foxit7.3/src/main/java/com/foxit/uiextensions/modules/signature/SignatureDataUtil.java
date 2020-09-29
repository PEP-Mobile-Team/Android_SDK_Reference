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
package com.foxit.uiextensions.modules.signature;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;

import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.AppSQLite.FieldInfo;
import com.foxit.uiextensions.utils.AppUtil;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class SignatureDataUtil {
    private static final String TABLE_MODEL = SignatureConstants.getModelTableName();
    private static final String NAME = "_sg_name";
    private static final String LEFT = "_sg_left";
    private static final String TOP = "_sg_top";
    private static final String RIGHT = "_sg_right";
    private static final String BOTTOM = "_sg_bottom";
    private static final String BLOB = "_sg_bolb";
    private static final String COLOR = "_color";
    private static final String DIAMETER = "_diamter";
    private static final String DSG_PATH = SignatureConstants.SG_DSG_PATH_FIELD;
    private static final String TABLE_RECENT = SignatureConstants.getRecentTableName();
    private static final String[] COL = {NAME, BLOB};

    private static boolean mInit = false;

    synchronized public static List<String> getModelKeys(Context context) {
        if (!checkInit(context)) return null;
        List<String> list = null;
        Cursor cursor = AppSQLite.getInstance(context).select(TABLE_MODEL, COL, null, null, null, null, "_id desc");
        if (cursor == null) return null;
        int count = cursor.getCount();
        if (count > 0) {
            list = new ArrayList<String>(count);
            while (cursor.moveToNext()) {
                list.add(cursor.getString(cursor.getColumnIndex(NAME)));
            }
        }
        cursor.close();
        return list;
    }


    synchronized public static Bitmap getScaleBmpByKey(Context context, String key, int w, int h) {
        if (!checkInit(context)) return null;
        Bitmap bitmap = null;
        Cursor cursor = AppSQLite.getInstance(context).select(TABLE_MODEL, null, NAME + "=?", new String[]{key}, null, null, null);
        if (cursor == null) return null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            byte[] in = cursor.getBlob(cursor.getColumnIndex(BLOB));
            Bitmap bmp = BitmapFactory.decodeByteArray(in, 0, in.length);
            bitmap = Bitmap.createScaledBitmap(bmp, w, h, true);
            bmp.recycle();
            bmp = null;
        }
        cursor.close();
        return bitmap;
    }

    synchronized public static HashMap<String, Object> getBitmapByKey(Context context, String key) {
        if (!checkInit(context)) return null;
        HashMap<String, Object> map = null;
        Cursor cursor = AppSQLite.getInstance(context).select(TABLE_MODEL, null, NAME + "=?", new String[]{key}, null, null, null);
        if (cursor == null) return null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            map = new HashMap<String, Object>();
            map.put("key", key);
            byte[] in = cursor.getBlob(cursor.getColumnIndex(BLOB));
            Bitmap bmp = BitmapFactory.decodeByteArray(in, 0, in.length);
            int color = cursor.getInt(cursor.getColumnIndex(COLOR));
            map.put("color", color);
            map.put("bitmap", bmp);
            float diameter = cursor.getFloat(cursor.getColumnIndex(DIAMETER));
            map.put("diameter", diameter);
            int l = cursor.getInt(cursor.getColumnIndex(LEFT));
            int t = cursor.getInt(cursor.getColumnIndex(TOP));
            int r = cursor.getInt(cursor.getColumnIndex(RIGHT));
            int b = cursor.getInt(cursor.getColumnIndex(BOTTOM));
            Rect rect = new Rect(l, t, r, b);
            map.put("rect", rect);
            if (cursor.getColumnIndex(DSG_PATH) != -1) {
                try {
                    map.put("dsgPath", cursor.getString(cursor.getColumnIndex(DSG_PATH)));
                } catch (Exception e) {
                    map.put("dsgPath", null);
                }
            } else {
                map.put("dsgPath", null);
            }
        }
        cursor.close();
        return map;
    }

    synchronized public static boolean insertData(Context context, Bitmap bmp, Rect rect, int color, float diameter, String dsgPath) {
        if (!checkInit(context)) return false;
        ContentValues values = new ContentValues();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
        String key = UUID.randomUUID().toString();
        values.put(NAME, key);
        values.put(DIAMETER, diameter);
        values.put(COLOR, color);
        values.put(LEFT, rect.left);
        values.put(TOP, rect.top);
        values.put(RIGHT, rect.right);
        values.put(BOTTOM, rect.bottom);
        values.put(BLOB, os.toByteArray());
        values.put(DSG_PATH, dsgPath);
        AppSQLite.getInstance(context).insert(TABLE_MODEL, values);
        insertRecent(context, key);
        return true;
    }

    synchronized public static boolean updateByKey(Context context, String key, Bitmap bmp, Rect rect, int color, float diameter, String dsgPath) {
        if (!checkInit(context)) return false;
        if (isExistKey(context, TABLE_MODEL, key)) {
            ContentValues values = new ContentValues();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            values.put(BLOB, os.toByteArray());
            values.put(LEFT, rect.left);
            values.put(TOP, rect.top);
            values.put(RIGHT, rect.right);
            values.put(BOTTOM, rect.bottom);
            values.put(DIAMETER, diameter);
            values.put(COLOR, color);
            values.put(DSG_PATH, dsgPath);
            AppSQLite.getInstance(context).update(TABLE_MODEL, values, NAME, new String[]{key});
            insertRecent(context, key);
        } else {
            insertData(context, bmp, rect, color, diameter, dsgPath);
        }
        return true;
    }

    synchronized public static boolean deleteByKey(Context context, String table, String key) {
        if (!checkInit(context)) return false;
        if (!table.equals(TABLE_RECENT) && isExistKey(context, TABLE_RECENT, key)) {
            deleteByKey(context, TABLE_RECENT, key);
        }
        AppSQLite.getInstance(context).delete(table, NAME, new String[]{key});
        return true;
    }


    synchronized public static boolean insertRecent(Context context, String key) {
        if (!checkInit(context)) return false;
        if (isExistKey(context, TABLE_RECENT, key)) {
            deleteByKey(context, TABLE_RECENT, key);
        }
        ContentValues values = new ContentValues();
        values.put(NAME, key);
        AppSQLite.getInstance(context).insert(TABLE_RECENT, values);
        return true;
    }

    synchronized public static List<String> getRecentKeys(Context context) {
        if (!checkInit(context)) return null;
        List<String> list = null;
        Cursor cursor = AppSQLite.getInstance(context).select(TABLE_RECENT, new String[]{NAME}, null, null, null, null, "_id desc");
        if (cursor == null) return null;
        if (cursor.getCount() > 0) {
            list = new ArrayList<String>();
            List<String> temp = new ArrayList<String>();
            int count = 0;
            while (cursor.moveToNext()) {
                String key = cursor.getString(cursor.getColumnIndex(NAME));
                if (isExistKey(context, TABLE_MODEL, key))
                    list.add(key);
                else
                    temp.add(key);
            }
            if (temp.size() > 0) {
                for (int i = 0; i < temp.size(); i++) {
                    deleteByKey(context, TABLE_RECENT, temp.get(i));
                }
            }
        }
        cursor.close();
        return list;
    }

    synchronized public static HashMap<String, Object> getRecentData(Context context) {
        if (!checkInit(context)) return null;
        HashMap<String, Object> map = null;
        List<String> list = getRecentKeys(context);
        if (list != null && list.size() > 0) {
            map = getBitmapByKey(context, list.get(0));
        }
        return map;
    }

    synchronized public static HashMap<String, Object> getRecentNormalSignData(Context context) {
        if (!checkInit(context)) return null;
        HashMap<String, Object> map = null;
        List<String> list = getRecentKeys(context);
        if (list != null && list.size() > 0) {
            for (String key : list) {
                map = getBitmapByKey(context, key);
                if (map != null && map.get("dsgPath") == null /*|| AppUtil.isEmpty((String) map.get("dsgPath"))*/) {
                    insertRecent(context, key);
                    return map;
                }
            }
        }
        return null;
    }

    synchronized public static HashMap<String, Object> getRecentDsgSignData(Context context) {
        if (!checkInit(context)) return null;
        HashMap<String, Object> map = null;
        List<String> list = getRecentKeys(context);
        if (list != null && list.size() > 0) {
            for (String key : list) {
                map = getBitmapByKey(context, key);
                if (map != null && map.get("dsgPath") != null && !AppUtil.isEmpty((String) map.get("dsgPath"))) {
                    return map;
                }
            }
        }
        return null;
    }

    synchronized private static boolean isExistKey(Context context, String table, String key) {
        return AppSQLite.getInstance(context).isRowExist(table, NAME, new String[]{key});
    }

    synchronized private static boolean checkInit(Context context) {
        if (!AppSQLite.getInstance(context).isDBOpened()) {
            AppSQLite.getInstance(context).openDB();
        }
        if (!mInit) {
            mInit = init(context);
        }
        return mInit;
    }

    private static boolean init(Context context) {
        return createModelTable(context) && createRecentTable(context);
    }

    private static boolean createModelTable(Context context) {
        ArrayList<FieldInfo> tableList = new ArrayList<FieldInfo>();
        tableList.add(new FieldInfo(NAME, "VARCHAR"));
        tableList.add(new FieldInfo(LEFT, "INTEGER"));
        tableList.add(new FieldInfo(TOP, "INTEGER"));
        tableList.add(new FieldInfo(RIGHT, "INTEGER"));
        tableList.add(new FieldInfo(BOTTOM, "INTEGER"));
        tableList.add(new FieldInfo(COLOR, "INTEGER"));
        tableList.add(new FieldInfo(DIAMETER, "FLOAT"));
        tableList.add(new FieldInfo(BLOB, "BLOB"));
        tableList.add(new FieldInfo(DSG_PATH, "VARCHAR"));
        return AppSQLite.getInstance(context).createTable(TABLE_MODEL, tableList);
    }

    private static boolean createRecentTable(Context context) {

        ArrayList<FieldInfo> tableList = new ArrayList<FieldInfo>();
        tableList.add(new FieldInfo(NAME, "VARCHAR"));
        return AppSQLite.getInstance(context).createTable(TABLE_RECENT, tableList);
    }
}
