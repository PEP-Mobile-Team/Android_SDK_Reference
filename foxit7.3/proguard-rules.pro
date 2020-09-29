# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in \AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

##########################################################################
# Basic directive

-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontskipnonpubliclibraryclassmembers
-dontpreverify
-keepattributes EnclosingMethod
-keepattributes Exceptions
-keepattributes *PDFException*
-keepattributes *Annotation*,InnerClasses
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable
-optimizations !code/simplification/cast,!field/*,!class/merging/*
-verbose

##########################################################################
# Android basic

-keep public class * extends android.app.Activity
-keep public class * extends android.app.Appliction
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.app.backup.BackupAgentHelper
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class com.android.vending.licensing.ILicensingService
-keep class android.support.** {*;}
-keep class **.R$* {*;}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers class * extends android.app.Activity{
    public void *(android.view.View);
}
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}
-keep public class * extends android.view.View{
    *** get*();
    void set*(***);
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
#-keepclasseswithmembers class * {
#    public <init>(android.content.Context, android.util.AttributeSet);
#    public <init>(android.content.Context, android.util.AttributeSet, int);
#}
-keep class * implements android.os.Parcelable {
  public static final android.os.Parcelable$Creator *;
}
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}
-keepclassmembers class * {
    void *(**On*Event);
}
##########################################################################
# androidx
-keep class com.google.android.material.** {*;}
-keep class androidx.** {*;}
-keep public class * extends androidx.**
-keep interface androidx.** {*;}
-dontwarn com.google.android.material.**
-dontnote com.google.android.material.**
-dontwarn androidx.**

##########################################################################
# webView
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}
#-keepclassmembers class * extends android.webkit.webViewClient {
#    public void *(android.webkit.WebView, java.lang.String, android.graphics.Bitmap);
#    public boolean *(android.webkit.WebView, java.lang.String);
#}
#-keepclassmembers class * extends android.webkit.webViewClient {
#    public void *(android.webkit.webView, jav.lang.String);
#}

##########################################################################
# fsdk
-dontwarn com.foxit.sdk.**
-keep class com.foxit.sdk.** { *;}


##########################################################################
# uiextensions
-keep class com.foxit.uiextensions.DocumentManager{
    public *;
    protected *;
}

-keep interface com.foxit.uiextensions.Module{
    public *;
    protected *;
}

-keep class * implements com.foxit.uiextensions.Module {
      public *;
      protected *;
}

-keep interface com.foxit.uiextensions.ToolHandler{
    public *;
    protected *;
}

-keep class * implements com.foxit.uiextensions.ToolHandler {
      public *;
      protected *;
}

-keep class com.foxit.uiextensions.UIExtensionsManager{
    public *;
    protected *;
}

-keep class com.foxit.uiextensions.UIExtensionsManager$**{
    public *;
    protected *;
}

-keep interface com.foxit.uiextensions.UIExtensionsManager$**{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.IPDFReader{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.IPDFReader$BackEventListener{
     public *;
     protected *;
}

##########################################################################
# annots
-keep interface com.foxit.uiextensions.annots.AnnotContent{
    public *;
    protected *;
}

-keep interface com.foxit.uiextensions.annots.AnnotHandler{
    public *;
    protected *;
}

-keep interface com.foxit.uiextensions.annots.AnnotEventListener{
    public *;
    protected *;
}

-keep class * implements com.foxit.uiextensions.annots.AnnotEventListener {
      public *;
      protected *;
}

##########################################################################
# config
-keep class com.foxit.uiextensions.config.Config{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.config.modules.ModulesConfig{
     public void enableAnnotations(boolean);
}

##########################################################################
# controls.dialog
-keep class com.foxit.uiextensions.controls.dialog.**{
     public *;
     protected *;
}

##########################################################################
# controls.menu
-keep interface com.foxit.uiextensions.controls.menu.IMenuView{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MenuGroup{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MenuItem{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.menu.MenuViewCallback{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MenuViewImpl{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.menu.MenuViewImpl$MenuCallback{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MoreMenuConfig{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MoreMenuModule{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.menu.MoreMenuView{
     public *;
     protected *;
}

##########################################################################
# controls.panel
-keep interface com.foxit.uiextensions.controls.panel.PanelHost{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.panel.PanelSpec{
     public *;
     protected *;
}

-keep enum com.foxit.uiextensions.controls.panel.PanelSpec$PanelType{
     public *;
     protected *;
}

##########################################################################
# controls.panel.impl
-keep class com.foxit.uiextensions.controls.panel.impl.PanelHostImpl{
     public *;
     protected *;
}

##########################################################################
# controls.propertybar
-keep interface com.foxit.uiextensions.controls.propertybar.IMultiLineBar{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.propertybar.IMultiLineBar$IValueChangeListener{
     public *;
     protected *;
}

##########################################################################
# controls.toolbar
-keep interface com.foxit.uiextensions.controls.toolbar.BaseBar{
     public *;
     protected *;
}

-keep enum com.foxit.uiextensions.controls.toolbar.BaseBar$TB_Position{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.CircleItem{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBarsHandler{
     public *;
     protected *;
}

-keep enum com.foxit.uiextensions.controls.toolbar.IBarsHandler$BarName{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBarsHandler$IItemClickListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBaseItem{
     public *;
     protected *;
}

-keep enum com.foxit.uiextensions.controls.toolbar.IBaseItem$ItemType{
     public *;
     protected *;
}

-keep enum com.foxit.uiextensions.controls.toolbar.IBaseItem$SortType{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBaseItem$OnItemClickListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBaseItem$OnItemLongPressListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.controls.toolbar.IBaseItem$IResetParentLayoutListener{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.ToolbarItemConfig{
     public *;
     protected *;
}

##########################################################################
# controls.toolbar.impl
-keep class com.foxit.uiextensions.controls.toolbar.impl.BaseBarImpl{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.impl.BaseBarManager{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.impl.BaseItemImpl{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.impl.BottomBarImpl{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.impl.CircleItemImpl{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.controls.toolbar.impl.TopBarImpl{
     public *;
     protected *;
}

##########################################################################
# home
-keep interface com.foxit.uiextensions.home.IHomeModule{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.home.IHomeModule$onFileItemEventListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.home.local.LocalModule$IFinishEditListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.home.local.LocalModule$ICompareListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.home.local.LocalModule$ItemClickListener{
     public *;
     protected *;
}

##########################################################################
# modules
-keep class com.foxit.uiextensions.modules.doc.docinfo.DocInfoView{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.modules.SearchView{
     public *;
     protected *;
}
##########################################################################
# modules.panel
-keep interface com.foxit.uiextensions.modules.panel.IPanelManager{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.modules.panel.IPanelManager$OnShowPanelListener{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.modules.panel.PanelManager{
     public *;
     protected *;
}

# print
-keep interface com.foxit.uiextensions.modules.print.IPrintResultCallback{
     public *;
     protected *;
}

##########################################################################
# pdfreader
-keep interface com.foxit.uiextensions.pdfreader.ILifecycleEventListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.pdfreader.IStateChangeListener{
     public *;
     protected *;
}

-keep interface com.foxit.uiextensions.pdfreader.IMainFrame{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.pdfreader.config.ReadStateConfig{
     public *;
     protected *;
}

-keep class com.foxit.uiextensions.pdfreader.impl.MainFrame{
     public *;
     protected *;
}

##########################################################################
# provider
-keep class com.foxit.uiextensions.provider.FoxitFileProvider{
     public *;
     protected *;
}

##########################################################################
# utils
-keep class com.foxit.uiextensions.utils.**{
     public *;
     protected *;
}

-dontwarn com.edmodo.cropper.**
-keep class com.edmodo.cropper.** {*;}

-dontwarn org.bouncycastle**
-keep class org.bouncycastle.** {*;}

-dontwarn com.foxitsoftware.mobile.**
-keep class com.foxitsoftware.mobile.** {*;}
-keep class com.luratech.** {*;}