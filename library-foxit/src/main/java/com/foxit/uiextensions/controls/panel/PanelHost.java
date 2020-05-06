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
package com.foxit.uiextensions.controls.panel;

import android.view.View;
import android.view.ViewGroup;

/**
 * The interface that defines information for Panel container,
 * thc container include topbar,tabbar and panel content.
 * <p>
 * Through this interface you can add/remove/get/set panel via {@link PanelSpec}
 */
public interface PanelHost {
    /**
     * Interface used to allow the creator of a PanelHost to run some code when the
     * specified view is closed.
     */
    interface ICloseDefaultPanelCallback{
        /**
         * This method will be invoked when the specified view is closed.
         * @param v The specified view.
         */
        void closeDefaultPanel(View v);
    }

    /**
     * Retrieve the {@link View} attached to this PanelHost, if present.
     *
     * @return The View attached to the dialog or null if no View is present.
     */
    ViewGroup getContentView();

    /**
     * Add a specified {@link PanelSpec} to the PanelHost.
     * @param spec The specified PanelSpec.
     */
    void addSpec(PanelSpec spec);

    /**
     * Remove the specified {@link PanelSpec} from the PanelHost.
     * @param spec The specified PanelSpec.
     */
    void removeSpec(PanelSpec spec);

    /**
     * Set a {@link PanelSpec} by the specified {@link com.foxit.uiextensions.controls.panel.PanelSpec.PanelType}
     * as the current PanelSpec.
     * @param panelType The specified PanelType.
     */
    void setCurrentSpec(com.foxit.uiextensions.controls.panel.PanelSpec.PanelType panelType);

    /**
     * Get the current {@link PanelSpec}.
     * @return The current {@link PanelSpec}.
     */
    PanelSpec getCurrentSpec();

    /**
     * Get the {@link PanelSpec} by the specified {@link com.foxit.uiextensions.controls.panel.PanelSpec.PanelType}
     * @param panelType he specified PanelType.
     * @return The {@link PanelSpec} associated with the {@link com.foxit.uiextensions.controls.panel.PanelSpec.PanelType}.
     */
    PanelSpec getSpec(com.foxit.uiextensions.controls.panel.PanelSpec.PanelType panelType);
}
