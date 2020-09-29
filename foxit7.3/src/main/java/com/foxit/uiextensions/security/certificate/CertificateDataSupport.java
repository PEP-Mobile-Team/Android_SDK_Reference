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
package com.foxit.uiextensions.security.certificate;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.SparseArray;

import com.foxit.uiextensions.utils.AppSQLite;
import com.foxit.uiextensions.utils.AppSQLite.FieldInfo;

import java.util.ArrayList;
import java.util.List;

public class CertificateDataSupport {

    private static final String DB_TABLE_TRUST_CERT = "_trust_cert";
    private static final String DB_TABLE_CERT = "_cert";
    private static final String DB_TABLE_PFX = "_pfx";

    private static final String PUBLISHER = "publisher";
    private static final String ISSUER = "issuer";
    private static final String SERIALNUMBER = "serial_number";
    private static final String FILEPATH = "file_path";
    private static final String FILENAME = "file_name";
    private static final String PASSWORD = "password";
    public static final int FULLPERMCODE = 0xf3c;
    private static final String VALIDFROM = "valid_from";
    private static final String VALIDTO = "valid_to";
    private static final String EMAILADDRESS = "email_address";
    private static final String SUBJECT = "subject";
    private static final String KEYUSAGE = "keyUsage";
    private static final String CERTNAME = "cert_name";

    private Context mContext;

    public CertificateDataSupport(Context context) {
        mContext = context;
        init();
    }

