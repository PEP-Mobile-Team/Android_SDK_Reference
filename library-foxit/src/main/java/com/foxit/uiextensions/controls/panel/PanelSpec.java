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

import com.foxit.uiextensions.Module;

/**
 * The interface defines the specific content of the panel.
 * <p>
 * Through this interface, you can set the tab icon, top bar, panel content, etc. of the panel.
 */
public interface PanelSpec {

    /**
     * Enum class <CODE>PanelType</CODE> represents Panel type.
     */
    enum PanelType{
        /** Reading bookmark panel type. */
        ReadingBookmarks(0, Module.MODULE_NAME_BOOKMARK),
        /** Outline panel type. */
        Outline(1, Module.MODULE_NAME_OUTLINE),
        /** Annotation panel type. */
        Annotations(2, Module.MODULE_NAME_ANNOTPANEL) ,
        /** Attachment panel type. */
        Attachments(3, Module.MODULE_NAME_FILE_PANEL),
        /** Signature panel type. */
        Signatures(4, Module.MODULE_NAME_SIGNATUREPANEL);

        private int mTag;
        private String mModuleName;

        PanelType(int tag, String moduleName){
            this.mTag = tag;
            this.mModuleName = moduleName;
        }

        /**
         * @return the panel tag
         */
        public int getTag(){
            return mTag;
        }

        /**
         * @return the panel name
         */
        public String getModuleName(){
            return mModuleName;
        }
    };

    /**
     * @return a positive integer used to identify the panel tab icon
     */
    int getIcon();

    /**
     * @return the panel type{@link PanelType}
     */
    PanelType getPanelType();

    /**
     * @return the top toolbar of the panel
     */
    View getTopToolbar();

    /**
     * @return the content view of the panel
     */
    View getContentView();

    /**
     * Called when a panel is selected as the current panel.
     */
    void onActivated();

    /**
     * Called when the current panel is changed.
     */
    void onDeactivated();
}
