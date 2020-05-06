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
package com.foxit.uiextensions.pdfreader;

import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;

/**
 * Interface definition for a callback to be invoked when the read state changed.
 */
public interface IStateChangeListener {
    /**
     * called when the read state has changed.
     * @param oldState should be one of {@link ReadStateConfig#STATE_NORMAL},
     *      {@link ReadStateConfig#STATE_REFLOW}, {@link ReadStateConfig#STATE_SEARCH}, {@link ReadStateConfig#STATE_EDIT},
     *      {@link ReadStateConfig#STATE_SIGNATURE}, {@link ReadStateConfig#STATE_ANNOTTOOL}, {@link ReadStateConfig#STATE_PANZOOM}
     *      {@link ReadStateConfig#STATE_CREATE_FORM}
     * @param newState should be one of {@link ReadStateConfig#STATE_NORMAL},
     *      {@link ReadStateConfig#STATE_REFLOW}, {@link ReadStateConfig#STATE_SEARCH}, {@link ReadStateConfig#STATE_EDIT},
     *      {@link ReadStateConfig#STATE_SIGNATURE}, {@link ReadStateConfig#STATE_ANNOTTOOL}, {@link ReadStateConfig#STATE_PANZOOM}
     *      {@link ReadStateConfig#STATE_CREATE_FORM}
     */
    void onStateChanged(int oldState, int newState);
}
