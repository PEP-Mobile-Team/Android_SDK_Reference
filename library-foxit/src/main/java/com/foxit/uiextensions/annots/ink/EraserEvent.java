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
package com.foxit.uiextensions.annots.ink;

import com.foxit.uiextensions.annots.common.EditAnnotEvent;

import java.util.ArrayList;

public class EraserEvent extends EditAnnotEvent {
    private ArrayList<EditAnnotEvent> mEventList;
    public EraserEvent(int eventType, ArrayList<EditAnnotEvent> events) {
        mType = eventType;
        mEventList = events;
    }

    @Override
    public boolean add() {
        if (mEventList == null || mEventList.size() == 0) return false;
        for (EditAnnotEvent event: mEventList) {
            event.add();
        }
        return true;
    }

    @Override
    public boolean modify() {
        if (mEventList == null || mEventList.size() == 0) return false;
        for (EditAnnotEvent event: mEventList) {
            event.modify();
        }
        return true;
    }

    @Override
    public boolean delete() {
        if (mEventList == null || mEventList.size() == 0) return false;
        for (EditAnnotEvent event: mEventList) {
            event.delete();
        }
        return true;
    }
}
