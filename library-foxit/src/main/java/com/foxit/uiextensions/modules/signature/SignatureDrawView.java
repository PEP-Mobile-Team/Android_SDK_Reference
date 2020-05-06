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
package com.foxit.uiextensions.modules.signature;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.common.Path;
import com.foxit.sdk.pdf.PSI;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.ToolHandler;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.controls.dialog.UITextEditDialog;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.PropertyCircleItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.PropertyCircleItemImp;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureModule;
import com.foxit.uiextensions.security.digitalsignature.DigitalSignatureUtil;
import com.foxit.uiextensions.security.digitalsignature.IDigitalSignatureCallBack;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class SignatureDrawView {
    private static final int MSG_DRAW = 0x01;
    private static final int MSG_CLEAR = 0x02;
    private static final int MSG_COLOR = 0x04;
    private static final int MSG_DIAMETER = 0x08;
    private static final int MSG_RELEASE = 0x10;
    private boolean mIsFromSignatureField = false;
    private static final int INK_DIAMETER_SCALE = 10;


    public interface OnDrawListener {
        boolean canDraw();

        void moveToTemplate();

        void onBackPressed();

        void result(Bitmap bitmap, Rect rect, int color, String dsgPath);
    }

    public void setOnDrawListener(OnDrawListener listener) {
        mListener = listener;
    }

    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private SignatureToolHandler mToolHandler;
    private OnDrawListener mListener;
    private DrawView mDrawView;

    private View mViewGroup;
    private AppDisplay mDisplay;
    private IBaseItem mBackItem;
    private IBaseItem mTitleItem;
    private IBaseItem mClearItem;
    private IBaseItem mSaveItem;
    private ViewGroup mDrawContainer;
    private PropertyBar mPropertyBar;

    private IBaseItem mCertificateItem;

    private RelativeLayout mSignCreateTopBarLayout;
    private RelativeLayout mSignCreateBottomBarLayout;

    private BaseBar mSignCreateTopBar;
    private BaseBar mSignCreateBottomBar;

    private View mMaskView;

    private String mKey;
    private Bitmap mBitmap;
    private Rect mRect = new Rect();
    private int mBmpHeight;
    private int mBmpWidth;
    private Rect mValidRect = new Rect();
    private Activity mActivity;
    private PSI mPsi = null;


    private boolean mCanDraw = false;

    private DigitalSignatureUtil mDsgUtil;
    private String mCurDsgPath;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            int what = msg.what;
            switch (what) {
                case MSG_DRAW:
                    if (mDrawView == null) return;
                    mCanDraw = true;
                    mDrawView.invalidate();
                    break;
                case MSG_CLEAR:
                    mCanDraw = true;
                    mSaveItem.setEnable(false);
                    mDrawView.invalidate();
                    break;
                case MSG_COLOR:
                    mCanDraw = true;

                    break;
                case MSG_DIAMETER:
                    mCanDraw = true;
                    break;
                case MSG_RELEASE:
                    mCanDraw = false;
                    if (mBitmap != null && mBitmap.isRecycled()) {
                        mBitmap.recycle();
                    }
                    mBitmap = null;
                    break;
                default:
                    break;
            }
        }

        ;
    };

    public SignatureDrawView(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl) {
        mContext = context.getApplicationContext();
        mPdfViewCtrl = pdfViewCtrl;
        mViewGroup = View.inflate(mContext, R.layout.rv_sg_create, null);
        mDisplay = AppDisplay.getInstance(mContext);
        mSignCreateTopBarLayout = (RelativeLayout) mViewGroup.findViewById(R.id.sig_create_top_bar_layout);
        mSignCreateBottomBarLayout = (RelativeLayout) mViewGroup.findViewById(R.id.sig_create_bottom_bar_layout);
        Module dsgModule = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_DIGITALSIGNATURE);
        if (dsgModule != null) {
            mDsgUtil = ((DigitalSignatureModule) dsgModule).getDigitalSignatureUtil();
        }
        SignatureModule sigModule = (SignatureModule)((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PSISIGNATURE);
        if (sigModule != null) {
            mToolHandler = (SignatureToolHandler) sigModule.getToolHandler();
        }
        initBarLayout();
        mDrawContainer = (ViewGroup) mViewGroup.findViewById(R.id.sig_create_canvas);
        mDrawView = new DrawView(mContext);
        mDrawContainer.addView(mDrawView);

    }

    private void initBarLayout() {
        if (mSignCreateTopBar != null) {
            return;
        }
        initTopBar();
        if (mDsgUtil != null) {
            initBottomBar();
        }

        mSignCreateTopBarLayout.addView(mSignCreateTopBar.getContentView());
        if (mDsgUtil != null) {
            mSignCreateBottomBarLayout.addView(mSignCreateBottomBar.getContentView());
        }
    }

    private void initTopBar() {
        mSignCreateTopBar = new SignatureCreateSignTitleBar(mContext);
        mSignCreateTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));

        mBackItem = new BaseItemImpl(mContext);
        mBackItem.setImageResource(R.drawable.rd_sg_back_selector);
        mBackItem.setId(R.id.sig_create_back);
        mBackItem.setOnClickListener(mOnClickListener);

        mClearItem = new BaseItemImpl(mContext);
        mClearItem.setImageResource(R.drawable.rd_sg_clear_selector);
        mClearItem.setId(R.id.sig_create_delete);
        mClearItem.setOnClickListener(mOnClickListener);

        mSaveItem = new BaseItemImpl(mContext);
        mSaveItem.setImageResource(R.drawable.rd_sg_save_selector);
        mSaveItem.setId(R.id.sig_create_save);
        mSaveItem.setOnClickListener(mOnClickListener);

        mPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);
        mProItem = new PropertyCircleItemImp(mContext) {
            @Override
            public void onItemLayout(int l, int t, int r, int b) {
                if (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() != null && ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler().getType() == ToolHandler.TH_TYPE_SIGNATURE) {
                    if (mPropertyBar.isShowing()) {
                        Rect rect = new Rect();
                        mProItem.getContentView().getGlobalVisibleRect(rect);
                        mPropertyBar.update(getView(), new RectF(rect));
                    }
                }
            }
        };
        mProItem.setId(R.id.sig_create_property);
        mProItem.setOnClickListener(mOnClickListener);

        mTitleItem = new BaseItemImpl(mContext);
        mTitleItem.setTextSize(18);
        mTitleItem.setText(AppResource.getString(mContext, R.string.rv_sign_create));
        if (!mDisplay.isPad()) {
            mSignCreateTopBar.setItemInterval(mDisplay.dp2px(16));
        }
        mSignCreateTopBar.addView(mBackItem, BaseBar.TB_Position.Position_LT);
        mSignCreateTopBar.addView(mTitleItem, BaseBar.TB_Position.Position_LT);
        mSignCreateTopBar.addView(mProItem, BaseBar.TB_Position.Position_RB);
        mSignCreateTopBar.addView(mClearItem, BaseBar.TB_Position.Position_RB);
        mSignCreateTopBar.addView(mSaveItem, BaseBar.TB_Position.Position_RB);

    }

    private void initBottomBar() {
        mSignCreateBottomBar = new BottomBarImpl(mContext);
        mSignCreateBottomBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));

        mCertificateItem = new BaseItemImpl(mContext);
        mCertificateItem.setImageResource(R.drawable.sg_cert_add_selector);
        mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_add_text));
        mCertificateItem.setTextSize(18);
        mCertificateItem.setRelation(IBaseItem.RELATION_RIGNT);
        mCertificateItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mDsgUtil.addCertList(new IDigitalSignatureCallBack() {
                    @Override
                    public void onCertSelect(String path, String name) {
                        if (!AppUtil.isEmpty(path) && !AppUtil.isEmpty(name)) {
                            mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text);
                            mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_current_name_title) + name);
                            mCurDsgPath = path;
                        } else {
                            mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text_Image);
                            mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_add_text));
                            mCurDsgPath = null;
                        }
                    }
                });
            }
        });
        mSignCreateBottomBar.addView(mCertificateItem, BaseBar.TB_Position.Position_CENTER);
    }

    private PropertyCircleItem mProItem;

    private void addMask() {
        if (mMaskView == null) {
            mMaskView = mViewGroup.findViewById(R.id.sig_create_mask_layout);
            mMaskView.setBackgroundColor(mContext.getResources().getColor(R.color.ux_color_mask_background));

        }
        mPropertyBar.setDismissListener(mPropertyBarDismissListener);
        mMaskView.setVisibility(View.VISIBLE);
    }

    private void preparePropertyBar() {
        int[] colors = new int[PropertyBar.PB_COLORS_SIGN.length];
        System.arraycopy(PropertyBar.PB_COLORS_SIGN, 0, colors, 0, colors.length);
        colors[0] = PropertyBar.PB_COLORS_SIGN[0];
        mPropertyBar.setColors(colors);
        mPropertyBar.setProperty(PropertyBar.PROPERTY_COLOR, mToolHandler.getColor());
        mPropertyBar.setProperty(PropertyBar.PROPERTY_LINEWIDTH, translate2LineWidth(mToolHandler.getDiameter()));
        mPropertyBar.setArrowVisible(true);
        mPropertyBar.reset(getSupportedProperties());
        mPropertyBar.setPropertyChangeListener(propertyChangeListener);
    }

    private long getSupportedProperties() {
        return PropertyBar.PROPERTY_COLOR
                | PropertyBar.PROPERTY_LINEWIDTH;
    }

    private PropertyBar.DismissListener mPropertyBarDismissListener = new PropertyBar.DismissListener() {
        @Override
        public void onDismiss() {
            if (mMaskView != null) {
                mMaskView.setVisibility(View.INVISIBLE);
            }
        }
    };



    private PropertyBar.PropertyChangeListener propertyChangeListener = new PropertyBar.PropertyChangeListener() {
        @Override
        public void onValueChanged(long property, int value) {
            if (property == PropertyBar.PROPERTY_COLOR) {
                if (value == mToolHandler.getColor()) return;
                mToolHandler.setColor(value);
                setInkColor(value);
                mProItem.setCentreCircleColor(value);
            } else if (property == PropertyBar.PROPERTY_SELF_COLOR) {
                if (value == mToolHandler.getColor()) return;
                mToolHandler.setColor(value);
                setInkColor(value);
                mProItem.setCentreCircleColor(value);
            }
        }

        @Override
        public void onValueChanged(long property, float value) {
            if (property == PropertyBar.PROPERTY_LINEWIDTH) {
                if (mToolHandler.getDiameter() == unTranslate(value)) return;
                float diameter = unTranslate(value);
                mToolHandler.setDiameter(diameter);
                setInkDiameter(diameter);
            }
        }

        @Override
        public void onValueChanged(long property, String value) {

        }
    };

    private float unTranslate(float r) {
        if (r <= 1) {
            r = 1.4999f;
        }
        return (r - 1) / 2;
    }


    private float translate2LineWidth(float d) {
        return (2 * d + 1);
    }

    public View getView() {
        return mViewGroup;
    }


    public void resetLanguage() {
        if (mViewGroup != null) {

            if (mTitleItem != null) {
                mTitleItem.setText(AppResource.getString(mContext, R.string.rv_sign_create));
            }
        }
    }

    public void init(int width, int height, String dsgPath) {
        mBmpWidth = width;
        if (mDisplay.isPad()) {
            mBmpHeight = height - (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad);
        } else {
            mBmpHeight = height - (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_phone);
        }
        mValidRect.set(mDisplay.dp2px(3),
                mDisplay.dp2px(7),
                mBmpWidth - mDisplay.dp2px(3),
                mBmpHeight - mDisplay.dp2px(7));
        mKey = null;
        mRect.setEmpty();
        mSaveItem.setEnable(false);

        com.foxit.uiextensions.config.Config config = ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getConfig();
        if (mToolHandler.getColor() == 0) mToolHandler.setColor(config.uiSettings.signature.color);
        mProItem.setCentreCircleColor(mToolHandler.getColor());

        if (mToolHandler.getDiameter() == 0) {
            float diameter = unTranslate(config.uiSettings.signature.thickness);
            mToolHandler.setDiameter(diameter);
        }

        if (mBitmap == null) {
            try {
                mBitmap = Bitmap.createBitmap(mBmpWidth, mBmpHeight, Config.ARGB_8888);
            } catch (OutOfMemoryError error) {
                error.printStackTrace();
                if (mListener != null) {
                    mListener.onBackPressed();
                }
                return;
            }
        }
        mBitmap.eraseColor(0xFFFFFFFF);
        mCanDraw = false;
        initCanvas();

        mCurDsgPath = dsgPath;
        setCertificateItem(mCurDsgPath);
    }


    public void init(int width, int height, String key, Bitmap bitmap, Rect rect, int color, float diameter, String dsgPath) {
        if (bitmap == null || rect == null) {
            init(width, height, dsgPath);
            return;
        }
        mBmpWidth = width;
        if (mDisplay.isPad()) {
            mBmpHeight = height - (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_pad);
        } else {
            mBmpHeight = height - (int) AppResource.getDimension(mContext, R.dimen.ux_toolbar_height_phone);
        }
        mValidRect.set(mDisplay.dp2px(3),
                mDisplay.dp2px(7),
                mBmpWidth - mDisplay.dp2px(3),
                mBmpHeight - mDisplay.dp2px(7));
        mKey = key;
        mRect.set(rect);
        mSaveItem.setEnable(true);
        if (mBitmap != null) {
            if (!mBitmap.isRecycled()) mBitmap.recycle();
            mBitmap = null;
        }
        int[] colors;
        try {
            mBitmap = Bitmap.createBitmap(mBmpWidth, mBmpHeight, Config.ARGB_8888);
            colors = new int[mBmpWidth * mBmpHeight];
        } catch (OutOfMemoryError error) {
            error.printStackTrace();
            if (mListener != null) {
                mListener.onBackPressed();
            }
            return;
        }
        try {
            bitmap.getPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, mBmpHeight);
            mBitmap.setPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, mBmpHeight);
        } catch (Exception e) {
            int oldVerBmpHeight = height - mDisplay.dp2px(80);//for supper old version
            if (oldVerBmpHeight > bitmap.getHeight()) {
                bitmap.getPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, bitmap.getHeight());
                mBitmap.setPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, bitmap.getHeight());
            } else {
                bitmap.getPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, oldVerBmpHeight);
                mBitmap.setPixels(colors, 0, mBmpWidth, 0, 0, mBmpWidth, oldVerBmpHeight);
            }

        }
        bitmap.recycle();
        bitmap = null;
        mToolHandler.setColor(color);
        mProItem.setCentreCircleColor(color);
        mToolHandler.setDiameter(diameter);
        mCanDraw = false;
        initCanvas();

        mCurDsgPath = dsgPath;
        setCertificateItem(mCurDsgPath);
    }

    public void unInit() {
        releaseCanvas();
        mPropertyBar.setDismissListener(null);
    }

    private void setCertificateItem(String dsgPath) {
        if (mDsgUtil == null) {
            return;
        }

        if (mCertificateItem != null) {
            if (!AppUtil.isEmpty(dsgPath)) {
                mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text);
                File file = new File(dsgPath);
                mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_current_name_title) + file.getName());
            } else {
                mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text_Image);
                mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_add_text));
            }
        }
    }

    private OnClickListener mOnClickListener = new OnClickListener() {

        @Override
        public void onClick(View v) {
            if (AppUtil.isFastDoubleClick()) return;
            int id = v.getId();
            if (R.id.sig_create_back == id) {
                if (mListener != null) {
                    mListener.onBackPressed();
                }
                return;
            }
            if (R.id.sig_create_property == id) {
                preparePropertyBar();
                addMask();
                Rect rect = new Rect();
                mProItem.getContentView().getGlobalVisibleRect(rect);
                mPropertyBar.show(getView(), new RectF(rect), false);
            }
            if (R.id.sig_create_delete == id) {
                clearCanvas();
                return;
            }
            if (R.id.sig_create_save == id) {
                if (mDrawView == null || mDrawView.getBmp() == null) return;

                saveSign();
                if (!mIsFromSignatureField){
                    if (!(((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getCurrentToolHandler() instanceof SignatureToolHandler)) {
                        ((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).setCurrentToolHandler(((SignatureModule) (((UIExtensionsManager) mPdfViewCtrl.getUIExtensionsManager()).getModuleByName(Module.MODULE_NAME_PSISIGNATURE))).getToolHandler());
                    }
                }
            }

        }
    };

    private void saveSign() {
        if (mIsFromSignatureField && TextUtils.isEmpty(mCurDsgPath)){

            if (mActivity != null){
                final UITextEditDialog dialog = new UITextEditDialog(mActivity);
                dialog.getInputEditText().setVisibility(View.GONE);
                dialog.setTitle(AppResource.getString(mContext, R.string.fx_string_warning));
                dialog.getPromptTextView().setText(AppResource.getString(mContext, R.string.sg_cert_add_text));
                dialog.getCancelButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                });
                dialog.getOKButton().setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mDsgUtil.addCertList(new IDigitalSignatureCallBack() {
                            @Override
                            public void onCertSelect(String path, String name) {
                                if (!AppUtil.isEmpty(path) && !AppUtil.isEmpty(name)) {
                                    mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text);
                                    mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_current_name_title) + name);
                                    mCurDsgPath = path;
                                } else {
                                    mCertificateItem.setDisplayStyle(IBaseItem.ItemType.Item_Text_Image);
                                    mCertificateItem.setText(AppResource.getString(mContext, R.string.sg_cert_add_text));
                                    mCurDsgPath = null;
                                }
                            }
                        });
                        dialog.dismiss();
                    }
                });
                dialog.show();
            } else {
                mSaveItem.setEnable(false);
            }
            return;
        }

        Bitmap bitmap = mDrawView.getBmp();
        if (mKey == null) {
            SignatureDataUtil.insertData(mContext, bitmap, mRect, mToolHandler.getColor(), mToolHandler.getDiameter(), mCurDsgPath);
        } else {
            SignatureDataUtil.updateByKey(mContext, mKey, bitmap, mRect, mToolHandler.getColor(), mToolHandler.getDiameter(), mCurDsgPath);
        }
        if (mListener != null) {
            mListener.result(bitmap, mRect, mToolHandler.getColor(), mCurDsgPath);
        }
    }

    private void adjustCanvasRect() {
        if (mBitmap == null) return;
        if (mRect.left < 0) mRect.left = 0;
        if (mRect.top < 0) mRect.top = 0;
        if (mRect.right > mBmpWidth) mRect.right = mBmpWidth;
        if (mRect.bottom > mBmpHeight) mRect.bottom = mBmpHeight;
    }

    private void initCanvas() {
        if (mBitmap == null) return;

        try {
            mPsi = new PSI(mBitmap, true);
            mPsi.setColor(mToolHandler.getColor());
            int diameter = (int) (mToolHandler.getDiameter() * INK_DIAMETER_SCALE);
            if(diameter == 0) {
                diameter = 1;
            }
            mPsi.setDiameter(diameter);
            mPsi.setOpacity(1f);

            mHandler.sendEmptyMessage(MSG_DRAW);
        } catch (PDFException e) {
            e.printStackTrace();
        }

    }

    private void setInkColor(int color) {
        if (mPsi == null || mPsi.isEmpty()) return;
        try {
            mPsi.setColor(color);
            mHandler.sendEmptyMessage(MSG_COLOR);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void setInkDiameter(float diameter) {
        if (mPsi == null || mPsi.isEmpty()) return;
        try {
            int tmp = (int) (diameter * INK_DIAMETER_SCALE);
            if(tmp == 0) {
                tmp = 1;
            }
            mPsi.setDiameter(tmp);

            mHandler.sendEmptyMessage(MSG_DIAMETER);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void clearCanvas() {
        mBitmap.eraseColor(0xFFFFFFFF);
        mHandler.sendEmptyMessage(MSG_CLEAR);
    }

    private void addPoint(final List<PointF> points, final List<Float> pressures, final int flag) {
        try {
            if (mPsi == null || mPsi.isEmpty()) return;
            for (int i = 0; i < points.size(); i++) {
                PointF point = points.get(i);
                Float pressure = pressures!=null?pressures.get(i):1.0f;
                mPsi.addPoint(new com.foxit.sdk.common.fxcrt.PointF(point.x, point.y), flag, pressure);
            }

            com.foxit.sdk.common.fxcrt.RectF rect = mPsi.getContentsRect();
            Rect contentRect = new Rect((int) rect.getLeft(), (int) rect.getTop(), (int) (rect.getRight() + 0.5), (int) (rect.getBottom() + 0.5));
            if (mRect.isEmpty())
                mRect.set(contentRect);
            else
                mRect.union(contentRect);

            adjustCanvasRect();
            mSaveItem.setEnable(true);
            mDrawView.invalidate(contentRect);
        } catch (PDFException e) {
            e.printStackTrace();
        }
    }

    private void releaseCanvas() {
        mHandler.sendEmptyMessage(MSG_RELEASE);
    }

    class DrawView extends View {

        private Paint mPaint;

        public DrawView(Context context) {
            super(context);
            this.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            if (Build.VERSION.SDK_INT >= 11) {
                this.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            }
            mPaint = new Paint();
            mPaint.setAntiAlias(true);
            mPaint.setFilterBitmap(true);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mPsi != null && !mPsi.isEmpty()) {
                canvas.drawBitmap(mBitmap, 0, 0, mPaint);
            }
        }

        private float getDistanceOfTwoPoint(PointF p1, PointF p2) {
            return (float) Math.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (!mCanDraw || mListener == null || !mListener.canDraw()) return false;
            int count = event.getPointerCount();
            PointF point = new PointF(event.getX(), event.getY());
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    if (mValidRect.contains((int) event.getX(), (int) event.getY())) {
                        List<PointF> points = new ArrayList<PointF>();
                        points.add(point);
                        addPoint(points, null, Path.e_TypeMoveTo);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mValidRect.contains((int) event.getX(), (int) event.getY())) {
                            List<PointF> points = new ArrayList<PointF>();
                            points.add(point);
                            addPoint(points, null, Path.e_TypeLineTo);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    List<PointF> points = new ArrayList<PointF>();
                    points.add(point);
                    addPoint(points, null, Path.e_TypeLineToCloseFigure);
                    break;
                case MotionEvent.ACTION_CANCEL:
                default:
                    break;
            }
            return true;
        }

        public Bitmap getBmp() {
            Bitmap bitmap = null;
            if (mBitmap != null) {
                bitmap = Bitmap.createBitmap(mBitmap);
            }
            return bitmap;
        }
    }

    public void setIsFromSignatureField(boolean isFromSignatureField) {
        mIsFromSignatureField = isFromSignatureField;
    }

    public void setActivity(Activity activity) {
        mActivity = activity;
    }

}
