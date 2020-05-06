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
package com.foxit.uiextensions;

/**
 * Interface that defines information for modules.
 */
public interface Module {
    /** Module name: local module, provide PDF file list view and management*/
    public static final String MODULE_NAME_LOCAL = "Local Module";

    /** Module name: Default annotation module*/
    public static final String MODULE_NAME_DEFAULT = "Default";
    /** Module name: Note annotation module*/
    public static final String MODULE_NAME_NOTE = "Note Module";
    /** Module name: Highlight annotation module*/
    public static final String MODULE_NAME_HIGHLIGHT = "Highlight Module";
    /** Module name: Underline annotation module*/
    public static final String MODULE_NAME_UNDERLINE = "Underline Module";
    /** Module name: Strikeout annotation module*/
    public static final String MODULE_NAME_STRIKEOUT = "Strikeout Module";
    /** Module name: Squiggly annotation module*/
    public static final String MODULE_NAME_SQUIGGLY = "Squiggly Module";
    /** Module name: Link annotation module*/
    public static final String MODULE_NAME_LINK = "Link Module";
    /** Module name: Circle annotation module*/
    public static final String MODULE_NAME_CIRCLE = "Circle Module";
    /** Module name: Rectangle annotation module*/
    public static final String MODULE_NAME_SQUARE = "Rectangle Module";
    /** Module name: Typewriter annotation module*/
    public static final String MODULE_NAME_TYPEWRITER = "Typewriter Module";
    /** Module name: Textbox annotation module*/
    public static final String MODULE_NAME_TEXTBOX = "Textbox Module";
    /** Module name: Callout annotation module*/
    public static final String MODULE_NAME_CALLOUT = "Callout Module";
    /** Module name: Caret annotation module*/
    public static final String MODULE_NAME_CARET = "Caret Module";
    /** Module name: Ink annotation module*/
    public static final String MODULE_NAME_INK = "Ink Module";
    /** Module name: Eraser module*/
    public static final String MODULE_NAME_ERASER = "Eraser Module";
    /** Module name: Stamp annotation module*/
    public static final String MODULE_NAME_STAMP = "Stamp Module";
    /** Module name: Line annotation module*/
    public static final String MODULE_NAME_LINE = "Line Module";
    /** Module name: Polygon annotation module*/
    public static final String MODULE_NAME_POLYGON = "Polygon Module";
    /** Module name: Polyline annotation module*/
    public static final String MODULE_NAME_POLYLINE = "PolyLine Module";
    /** Module name: File attachment annotation module*/
    public static final String MODULE_NAME_FILEATTACHMENT = "FileAttachment Module";
    /** Module name: Screen annotation(PDF Image) module*/
    public static final String MODULE_NAME_IMAGE= "PDFImage Module";
    /** Module name: Screen annotation(Multimedia) module*/
    public static final String MODULE_NAME_MEDIA= "Multimedia Module";
    /** Module name: Reply module*/
    public static final String MODULE_NAME_REPLY = "Reply Module";

