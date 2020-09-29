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

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.widget.ZoomButtonsController;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.fxcrt.Matrix2D;
import com.foxit.sdk.pdf.PageBasicInfo;
import com.foxit.sdk.pdf.actions.Destination;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.foxit.sdk.common.Constants.e_ErrDeviceLimitation;
import static com.foxit.sdk.common.Constants.e_ErrFormat;
import static com.foxit.sdk.common.Constants.e_ErrInvalidLicense;
import static com.foxit.sdk.common.Constants.e_ErrNoComparisonModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoComplianceModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoConnectedPDFModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoConversionModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoOCRModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoOptimizerModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoRMSModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoRedactionModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrNoRights;
import static com.foxit.sdk.common.Constants.e_ErrNoXFAModuleRight;
import static com.foxit.sdk.common.Constants.e_ErrRightsExpired;

public class AppUtil {
    public static final int ALERT_OK = 1;
    public static final int ALERT_CANCEL = 2;

    public static boolean isEmailFormatForRMS(String userId) {
        Pattern emailPattern = Pattern.compile("[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-zA-Z0-9!#$%&'*+/=?^_`{|}~-]+)*@(?:[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?\\.)+[a-zA-Z0-9](?:[a-zA-Z0-9-]*[a-zA-Z0-9])?");
        Matcher matcher = emailPattern.matcher(userId);
        return matcher.find();
    }

    private static long sLastTimeMillis;

    public static boolean isFastDoubleClick() {
        long currentTimeMillis = System.currentTimeMillis();
        long delta = currentTimeMillis - sLastTimeMillis;
        if (Math.abs(delta) < 500) {
            return true;
        }
        sLastTimeMillis = currentTimeMillis;
        return false;
    }

    public static void showSoftInput(final View editText) {
        if (editText == null) return;
        editText.requestFocus();
        editText.post(new Runnable() {
            @Override
            public void run() {
                InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showSoftInput(editText, 0);
            }
        });
    }

    public static void dismissInputSoft(final View editText) {
        if (editText == null) return;
        InputMethodManager imm = (InputMethodManager) editText.getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
    }

    public static void fixBackgroundRepeat(View view) {
        Drawable bg = view.getBackground();
        if (bg != null) {
            if (bg instanceof BitmapDrawable) {
                BitmapDrawable bmp = (BitmapDrawable) bg;
                bmp.mutate();
                bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
            }
        }
    }

