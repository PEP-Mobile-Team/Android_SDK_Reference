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
package com.foxit.uiextensions.controls.menu;

/**
 * Interface definition for a callback to be invoked when a menu view is clicked.
 */
public interface MenuViewCallback {
    /**
     * Called while a menu item has been clicked.
     * @param item the menu item was clicked.
     */
    void onClick(MenuItem item);
}
