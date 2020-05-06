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
package com.foxit.uiextensions.controls.menu;

/**
 * The unique identifier of More menu group and group item {@link IMenuView} {@link MoreMenuModule}
 */
public class MoreMenuConfig {
    /** more menu group type: file*/
    public static final int GROUP_FILE = 100;
    /** more menu group type: protect*/
    public static final int GROUP_PROTECT = 101;
    /** more menu group type: annotation*/
    public static final int GROUP_ANNOTATION = 102;
    /** more menu group type: form*/
    public static final int GROUP_FORM = 103;

    //Group_file
    /** The item of file group: document information */
    public static final int ITEM_DOCINFO = 0;
    /** The item of file group: reduce file size */
    public static final int ITEM_REDUCE_FILE_SIZE = 1;
    /** The item of file group: print file */
    public static final int ITEM_PRINT_FILE = 2;
    /** The item of file group: snapshot */
    public static final int ITEM_SNAPSHOT = 3;


    //Group_protect
    /** The item of protect group: password */
    public static final int ITEM_PASSWORD = 0;
//    /** The item of protect group: certificate */
//    public static final int ITEM_CETIFICATE = 1;
//    /** The item of protect group: AD_RMD */
//    public static final int ITEM_AD_RMD = 2;
//    /** The item of protect group: sign certify */
//    public static final int ITEM_SIGN_CERTIFY = 3;
    /** The item of protect group: remove security password */
    public static final int ITEM_REMOVESECURITY_PASSWORD = 4;
//    /** Group_Protect_Remove_Security_Pubkey */
//    public static final int ITEM_REMOVESECURITY_PUBKEY = 5;
//    /** Group_Protect_Romove_security_Rms */
//    public static final int ITEM_REMOVESECURITY_RMS = 6;
//    /** Group_Protect_CpdForm */
//    public static final int ITEM_CPDFDRM = 7;
//    /** Group_Protect_Remove_Security_CpdForm */
//    public static final int ITEM_REMOVESECURITY_CPDFDRM = 8;

    //Group_form
    public static final int ITEM_CREATE_FORM = 0;
    /** The item of form group: reset form */
    public static final int ITEM_RESET_FORM = 1;
    /** The item of form group: import form */
    public static final int ITEM_IMPORT_FORM = 2;
    /** The item of form group: export form */
    public static final int ITEM_EXPORT_FORM = 3;

    // Group annotation
    /** The item of form group: import annotation */
    public static final int ITEM_ANNOTATION_IMPORT = 0;
    /** The item of form group: export annotation */
    public static final int ITEM_ANNOTATION_EXPORT = 1;
}
