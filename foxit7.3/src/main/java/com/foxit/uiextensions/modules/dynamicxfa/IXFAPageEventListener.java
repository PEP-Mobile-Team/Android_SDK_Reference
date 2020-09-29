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
package com.foxit.uiextensions.modules.dynamicxfa;


/**
 * Interface that defines xfa page events.
 */
public interface IXFAPageEventListener {

    /**
     * Called when the specified xfa page has been inserted.
     * @param success true means success.
     * @param pageIndex The specified page index to be inserted.
     */
    void onPagesInserted(boolean success, int pageIndex);

    /**
     * Called when the specified xfa page has been removed.
     * @param success true means success.
     * @param pageIndex The specified page index to be removed.
     */
    void onPagesRemoved(boolean success, int pageIndex);
}
