package bg.nijel.xswiftkey.helpers;

import java.lang.reflect.Field;

public class Swiftkey {

    private static String[] classSources = {
            "ThemeManager.java",
            "AssetThemeHeader.java",
            "PreInstalledThemeHeader.java",
            "ThemeHeader.java",
            "DownloadedThemeHeader.java",
            "ThemeStorage.java",
            "ImmutableMap.java",
            "ThemesListAdapter.java"};

    private static String[] methodsArgs = {
            "b[Landroid/content/Context;]",
            "h[Landroid/content/Context;]",
            "n[Landroid/content/Context;]",
            "p[Landroid/content/Context;]",
            "q[Landroid/content/Context;]",
            "r[Landroid/content/Context;]",
            "i[Landroid/content/Context;]",
            "a[Landroid/content/Context;, Lcom/touchtype/keyboard/theme/k;]",
            "a[Lcom/touchtype/keyboard/theme/d;, Landroid/content/Context;]",
            "a[Landroid/content/Context;, Lcom/touchtype/keyboard/h/k;]",
            "a[Lcom/touchtype/keyboard/h/d;, Landroid/content/Context;]",
            "f[Landroid/content/Context;]",
            "a[Landroid/content/Context;, Lcom/touchtype/themes/e/a;]",
            "d[Landroid/content/Context;]",
            "a[Landroid/util/DisplayMetrics;]",
            "b[Ljava/lang/Object;, Ljava/lang/Object;]",
            "onClick[Landroid/view/View;]"};

    //* Swiftkey package names
    public static final String PACKAGE_NAME = "com.touchtype.swiftkey";
    public static final String PACKAGE_NAME_BETA = "com.touchtype.swiftkey.beta";

    //* classes and methods we are interested in
    private static final String CLASS_ThemeManager = null/*"com.touchtype.keyboard.theme.n"*/;
    private static final String METHOD_THEME_FOLDER = "b";  // args [Context.class]
    private static final String METHOD_THEMELIST = "h";  // args [Context.class]
    private static final String METHOD_ASSETS_THEMES = "q";  // args [Context.class]
    private static final String METHOD_STORE_THEMES = "n";  // args [Context.class]
    private static final String METHOD_PREINSTALLED_THEMES = "p";  // args [Context.class]
    private static final String METHOD_DELETE_THEMES = "r";  // args [Context.class]
    private static final String METHOD_DELETE_THEMELIST = "i";  // args [Context.class]
    private static final String METHOD_EMPTY_THEMELIST = "a";  // args [Context.class, CLASS_THEME_HEADER]
    private static final String METHOD_UNZIP_THEME = "a";  // args [CLASS_DOWNLOADED_THEME_HEADER, Context.class]

    private static final String CLASS_AssetThemeHeader = null/*"com.touchtype.keyboard.theme.a"*/;

    private static final String CLASS_PreInstalledThemeHeader = null/*"com.touchtype.keyboard.theme.h"*/;


    private static final String CLASS_ThemeHeader = null/*"com.touchtype.keyboard.theme.k"*/;

    private static final String CLASS_DownloadedThemeHeader = null/*"com.touchtype.keyboard.theme.d"*/;
    private static final String METHOD_APPLY_THEME_FOLDER = "f";  // args [Context.class]
    private static final String METHOD_STORE_THEME_TUMBNAIL = "a";  // args [Context.class, CLASS_THEME_STORAGE]
    private static final String METHOD_PREINSTALLED_THEME_TUMBNAIL = "d";  // args [Context.class]

    private static final String CLASS_ThemeStorage = null/*"com.touchtype.themes.e.a"*/;

    private static final String CLASS_ThemeStorage_A = null/*"com.touchtype.themes.e.a.a"*/;
    private static final String METHOD_DPI_THEME_SUBFOLDER = "a";  // args [DisplayMetrics.class]

    private static final String CLASS_ImmutableMap_A = null/*"com.google.common.collect.av.a"*/;
    private static final String METHOD_THEMES_SET = "b";  // args [Object.class, Object.class]

    private static final String CLASS_ThemesListAdapter = null/*"com.touchtype.materialsettings.themessettings.e"*/;
    private static final String METHOD_ON_CLICK = "onClick";  // args [View.class]

    //* string resources we are interested in
    public static final String LETTER_KEY_BOTTOM_TEXT_SCALE = "letter_key_bottom_text_scale";
    public static final String LETTER_KEY_MAIN_TEXT_HEIGHT = "letter_key_main_text_height";
    public static final String LETTER_PREVIEW_POPUP_TEXT_SCALE = "letter_preview_popup_text_scale";
    public static final String THEMES_CURRENT_TITLE = "themes_current_title";

    public static String getClassNameForPackage(String clazz) {
        try {
            Field field = Swiftkey.class.getDeclaredField("CLASS_" + clazz);
            clazz = (String) field.get(field);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return clazz;
    }

    public static String getMethodNameForPackage(String method) {
        try {
            Field field = Swiftkey.class.getDeclaredField("METHOD_" + method);
            method = (String) field.get(field);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return method;
    }

    public static boolean setClassFieldFor(String[] fieldValue) {
        try {
            Field field = Swiftkey.class.getDeclaredField("CLASS_" + fieldValue[0]);
            field.setAccessible(true);
            field.set(field, fieldValue[1]);
            String clazz = (String) field.get(field);
            return clazz.equals("CLASS_" + fieldValue[0]);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String[] getClassSources() {
        return classSources;
    }


    public static String[] getMethodsArgs() {
        return methodsArgs;
    }
}
