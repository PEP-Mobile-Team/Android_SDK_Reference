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
package com.foxit.uiextensions.controls.menu;

import android.view.View;

import com.foxit.uiextensions.UIExtensionsManager;

/**
 * Control {@link MoreMenuModule} group and group's submenus display and hide,
 * as well as add group ,or group submenus to the {@link MoreMenuModule}
 * <br/><br/>
 * You can use it through {@link MoreMenuModule#getMenuView()}, or {@link UIExtensionsManager#getMenuView()}
 */
public interface IMenuView {

    /**
     * Add a group
     * <p>
     * Note1: We use this tag to sort/add/remove/get... group,so the tag must be unique when initializing the MenuGroup .
     * Note2: The tag must be less than 100, or more than 150, because the tag between 100 and 150 has been used or may be used in the future.
     *
     * @param group the group to be added to this menu view.
     */
    void addMenuGroup(MenuGroup group);

    /**
     * According to the tag to remove group.
     *
     * @param tag the group id and is unique, it may be the existing tag<br/>
     *            <ul>
     *             <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *             <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *             <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *             <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *            </ul>
     *            or you custom tag.
     */
    void removeMenuGroup(int tag);

    /**
     * According to the tag Set the enabled state of this group.
     *
     * @param visibility One of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     *                   <p/>
     * @param tag    the group id and is unique, it may be the existing tag<br/>
     *                <ul>
     *                 <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                 <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                </ul>
     *                or you custom tag.
     */
    void setGroupVisibility(int visibility, int tag);

    /**
     * Returns the visibility status for this view by the groupTag.
     * @param tag    the group id and is unique, it may be the existing tag<br/>
     *                <ul>
     *                 <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                 <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                </ul>
     *                or you custom tag.
     *
     * @return One of {@link View#VISIBLE}, {@link View#INVISIBLE}, {@link View#GONE} or -1.
     * if return -1,means can't find this group.
     */
    int getGroupVisibility(int tag);

    /**
     * According to the tag get the group info.
     *
     * @param tag     the group id and is unique, it may be the existing tag<br/>
     *                <ul>
     *                 <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                 <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *               </ul>
     *               or you custom tag.
     * @return the group info
     */
    MenuGroup getMenuGroup(int tag);

    /**
     * Add the item to the group according to the groupTag.
     * <p>
     * Note: We use this tag to sort/get/remove... item,so the tag must be unique when initializing the MenuItem.
     *
     * @param groupTag     the group id and is unique, it may be the existing tag<br/>
     *                       <ul>
     *                       <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                       <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                      </ul>
     *               or you custom tag.
     * @param item     the item to be added to the specified group
     */
    void addMenuItem(int groupTag, MenuItem item);

    /**
     * Remove item by grouptag and itemtag
     *
     * @param groupTag     the group id and is unique, it may be the existing tag<br/>
     *                      <ul>
     *                       <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                       <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                      </ul>
     *                      or you custom tag.
     *                 <p/>
     * @param itemTag  the item id ,it belongs to the group and is unique
     *                 you can customize it, but you have tomake sure that the tag is unique,and we use this to sort item.
     *                 <p/>
     *                 The relationship between item and group is as follows:
     *                 <p/>
     *                 <ul>
     *                 <li>{@link MoreMenuConfig#GROUP_FILE }</li>
     *                 GROUP_FILE: [ <br/>
     *                 {@link MoreMenuConfig#ITEM_DOCINFO}, <br/>
     *                 {@link MoreMenuConfig#ITEM_REDUCE_FILE_SIZE}<br/>
     *                 ] <br/>
     *                 <p/>
     *                 <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                 GROUP_PROTECT: [ <br/>
     *                 {@link MoreMenuConfig#ITEM_PASSWORD}, <br/>
     *                 {@link MoreMenuConfig#ITEM_REMOVESECURITY_PASSWORD} <br/>
     *                 ] <br/>
     *                 <p/>
     *                 <li>{@link MoreMenuConfig#GROUP_FORM}</li>
     *                 GROUP_FORM: [<br/>
     *                 {@link MoreMenuConfig#ITEM_CREATE_FORM},<br/>
     *                 {@link MoreMenuConfig#ITEM_RESET_FORM}, <br/>
     *                 {@link MoreMenuConfig#ITEM_IMPORT_FORM}, <br/>
     *                 {@link MoreMenuConfig#ITEM_EXPORT_FORM} <br/>
     *                 ] <br/>
     *                 <p/>
     *                 <li>{@link MoreMenuConfig#GROUP_ANNOTATION}</li>
     *                 GROUP_ANNOTATION: [<br/>
     *                {@link MoreMenuConfig#ITEM_ANNOTATION_IMPORT}<br/>
     *                {@link MoreMenuConfig#ITEM_ANNOTATION_EXPORT}<br/>
     *                 ]<br/>
     *                 </ul>
     * @see MoreMenuConfig
     */
    void removeMenuItem(int groupTag, int itemTag);