    /** Module name: File attachment list module, list all the file attachments in the document on the panel, including file attach annotation and document annotation.*/
    public static final String MODULE_NAME_FILE_PANEL = "FileAttachment List Module";
    /** Module name: Annotation Panel module. It will list all the annotations in the pdf document on the panel view.*/
    public static final String MODULE_NAME_ANNOTPANEL = "Annotations Panel Module";
    /** Module name: Bookmark module. It will list all the custom bookmarks contained in the pdf document.*/
    public static final String MODULE_NAME_BOOKMARK = "Bookmark Module";
    /** Module name: Outline module. It will list a tree view of outline in the pdf document.*/
    public static final String MODULE_NAME_OUTLINE = "Outline Module";
    /** Module name: Digital signature module. Create a digital signature on the document, use certificate to sign the document, and verify the validity of signature.*/
    public static final String MODULE_NAME_DIGITALSIGNATURE = "Digital Signature Module";
    /** Module name: PSI Signature module. Create and add the handwriting signature to PDF document.*/
    public static final String MODULE_NAME_PSISIGNATURE = "PSI Signature Module";
    /** Module name: Form Filler module. Fill the PDF form, export/import form data to/from document, run the form associated JavaScript, etc.*/
    public static final String MODULE_NAME_FORMFILLER = "FormFiller Module";
    /** Module name: Form Navigation module. Navigate between the form fields, it help to quickly locate the next or previous form field to input.*/
    public static final String MODULE_NAME_FORM_NAVIGATION = "Navigation Module";
    /** Module name: Search module. Search the text on the PDF document.*/
    public static final String MODULE_NAME_SEARCH = "Search Module";
    /** Module name: more menu module. Features that could not be classified are placed on the more menu.*/
    public static final String MODULE_MORE_MENU = "More Menu Module";
    /** Module name: Document Information module. Provide the meta data information of PDF document.*/
    public static final String MODULE_NAME_DOCINFO = "DocumentInfo Module";
    /** Module name: Text Select module. Select the text on the PDF pages.*/
    public static final String MODULE_NAME_SELECTION = "TextSelect Module";
    /** Module name: Page Navigation module, Navigate between the PDF pages.*/
    public static final String MODULE_NAME_PAGENAV = "Page Navigation Module";
    /** Module name: Thumbnail module. It list the thumbnail image of all the PDF pages, and it could add new pages to document, change the page index , rotate pages, or delete the specified pages.*/
    public static final String MODULE_NAME_THUMBNAIL = "Thumbnail Module";
    /** Module name: Brightness module. It handles the screen brightness of view control.*/
    public static final String MODULE_NAME_BRIGHTNESS = "Brightness Module";
    /** Module name: Reflow module. Reflow the PDF pages, so the layout of PDF pages could be fit to the small screen of mobile device, which is more friendly to read.*/
    public static final String MODULE_NAME_REFLOW = "Reflow Module";
    /** Module name: undo&redo module. Support undo/redo actions for operations on the annotation.*/
    public static final String MODULE_NAME_UNDO = "Undo Redo Module";
    /** Module name: password security module. Provide the standard PDF security for encrypting PDF document with password protection.*/
    public static final String MODULE_NAME_PASSWORD = "Password Module";
    /** Module name: Crop module. Cut the white edge of PDF page, so the content of page will be displayed more compactly.*/
    public static final String MODULE_NAME_CROP = "Crop Module";
    /** Module name: Print module. Print the document to a wireless printer.*/
    public static final String MODULE_NAME_PRINT = "Print Module";
    /** Module name: Pan&Zoom module. A small window which display the thumbnail image of current page, pan or zoom on the window will be reflected on the current page. This is useful when the size of PDF page is large.*/
    public static final String MODULE_NAME_PANZOOM = "Pan Zoom Module";
    /** Module name: The login in module of connected PDF.*/
    public static final String MODULE_NAME_ACCOUNT = "Default Account Module";
    /** Module name: Dynamic XFA module. It support form filling for XFA file.*/
    public static final String MODULE_NAME_DYNAMICXFA = "Dynamic XFA Module";
    /** Module name: Screen lock module. Lock or unlock the screen, the pdf view will not be rotated if the screen is locked. */
    public static final String MODULE_NAME_SCREENLOCK = "ScreenLock Module";
    /** Module name: Select multiple annotations. Support to select multiple annotations at once, and then change their appearance at once.*/
    public static final String MODULE_NAME_SELECT_ANNOTATIONS = "Select Annotations Module";
    /** Module name: Compare the selected two pdf files.*/
    public static final String MODULE_NAME_COMPARISON = "Comparison Module";
    /** Module name: Redact annotation module*/
    public static final String MODULE_NAME_REDACT = "Redact Module";

    /** Module name: Signature Panel module. It will list all the signatures in the pdf document on the panel view.*/
    public static final String MODULE_NAME_SIGNATUREPANEL = "Signature Panel Module";

    /**
     * Return a module name
     *
     * @return a module name
     */
    String getName();

    /** Load module to the extension manager*/
    boolean loadModule();

    /** Unload module from the extension manager*/
    boolean unloadModule();
}
