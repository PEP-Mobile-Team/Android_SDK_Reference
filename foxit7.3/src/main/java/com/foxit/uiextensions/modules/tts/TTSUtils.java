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
package com.foxit.uiextensions.modules.tts;


import android.graphics.RectF;
import android.text.TextUtils;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Progressive;
import com.foxit.sdk.common.fxcrt.PointF;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.sdk.pdf.annots.QuadPoints;
import com.foxit.sdk.pdf.annots.QuadPointsArray;
import com.foxit.sdk.pdf.annots.TextMarkup;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.utils.AppUtil;

import java.util.ArrayList;

public class TTSUtils {

    //int[][0] - StartUnicode
    //int[][1] - EndUnicode
    //int[][2] - BitField
    //int[][3] - CodePage
    private final static int[][] unicodeArrs = {
            {0x0000, 0x007F, 0, 1252},    // Basic Latin
            {0x0080, 0x00FF, 1, 1252},    // Latin-1 Supplement
            {0x0100, 0x017F, 2, 1250},    // Latin Extended-A
            {0x0180, 0x024F, 3, 1250},    // Latin Extended-B
            {0x0250, 0x02AF, 4, 0xFFFF},    // IPA Extensions
            {0x02B0, 0x02FF, 5, 0xFFFF},    // Spacing Modifier Letters
            {0x0300, 0x036F, 6, 0xFFFF},    // Combining Diacritical Marks
            {0x0370, 0x03FF, 7, 1253},    // Greek and Coptic
            {0x0400, 0x04FF, 9, 1251},    // Cyrillic
            {0x0500, 0x052F, 9, 0xFFFF},    // Cyrillic Supplement
            {0x0530, 0x058F, 10, 0xFFFF},    // Armenian
            {0x0590, 0x05FF, 11, 1255},    // Hebrew
            {0x0600, 0x06FF, 13, 1256},    // Arabic
            {0x0700, 0x074F, 71, 0xFFFF},    // Syriac
            {0x0750, 0x077F, 13, 0xFFFF},    // Arabic Supplement
            {0x0780, 0x07BF, 72, 0xFFFF},    // Thaana
            {0x07C0, 0x07FF, 14, 0xFFFF},    // NKo
            {0x0800, 0x08FF, 999, 0xFFFF},    //
            {0x0900, 0x097F, 15, 0xFFFF},    // Devanagari
            {0x0980, 0x09FF, 16, 0xFFFF},    // Bengali
            {0x0A00, 0x0A7F, 17, 0xFFFF},    // Gurmukhi
            {0x0A80, 0x0AFF, 18, 0xFFFF},    // Gujarati
            {0x0B00, 0x0B7F, 19, 0xFFFF},    // Oriya
            {0x0B80, 0x0BFF, 20, 0xFFFF},    // Tamil
            {0x0C00, 0x0C7F, 21, 0xFFFF},    // Telugu
            {0x0C80, 0x0CFF, 22, 0xFFFF},    // Kannada
            {0x0D00, 0x0D7F, 23, 0xFFFF},    // Malayalam
            {0x0D80, 0x0DFF, 73, 0xFFFF},    // Sinhala
            {0x0E00, 0x0E7F, 24, 874},    // Thai
            {0x0E80, 0x0EFF, 25, 0xFFFF},    // Lao
            {0x0F00, 0x0FFF, 70, 0xFFFF},    // Tibetan
            {0x1000, 0x109F, 74, 0xFFFF},    // Myanmar
            {0x10A0, 0x10FF, 26, 0xFFFF},    // Georgian
            {0x1100, 0x11FF, 28, 949},        // Hangul Jamo
            {0x1200, 0x137F, 75, 0xFFFF},    // Ethiopic
            {0x1380, 0x139F, 75, 0xFFFF},    // Ethiopic Supplement
            {0x13A0, 0x13FF, 76, 0xFFFF},    // Cherokee
            {0x1400, 0x167F, 77, 0xFFFF},    // Unified Canadian Aboriginal Syllabics
            {0x1680, 0x169F, 78, 0xFFFF},    // Ogham
            {0x16A0, 0x16FF, 79, 0xFFFF},    // Runic
            {0x1700, 0x171F, 84, 0xFFFF},    // Tagalog
            {0x1720, 0x173F, 84, 0xFFFF},    // Hanunoo
            {0x1740, 0x175F, 84, 0xFFFF},    // Buhid
            {0x1760, 0x177F, 84, 0xFFFF},    // Tagbanwa
            {0x1780, 0x17FF, 80, 0xFFFF},    // Khmer
            {0x1800, 0x18AF, 81, 0xFFFF},    // Mongolian
            {0x18B0, 0x18FF, 999, 0xFFFF},    //
            {0x1900, 0x194F, 93, 0xFFFF},    // Limbu
            {0x1950, 0x197F, 94, 0xFFFF},    // Tai Le
            {0x1980, 0x19DF, 95, 0xFFFF},    // New Tai Lue
            {0x19E0, 0x19FF, 80, 0xFFFF},    // Khmer Symbols
            {0x1A00, 0x1A1F, 96, 0xFFFF},    // Buginese
            {0x1A20, 0x1AFF, 999, 0xFFFF},    //
            {0x1B00, 0x1B7F, 27, 0xFFFF},    // Balinese
            {0x1B80, 0x1BBF, 112, 0xFFFF},    // Sundanese
            {0x1BC0, 0x1BFF, 999, 0xFFFF},    //
            {0x1C00, 0x1C4F, 113, 0xFFFF},    // Lepcha
            {0x1C50, 0x1C7F, 114, 0xFFFF},    // Ol Chiki
            {0x1C80, 0x1CFF, 999, 0xFFFF},    //
            {0x1D00, 0x1D7F, 4, 0xFFFF},    // Phonetic Extensions
            {0x1D80, 0x1DBF, 4, 0xFFFF},    // Phonetic Extensions Supplement
            {0x1DC0, 0x1DFF, 6, 0xFFFF},    // Combining Diacritical Marks Supplement
            {0x1E00, 0x1EFF, 29, 1258},        // Latin Extended Additional
            {0x1F00, 0x1FFF, 30, 1253},        // Greek Extended
            {0x2000, 0x206F, 31, 936},        // General Punctuation
            {0x2070, 0x209F, 32, 0xFFFF},    // Superscripts And Subscripts
            {0x20A0, 0x20CF, 33, 0xFFFF},    // Currency Symbols
            {0x20D0, 0x20FF, 34, 0xFFFF},    // Combining Diacritical Marks For Symbols
            {0x2100, 0x214F, 35, 0xFFFF},    // Letterlike Symbols
            {0x2150, 0x215F, 36, 0xFFFF},    // Number Forms
            {0x2160, 0x216B, 36, 936},    // Number Forms
            {0x216C, 0x216F, 36, 0xFFFF},    // Number Forms
            {0x2170, 0x2179, 36, 936},    // Number Forms
            {0x217A, 0x218F, 36, 0xFFFF},    // Number Forms
            {0x2190, 0x2199, 37, 949},    // Arrows
            {0x219A, 0x21FF, 37, 0xFFFF},    // Arrows
            {0x2200, 0x22FF, 38, 0xFFFF},    // Mathematical Operators
            {0x2300, 0x23FF, 39, 0xFFFF},    // Miscellaneous Technical
            {0x2400, 0x243F, 40, 0xFFFF},    // Control Pictures
            {0x2440, 0x245F, 41, 0xFFFF},    // Optical Character Recognition
            {0x2460, 0x2473, 42, 932},    // Enclosed Alphanumerics
            {0x2474, 0x249B, 42, 936},    // Enclosed Alphanumerics
            {0x249C, 0x24E9, 42, 949},    // Enclosed Alphanumerics
            {0x24EA, 0x24FF, 42, 0xFFFF},    // Enclosed Alphanumerics
            {0x2500, 0x2573, 43, 936},    // Box Drawing
            {0x2574, 0x257F, 43, 0xFFFF},    // Box Drawing
            {0x2580, 0x2580, 44, 0xFFFF},    // Block Elements
            {0x2581, 0x258F, 44, 936},    // Block Elements
            {0x2590, 0x259F, 44, 0xFFFF},    // Block Elements
            {0x25A0, 0x25FF, 45, 936},    // Geometric Shapes
            {0x2600, 0x26FF, 46, 0xFFFF},    // Miscellaneous Symbols
            {0x2700, 0x27BF, 47, 0xFFFF},    // Dingbats
            {0x27C0, 0x27EF, 38, 0xFFFF},    // Miscellaneous Mathematical Symbols-A
            {0x27F0, 0x27FF, 37, 0xFFFF},    // Supplemental Arrows-A
            {0x2800, 0x28FF, 82, 0xFFFF},    // Braille Patterns
            {0x2900, 0x297F, 37, 0xFFFF},    // Supplemental Arrows-B
            {0x2980, 0x29FF, 38, 0xFFFF},    // Miscellaneous Mathematical Symbols-B
            {0x2A00, 0x2AFF, 38, 0xFFFF},    // Supplemental Mathematical Operators
            {0x2B00, 0x2BFF, 37, 0xFFFF},    // Miscellaneous Symbols and Arrows
            {0x2C00, 0x2C5F, 97, 0xFFFF},    // Glagolitic
            {0x2C60, 0x2C7F, 29, 0xFFFF},    // Latin Extended-C
            {0x2C80, 0x2CFF, 8, 0xFFFF},    // Coptic
            {0x2D00, 0x2D2F, 26, 0xFFFF},    // Georgian Supplement
            {0x2D30, 0x2D7F, 98, 0xFFFF},    // Tifinagh
            {0x2D80, 0x2DDF, 75, 0xFFFF},    // Ethiopic Extended
            {0x2DE0, 0x2DFF, 9, 0xFFFF},    // Cyrillic Extended-A
            {0x2E00, 0x2E7F, 31, 0xFFFF},    // Supplemental Punctuation
            {0x2E80, 0x2EFF, 59, 0xFFFF},    // CJK Radicals Supplement
            {0x2F00, 0x2FDF, 59, 0xFFFF},    // Kangxi Radicals
            {0x2FE0, 0x2FEF, 999, 0xFFFF},    //
            {0x2FF0, 0x2FFF, 59, 0xFFFF},    // Ideographic Description Characters
            {0x3000, 0x303F, 48, 936},    // CJK Symbols And Punctuation
            {0x3040, 0x309F, 49, 932},    // Hiragana
            {0x30A0, 0x30FF, 50, 932},    // Katakana
            {0x3100, 0x3129, 51, 936},    // Bopomofo
            {0x312A, 0x312F, 51, 0xFFFF},    // Bopomofo
            {0x3130, 0x318F, 52, 949},    // Hangul Compatibility Jamo
            {0x3190, 0x319F, 59, 0xFFFF},    // Kanbun
            {0x31A0, 0x31BF, 51, 0xFFFF},    // Bopomofo Extended
            {0x31C0, 0x31EF, 61, 0xFFFF},    // CJK Strokes
            {0x31F0, 0x31FF, 50, 932},        // Katakana Phonetic Extensions
            {0x3200, 0x321C, 54, 949},    // Enclosed CJK Letters And Months
            {0x321D, 0x325F, 54, 0xFFFF},    // Enclosed CJK Letters And Months
            {0x3260, 0x327F, 54, 949},    // Enclosed CJK Letters And Months
            {0x3280, 0x32FF, 54, 0xFFFF},    // Enclosed CJK Letters And Months
            {0x3300, 0x3387, 55, 0xFFFF},    // CJK Compatibility
            {0x3388, 0x33D0, 55, 949},    // CJK Compatibility
            {0x33D1, 0x33FF, 55, 0xFFFF},    // CJK Compatibility
            {0x3400, 0x4DBF, 59, 0xFFFF},    // CJK Unified Ideographs Extension A
            {0x4DC0, 0x4DFF, 99, 0xFFFF},    // Yijing Hexagram Symbols
            {0x4E00, 0x9FA5, 59, 936},    // CJK Unified Ideographs
            {0x9FA6, 0x9FFF, 59, 0xFFFF},    // CJK Unified Ideographs
            {0xA000, 0xA48F, 83, 0xFFFF},    // Yi Syllables
            {0xA490, 0xA4CF, 83, 0xFFFF},    // Yi Radicals
            {0xA4D0, 0xA4FF, 999, 0xFFFF},    //
            {0xA500, 0xA63F, 12, 0xFFFF},    // Vai
            {0xA640, 0xA69F, 9, 0xFFFF},    // Cyrillic Extended-B
            {0xA6A0, 0xA6FF, 999, 0xFFFF},    //
            {0xA700, 0xA71F, 5, 0xFFFF},    // Modifier Tone Letters
            {0xA720, 0xA7FF, 29, 0xFFFF},    // Latin Extended-D
            {0xA800, 0xA82F, 100, 0xFFFF},    // Syloti Nagri
            {0xA830, 0xA8FF, 999, 0xFFFF},    //
            {0xA840, 0xA87F, 53, 0xFFFF},    // Phags-pa
            {0xA880, 0xA8DF, 115, 0xFFFF},    // Saurashtra
            {0xA8E0, 0xA8FF, 999, 0xFFFF},    //
            {0xA900, 0xA92F, 116, 0xFFFF},    // Kayah Li
            {0xA930, 0xA95F, 117, 0xFFFF},    // Rejang
            {0xA960, 0xA9FF, 999, 0xFFFF},    //
            {0xAA00, 0xAA5F, 118, 0xFFFF},    // Cham
            {0xAA60, 0xABFF, 999, 0xFFFF},    //
            {0xAC00, 0xD7AF, 56, 949},    // Hangul Syllables
            {0xD7B0, 0xD7FF, 999, 0xFFFF},    //
            {0xD800, 0xDB7F, 57, 0xFFFF},    // High Surrogates
            {0xDB80, 0xDBFF, 57, 0xFFFF},    // High Private Use Surrogates
            {0xDC00, 0xDFFF, 57, 0xFFFF},    // Low Surrogates
            {0xE000, 0xE814, 60, 0xFFFF},    // Private Use Area
            {0xE815, 0xE864, 60, 936},    // Private Use Area
            {0xE865, 0xEFFF, 60, 0xFFFF},    // Private Use Area
            {0xF000, 0xF0FF, 60, 42},    // Private Use Area
            {0xF100, 0xF8FF, 60, 0xFFFF},    // Private Use Area
            {0xF900, 0xFA0B, 61, 949},    // CJK Compatibility Ideographs
            {0xFA0C, 0xFA0D, 61, 936},    // CJK Compatibility Ideographs
            {0xFA0E, 0xFA2D, 61, 932},    // CJK Compatibility Ideographs
            {0xFA2E, 0xFAFF, 61, 0xFFFF},    // CJK Compatibility Ideographs
            {0xFB00, 0xFB4F, 62, 0xFFFF},    // Alphabetic Presentation Forms
            {0xFB50, 0xFDFF, 63, 1256},    // Arabic Presentation Forms-A
            {0xFE00, 0xFE0F, 91, 0xFFFF},    // Variation Selectors
            {0xFE10, 0xFE1F, 65, 0xFFFF},    // Vertical Forms
            {0xFE20, 0xFE2F, 64, 0xFFFF},    // Combining Half Marks
            {0xFE30, 0xFE4F, 65, 0xFFFF},    // CJK Compatibility Forms
            {0xFE50, 0xFE6F, 66, 0xFFFF},    // Small Form Variants
            {0xFE70, 0xFEFF, 67, 1256},    // Arabic Presentation Forms-B
            {0xFF00, 0xFF5F, 68, 936},    // Halfwidth And Fullwidth Forms
            {0xFF60, 0xFF9F, 68, 932},    // Halfwidth And Fullwidth Forms
            {0xFFA0, 0xFFEF, 68, 0xFFFF},    // Halfwidth And Fullwidth Forms
    };