    /**
     * According to the groupTag and itemTag Set the enabled state of this item.
     *
     * Note:
     * 1:If the state of the group that the item belongs to is <CODE>View.GONE</CODE>,then using this method to set the state of the item is not effective.
     * 2: if the groupTag is {@link MoreMenuConfig#GROUP_PROTECT},this method can be used only when the Doc is open success,otherwise, the use is not normal.
     *
     * @param visibility One of {@link View#VISIBLE}, {@link View#INVISIBLE}, or {@link View#GONE}.
     *                   <p>
     * @param groupTag     the group id and is unique ,it may be the existing tag<br/>
     *                      <ul>
     *                       <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                       <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                       <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                      </ul>
     *                      or you custom tag.
     *                     <p/>
     * @param itemTag    the item id ,it belongs to the group and is unique
     *                   you can customize it, but you have tomake sure that the tag is unique,and we use this to sort item.
     *                   <p>
     *                   The relationship between item and group is as follows:
     *                   <p>
     *                   <ul>
     *                   <li>{@link MoreMenuConfig#GROUP_FILE }</li>
     *                   GROUP_FILE: [<br/>
     *                   {@link MoreMenuConfig#ITEM_DOCINFO},<br/>
     *                   {@link MoreMenuConfig#ITEM_REDUCE_FILE_SIZE}<br/>
     *                   ]<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                   GROUP_PROTECT: [<br/>
     *                   {@link MoreMenuConfig#ITEM_PASSWORD},<br/>
     *                   {@link MoreMenuConfig#ITEM_REMOVESECURITY_PASSWORD}<br/>
     *                   ],<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_FORM}</li>
     *                   GROUP_FORM: [<br/>
     *                   {@link MoreMenuConfig#ITEM_CREATE_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_RESET_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_IMPORT_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_EXPORT_FORM}<br/>
     *                   ]<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_ANNOTATION}</li>
     *                   GROUP_ANNOTATION: [<br/>
     *                  {@link MoreMenuConfig#ITEM_ANNOTATION_IMPORT}<br/>
     *                  {@link MoreMenuConfig#ITEM_ANNOTATION_EXPORT}<br/>
     *                    ]<br/>
     *                   </ul>
     * @see MoreMenuConfig
     */
    void setItemVisibility(int visibility, int groupTag, int itemTag);

    /**
     * Returns the visibility status for this view by the groupTag and itemTag.
     *
     * Note: if the groupTag is {@link MoreMenuConfig#GROUP_PROTECT},this method can be used only when the Doc is open success,otherwise, the use is not normal.
     *
     * @param groupTag    the group id and is unique, it may be the existing tag<br/>
     *                <ul>
     *                 <li>{@link MoreMenuConfig#GROUP_FILE }</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_FORM}</li>,
     *                 <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                 <li>{@link MoreMenuConfig#GROUP_ANNOTATION}/li>
     *                </ul>
     *                or you custom tag.
     * @param itemTag    the item id ,it belongs to the group and is unique
     *                   you can customize it, but you have to make sure that the tag is unique,and we use this to sort item.
     *                   <p>
     *                   The relationship between item and group is as follows:
     *                   <p>
     *                   <ul>
     *                   <li>{@link MoreMenuConfig#GROUP_FILE }</li>
     *                   GROUP_FILE: [<br/>
     *                   {@link MoreMenuConfig#ITEM_DOCINFO},<br/>
     *                   {@link MoreMenuConfig#ITEM_REDUCE_FILE_SIZE}<br/>
     *                   ]<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_PROTECT}</li>
     *                   GROUP_PROTECT: [<br/>
     *                   {@link MoreMenuConfig#ITEM_PASSWORD},<br/>
     *                   {@link MoreMenuConfig#ITEM_REMOVESECURITY_PASSWORD}<br/>
     *                   ],<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_FORM}</li>
     *                   GROUP_FORM: [<br/>
     *                   {@link MoreMenuConfig#ITEM_CREATE_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_RESET_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_IMPORT_FORM},<br/>
     *                   {@link MoreMenuConfig#ITEM_EXPORT_FORM}<br/>
     *                   ]<br/>
     *                   <p/>
     *                   <li>{@link MoreMenuConfig#GROUP_ANNOTATION}</li>
     *                   GROUP_ANNOTATION: [<br/>
     *                  {@link MoreMenuConfig#ITEM_ANNOTATION_IMPORT}<br/>
     *                  {@link MoreMenuConfig#ITEM_ANNOTATION_EXPORT}<br/>
     *                   ]<br/>
     *                   </ul>
     *
     * @return One of {@link View#VISIBLE}, {@link View#INVISIBLE}, {@link View#GONE} or -1.
     * if return -1,means can't find this item.
     */
    int getItemVisibility(int groupTag, int itemTag);


    /** the content view of the menu.*/
    View getContentView();
}
