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


import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.foxit.sdk.PDFException;
import com.foxit.sdk.PDFViewCtrl;
import com.foxit.sdk.Task;
import com.foxit.sdk.common.Constants;
import com.foxit.sdk.pdf.PDFDoc;
import com.foxit.sdk.pdf.PDFPage;
import com.foxit.sdk.pdf.TextPage;
import com.foxit.uiextensions.Module;
import com.foxit.uiextensions.R;
import com.foxit.uiextensions.UIExtensionsManager;
import com.foxit.uiextensions.annots.textmarkup.TextSelector;
import com.foxit.uiextensions.controls.propertybar.IMultiLineBar;
import com.foxit.uiextensions.controls.propertybar.PropertyBar;
import com.foxit.uiextensions.controls.propertybar.imp.PropertyBarImpl;
import com.foxit.uiextensions.controls.toolbar.BaseBar;
import com.foxit.uiextensions.controls.toolbar.IBaseItem;
import com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl;
import com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl;
import com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl;
import com.foxit.uiextensions.pdfreader.ILayoutChangeListener;
import com.foxit.uiextensions.pdfreader.ILifecycleEventListener;
import com.foxit.uiextensions.pdfreader.IStateChangeListener;
import com.foxit.uiextensions.pdfreader.config.AppBuildConfig;
import com.foxit.uiextensions.pdfreader.config.ReadStateConfig;
import com.foxit.uiextensions.pdfreader.impl.LifecycleEventListener;
import com.foxit.uiextensions.utils.AppDisplay;
import com.foxit.uiextensions.utils.AppDmUtil;
import com.foxit.uiextensions.utils.AppResource;
import com.foxit.uiextensions.utils.AppUtil;
import com.foxit.uiextensions.utils.IResult;
import com.foxit.uiextensions.utils.thread.AppThreadManager;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class TTSModule implements Module {
    private Context mContext;
    private PDFViewCtrl mPdfViewCtrl;
    private UIExtensionsManager mUiExtensionsManager;

    private TextToSpeech mTTS;
    private HashMap<String, String> mTTSUtteranceIdMap = new HashMap<>();
    private float mTtsSpeechRate;
    private float mTtsPitch = 1.0f;

    private ArrayList<TTSInfo> mTtsRectInfoList = new ArrayList<>();
    private TTSInfo mCurTtsRectInfo = null;
    private ArrayList<String> mCurTtsStrList = new ArrayList<>();
    private ArrayList<Locale> mCurTtsLocalLanguageList = new ArrayList<>();

    private ArrayList<SearchTTSInfoTask> mSearchTextTaskArrays = new ArrayList<>();
    private SparseArray<SparseArray<ArrayList<TTSInfo>>> mTTSInfoArrays = new SparseArray<>();

    private BottomSheetDialog mSpeedRatesSheetDialog;
    private PropertyBar mSpeedPropertyBar;

    private View mSpeedRateView;
    private IMultiLineBar mSettingBar;
    private BaseBar mTtsTopBar;
    private BaseBar mTtsBottomBar;

    private IBaseItem mTtsBackItem;
    private IBaseItem mTtsPreviousPageItem;
    private IBaseItem mTtsNextPageItem;
    private IBaseItem mSpeedItem;
    private IBaseItem mTtsStartItem;
    private IBaseItem mTtsStopItem;
    private IBaseItem mTtsCycleItem;

    private boolean mHasInitTts = false;
    private boolean mSupperTts = false;
    private boolean mIsSpeaking = false;
    private boolean mIsPause = false;
    private boolean mIsCycleSpeak = false;
    private boolean mIsExit = true;

    private int mCurTtsRectPageIndex = -1;

    public TTSModule(Context context, ViewGroup parent, PDFViewCtrl pdfViewCtrl, PDFViewCtrl.UIExtensionsManager uiExtensionsManager) {
        mContext = context;
        mPdfViewCtrl = pdfViewCtrl;
        mUiExtensionsManager = (UIExtensionsManager) uiExtensionsManager;
    }

    @Override
    public String getName() {
        return Module.MODULE_NAME_TTS;
    }

    @Override
    public boolean loadModule() {
        mTtsSpeechRate = 1.0f;
        mSettingBar = mUiExtensionsManager.getMainFrame().getSettingBar();

        initPaint();
        initBars();

        mUiExtensionsManager.registerModule(this);
        mUiExtensionsManager.registerLayoutChangeListener(mLayoutChangeListener);
        mUiExtensionsManager.registerStateChangeListener(mStatusChangeListener);
        mUiExtensionsManager.registerLifecycleListener(mRDLifecycleEventListener);
        mPdfViewCtrl.registerDocEventListener(mDocEventListener);
        mPdfViewCtrl.registerDrawEventListener(mDrawEventListener);
        return true;
    }

    @Override
    public boolean unloadModule() {
        mUiExtensionsManager.unregisterLayoutChangeListener(mLayoutChangeListener);
        mUiExtensionsManager.unregisterStateChangeListener(mStatusChangeListener);
        mUiExtensionsManager.unregisterLifecycleListener(mRDLifecycleEventListener);
        mPdfViewCtrl.unregisterDocEventListener(mDocEventListener);
        mPdfViewCtrl.unregisterDrawEventListener(mDrawEventListener);
        return true;
    }

    private void initBars() {
        initTopBar();
        initBottomBar();

        RelativeLayout.LayoutParams ttsTopLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttsTopLp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
        mUiExtensionsManager.getMainFrame().getContentView().addView(mTtsTopBar.getContentView(), ttsTopLp);

        RelativeLayout.LayoutParams ttsBottomLp = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ttsBottomLp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mUiExtensionsManager.getMainFrame().getContentView().addView(mTtsBottomBar.getContentView(), ttsBottomLp);

        mTtsTopBar.getContentView().setVisibility(View.INVISIBLE);
        mTtsBottomBar.getContentView().setVisibility(View.INVISIBLE);
    }

    private void initTopBar() {
        mTtsTopBar = new TopBarImpl(mContext);
        mTtsTopBar.setBackgroundColor(mContext.getResources().getColor(R.color.ux_bg_color_toolbar_light));

        mTtsBackItem = new BaseItemImpl(mContext);
        mTtsBackItem.setImageResource(R.drawable.rd_reflow_back_selector);
        mTtsBackItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                cancelSearchTask();
                mUiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);
            }
        });

        IBaseItem titleItem = new BaseItemImpl(mContext);
        titleItem.setText(AppResource.getString(mContext.getApplicationContext(), R.string.rd_tts_speak));
        titleItem.setTextSize(AppDisplay.getInstance(mContext).px2dp(mContext.getResources().getDimension(R.dimen.ux_text_height_subhead)));
        titleItem.setTextColor(mContext.getResources().getColor(R.color.ux_text_color_title_dark));

        mTtsTopBar.addView(mTtsBackItem, BaseBar.TB_Position.Position_LT);
        mTtsTopBar.addView(titleItem, BaseBar.TB_Position.Position_CENTER);
    }

    private void initBottomBar() {
        mTtsBottomBar = new BottomBarImpl(mContext);
        if (AppDisplay.getInstance(mContext).isPad()) {
            mTtsBottomBar.setItemInterval(AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_button_space_pad));
        } else {
            mTtsBottomBar.setItemInterval(AppResource.getDimensionPixelSize(mContext, R.dimen.ux_bottombar_button_space_pad));
        }

        mTtsPreviousPageItem = new BaseItemImpl(mContext, R.drawable.rd_speak_previous_page_normal);
        mTtsStartItem = new BaseItemImpl(mContext, R.drawable.rd_speak_start_normal);
        mTtsStopItem = new BaseItemImpl(mContext, R.drawable.rd_speak_stop_normal);
        mSpeedItem = new BaseItemImpl(mContext, _getSpeechRateString(mTtsSpeechRate));
        mSpeedItem.setTextColor(AppResource.getColor(mContext, R.color.ux_color_black, null));
        mSpeedItem.setTextSize(16f);
        mTtsNextPageItem = new BaseItemImpl(mContext, R.drawable.rd_speak_next_page_normal);
        mTtsCycleItem = new BaseItemImpl(mContext, R.drawable.rd_speak_cycle_normal);

        addBottomBarItemListener();

        mTtsBottomBar.addView(mTtsPreviousPageItem, BaseBar.TB_Position.Position_CENTER);
        mTtsBottomBar.addView(mTtsStartItem, BaseBar.TB_Position.Position_CENTER);
        mTtsBottomBar.addView(mTtsStopItem, BaseBar.TB_Position.Position_CENTER);
        mTtsBottomBar.addView(mTtsNextPageItem, BaseBar.TB_Position.Position_CENTER);
        mTtsBottomBar.addView(mTtsCycleItem, BaseBar.TB_Position.Position_CENTER);
        mTtsBottomBar.addView(mSpeedItem, BaseBar.TB_Position.Position_CENTER);
    }

    private String _getSpeechRateString(float rate) {
        if (rate == 1 || rate == 2) {
            return (int) rate + "X";
        } else {
            return rate + "X";
        }
    }

    private void addBottomBarItemListener() {
        mTtsPreviousPageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int pageIndex = mCurTtsRectPageIndex - 1;
                PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
                if (page == null || page.isEmpty())
                    return;

                cancelSearchTask();
                final int startIndex = 0;
                final ArrayList<TTSInfo> ttsInfos = getTTSInfosFromCache(pageIndex, startIndex);
                if (ttsInfos != null) {
                    startSpeak(ttsInfos, pageIndex);
                } else {
                    final SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                        @Override
                        public void onResult(boolean success, ArrayList<TTSInfo> ttsInfos, Object p2, Object p3) {
                            if (success && ttsInfos != null) {
                                saveTTSInfosToCache(pageIndex, startIndex, ttsInfos);
                                startSpeak(ttsInfos, pageIndex);
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(searchTask);
                    mSearchTextTaskArrays.add(searchTask);
                }
            }
        });

        mTtsStartItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onStartItemClicked();
            }
        });

        mTtsStopItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }
                cancelSearchTask();
                onStopItemClicked();
            }
        });

        mSpeedItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AppUtil.isFastDoubleClick()) {
                    return;
                }

                showSpeedPopup();
            }
        });

        mTtsNextPageItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int pageIndex = mCurTtsRectPageIndex + 1;
                PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
                if (page == null || page.isEmpty())
                    return;

                cancelSearchTask();
                final int startIndex = 0;
                final ArrayList<TTSInfo> ttsInfos = getTTSInfosFromCache(pageIndex, startIndex);
                if (ttsInfos != null) {
                    startSpeak(ttsInfos, pageIndex);
                } else {
                    SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                        @Override
                        public void onResult(boolean success, ArrayList<TTSInfo> ttsInfos, Object p2, Object p3) {
                            if (success && ttsInfos != null) {
                                saveTTSInfosToCache(pageIndex, startIndex, ttsInfos);
                                startSpeak(ttsInfos, pageIndex);
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(searchTask);
                    mSearchTextTaskArrays.add(searchTask);
                }
            }
        });

        mTtsCycleItem.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mIsCycleSpeak = !mIsCycleSpeak;
                setAllTtsItemByState();
            }
        });
    }

    public boolean isSupperTts() {
        if (mHasInitTts)
            return mSupperTts;
        else
            return true;
    }

    private boolean mTsChangeStatus;
    private boolean mbOnlySelect;

    public void speakFromTs(TTSInfo info) {
        if (mTTS != null && mTTS.isSpeaking()) {
            mTTS.stop();
            resetState();
            resetAllTtsItem();
            mPdfViewCtrl.invalidate();
        }

        if (mUiExtensionsManager.getState() != ReadStateConfig.STATE_TTS) {
            mTsChangeStatus = true;
            mUiExtensionsManager.changeState(ReadStateConfig.STATE_TTS);
            if (!mUiExtensionsManager.getMainFrame().isToolbarsVisible())
                mUiExtensionsManager.getMainFrame().showToolbars();
        }
        mCurTtsRectPageIndex = info.mPageIndex;
        mTtsRectInfoList.clear();

        setAllTtsItemByState();
        parsingTtsRectInfo(info.mText);
        mCurTtsRectInfo = info;
        speakStringAfterParsing();
        mbOnlySelect = true;
    }

    public void speakFromTp(TTSInfo info) {
        if (mTTS != null && mTTS.isSpeaking()) {
            mTTS.stop();
            resetState();
            resetAllTtsItem();
            mPdfViewCtrl.invalidate();
        }

        if (mUiExtensionsManager.getState() != ReadStateConfig.STATE_TTS) {
            mTsChangeStatus = true;
            mUiExtensionsManager.changeState(ReadStateConfig.STATE_TTS);
            if (!mUiExtensionsManager.getMainFrame().isToolbarsVisible())
                mUiExtensionsManager.getMainFrame().showToolbars();
        }
        final int pageIndex = info.mPageIndex;
        PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
        if (page == null || page.isEmpty()) return;

        cancelSearchTask();
        final int startIndex = info.mStart;
        final ArrayList<TTSInfo> infos = getTTSInfosFromCache(pageIndex, startIndex);
        if (infos != null) {
            if (infos.size() != 0) {
                mTtsRectInfoList.addAll(infos);
                mCurTtsRectPageIndex = pageIndex;

                if (mTtsRectInfoList.size() > 0) {
                    mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                    parsingTtsRectInfo(mCurTtsRectInfo.mText);
                    speakStringAfterParsing();
                    setAllTtsItemByState();
                    mPdfViewCtrl.invalidate();
                }
            }
        } else {
            SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                @Override
                public void onResult(boolean success, ArrayList<TTSInfo> ttsInfos, Object p2, Object p3) {
                    if (success && ttsInfos != null) {
                        if (ttsInfos.size() != 0) {
                            mTtsRectInfoList.addAll(ttsInfos);
                            mCurTtsRectPageIndex = pageIndex;

                            if (mTtsRectInfoList.size() > 0) {
                                mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                                parsingTtsRectInfo(mCurTtsRectInfo.mText);
                                speakStringAfterParsing();
                                setAllTtsItemByState();
                                mPdfViewCtrl.invalidate();
                            }
                        }
                        saveTTSInfosToCache(pageIndex, startIndex, ttsInfos);
                    }
                }
            });
            mPdfViewCtrl.addTask(searchTask);
            mSearchTextTaskArrays.add(searchTask);
        }

    }

    private void setAllTtsItemByState() {
        if (mIsSpeaking) {
            if (mIsPause) {
                mTtsStartItem.setImageResource(R.drawable.rd_speak_start_normal);
                muteAudioFocus(mContext, false);
            } else {
                mTtsStartItem.setImageResource(R.drawable.rd_speak_paused_normal);
                muteAudioFocus(mContext, true);
            }
            if (mCurTtsRectPageIndex > 0) {
                mTtsPreviousPageItem.setEnable(true);
            } else {
                mTtsPreviousPageItem.setEnable(false);
            }
            if (mPdfViewCtrl.getDoc() != null && mCurTtsRectPageIndex < mPdfViewCtrl.getPageCount() - 1) {
                mTtsNextPageItem.setEnable(true);
            } else {
                mTtsNextPageItem.setEnable(false);
            }

            mTtsStartItem.setEnable(true);
            mTtsStopItem.setEnable(true);
        } else {
            mTtsStartItem.setImageResource(R.drawable.rd_speak_start_normal);
            mTtsPreviousPageItem.setEnable(false);
            mTtsNextPageItem.setEnable(false);
            mTtsStopItem.setEnable(false);
            muteAudioFocus(mContext, false);
        }
        if (mIsCycleSpeak) {
            mTtsCycleItem.setChecked(true);
        } else {
            mTtsCycleItem.setChecked(false);
        }
    }

    private void onStartItemClicked() {
        if (mIsSpeaking && !mIsPause) {
            mIsPause = true;
            mTTSUtteranceIdMap.clear();
            mTTS.stop();
            setAllTtsItemByState();
            mPdfViewCtrl.invalidate();
        } else if (mbOnlySelect) {
            if (mCurTtsRectInfo != null && (mCurTtsStrList.size() == 0 || mCurTtsStrList.size() != mCurTtsLocalLanguageList.size())) {
                parsingTtsRectInfo(mCurTtsRectInfo.mText);
            }
            speakStringAfterParsing();
        } else if (mIsPause && mCurTtsRectInfo != null) {
            parsingTtsRectInfo(mCurTtsRectInfo.mText);
            speakStringAfterParsing();
        } else {
            startSpeak();
        }
    }

    private void onStopItemClicked() {
        mTtsRectInfoList.clear();
        mIsExit = true;
        if (mTTS != null && (mTTS.isSpeaking() || mIsPause)) {
            mbOnlySelect = false;
            mTTSUtteranceIdMap.clear();
            mTTS.stop();
            mIsSpeaking = false;
            mIsPause = false;
            mCurTtsRectInfo = null;
            mCurTtsStrList.clear();
            mCurTtsLocalLanguageList.clear();
            mCurTtsRectPageIndex = -1;
            setAllTtsItemByState();
            mPdfViewCtrl.invalidate();
        }
    }

    private boolean muteAudioFocus(Context context, boolean bStopOtherMusic) {
        if (context == null) {
            return false;
        }
        boolean bool;
        AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (bStopOtherMusic) {
            int result = am.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } else {
            int result = am.abandonAudioFocus(null);
            bool = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        }
        return bool;
    }

    public boolean onKeyBack() {
        if (mUiExtensionsManager.getState() == ReadStateConfig.STATE_TTS) {
            cancelSearchTask();
            onStopItemClicked();
            mUiExtensionsManager.changeState(ReadStateConfig.STATE_NORMAL);
            return true;
        }
        return false;
    }

    private void parsingTtsRectInfo(String ttsString) {
        if (!AppUtil.isEmpty(ttsString)) {
            mCurTtsStrList.clear();
            mCurTtsLocalLanguageList.clear();
            ArrayList<Integer> codePageList = new ArrayList<>();
            TTSUtils.splitSentenceByLanguage(ttsString, mCurTtsStrList, codePageList);
            for (int codePage : codePageList) {
                mCurTtsLocalLanguageList.add(getLocaleLanguageByPageCode(codePage));
            }
        }
    }

    private Locale getLocaleLanguageByPageCode(int pageCode) {
        Locale locale;
        switch (pageCode) {
            case 874:// Thai
                locale = new Locale("th", "TH", "TH");
                break;
            case 932:// Shift JIS
                locale = Locale.JAPANESE;
                break;
            case 936:// Chinese Simplified (GBK)
                locale = Locale.CHINA;
                break;
            case 949:// Korean
                locale = Locale.KOREAN;
                break;
            case 950:// Big5
                locale = Locale.TAIWAN;
                break;
//            case 1250:// Eastern European
//                locale = Locale.Eastern_European;
//                break;
            case 1251:// Cyrillic
                locale = new Locale("hr", "HR", "HR");
                break;
            case 1253:// Greek
                locale = new Locale("el", "GR", "GR");
                break;
            case 1254:// Turkish
                locale = new Locale("tr", "TR", "TR");
                break;
            case 1255:// Hebrew
                locale = new Locale("he", "IL", "IL");
                break;
            case 1256:// Arabic
                locale = new Locale("ar");
                break;
//            case 1257:// Baltic
//                locale = Locale.Baltic;
//                break;
            case 1258:// Vietnamese vi_VN
                locale = new Locale("vi", "VN", "VN");
                break;
//            case 1361://Johab
//                locale = Locale.Johab;
//                break;
            case 10001:// Mac Shift Jis
                locale = Locale.JAPANESE;
                break;
            case 10003:// Mac Korean
                locale = Locale.KOREAN;
                break;
            case 10008:// // Mac Chinese Simplified (GBK)
                locale = Locale.CHINA;
                break;
            case 10002:// Mac Big5
                locale = Locale.TAIWAN;
                break;
            case 10005:// Mac Hebrew
                locale = new Locale("he", "IL", "IL");
                break;
            case 10004:// Mac Arabic
                locale = new Locale("ar");
                break;
            case 10006:// Mac Greek
                locale = new Locale("el", "GR", "GR");
                break;
            case 10081:// Mac Turkish
                locale = new Locale("tr", "TR", "TR");
                break;
            case 10021:// Mac Thai
                locale = new Locale("th", "TH", "TH");
                break;
//            case 10029:// Mac Eastern European (Latin 2)
//                locale = Locale.Eastern_European;
//                break;
            case 10007:// Mac Cyrillic
                locale = new Locale("hr", "HR", "HR");
                break;
            default:
                return getDefaultLocaleLanguage();
        }
        if (locale != null && mTTS != null) {
            int ret = mTTS.isLanguageAvailable(locale);
            if (ret == TextToSpeech.LANG_MISSING_DATA || ret == TextToSpeech.LANG_NOT_SUPPORTED) {
                return getDefaultLocaleLanguage();
            } else {
                return locale;
            }
        } else {
            return getDefaultLocaleLanguage();
        }
    }

    private Locale getDefaultLocaleLanguage() {
        if (mTTS != null) {
            Locale locale = mUiExtensionsManager.getAttachedActivity().getApplicationContext().getResources().getConfiguration().locale;
            if (AppBuildConfig.SDK_VERSION >= 18) {
                locale = mTTS.getDefaultLanguage();
            }
            int ret = mTTS.isLanguageAvailable(locale);
            if (ret != TextToSpeech.LANG_MISSING_DATA && ret != TextToSpeech.LANG_NOT_SUPPORTED) {
                return locale;
            }
        }
        return Locale.US;
    }

    private boolean speakStringAfterParsing() {
        if (mTTS == null) {
            return false;
        }
        if (mCurTtsStrList.size() != 0 && mCurTtsStrList.size() == mCurTtsLocalLanguageList.size()) {
            int ret = mTTS.setLanguage(mCurTtsLocalLanguageList.remove(0));
            if (ret == TextToSpeech.LANG_MISSING_DATA || ret == TextToSpeech.LANG_NOT_SUPPORTED) {
                mCurTtsStrList.remove(0);
                return false;
            }
            speak(mCurTtsStrList.remove(0));
            return true;
        } else {
            return false;
        }
    }

    private void speak(String things) {
        mIsSpeaking = true;
        mIsPause = false;
        mIsExit = false;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
            mTTS.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {
                    AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                        @Override
                        public void run() {
                            setAllTtsItemByState();
                            mPdfViewCtrl.invalidate();
                        }
                    });
                }

                @Override
                public void onDone(String utteranceId) {
                    if (!AppUtil.isEmpty(utteranceId) && utteranceId.equals(mTTSUtteranceIdMap.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))) {
                        AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                mTTSUtteranceIdMap.clear();
                                if (mIsExit) {
                                    mIsSpeaking = false;
                                    mIsPause = false;
                                    mCurTtsRectInfo = null;
                                    mCurTtsStrList.clear();
                                    mCurTtsLocalLanguageList.clear();
                                    mCurTtsRectPageIndex = -1;
                                } else {
                                    speakNextLine();
                                }
                                setAllTtsItemByState();
                                mPdfViewCtrl.invalidate();
                            }
                        });
                    }
                }

                @Override
                public void onError(String utteranceId) {
                    if (!AppUtil.isEmpty(utteranceId) && utteranceId.equals(mTTSUtteranceIdMap.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID))) {
                        AppThreadManager.getInstance().getMainThreadHandler().post(new Runnable() {
                            @Override
                            public void run() {
                                mTTSUtteranceIdMap.clear();
                                if (mIsExit) {
                                    mIsSpeaking = false;
                                    mIsPause = false;
                                    mCurTtsRectInfo = null;
                                    mCurTtsStrList.clear();
                                    mCurTtsLocalLanguageList.clear();
                                    mCurTtsRectPageIndex = -1;
                                } else {
                                    speakNextLine();
                                }
                                setAllTtsItemByState();
                                mPdfViewCtrl.invalidate();
                            }
                        });
                    }
                }
            });
        }
        mTTSUtteranceIdMap.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "UniqueID:" + Math.random());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mTTS.speak(things, TextToSpeech.QUEUE_FLUSH, null, mTTSUtteranceIdMap.get(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID));
        } else {
            mTTS.speak(things, TextToSpeech.QUEUE_FLUSH, mTTSUtteranceIdMap);
        }
    }

    private void speakNextLine() {
        if (mIsPause) {
            return;
        }
        if (speakStringAfterParsing()) {
            return;
        }
        if (mTtsRectInfoList.size() != 0) {
            mCurTtsRectInfo = mTtsRectInfoList.remove(0);
            parsingTtsRectInfo(mCurTtsRectInfo.mText);
            speakStringAfterParsing();
        } else {
            if (mbOnlySelect) {
                if (mIsCycleSpeak) {
                    mCurTtsRectPageIndex = mCurTtsRectInfo.mPageIndex;
                    parsingTtsRectInfo(mCurTtsRectInfo.mText);
                    speakStringAfterParsing();
                } else {
                    mIsSpeaking = false;
                    mIsPause = false;
                    mIsExit = true;
                    mCurTtsRectInfo = null;
                    mCurTtsStrList.clear();
                    mCurTtsLocalLanguageList.clear();
                    mCurTtsRectPageIndex = -1;
                    mbOnlySelect = false;
                }
                return;
            }

            if (mIsCycleSpeak) {
                final int pageIndex = mCurTtsRectPageIndex;
                PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
                if (page == null || page.isEmpty()) return;

                cancelSearchTask();
                final int startIndex = 0;
                final ArrayList<TTSInfo> ttsInfos = getTTSInfosFromCache(pageIndex, startIndex);
                if (ttsInfos != null) {
                    if (ttsInfos.size() != 0) {
                        mTtsRectInfoList.addAll(ttsInfos);
                        mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                        parsingTtsRectInfo(mCurTtsRectInfo.mText);
                        speakStringAfterParsing();
                    } else {
                        mIsSpeaking = false;
                        mIsPause = false;
                        mIsExit = true;
                        mCurTtsRectInfo = null;
                        mCurTtsStrList.clear();
                        mCurTtsLocalLanguageList.clear();
                        mCurTtsRectPageIndex = -1;
                    }
                } else {
                    SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                        @Override
                        public void onResult(boolean success, ArrayList<TTSInfo> ttsInfos, Object p2, Object p3) {
                            if (success && ttsInfos != null) {
                                if (ttsInfos.size() != 0) {
                                    mTtsRectInfoList.addAll(ttsInfos);
                                    mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                                    parsingTtsRectInfo(mCurTtsRectInfo.mText);
                                    speakStringAfterParsing();
                                } else {
                                    mIsSpeaking = false;
                                    mIsPause = false;
                                    mIsExit = true;
                                    mCurTtsRectInfo = null;
                                    mCurTtsStrList.clear();
                                    mCurTtsLocalLanguageList.clear();
                                    mCurTtsRectPageIndex = -1;
                                }

                                saveTTSInfosToCache(pageIndex, startIndex, ttsInfos);
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(searchTask);
                    mSearchTextTaskArrays.add(searchTask);
                }
            } else {
                final int pageIndex = mCurTtsRectPageIndex + 1;
                PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
                if (page == null || page.isEmpty()) {
                    mIsSpeaking = false;
                    mIsPause = false;
                    mIsExit = true;
                    mCurTtsRectInfo = null;
                    mCurTtsStrList.clear();
                    mCurTtsLocalLanguageList.clear();
                    mCurTtsRectPageIndex = -1;
                    return;
                }

                cancelSearchTask();
                final int startIndex = 0;
                final ArrayList<TTSInfo> infos = getTTSInfosFromCache(pageIndex, startIndex);
                if (infos != null) {
                    if (infos.size() != 0) {
                        mCurTtsRectPageIndex = pageIndex;
                        endEditStatus();
                        mPdfViewCtrl.gotoPage(pageIndex, 0, 0);
                        mTtsRectInfoList.addAll(infos);
                        mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                        parsingTtsRectInfo(mCurTtsRectInfo.mText);
                        speakStringAfterParsing();
                    } else {
                        mCurTtsRectPageIndex = pageIndex;
                        speakNextLine();
                    }
                } else {
                    SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                        @Override
                        public void onResult(boolean success, ArrayList<TTSInfo> infos, Object p2, Object p3) {
                            if (success && infos != null) {
                                if (infos.size() != 0) {
                                    mCurTtsRectPageIndex = pageIndex;
                                    endEditStatus();
                                    mPdfViewCtrl.gotoPage(pageIndex, 0, 0);
                                    mTtsRectInfoList.addAll(infos);
                                    mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                                    parsingTtsRectInfo(mCurTtsRectInfo.mText);
                                    speakStringAfterParsing();
                                } else {
                                    mCurTtsRectPageIndex = pageIndex;
                                    speakNextLine();
                                }
                                saveTTSInfosToCache(pageIndex, startIndex, infos);
                            }
                        }
                    });
                    mPdfViewCtrl.addTask(searchTask);
                    mSearchTextTaskArrays.add(searchTask);
                }
            }
        }
    }

    private void endEditStatus() {
        if (mUiExtensionsManager.getCurrentToolHandler() != null) {
            mUiExtensionsManager.setCurrentToolHandler(null);
        }
        if (mUiExtensionsManager.getDocumentManager().getCurrentAnnot() != null) {
            mUiExtensionsManager.getDocumentManager().setCurrentAnnot(null);
        }
    }

    private void registerMLListener() {
        mSettingBar.registerListener(mMulChangeTtsListener);
    }

    private void unRegisterMLListener() {
        mSettingBar.unRegisterListener(mMulChangeTtsListener);
    }

    private boolean mAutoSpeak = false;
    private IMultiLineBar.IValueChangeListener mMulChangeTtsListener = new IMultiLineBar.IValueChangeListener() {

        @Override
        public void onValueChanged(int type, Object value) {
            if (type == IMultiLineBar.TYPE_TTS) {
                if (value instanceof Boolean && (boolean) value) {
                    mAutoSpeak = true;
                    mUiExtensionsManager.changeState(ReadStateConfig.STATE_TTS);
                    if (!mUiExtensionsManager.getMainFrame().isToolbarsVisible())
                        mUiExtensionsManager.getMainFrame().showToolbars();
                }
                mUiExtensionsManager.getMainFrame().hideSettingBar();
            }
        }

        @Override
        public void onDismiss() {
        }

        @Override
        public int getType() {
            return IMultiLineBar.TYPE_TTS;
        }

    };

    private ILayoutChangeListener mLayoutChangeListener = new ILayoutChangeListener() {
        @Override
        public void onLayoutChange(View v, int newWidth, int newHeight, int oldWidth, int oldHeight) {
            if (newWidth != oldWidth || newHeight != oldHeight) {
                if (mSpeedRatesSheetDialog != null && mSpeedRatesSheetDialog.isShowing()) {
                    showBottomSheetDialog();
                }

                if (mSpeedPropertyBar != null && mSpeedPropertyBar.isShowing()) {
                    Rect rect = new Rect();
                    mSpeedItem.getContentView().getGlobalVisibleRect(rect);
                    mSpeedPropertyBar.update(new RectF(rect));
                }
            }
        }
    };

    private IStateChangeListener mStatusChangeListener = new IStateChangeListener() {
        @Override
        public void onStateChanged(int oldState, int newState) {
            onStatusChanged(oldState, newState);
            if (newState == ReadStateConfig.STATE_TTS && oldState != ReadStateConfig.STATE_TTS) {
                initTTS();
            } else if (newState != ReadStateConfig.STATE_TTS && oldState == ReadStateConfig.STATE_TTS) {
                exitTts();
            }
            if (isSupperTts()
                    && mPdfViewCtrl.getDoc() != null
                    && mUiExtensionsManager.getDocumentManager().canCopy()) {
                if (mTtsStartItem != null) {
                    mTtsStartItem.setEnable(true);
                    mSettingBar.enableBar(IMultiLineBar.TYPE_TTS, true);
                }
            } else {
                if (mTtsStartItem != null) {
                    mTtsStartItem.setEnable(false);
                    mSettingBar.enableBar(IMultiLineBar.TYPE_TTS, false);
                }
            }
        }
    };

    private void initTTS() {
        if (mTsChangeStatus) {
            mTsChangeStatus = false;
            mTtsStartItem.setEnable(true);
        } else {
            if (mTTS != null) {
                mTTS.shutdown();
                mTTS = null;
            }
            resetState();
            resetAllTtsItem();
            AppThreadManager.getInstance().getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mTTS = new TextToSpeech(mUiExtensionsManager.getAttachedActivity(), mTextToSpeechOnInitListener);
                }
            }, 200);
        }
    }

    private ILifecycleEventListener mRDLifecycleEventListener = new LifecycleEventListener() {
        @Override
        public void onCreate(Activity act, Bundle savedInstanceState) {
            mTTS = new TextToSpeech(act, mTextToSpeechOnInitListener);
        }

        @Override
        public void onDestroy(Activity act) {
            if (mTTS != null) {
                mTTS.shutdown();
            }
            mTTS = null;
            mIsSpeaking = false;
            mIsPause = false;
            mIsExit = true;
            mTtsRectInfoList.clear();
            mCurTtsRectInfo = null;
            mCurTtsStrList.clear();
            mCurTtsLocalLanguageList.clear();
            mCurTtsRectPageIndex = -1;
        }
    };

    private PDFViewCtrl.IDocEventListener mDocEventListener = new PDFViewCtrl.IDocEventListener() {
        @Override
        public void onDocWillOpen() {
        }

        @Override
        public void onDocOpened(PDFDoc document, int errCode) {
            if (errCode == Constants.e_ErrSuccess) {
                boolean enabled;
                if (isSupperTts()
                        && document != null
                        && !mPdfViewCtrl.isDynamicXFA()
                        && mUiExtensionsManager.getDocumentManager().canCopy()) {
                    enabled = true;
                } else {
                    enabled = false;
                }

                if (mTtsStartItem != null)
                    mTtsStartItem.setEnable(enabled);
                mSettingBar.enableBar(IMultiLineBar.TYPE_TTS, enabled);
                registerMLListener();
            }
        }

        @Override
        public void onDocWillClose(PDFDoc document) {
        }

        @Override
        public void onDocClosed(PDFDoc document, int errCode) {
            cancelSearchTask();
            unRegisterMLListener();
            mTTSInfoArrays.clear();
        }

        @Override
        public void onDocWillSave(PDFDoc document) {
        }

        @Override
        public void onDocSaved(PDFDoc document, int errCode) {
        }
    };

    private RectF tmpRectF;
    private Paint mPaint = new Paint();

    private void initPaint() {
        mPaint.setAntiAlias(true);
        mPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
        mPaint.setColor(AppDmUtil.calColorByMultiply(0x73C1E1, 150));
    }

    private PDFViewCtrl.IDrawEventListener mDrawEventListener = new PDFViewCtrl.IDrawEventListener() {


        @Override
        public void onDraw(int pageIndex, Canvas canvas) {
            if ((mCurTtsRectInfo != null && mCurTtsRectPageIndex == pageIndex)) {
                for (RectF rectF : mCurTtsRectInfo.mRects) {
                    tmpRectF = new RectF(rectF);
                    mPdfViewCtrl.convertPdfRectToPageViewRect(tmpRectF, tmpRectF, pageIndex);
                    canvas.drawRect(tmpRectF, mPaint);
                }
            }
        }
    };

    private TextToSpeech.OnInitListener mTextToSpeechOnInitListener = new TextToSpeech.OnInitListener() {
        @Override
        public void onInit(int status) {
            mHasInitTts = true;

            boolean enabled;
            if (status == TextToSpeech.SUCCESS) {
                try {
                    mTTS.setPitch(mTtsPitch);
                    mTTS.setSpeechRate(mTtsSpeechRate);
                    int ret = mTTS.isLanguageAvailable(Locale.US);
                    if (ret == TextToSpeech.LANG_MISSING_DATA || ret == TextToSpeech.LANG_NOT_SUPPORTED) {
                    } else {
                        ret = mTTS.setLanguage(Locale.US);
                    }
                    if (ret == TextToSpeech.LANG_MISSING_DATA || ret == TextToSpeech.LANG_NOT_SUPPORTED) {
                        mSupperTts = false;
                        enabled = false;
                    } else {
                        mSupperTts = true;
                        enabled = true;
                    }
                } catch (Exception e) {
                    mSupperTts = false;
                    enabled = false;
                }
            } else {
                mSupperTts = false;
                enabled = false;
            }

            if (mTtsStartItem != null) {
                mTtsStartItem.setEnable(enabled);
                mSettingBar.enableBar(IMultiLineBar.TYPE_TTS, enabled);
            }

            if (mAutoSpeak) {
                mAutoSpeak = false;
                startSpeak();
            }
        }
    };

    private void startSpeak() {
        final int pageIndex = mPdfViewCtrl.getCurrentPage();
        PDFPage page = mUiExtensionsManager.getDocumentManager().getPage(pageIndex, false);
        if (page == null || page.isEmpty()) return;

        cancelSearchTask();
        final int startIndex = 0;
        final ArrayList<TTSInfo> infos = getTTSInfosFromCache(pageIndex, startIndex);
        if (infos != null) {
            if (infos.size() != 0) {
                mTtsRectInfoList.addAll(infos);
                mCurTtsRectPageIndex = pageIndex;
                mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                while ((mCurTtsRectInfo == null || AppUtil.isEmpty(mCurTtsRectInfo.mText)) && mTtsRectInfoList.size() != 0) {
                    mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                }
                if (mCurTtsRectInfo == null || AppUtil.isEmpty(mCurTtsRectInfo.mText)) {
                    if (mIsCycleSpeak) {
                        mIsSpeaking = false;
                        mIsPause = false;
                        mIsExit = true;
                        mCurTtsRectPageIndex = -1;
                        mCurTtsRectInfo = null;
                        mCurTtsStrList.clear();
                        mCurTtsLocalLanguageList.clear();
                        setAllTtsItemByState();
                        return;
                    } else {
                        speakNextLine();
                        setAllTtsItemByState();
                        mPdfViewCtrl.invalidate();
                        return;
                    }
                }

                parsingTtsRectInfo(mCurTtsRectInfo.mText);
                speakStringAfterParsing();
                setAllTtsItemByState();
                mPdfViewCtrl.invalidate();
            } else {
                if (!mIsCycleSpeak) {
                    mCurTtsRectPageIndex = pageIndex;
                    speakNextLine();
                    setAllTtsItemByState();
                    mPdfViewCtrl.invalidate();
                }
            }
        } else {
            SearchTTSInfoTask searchTask = new SearchTTSInfoTask(mPdfViewCtrl, page, startIndex, new IResult<ArrayList<TTSInfo>, Object, Object>() {
                @Override
                public void onResult(boolean success, ArrayList<TTSInfo> infos, Object p2, Object p3) {
                    if (success && infos != null) {
                        if (infos.size() != 0) {
                            mTtsRectInfoList.addAll(infos);
                            mCurTtsRectPageIndex = pageIndex;
                            mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                            while ((mCurTtsRectInfo == null || AppUtil.isEmpty(mCurTtsRectInfo.mText)) && mTtsRectInfoList.size() != 0) {
                                mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                            }
                            if (mCurTtsRectInfo == null || AppUtil.isEmpty(mCurTtsRectInfo.mText)) {
                                if (mIsCycleSpeak) {
                                    mIsSpeaking = false;
                                    mIsPause = false;
                                    mIsExit = true;
                                    mCurTtsRectPageIndex = -1;
                                    mCurTtsRectInfo = null;
                                    mCurTtsStrList.clear();
                                    mCurTtsLocalLanguageList.clear();
                                    setAllTtsItemByState();
                                    return;
                                } else {
                                    speakNextLine();
                                    setAllTtsItemByState();
                                    mPdfViewCtrl.invalidate();
                                    return;
                                }
                            }

                            parsingTtsRectInfo(mCurTtsRectInfo.mText);
                            speakStringAfterParsing();
                            setAllTtsItemByState();
                            mPdfViewCtrl.invalidate();
                        } else {
                            if (!mIsCycleSpeak) {
                                mCurTtsRectPageIndex = pageIndex;
                                speakNextLine();
                                setAllTtsItemByState();
                                mPdfViewCtrl.invalidate();
                            }
                        }
                        saveTTSInfosToCache(pageIndex, startIndex, infos);
                    }
                }
            });
            mPdfViewCtrl.addTask(searchTask);
            mSearchTextTaskArrays.add(searchTask);
        }
    }

    private void onStatusChanged(int oldState, int newState) {
        if (mPdfViewCtrl.getDoc() == null) {
            return;
        }
        if (mUiExtensionsManager.getState() == ReadStateConfig.STATE_TTS) {
            if (mUiExtensionsManager.getMainFrame().isToolbarsVisible()) {
                mTtsTopBar.getContentView().startAnimation(mUiExtensionsManager.getMainFrame().getTopbarShowAnimation());
                mTtsBottomBar.getContentView().startAnimation(mUiExtensionsManager.getMainFrame().getBottombarShowAnimation());

                mTtsTopBar.getContentView().setVisibility(View.VISIBLE);
                mTtsBottomBar.getContentView().setVisibility(View.VISIBLE);
            } else {
                if (mTtsTopBar.getContentView().getVisibility() == View.VISIBLE) {
                    mTtsTopBar.getContentView().startAnimation(mUiExtensionsManager.getMainFrame().getTopbarHideAnimation());
                    mTtsTopBar.getContentView().setVisibility(View.INVISIBLE);
                }

                if (mTtsBottomBar.getContentView().getVisibility() == View.VISIBLE) {
                    mTtsBottomBar.getContentView().startAnimation(mUiExtensionsManager.getMainFrame().getBottombarHideAnimation());
                    mTtsBottomBar.getContentView().setVisibility(View.INVISIBLE);
                }
            }
        } else {
            if (mTtsBottomBar != null) {
                mTtsBottomBar.getContentView().setVisibility(View.INVISIBLE);
                mTtsTopBar.getContentView().setVisibility(View.INVISIBLE);
            }
        }
    }

    private void exitTts() {
        mTtsRectInfoList.clear();
        mIsExit = true;
        if (mTTS != null && mTTS.isSpeaking()) {
            mTTS.stop();
        }
        mCurTtsRectInfo = null;
        mCurTtsStrList.clear();
        mCurTtsLocalLanguageList.clear();
        mCurTtsRectPageIndex = -1;
        resetAllTtsItem();
        mAutoSpeak = false;
    }

    private void resetState() {
        mbOnlySelect = false;
        mIsSpeaking = false;
        mIsPause = false;
        mIsExit = true;
        mIsCycleSpeak = false;
        mTtsPitch = 1.0f;
        mCurTtsRectInfo = null;
        mCurTtsStrList.clear();
        mTTSUtteranceIdMap.clear();
        mCurTtsLocalLanguageList.clear();
        mTtsRectInfoList.clear();
        mCurTtsRectPageIndex = -1;
    }

    private void resetAllTtsItem() {
        mTtsStartItem.setImageResource(R.drawable.rd_speak_start_normal);
        mTtsPreviousPageItem.setEnable(false);
        mTtsNextPageItem.setEnable(false);
        mTtsStartItem.setEnable(false);
        mTtsStopItem.setEnable(false);
        if (mIsCycleSpeak) {
            mTtsCycleItem.setChecked(true);
        } else {
            mTtsCycleItem.setChecked(false);
        }
    }

    private void showSpeedPopup() {
        if (AppDisplay.getInstance(mContext).isPad()) {
            if (mSpeedPropertyBar == null) {
                mSpeedPropertyBar = new PropertyBarImpl(mContext, mPdfViewCtrl);

                if (mSpeedRateView == null)
                    mSpeedRateView = getSpeedRateView();
                if (mSpeedRateView.getParent() != null)
                    ((ViewGroup) mSpeedRateView.getParent()).removeAllViews();

                mSheetLine.setVisibility(View.GONE);
                mSpeedPropertyBar.addTab("", 0, AppResource.getString(mContext, R.string.tts_speechrate_title), 0);
                mSpeedPropertyBar.addCustomItem(PropertyBar.PROPERTY_TTS_SPEED_RATES, mSpeedRateView, 0, 0);
            }

            mSpeedPropertyBar.setArrowVisible(true);
            Rect rect = new Rect();
            mSpeedItem.getContentView().getGlobalVisibleRect(rect);
            mSpeedPropertyBar.show(new RectF(rect), true);
        } else {
            if (mSpeedRateView == null)
                mSpeedRateView = getSpeedRateView();
            if (mSpeedRateView.getParent() != null)
                ((ViewGroup) mSpeedRateView.getParent()).removeAllViews();

            mSheetLine.setVisibility(View.VISIBLE);
            mSpeedRatesSheetDialog = new BottomSheetDialog(mUiExtensionsManager.getAttachedActivity());
            mSpeedRatesSheetDialog.setContentView(mSpeedRateView);
            showBottomSheetDialog();
        }
    }

    private void showBottomSheetDialog() {
        if (mSpeedRatesSheetDialog != null && !mSpeedRatesSheetDialog.isShowing()) {
            mSpeedRateView.measure(0, 0);
            int viewHeight = mSpeedRateView.getHeight();
            if (AppDisplay.getInstance(mContext).getScreenHeight() < viewHeight) {
                mSpeedRatesSheetDialog.getBehavior().setPeekHeight(AppDisplay.getInstance(mContext).getScreenHeight() / 2);
            } else {
                mSpeedRatesSheetDialog.getBehavior().setState(BottomSheetBehavior.STATE_EXPANDED);
            }
            mSpeedRatesSheetDialog.show();
        }
    }

    private View mSheetLine;

    private View getSpeedRateView() {
        List<SpeechRateAdapter.SpeechRateItemInfo> rateItemInfos = new ArrayList<>();
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("0.5X", false));
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("0.75X", false));
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("1X", true));
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("1.25X", false));
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("1.5X", false));
        rateItemInfos.add(new SpeechRateAdapter.SpeechRateItemInfo("2X", false));

        View view = View.inflate(mContext, R.layout.tts_speech_rate_view, null);
        mSheetLine = view.findViewById(R.id.tts_sheet_line);
        RecyclerView recyclerView = view.findViewById(R.id.fx_tts_speech_rate_list);
        SpeechRateAdapter adapter = new SpeechRateAdapter(mContext, rateItemInfos);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false));
        recyclerView.setItemAnimator(new DefaultItemAnimator());

        adapter.setLastSelectedIndex(2);
        adapter.setSelectedSpeedCallback(new SpeechRateAdapter.ISelectedSpeedRateCallback() {
            @Override
            public void onItemClick(int position, SpeechRateAdapter.SpeechRateItemInfo itemInfo) {
                String strRate = itemInfo.speedRate;
                mSpeedItem.setText(strRate);
                mTtsSpeechRate = Float.parseFloat(strRate.substring(0, strRate.lastIndexOf("X")));
                mTTS.setSpeechRate(mTtsSpeechRate);
            }
        });
        return view;
    }

    private void startSpeak(final ArrayList<TTSInfo> infos, final int pageIndex) {
        if (infos != null && infos.size() != 0) {
            if (mTTS.isSpeaking()) {
                mTTSUtteranceIdMap.clear();
                mTTS.stop();
            }
            AppThreadManager.getInstance().getMainThreadHandler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mIsSpeaking = false;
                    mTtsRectInfoList.clear();
                    mCurTtsRectPageIndex = pageIndex;
                    endEditStatus();
                    mPdfViewCtrl.gotoPage(pageIndex, 0, 0);
                    mTtsRectInfoList.addAll(infos);
                    mCurTtsRectInfo = mTtsRectInfoList.remove(0);
                    parsingTtsRectInfo(mCurTtsRectInfo.mText);
                    speakStringAfterParsing();
                }
            }, 200);
        } else {
            if (mTTS.isSpeaking()) {
                mTTSUtteranceIdMap.clear();
                mTTS.stop();
            }

            mCurTtsRectInfo = null;
            mIsSpeaking = false;
            mTtsRectInfoList.clear();
            mCurTtsStrList.clear();
            mCurTtsRectPageIndex = pageIndex;
            endEditStatus();
            mPdfViewCtrl.gotoPage(pageIndex, 0, 0);
            setAllTtsItemByState();
            mPdfViewCtrl.invalidate();
        }
    }

    private ArrayList<TTSInfo> getTTSInfosFromCache(int pageIndex, int startIndex) {
        final SparseArray<ArrayList<TTSInfo>> pageTTSInfos = mTTSInfoArrays.get(pageIndex);
        if (pageTTSInfos != null && pageTTSInfos.get(startIndex) != null) {
            return pageTTSInfos.get(startIndex);
        }
        return null;
    }

    private void saveTTSInfosToCache(int pageIndex, int startIndex, ArrayList<TTSInfo> ttsInfos) {
        SparseArray<ArrayList<TTSInfo>> speakInfoArrays = mTTSInfoArrays.get(pageIndex);
        if (speakInfoArrays == null) {
            speakInfoArrays = new SparseArray<>();
            mTTSInfoArrays.put(pageIndex, speakInfoArrays);
        }
        speakInfoArrays.put(startIndex, ttsInfos);
    }

    private class SearchTTSInfoTask extends Task {
        private ArrayList<TTSInfo> ttsInfos;
        private PDFViewCtrl pdfViewCtrl;
        private PDFPage pdfPage;
        private int startIndex;
        private boolean success;
        private boolean cancel;

        public SearchTTSInfoTask(PDFViewCtrl viewCtrl, PDFPage page, int startIndex, final IResult<ArrayList<TTSInfo>, Object, Object> result) {
            super(new CallBack() {
                @Override
                public void result(Task task) {
                    SearchTTSInfoTask searchTask = (SearchTTSInfoTask) task;
                    result.onResult(searchTask.success, searchTask.ttsInfos, null, null);
                }
            });
            this.pdfViewCtrl = viewCtrl;
            this.pdfPage = page;
            this.startIndex = startIndex;
        }

        @Override
        protected void execute() {
            try {
                if (cancel || mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
                    success = false;
                    return;
                }

                ttsInfos = new ArrayList<>();
                if (startIndex < 0)
                    startIndex = 0;

                StringBuilder sb = new StringBuilder();
                TextPage textPage = new TextPage(pdfPage, TextPage.e_ParseTextNormal);
                String content = textPage.getChars(startIndex, textPage.getCharCount() - startIndex + 1);
                char[] charContents = content.toCharArray();
                int length = charContents.length;

                int contentIndex = 0;
                for (int i = contentIndex; i < length; i++) {
                    if (cancel || mPdfViewCtrl == null || mPdfViewCtrl.getDoc() == null) {
                        success = false;
                        return;
                    }

                    char charContent = charContents[i];
                    sb.append(charContent);

                    boolean isEnd = TTSUtils.isEnd(charContent);
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
                        TextSelector ts = new TextSelector(pdfViewCtrl);
                        ts.computeSelected(pdfPage, startIndex + contentIndex, startIndex + i);
                        ttsInfo.mRects.addAll(ts.getRectFList());
                        ttsInfos.add(ttsInfo);

                        //reset index
                        contentIndex = i + 1;
                        sb.setLength(0);
                    }
                }

                success = !cancel && mPdfViewCtrl != null && mPdfViewCtrl.getDoc() != null;
            } catch (PDFException e) {
                success = false;
                e.printStackTrace();
            }
        }

        void cancelTask() {
            cancel = true;
        }
    }

    private void cancelSearchTask() {
        for (SearchTTSInfoTask searchTask : mSearchTextTaskArrays) {
            searchTask.cancelTask();
        }
        mSearchTextTaskArrays.clear();
    }

}