    private final static char[] puncts = {',',
            '.',
            '!',
            '?',
            ';',
            ':',
            '¿',
            '¡',
            '，',
            '。',
            '！',
            '？',
            '；',
            '：',
            '、'};

    public static int getCodePageFormUnicode(int unicode) {
        int iEnd = unicodeArrs.length - 1;
        int iStart = 0, iMid;
        do {
            iMid = (iStart + iEnd) / 2;
            int[] usb = unicodeArrs[iMid];

            if (unicode < usb[0])
                iEnd = iMid - 1;
            else if (unicode > usb[1])
                iStart = iMid + 1;
            else if (usb[3] != 0xFFFF)
                return usb[3];
            else
                break;
        } while (iStart <= iEnd);
        return 0;
    }

    public static void splitSentenceByLanguage(String content, ArrayList<String> strListResult, ArrayList<Integer> codePageListResult) {
        if (TextUtils.isEmpty(content))
            return;

        ArrayList<Integer> codePageList = new ArrayList<>();
        ArrayList<String> strList = new ArrayList<>();

        char[] charContents = content.toCharArray();
        int length = charContents.length;
        for (int i = 0; i < length; i++) {
            char charContent = charContents[i];
            int unicode = Integer.parseInt(Integer.toHexString(charContent), 16);
            int codePage = getCodePageFormUnicode(unicode);
            boolean isPunct = isPunct(charContent);

            int size = codePageList.size();
            if (size > 0 && codePageList.get(size - 1) == codePage) {
                String str = strList.get(size - 1) + charContent;
                strList.set(size - 1, str);
                if (isPunct) {
                    codePageList.add(codePage);
                    strList.add("");
                }
            } else {
                codePageList.add(codePage);
                strList.add(String.valueOf(charContent));
            }
        }

        for (int i = 0; i < strList.size(); i++) {
            if (strList.get(i).length() > 0) {
                strListResult.add(strList.get(i));
                codePageListResult.add(codePageList.get(i));
            }
        }
    }

