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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

public class AppSQLite {

	public static class FieldInfo {
		private String fieldName;
		private String fieldType;

		public FieldInfo(String name, String value) {
			fieldName = name;
			fieldType = value;
		}

		/**
		 * get the fieldName
		 */
		public String getFieldName() {
			return fieldName;
		}

		/**
		 * set the fieldName
		 */
		public void setFieldName(String fieldName) {
			this.fieldName = fieldName;
		}

		/**
		 * get the fieldType
		 */
		public String getFieldType() {
			return fieldType;
		}

		/**
		 * set the fieldType
		 */
		public void setFieldType(String fieldType) {
			this.fieldType = fieldType;
		}
	}

	//support fields
	public static final String		KEY_ID		   		= "_id";//column _id----must needed
	public static final String 		KEY_TYPE_INT     	= "INTEGER";
	public static final String 		KEY_TYPE_VARCHAR 	= "VARCHAR";
	public static final String 		KEY_TYPE_DOUBLE  	= "DOUBLE";
	public static final String 		KEY_TYPE_DATE    	= "DATE";
	public static final String 		KEY_TYPE_FLOAT    	= "FLOAT";
	public static final String 		KEY_TYPE_BLOB   	= "BLOB";

	private int						mRefCount 			= 0;
	private Context					mContext		    = null;
	private SQLiteDatabase			mSQLiteDatabase		= null;
	private AppDatabaseHelper mDatabaseHelper		= null;

	private static AppSQLite mAppSQLite = null;

	public static synchronized AppSQLite getInstance(Context context) {
		if (mAppSQLite == null) {
			mAppSQLite = new AppSQLite(context);
		}
		return mAppSQLite;
	}

	public  AppSQLite(Context context) {
		super();
		mContext = context;
		mDatabaseHelper = new AppDatabaseHelper(mContext);
	}

	synchronized public void openDB() throws SQLException {
		if (mSQLiteDatabase == null) {
			mSQLiteDatabase = mDatabaseHelper.getWritableDatabase();
		}
		mRefCount ++;
	}

	synchronized public void closeDB() {
		if (mSQLiteDatabase != null) {
			mRefCount --;
			if (mRefCount == 0) {
				mDatabaseHelper.close();
				mSQLiteDatabase = null;
			}
		}
	}

	synchronized public boolean isDBOpened() {
		return mSQLiteDatabase != null;
	}
	
	synchronized public SQLiteDatabase getSQLiteDatabase() {
		if (mSQLiteDatabase == null || !mSQLiteDatabase.isOpen()) {
			openDB();
		}
		return mSQLiteDatabase;
	}

	//create any table you wanted
	synchronized public boolean createTable(String tableName, ArrayList<FieldInfo> fieldInfo) {
		if (mSQLiteDatabase == null) return false;

		int count = fieldInfo.size();
		StringBuffer buffer = new StringBuffer();
		String resultString;
		for (int i = 0; i < count; i++) {
			String name = fieldInfo.get(i).getFieldName();
			String type = fieldInfo.get(i).getFieldType();

			if (i != count - 1) {
				resultString = name + " " + type + ",";
			} else {
				resultString = name + " " + type;
			}
			buffer.append(resultString);
		}
		String sql = "CREATE TABLE  IF NOT EXISTS " + tableName + "(_id INTEGER PRIMARY KEY," + buffer + ")";
		mSQLiteDatabase.execSQL(sql);

		return true;
	}

	/** 
	 * is the table exist
	 */  
	synchronized public boolean isTableExist(String tableName) {
		if (mSQLiteDatabase == null) return false;

		boolean result = false;  
		if (tableName == null) {
			return false;  
		}  
		Cursor cursor = null;  
		try {
			String sql = "select count(*) as CNT from sqlite_master where type ='table' and name ='" + tableName.trim() + "'";  
			cursor = mSQLiteDatabase.rawQuery(sql, null);  
			if (cursor.moveToNext()) {  
				int count = cursor.getInt(0);  
				if (count > 0) {
					cursor.close();
					result = true;  
				}  
			}  
		} catch (Exception e) {

		}
		if (cursor != null){
			cursor.close();
		}
		return result;
	}

	//is the row data exist
	synchronized public boolean isRowExist(String tableName, String fieldName, String[] matchValues) {
		if (mSQLiteDatabase == null) return false;

		boolean isExist = true;
		int count = 0;
		String sql = "select " + fieldName + " from "
				+ tableName + " where " + fieldName + " in(?)";
		Cursor cursor = mSQLiteDatabase.rawQuery(sql, matchValues);
		count = cursor.getCount();
		if (count == 0) {
			isExist = false;
		}
		cursor.close();
		return isExist;
	}
	
	//insert a data you needed
	synchronized public void insert(String tableName, ContentValues values) {
		if (mSQLiteDatabase == null) return;

		mSQLiteDatabase.insert(tableName, KEY_ID, values);
	}
	
	//replace a data you needed
	synchronized public void replace(String tableName, ContentValues values) {
		if (mSQLiteDatabase == null) return;
		mSQLiteDatabase.replace(tableName, KEY_ID, values);
	}

	//delete a data
	synchronized public void delete(String tableName, String columnName, String[] matchValues) {
		if (mSQLiteDatabase == null) return;
		
		mSQLiteDatabase.delete(tableName, columnName + " = ?", matchValues);
	}
	
	//delete multiple data
	synchronized public void delete(String tableName, ContentValues values) {
		if (mSQLiteDatabase == null) return;

		Set<Map.Entry<String, Object>> keys = values.valueSet();
		for (Map.Entry<String, Object> entry : keys) {
			mSQLiteDatabase.delete(tableName, entry.getKey() + "= ?", new String[]{ values.getAsString(entry.getKey()) });
		}
	}
	
	//select data
	synchronized public Cursor select(String tableName, String[] columns, String selection, 
			String[] selectionArgs, String groupBy, String having, String orderBy){
		if (mSQLiteDatabase == null) return null;

		Cursor cursor = null;
		cursor = mSQLiteDatabase.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy);

		return cursor;
	}

	synchronized public void update(String tableName, ContentValues values, String fieldName, String[] matchValues) {
		if (mSQLiteDatabase == null) return;

		mSQLiteDatabase.update(
				tableName, 
				values, 
				fieldName + " = ? ", 
				matchValues);
	}
}