    public static void openUrl(final Activity act, String url) {
        final String myurl = url;

        final UITextEditDialog dialog = new UITextEditDialog(act);
        dialog.getInputEditText().setVisibility(View.GONE);
        dialog.setTitle(act.getApplicationContext().getString(R.string.rv_url_dialog_title));
        dialog.getPromptTextView().setText(act.getApplicationContext().getString(R.string.rv_urldialog_title) +
                url +
                act.getApplicationContext().getString(R.string.rv_urldialog_title_ko) +
                "?");
        dialog.getOKButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri;
                if (myurl.toLowerCase().startsWith("http://") || myurl.toLowerCase().startsWith("https://")) {
                    uri = Uri.parse(myurl);
                } else {
                    uri = Uri.parse("http://" + myurl);
                }
                Intent it = new Intent(Intent.ACTION_VIEW, uri);
                act.startActivity(it);
                dialog.dismiss();
            }
        });

        dialog.getCancelButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public static void mailTo(Activity act, String uri) {
        if (isEmpty(uri) || isFastDoubleClick()) return;
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        if (uri.startsWith("mailto:")) {
            intent.setData(Uri.parse(uri));
        } else {
            intent.setData(Uri.parse("mailto:" + uri));
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        act.startActivity(Intent.createChooser(intent, ""));
    }

    public static boolean isEmpty(CharSequence str) {
        if (str == null || str.length() == 0)
            return true;
        else
            return false;
    }

    public static boolean isEqual(CharSequence str1, CharSequence str2) {
        if (str1 == null && str2 == null)
            return true;
        if (str1 == null || str2 == null)
            return false;
        return str1.equals(str2);
    }

    public static void alert(Activity act, final String title, final String prompt, final int buttons) {

        final UITextEditDialog dlg = new UITextEditDialog(act);
        dlg.setTitle(title);
        dlg.getPromptTextView().setText(prompt);
        dlg.getInputEditText().setVisibility(View.GONE);
        if ((buttons & ALERT_OK) == 0)
            dlg.getOKButton().setVisibility(View.GONE);
        if ((buttons & ALERT_CANCEL) == 0)
            dlg.getCancelButton().setVisibility(View.GONE);
        dlg.show();

        dlg.getOKButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });

        dlg.getCancelButton().setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                dlg.dismiss();
            }
        });
        dlg.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                dlg.dismiss();
            }
        });
    }

    private static int getStartPos(String allData, String entry) {
        int startPos = allData.indexOf(entry);
        if (startPos > 0 && !allData.substring(startPos - 1, startPos).contentEquals(",")) {
            return getStartPos(allData.substring(startPos + entry.length()), entry);
        }
        return startPos;
    }

    public static String getEntryName(String allData, String entry) {
        if (allData == null || entry == null)
            return null;
        //CN=X,O=X,OU=X,E=X,C=X
        String name = null;

        int startPos = getStartPos(allData, entry);//allData.indexOf(entry);
        if (startPos < 0) {
            return "";
        }
        int endPos = 0;
        if (entry.contentEquals("C=")) {
            endPos = allData.length();
        } else {
            endPos = allData.indexOf(",", startPos);
        }

        if (endPos == -1) {
            endPos = allData.length();
        }

        name = allData.substring(startPos + entry.length(), endPos);

        return name;
    }

    public static String getFileName(String filePath) {
        int index = filePath.lastIndexOf('/');
        return (index < 0) ? filePath : filePath.substring(index + 1, filePath.length());
    }


    public static String fileSizeToString(long size) {
        float fsize = size;
        char unit[] = {'B', 'K', 'M'};
        for (int i = 0; i < unit.length; i++) {
            if (fsize < 1024 || i == unit.length - 1) {
                BigDecimal b = new BigDecimal(fsize);
                fsize = b.setScale(2, BigDecimal.ROUND_HALF_UP).floatValue();
                return String.valueOf(fsize) + unit[i];
            }
            fsize /= 1024;
        }
        return "";
    }

    public static String getFileFolder(String filePath) {
        int index = filePath.lastIndexOf('/');
        if (index < 0) return "";
        return filePath.substring(0, index);
    }

    public static boolean isBlank(String str) {
        int strLen;
        if (str != null && (strLen = str.length()) != 0) {
            for (int i = 0; i < strLen; ++i) {
                if (!Character.isWhitespace(str.charAt(i))) {
                    return false;
                }
            }
            return true;
        } else {
            return true;
        }
    }

    /**
     * Returns {@code o} if non-null, or throws {@code NullPointerException}.
     */
    public static <T> T requireNonNull(T o) {
        if (o == null) {
            throw new NullPointerException();
        }
        return o;
    }

    /**
     * Returns {@code o} if non-null, or throws {@code NullPointerException}.
     */
    public static <T> T requireNonNull(T o, String detailMessage) {
        if (o == null) {
            throw new NullPointerException(detailMessage);
        }
        return o;
    }

    public static void setWebViewZoomControlButtonGone(WebView view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            view.getSettings().setDisplayZoomControls(false);
            return;
        }
        Class classType;
        Field field;
        try {
            classType = WebView.class;
            field = classType.getDeclaredField("mZoomButtonsController");
            field.setAccessible(true);
            ZoomButtonsController mZoomButtonsController = new ZoomButtonsController(view);
            mZoomButtonsController.getZoomControls().setVisibility(View.GONE);
            try {
                field.set(view, mZoomButtonsController);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public static Rect toRect(com.foxit.sdk.common.fxcrt.RectI rectI) {
        return new Rect(rectI.getLeft(), rectI.getTop(), rectI.getRight(), rectI.getBottom());
    }

    public static RectF toRectF(com.foxit.sdk.common.fxcrt.RectI rect) {
        return new RectF(rect.getLeft(), rect.getTop(), rect.getRight(), rect.getBottom());
    }

    public static RectF toRectF(com.foxit.sdk.common.fxcrt.RectF rectF) {
        return new RectF(rectF.getLeft(), rectF.getTop(), rectF.getRight(), rectF.getBottom());
    }

    public static com.foxit.sdk.common.fxcrt.RectF toFxRectF(RectF rectF) {
        return new com.foxit.sdk.common.fxcrt.RectF(rectF.left, rectF.bottom, rectF.right, rectF.top);
    }

    public static PointF toPointF(com.foxit.sdk.common.fxcrt.PointF pointF) {
        return new PointF(pointF.getX(), pointF.getY());
    }

    public static com.foxit.sdk.common.fxcrt.PointF toFxPointF(PointF pointF) {
        return new com.foxit.sdk.common.fxcrt.PointF(pointF.x, pointF.y);
    }

    public static com.foxit.sdk.common.fxcrt.PointF toFxPointF(float x, float y) {
        return new com.foxit.sdk.common.fxcrt.PointF(x, y);
    }

    public static RectF toGlobalVisibleRectF(View rootView, RectF viewRectF) {
        int[] location = new int[2];
        rootView.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        return new RectF(viewRectF.left + x, viewRectF.top + y, viewRectF.right + x, viewRectF.bottom + y);
    }

    public static String getMessage(Context context, int errCode) {
        String message = AppResource.getString(context.getApplicationContext(), R.string.rv_document_open_failed);
        switch (errCode) {
            case e_ErrFormat:
                message = AppResource.getString(context.getApplicationContext(), R.string.rv_format_error);
                break;
            case e_ErrNoConnectedPDFModuleRight:
            case e_ErrNoXFAModuleRight:
            case e_ErrNoRedactionModuleRight:
            case e_ErrNoRMSModuleRight:
            case e_ErrNoOCRModuleRight:
            case e_ErrNoComparisonModuleRight:
            case e_ErrNoComplianceModuleRight:
            case e_ErrNoOptimizerModuleRight:
            case e_ErrNoConversionModuleRight:
            case e_ErrInvalidLicense:
                message = AppResource.getString(context.getApplicationContext(), R.string.rv_invalid_license);
                break;
            case e_ErrDeviceLimitation:
                message = AppResource.getString(context.getApplicationContext(), R.string.rv_deivce_limitation);
                break;
            case e_ErrNoRights:
                message = AppResource.getString(context.getApplicationContext(), R.string.rv_no_rights);
                break;
            case e_ErrRightsExpired:
                message = AppResource.getString(context.getApplicationContext(), R.string.rv_rights_expired);
                break;
            default:
                break;
        }
        return message;
    }

    public static Matrix2D toMatrix2D(Matrix matrix) {
        if (matrix == null) return null;
        float[] values = new float[9];
        matrix.getValues(values);
        return new Matrix2D(values[0], values[3], values[1], values[4], values[2], values[5]);
    }

    //https://developer.android.com/training/system-ui/immersive.html?hl=zh-cn#java
    public static void hideSystemUI(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (activity != null) {
                // Enables regular immersive mode.
                // For "lean back" mode, remove SYSTEM_UI_FLAG_IMMERSIVE.
                // Or for "sticky immersive," replace it with SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                activity.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE
                                // Set the content to appear under the system bars so that the
                                // content doesn't resize when the system bars hide and show.
                                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                                // Hide the nav bar and status bar
                                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                | View.SYSTEM_UI_FLAG_FULLSCREEN);
            }
        }
    }

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    public static void showSystemUI(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            if (activity != null) {
                activity.getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
        }
    }

    public static void normalizePDFRect(RectF rectF) {
        if (rectF.left > rectF.right) {
            float tmp = rectF.left;
            rectF.left = rectF.right;
            rectF.right = tmp;
        }

        if (rectF.top < rectF.bottom) {
            float tmp = rectF.top;
            rectF.top = rectF.bottom;
            rectF.bottom = tmp;
        }

        if (rectF.left == rectF.right) rectF.right += 1;
        if (rectF.top == rectF.bottom) rectF.top += 1;
    }

    public static PointF getDestinationPoint(PDFViewCtrl viewCtrl, Destination destination) {
        if (destination == null || destination.isEmpty()) {
            return null;
        }

        PointF pt = new PointF(0, 0);
        try {
            switch (destination.getZoomMode()) {
                case Destination.e_ZoomXYZ:
                    pt.x = destination.getLeft();
                    pt.y = destination.getTop();
                    break;
                case Destination.e_ZoomFitHorz:
                case Destination.e_ZoomFitBHorz:
                    pt.y = destination.getTop();
                    break;
                case Destination.e_ZoomFitVert:
                case Destination.e_ZoomFitBVert:
                    pt.x = destination.getLeft();
                    break;
                case Destination.e_ZoomFitRect:
                    pt.x = destination.getLeft();
                    pt.y = destination.getBottom();
                    break;
                default:
                    PageBasicInfo pageInfo = viewCtrl.getDoc().getPageBasicInfo(destination.getPageIndex(viewCtrl.getDoc()));
                    pt.x = 0;
                    pt.y = pageInfo.getHeight();
                    break;
            }
        } catch (PDFException e) {
            e.printStackTrace();
        }
        return pt;
    }

    public static RectF calculateRect(RectF desRectF, RectF srcRectF) {
        if (srcRectF.isEmpty()) return desRectF;
        int count = 0;
        if (desRectF.left == srcRectF.left && desRectF.top == srcRectF.top) count++;
        if (desRectF.right == srcRectF.right && desRectF.top == srcRectF.top) count++;
        if (desRectF.left == srcRectF.left && desRectF.bottom == srcRectF.bottom) count++;
        if (desRectF.right == srcRectF.right && desRectF.bottom == srcRectF.bottom) count++;
        RectF tmpDesRect = new RectF(desRectF);
        if (count == 2) {
            tmpDesRect.union(srcRectF);
            RectF rectF = new RectF(tmpDesRect);
            tmpDesRect.intersect(srcRectF);
            rectF.intersect(tmpDesRect);
            return rectF;
        } else if (count == 3 || count == 4) {
            return tmpDesRect;
        } else {
            tmpDesRect.union(srcRectF);
            return tmpDesRect;
        }
    }

    public static int timeCompare(Date d1, Date d2) {
        if (d1 == null || d2 == null) return 0;
        if (d1.after(d2)) {
            return 1;
        } else if (d1.before(d2)) {
            return -1;
        } else {
            return 0;
        }
    }

}

