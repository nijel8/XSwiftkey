package bg.nijel.xswiftkey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.BaseAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.util.Arrays;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class XSwiftkeyMod implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static String MY_PACKAGE_NAME;
    private String selectedThemeId;
    private static XSharedPreferences myPrefs;
    private static String scrDensityFolder;
    private int count;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        MY_PACKAGE_NAME = XSwiftkeyMod.class.getPackage().getName();
        loadPrefs();
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (resparam.packageName.contains("com.touchtype.swiftkey")) {
            try {
                //* if setting enabled lets resize key text labels
                if (myPrefs.getBoolean(XSwiftkeyActivity.RESIZE_KEYBOARD, false)) {
                    resparam.res.setReplacement(resparam.packageName, "string", "letter_key_bottom_text_scale", myPrefs.getString(XSwiftkeyActivity.KEY_LETTER_SCALE, "0.8"));//1
                    resparam.res.setReplacement(resparam.packageName, "string", "letter_key_main_text_height", myPrefs.getString(XSwiftkeyActivity.KEY_LETTER_HEIGHT, "0.5"));//0.6
                    resparam.res.setReplacement(resparam.packageName, "string", "letter_preview_popup_text_scale", myPrefs.getString(XSwiftkeyActivity.KEY_POPUP_LETTER_SCALE, "0.7"));//0.8
                }
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST
                , "Not set").equals("Not set")) {
            if (lpparam.packageName.contains("com.touchtype.swiftkey")) {
                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                    XposedBridge.log("xswiftkey PACKAGE: >" + lpparam.packageName + "<");
                }
                try {
                    selectedThemeId = myPrefs.getString(SaveThemeIdIntentService.CURRENT_THEME, null);

                    //* changing swiftkey downloaded themes folder to my themes folder...
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "b", Context.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            param.setResult(getMyThemesFolder());
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                XposedBridge.log("xswiftkey THEMES FOLDER: >" + param.getResult() + "<");
                            }
                        }
                    });

                    //* consider my themes as preinstalled so we can select them in swiftkey themes preferences...
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "h", Context.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            param.setResult(new File(myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST, "")));
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                XposedBridge.log("xswiftkey THEMELIST: >" + param.getResult() + "<");
                            }
                        }
                    });

                    //* don"t add swiftkey downloded themes to preinstalled (my) themes set,
                    //* also don't add theme from assets if we already have same theme ID in our themes
                    count = 1;
                    findAndHookMethod("com.google.common.collect.av.a", lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            if (param.args[0] instanceof String) {
                                String id = (String) param.args[0];
                                if (!isMyTheme(id) && param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.h")) {
                                    param.setResult(null);
                                }
                                if (isMyTheme(id) && param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.a")) {
                                    param.setResult(null);
                                }
                                if (id.contains("default") && param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.a")) {
                                    param.setResult(null);
                                }
                            }
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                if (param.getResult() != null && param.args[0] instanceof String) {
                                    if (param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.a")
                                            || param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.d")
                                            || param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.h")) {
                                        XposedBridge.log("xswiftkey THEMES MAPSET: >" + count + "-" + Arrays.toString(param.args) + "<");
                                        count++;
                                    }
                                }
                            }
                        }
                    });

                    //* don't add my themes to downloaded themes set
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "n", Context.class, new XC_MethodHook() {
                        XC_MethodHook.Unhook m;

                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {

                            m = findAndHookMethod("com.google.common.collect.av.a", lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args[0] instanceof String) {
                                        String id = (String) param.args[0];
                                        if (isMyTheme(id) && param.args[1].getClass().getName().equals("com.touchtype.keyboard.theme.d")) {
                                            param.setResult(null);
                                        }
                                    }
                                }
                            });

                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            m.unhook();
                        }
                    });

                    //* blocking delete themes
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "r", Context.class, XC_MethodReplacement.DO_NOTHING);

                    //* another one don't delete my stuff...
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "i", Context.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            callMethod(param.thisObject, "l", param.args[0]);
                            return null;
                        }
                    });

                    //* blocking attempts to unzip and checksum verify of my themes
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "a",
                            findClass("com.touchtype.keyboard.theme.d", lpparam.classLoader), Context.class, new XC_MethodHook() {
                                protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                    String sha = (String) callMethod(param.args[0], "a");
                                    if (sha == null) {
                                        param.setResult(null);
                                    }
                                }
                            });

                    //* blocking new empty themelist write if load theme failed
                    findAndHookMethod("com.touchtype.keyboard.theme.n", lpparam.classLoader, "a", Context.class,
                            findClass("com.touchtype.keyboard.theme.k", lpparam.classLoader), new XC_MethodHook() {
                                protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                    String id = (String) callMethod(param.args[1], "b");
                                    if (id.equals(selectedThemeId)) {
                                        param.setResult(null);
                                    }
                                }
                            });

                    //* get our theme folder name or "default" which is the name for store downloded themes
                    findAndHookMethod("com.touchtype.keyboard.theme.d", lpparam.classLoader, "f", Context.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (isMyTheme(selectedThemeId)) {
                                param.setResult(selectedThemeId);
                            }
                        }
                    });

                    //* dealing with themes thumbnails... don't copy swiftkey themes thumbnails...
                    findAndHookMethod("com.touchtype.keyboard.theme.d", lpparam.classLoader, "a", Context.class,
                            findClass("com.touchtype.themes.e.a", lpparam.classLoader), XC_MethodReplacement.DO_NOTHING);

                    //* ... get display density...
                    if (scrDensityFolder == null) {
                        XC_MethodHook.Unhook dpi = findAndHookMethod("com.touchtype.themes.e.a.a", lpparam.classLoader, "a", DisplayMetrics.class, new XC_MethodHook() {
                            protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                scrDensityFolder = (String) callMethod(param.getResult(), "b");
                            }
                        });
                        if (scrDensityFolder != null) {
                            dpi.unhook();
                        }
                    }

                    //* ... and use my existing thumbnails (don't copy them)
                    findAndHookMethod("com.touchtype.keyboard.theme.d", lpparam.classLoader, "d", Context.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            String id = (String) callMethod(param.thisObject, "b");
                            if (isMyTheme(id)) {
                                return new FileInputStream(new File(getMyThemesFolder(), id + File.separator
                                        + scrDensityFolder + File.separator + "thumbnail.png"));
                            } else {
                                return null;
                            }
                        }
                    });

                    //* save applied theme to my preferences so we can alter swiftkey behavior based on the active theme
                    findAndHookMethod("com.touchtype.materialsettings.themessettings.e", lpparam.classLoader, "onClick", View.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            BaseAdapter adapter = (BaseAdapter) getObjectField(param.thisObject, "c");
                            Object storeImageData = adapter.getItem(getIntField(param.thisObject, "a")); //get selected theme from adapter
                            selectedThemeId = (String) callMethod(storeImageData, "a"); // get selected theme ID
                            Activity activity = (Activity) getObjectField(getObjectField(param.thisObject, "c"), "a");
                            Context swiftContext = activity.getApplicationContext();
                            saveCurrentThemeId(swiftContext, selectedThemeId);
                        }
                    });
                } catch (Throwable t) {
                    XposedBridge.log(t);
                }
            }
        }

        //* notifies my Settings activity module is running
        if (lpparam.packageName.equals(MY_PACKAGE_NAME)) {
            findAndHookMethod(MY_PACKAGE_NAME + ".XSwiftkeyActivity", lpparam.classLoader, "isModuleRunning", new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                    return true;
                }
            });
        }
    }

    private static void loadPrefs() {
        myPrefs = new XSharedPreferences(MY_PACKAGE_NAME, MY_PACKAGE_NAME + "_preferences");
        myPrefs.makeWorldReadable();
    }

    private static File getMyThemesFolder() {
        File list = new File(myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST, ""));
        return list.getParentFile();
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    private void saveCurrentThemeId(Context context, String selectedThemeId) {
        Intent i = new Intent(SaveThemeIdIntentService.SAVE_CURRENT_THEME);
        i.putExtra("saveTheme", selectedThemeId);
        context.startService(i);
    }

    private boolean isMyTheme(String theme) {
        if (theme == null){
            return false;
        }
        File file = new File(getMyThemesFolder(), theme);
        return file.exists() && file.isDirectory() && !theme.endsWith("_resources") && !theme.equals("default");
    }
}
