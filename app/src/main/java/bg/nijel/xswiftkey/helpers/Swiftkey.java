package bg.nijel.xswiftkey.helpers;

import java.lang.reflect.Field;

public class Swiftkey {

    //* Swiftkey package names
    public static final String PACKAGE_NAME = "com.touchtype.swiftkey";
    public static final String PACKAGE_NAME_BETA = "com.touchtype.swiftkey.beta";

    //* classes and methods we are interested in
    private static final String CLASS_THEME_MANAGER = "com.touchtype.keyboard.theme.n";
    private static final String CLASS_THEME_MANAGER_BETA = "com.touchtype.keyboard.h.n";
    private static final String METHOD_THEME_FOLDER = "b";  // args [Context.class]
    private static final String METHOD_THEMELIST = "h";  // args [Context.class]
    private static final String METHOD_ASSETS_THEMES = "q";  // args [Context.class]
    private static final String METHOD_STORE_THEMES = "n";  // args [Context.class]
    private static final String METHOD_PREINSTALLED_THEMES = "p";  // args [Context.class]
    private static final String METHOD_DELETE_THEMES = "r";  // args [Context.class]
    private static final String METHOD_DELETE_THEMELIST = "i";  // args [Context.class]
    private static final String METHOD_EMPTY_THEMELIST = "a";  // args [Context.class, CLASS_THEME_HEADER]
    private static final String METHOD_UNZIP_THEME = "a";  // args [CLASS_DOWNLOADED_THEME_HEADER, Context.class]

    private static final String CLASS_ASSET_THEME_HEADER = "com.touchtype.keyboard.theme.a";
    private static final String CLASS_ASSET_THEME_HEADER_BETA = "com.touchtype.keyboard.h.a";

    private static final String CLASS_PREINSTALLED_THEME_HEADER = "com.touchtype.keyboard.theme.h";
    private static final String CLASS_PREINSTALLED_THEME_HEADER_BETA = "com.touchtype.keyboard.h.h";


    private static final String CLASS_THEME_HEADER = "com.touchtype.keyboard.theme.k";
    private static final String CLASS_THEME_HEADER_BETA = "com.touchtype.keyboard.h.k";

    private static final String CLASS_DOWNLOADED_THEME_HEADER = "com.touchtype.keyboard.theme.d";
    private static final String CLASS_DOWNLOADED_THEME_HEADER_BETA = "com.touchtype.keyboard.h.d";
    private static final String METHOD_APPLY_THEME_FOLDER = "f";  // args [Context.class]
    private static final String METHOD_STORE_THEME_TUMBNAIL = "a";  // args [Context.class, CLASS_THEME_STORAGE]
    private static final String METHOD_PREINSTALLED_THEME_TUMBNAIL = "d";  // args [Context.class]

    private static final String CLASS_THEME_STORAGE = "com.touchtype.themes.e.a";
    private static final String CLASS_THEME_STORAGE_BETA = "com.touchtype.themes.e.a";

    private static final String CLASS_THEME_STORAGE_A = "com.touchtype.themes.e.a.a";
    private static final String CLASS_THEME_STORAGE_A_BETA = "com.touchtype.themes.e.a.a";
    private static final String METHOD_DPI_THEME_SUBFOLDER = "a";  // args [DisplayMetrics.class]

    private static final String CLASS_IMMUTABLE_MAP_A = "com.google.common.collect.av.a";
    private static final String CLASS_IMMUTABLE_MAP_A_BETA = "com.google.common.collect.av.a";
    private static final String METHOD_THEMES_SET = "b";  // args [Object.class, Object.class]

    private static final String CLASS_THEME_LIST_ADAPTER = "com.touchtype.materialsettings.themessettings.e";
    private static final String CLASS_THEME_LIST_ADAPTER_BETA = "com.touchtype.materialsettings.themessettings.e";
    private static final String METHOD_ON_CLICK = "onClick";  // args [View.class]

    //* string resources we are interested in
    public static final String LETTER_KEY_BOTTOM_TEXT_SCALE = "letter_key_bottom_text_scale";
    public static final String LETTER_KEY_MAIN_TEXT_HEIGHT = "letter_key_main_text_height";
    public static final String LETTER_PREVIEW_POPUP_TEXT_SCALE = "letter_preview_popup_text_scale";
    public static final String THEMES_CURRENT_TITLE = "themes_current_title";

    public static String getClassNameForPackage(String clazz, boolean beta) {
        Field field;
        try {
            if (beta) {
                field = Swiftkey.class.getDeclaredField("CLASS_" + clazz + "_BETA");
            } else {
                field = Swiftkey.class.getDeclaredField("CLASS_" + clazz);
            }
            clazz = (String) field.get(field);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return clazz;
    }

    public static String getMethodNameForPackage(String method/*, boolean beta*/) {
        Field field;
        try {
            /*if (beta) {
                field = Swiftkey.class.getDeclaredField("METHOD_" + clazz + "_BETA");
            } else {*/
                field = Swiftkey.class.getDeclaredField("METHOD_" + method);
           // }
            method = (String) field.get(field);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return method;
    }
}