    private void init() {
        if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_CERT)) {
            ArrayList<AppSQLite.FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
            fieldInfos.add(new FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
            AppSQLite.getInstance(mContext).createTable(DB_TABLE_CERT, fieldInfos);
        }

        if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_PFX)) {
            ArrayList<AppSQLite.FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
            fieldInfos.add(new FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(PASSWORD, AppSQLite.KEY_TYPE_VARCHAR));
            AppSQLite.getInstance(mContext).createTable(DB_TABLE_PFX, fieldInfos);
        }

        if (!AppSQLite.getInstance(mContext).isTableExist(DB_TABLE_TRUST_CERT)) {
            ArrayList<AppSQLite.FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
            fieldInfos.add(new FieldInfo(SERIALNUMBER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(ISSUER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(PUBLISHER, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILEPATH, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(FILENAME, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(PASSWORD, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(KEYUSAGE, AppSQLite.KEY_TYPE_INT));
            fieldInfos.add(new FieldInfo(VALIDFROM, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(VALIDTO, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(EMAILADDRESS, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(SUBJECT, AppSQLite.KEY_TYPE_VARCHAR));
            fieldInfos.add(new FieldInfo(CERTNAME, AppSQLite.KEY_TYPE_VARCHAR));
            AppSQLite.getInstance(mContext).createTable(DB_TABLE_TRUST_CERT, fieldInfos);
        }
    }

    public boolean insertCert(String issuer, String publisher, String serialNumber, String path, String fileName) {
        ContentValues values = new ContentValues();
        values.put(ISSUER, issuer);
        values.put(PUBLISHER, publisher);
        values.put(SERIALNUMBER, serialNumber);
        values.put(FILEPATH, path);
        values.put(FILENAME, fileName);
        AppSQLite.getInstance(mContext).insert(DB_TABLE_CERT, values);
        return true;
    }

    public boolean removeCert(String filePath) {
        String selection = FILEPATH;
        AppSQLite.getInstance(mContext).delete(DB_TABLE_CERT, selection, new String[]{filePath});
        return true;
    }

    public boolean insertPfx(String issuer, String publisher, String serialNumber, String path, String fileName, String password) {
        ContentValues values = new ContentValues();
        values.put(ISSUER, issuer);
        values.put(PUBLISHER, publisher);
        values.put(SERIALNUMBER, serialNumber);
        values.put(FILEPATH, path);
        values.put(FILENAME, fileName);
        values.put(PASSWORD, password);
        AppSQLite.getInstance(mContext).insert(DB_TABLE_PFX, values);
        return true;
    }

    public boolean removePfx(String filePath) {
        String selection = FILEPATH;
        AppSQLite.getInstance(mContext).delete(DB_TABLE_PFX, selection, new String[]{filePath});
        return true;
    }

    /*
     * if don't find, will be return null
     * 0 represent path
     * 1 represent fileName
     * 2 represent password
     *
     */
    public SparseArray<String> getPfx(String issuer, String serialNumber) {
        SparseArray<String> sa = null;
        if (issuer == null || serialNumber == null) return sa;
        String where = ISSUER + "=? AND " + SERIALNUMBER + "=?";
        String[] whereValue = {issuer, serialNumber};
        Cursor cursor = AppSQLite.getInstance(mContext).getSQLiteDatabase().query(
                DB_TABLE_PFX,
                new String[]{FILEPATH, FILENAME, PASSWORD},
                where,
                whereValue, null, null, null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                sa = new SparseArray<String>();
                sa.put(0, cursor.getString(cursor.getColumnIndex(FILEPATH)));
                sa.put(1, cursor.getString(cursor.getColumnIndex(FILENAME)));
                sa.put(2, cursor.getString(cursor.getColumnIndex(PASSWORD)));
            }
            cursor.close();
        }
        return sa;
    }

    public void setPfxPassword(String issuer, String serialNumber, String password) {
        ContentValues values = new ContentValues();
        values.put(PASSWORD, password);
        String where = ISSUER + "=? AND " + SERIALNUMBER + "=?";
        String[] whereValue = {issuer, serialNumber};
        AppSQLite.getInstance(mContext).getSQLiteDatabase().update(DB_TABLE_PFX, values, where, whereValue);
    }

    public void getAllPfxs(List<CertificateFileInfo> infos) {
        Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_PFX, null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                CertificateFileInfo info = new CertificateFileInfo();
                info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                info.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                info.isCertFile = false;
                info.permCode = FULLPERMCODE;
                infos.add(info);
            }
            cursor.close();
        }
    }

    public void getAllCerts(List<CertificateFileInfo> infos) {
        Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_CERT, null, null, null, null, null, null);
        if (cursor != null) {
            while (cursor.moveToNext()) {
                CertificateFileInfo info = new CertificateFileInfo();
                info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                info.isCertFile = true;
                info.permCode = FULLPERMCODE;
                infos.add(info);
            }
            cursor.close();
        }
    }

    public boolean insertTrustCert(CertificateFileInfo certificateFileInfo) {
        ContentValues values = new ContentValues();
        values.put(ISSUER, certificateFileInfo.issuer);
        values.put(PUBLISHER, certificateFileInfo.publisher);
        values.put(SERIALNUMBER, certificateFileInfo.serialNumber);
        values.put(FILEPATH, certificateFileInfo.filePath);
        values.put(FILENAME, certificateFileInfo.fileName);
        values.put(KEYUSAGE, certificateFileInfo.keyUsage);
        values.put(VALIDFROM, certificateFileInfo.validFrom);
        values.put(VALIDTO, certificateFileInfo.validTo);
        values.put(EMAILADDRESS, certificateFileInfo.emailAddress);
        values.put(PASSWORD, certificateFileInfo.password);
        values.put(SUBJECT, certificateFileInfo.subject);
        values.put(CERTNAME, certificateFileInfo.certName);
        AppSQLite.getInstance(mContext).insert(DB_TABLE_TRUST_CERT, values);
        return true;
    }

    public boolean removeTrustCert(String serialNumber) {
        String selection = SERIALNUMBER;
        AppSQLite.getInstance(mContext).delete(DB_TABLE_TRUST_CERT, selection, new String[]{serialNumber});
        return true;
    }

    public List<CertificateFileInfo> getTrustCertList() {
        Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_TRUST_CERT, null, null, null, null, null, null);
        List<CertificateFileInfo> infos = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                CertificateFileInfo info = new CertificateFileInfo();
                info.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                info.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                info.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                info.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                info.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                info.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                info.keyUsage = cursor.getInt(cursor.getColumnIndex(KEYUSAGE));
                info.validFrom = cursor.getString(cursor.getColumnIndex(VALIDFROM));
                info.validTo = cursor.getString(cursor.getColumnIndex(VALIDTO));
                info.emailAddress = cursor.getString(cursor.getColumnIndex(EMAILADDRESS));
                info.subject = cursor.getString(cursor.getColumnIndex(SUBJECT));
                info.certName = cursor.getString(cursor.getColumnIndex(CERTNAME));
                infos.add(info);
            }
            cursor.close();
        }
        return infos;
    }

    public CertificateFileInfo queryTrustCert(String serialNumber) {
        Cursor cursor = AppSQLite.getInstance(mContext).select(DB_TABLE_TRUST_CERT, null, SERIALNUMBER + " = ?", new String[]{serialNumber}, null, null, null);
        CertificateFileInfo certInfo = null;
        if (cursor != null) {
            while (cursor.moveToNext()) {
                certInfo = new CertificateFileInfo();
                certInfo.serialNumber = cursor.getString(cursor.getColumnIndex(SERIALNUMBER));
                certInfo.issuer = cursor.getString(cursor.getColumnIndex(ISSUER));
                certInfo.publisher = cursor.getString(cursor.getColumnIndex(PUBLISHER));
                certInfo.filePath = cursor.getString(cursor.getColumnIndex(FILEPATH));
                certInfo.fileName = cursor.getString(cursor.getColumnIndex(FILENAME));
                certInfo.password = cursor.getString(cursor.getColumnIndex(PASSWORD));
                certInfo.keyUsage = cursor.getInt(cursor.getColumnIndex(KEYUSAGE));
                certInfo.validFrom = cursor.getString(cursor.getColumnIndex(VALIDFROM));
                certInfo.validTo = cursor.getString(cursor.getColumnIndex(VALIDTO));
                certInfo.emailAddress = cursor.getString(cursor.getColumnIndex(EMAILADDRESS));
                certInfo.subject = cursor.getString(cursor.getColumnIndex(SUBJECT));
                certInfo.certName = cursor.getString(cursor.getColumnIndex(CERTNAME));
            }
            cursor.close();
        }
        return certInfo;
    }

}