    public static boolean isPunct(char content) {
        int length = puncts.length;
        for (int i = 0; i < length; i++) {
            if (content == puncts[i]) {
                return true;
            }
        }
        return false;
    }

    public static ArrayList<TTSInfo> getAllSentencesAfterCombine(PDFViewCtrl viewCtrl, PDFPage page, int startIndex) {
        ArrayList<TTSInfo> senInfos = new ArrayList<>();
        try {
            if (startIndex < 0)
                startIndex = 0;

            StringBuilder sb = new StringBuilder();
            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            String content = textPage.getChars(startIndex, textPage.getCharCount() - startIndex + 1);
            char[] charContents = content.toCharArray();
            int length = charContents.length;

            int contentIndex = 0;
            for (int i = contentIndex; i < length; i++) {
                char charContent = charContents[i];
                sb.append(charContent);

                boolean isEnd = isEnd(charContent);
                if (isEnd || i == length - 1) {
                    String str = sb.toString().replaceAll("[\r\n]", "");
                    if (isEnd && str.length() <= 1) {
                        //reset index
                        contentIndex = i + 1;
                        sb.setLength(0);
                        continue;
                    }

                    TTSInfo ttsInfo = new TTSInfo();
                    //get string
                    ttsInfo.mText = str;

                    //get RectF
                    TextSelector ts = new TextSelector(viewCtrl);
                    ts.computeSelected(page, startIndex + contentIndex, startIndex + i);
                    ttsInfo.mRects.addAll(ts.getRectFList());
                    senInfos.add(ttsInfo);

                    //reset index
                    contentIndex = i + 1;
                    sb.setLength(0);
                }
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return senInfos;
    }

    static boolean isEnd(char content) {
        if (content == '.'
                || content == '!'
                || content == '。'
                || content == '！'
                || content == '？'
                || content == '?') {
            return true;
        }
        return false;
    }

    public static TTSInfo getTTSInfoFormTextMarkup(TextMarkup textMarkup) {
        if (textMarkup == null || textMarkup.isEmpty()) return null;
        try {
            PDFPage page = textMarkup.getPage();
            if (!page.isParsed()) {
                Progressive progressive = page.startParse(PDFPage.e_ParsePageNormal, null, false);
                int state = Progressive.e_ToBeContinued;
                while (state == Progressive.e_ToBeContinued) {
                    state = progressive.resume();
                }
            }

            TTSInfo ttsInfo = new TTSInfo();
            QuadPointsArray quadPointsArray = textMarkup.getQuadPoints();
            long pointSize = quadPointsArray.getSize();
            for (long i = 0; i < pointSize; i++) {
                QuadPoints quadPoints = quadPointsArray.getAt(i);
                RectF rectF = new RectF(quadPoints.getFirst().getX(), quadPoints.getFirst().getY(), quadPoints.getFourth().getX(), quadPoints.getFourth().getY());
                ttsInfo.mRects.add(rectF);
            }

            TextPage textPage = new TextPage(page, TextPage.e_ParseTextNormal);
            if (pointSize > 0) {
                PointF startPointF = quadPointsArray.getAt(0).getFirst();
                ttsInfo.mStart = textPage.getIndexAtPos(startPointF.getX(), startPointF.getY(), 5);
            }

            ttsInfo.mText = textPage.getTextUnderAnnot(textMarkup);
            ttsInfo.mPageIndex = page.getIndex();
            ttsInfo.mRect = AppUtil.toRectF(textMarkup.getRect());
            return ttsInfo;
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return null;
    }

}
