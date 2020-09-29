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

/**
 * Class for custom event.
 */
public class Event {

    /// @cond DEV
    /**
     * The event type.
     */
    public int mType;

    /// @endcond

    public Event() {
    }

    public Event(int type) {
        mType = type;
    }

    /**
     * Interface definition for a callback to allow the user to run some code.
     */
    public interface Callback {
        /**
         * The callback used to allow the user to run some code.
         * @param event The current event.
         * @param success true means the operation or task is success.
         */
        void result(Event event, boolean success);
    }
}
