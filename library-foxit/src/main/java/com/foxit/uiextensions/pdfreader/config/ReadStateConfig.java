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
package com.foxit.uiextensions.pdfreader.config;

/**
 * The read state config
 */
public class ReadStateConfig {
    /**
     * Read State: normal view
     */
    public static final int STATE_NORMAL = 1;
    /**
     * Read State: reflow
     */
    public static final int STATE_REFLOW = 2;
    /**
     * Read State: search
     */
    public static final int STATE_SEARCH = 3;
    /**
     * Read State: edit
     */
    public static final int STATE_EDIT = 4;
    /**
     * Read State: signature
     */
    public static final int STATE_SIGNATURE = 5;
    /**
     * Read State: annotation tool
     */
    public static final int STATE_ANNOTTOOL = 6;
    /**
     * Read State: pan zoom
     */
    public static final int STATE_PANZOOM = 7;
    /**
     * Read State: create form field
     */
    public static final int STATE_CREATE_FORM = 8;

    /**
     * Read State: page navigation
     */
    public static final int STATE_PAGENAVIGATION = 9;

    /**
     * Read State: compare
     */
    public static final int STATE_COMPARE = 10;

}
