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
package com.foxit.uiextensions.modules.print;


/**
 * Interface that defines information for print events.
 */
public interface IPrintResultCallback {

    /**
     * Called when finished printing
     */
    void printFinished();

    /**
     * Called when failed to print.
     */
    void printFailed();

    /**
     * Called when cancelled to print.
     */
    void printCancelled();
}
