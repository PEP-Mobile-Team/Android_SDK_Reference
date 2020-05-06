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
package com.foxit.uiextensions.modules.dynamicxfa;

import com.foxit.sdk.addon.xfa.XFAWidget;

/**
 * Interface that defines xfa widget events.
 */
public interface IXFAWidgetEventListener {
    /**
     * Called when the specified xfa widget has been added.
     * @param xfaWidget the xfa widget was added
     */
    void onXFAWidgetAdded(XFAWidget xfaWidget);

    /**
     * Called when the specified xfa widget will be removed.
     * @param xfaWidget the xfa widget will be removed
     */
    void onXFAWidgetWillRemove(XFAWidget xfaWidget);
}
