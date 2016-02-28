package bg.nijel.xswiftkey;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;

import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedAnnotation;
import org.jf.dexlib2.dexbacked.DexBackedClassDef;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import bg.nijel.xswiftkey.helpers.Swiftkey;
import bg.nijel.xswiftkey.service.SaveThemeIdIntentService;
import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

import static de.robv.android.xposed.XposedBridge.hookMethod;
import static de.robv.android.xposed.XposedHelpers.callMethod;
import static de.robv.android.xposed.XposedHelpers.findAndHookMethod;
import static de.robv.android.xposed.XposedHelpers.findClass;
import static de.robv.android.xposed.XposedHelpers.findMethodExact;
import static de.robv.android.xposed.XposedHelpers.getIntField;
import static de.robv.android.xposed.XposedHelpers.getObjectField;

public class XSwiftkeyMod implements IXposedHookInitPackageResources, IXposedHookLoadPackage, IXposedHookZygoteInit {

    private static String MY_PACKAGE_NAME;
    private String selectedThemeId;
    private static XSharedPreferences myPrefs;
    private static String scrDensityFolder;
    //* these are helping for better logging...
    private static HashMap<String, Object> themesSet;
    private static int count;
    private static long lastBootTime;

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) throws Throwable {
        lastBootTime = System.currentTimeMillis();
        MY_PACKAGE_NAME = XSwiftkeyMod.class.getPackage().getName();
        loadPrefs();
        themesSet = new HashMap<>();
        count = Integer.MAX_VALUE;
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (resparam.packageName.equals(whichSwiftkey())) {
            try {
                //* if setting enabled lets resize key text labels
                if (myPrefs.getBoolean(XSwiftkeyActivity.RESIZE_KEYBOARD, false)) {
                    resparam.res.setReplacement(resparam.packageName, "string", Swiftkey.LETTER_KEY_BOTTOM_TEXT_SCALE, myPrefs.getString(Swiftkey.LETTER_KEY_BOTTOM_TEXT_SCALE, "0.8"));//1
                    resparam.res.setReplacement(resparam.packageName, "string", Swiftkey.LETTER_KEY_MAIN_TEXT_HEIGHT, myPrefs.getString(Swiftkey.LETTER_KEY_MAIN_TEXT_HEIGHT, "0.5"));//0.6
                    resparam.res.setReplacement(resparam.packageName, "string", Swiftkey.LETTER_PREVIEW_POPUP_TEXT_SCALE, myPrefs.getString(Swiftkey.LETTER_PREVIEW_POPUP_TEXT_SCALE, "0.7"));//0.8
                }
                if (myPrefs.getBoolean(XSwiftkeyActivity.OVERRIDE_SWIFTKEY_TITLE, false) &&
                        !myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST, "Not set").equals("Not set")) {
                    resparam.res.setReplacement(resparam.packageName, "string", Swiftkey.THEMES_CURRENT_TITLE, "Themes in " + getPrefsTitle());
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
            if (lpparam.packageName.equals(whichSwiftkey())) {
                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                    Log.d("Xposed", "xswiftkey HANDLING PACKAGE: " + lpparam.packageName);
                }
                try {
                    enumClasses(lpparam.appInfo.sourceDir);
                    //* module wont work if we can't find this class(after Swiftkey update for example) so -> exiting
                    Class classThemeManager;
                    //* before do the job we first will try to find all methods responsible
                    //* for deleting our themes if load theme fails. If we can't find them
                    //* we will not continue. Otherwise Swiftkey might delete all our themes if theme loading fails.
                    //* This might happen after Swiftkey update if methods names/locations are changed....
                    //* Module still might work but we don't want to delete users themes...
                    Method methodDeleteThemes;
                    Method methodDeleteThemelist;
                    Method methodEmptyThemelist;
                    try {
                        classThemeManager = findClass(getClassFor("ThemeManager"), lpparam.classLoader);
                        methodDeleteThemes = findMethodExact(classThemeManager, getMethodFor("DELETE_THEMES"), Context.class);
                        methodDeleteThemelist = findMethodExact(classThemeManager, getMethodFor("DELETE_THEMELIST"), Context.class);
                        methodEmptyThemelist = findMethodExact(classThemeManager, getMethodFor("EMPTY_THEMELIST"), Context.class,
                                findClass(getClassFor("ThemeHeader"), lpparam.classLoader));
                    } catch (Throwable t) {
                        XposedBridge.log(t);
                        return;
                    }

                    //* get currently applied theme from my preferences
                    selectedThemeId = myPrefs.getString(SaveThemeIdIntentService.CURRENT_THEME, null);
                    if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                        Log.d("Xposed", "xswiftkey CURRENT THEME: " + selectedThemeId);
                    }

                    //* changing swiftkey downloaded themes folder to my themes folder...
                    findAndHookMethod(classThemeManager, "b", Context.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            param.setResult(getMyThemesFolder());
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                Log.d("Xposed", "xswiftkey THEMES FOLDER: >" + param.getResult() + "<");
                                logSaveToDeleteThemes();
                            }
                        }
                    });

                    //* consider my themes as preinstalled so we can select them in swiftkey themes preferences...
                    findAndHookMethod(classThemeManager, "h", Context.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            param.setResult(new File(myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST, "")));
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                Log.d("Xposed", "xswiftkey THEMELIST: >" + param.getResult() + "<");
                                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DUMP_LOGCAT, false)) {
                                    new File("/sdcard/Xswiftkey.logcat").delete();
                                }
                            }
                        }
                    });

                    //* just building themes set for logging
                    if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                        findAndHookMethod(getClassFor("ImmutableMap_A"), lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                            protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                if (param.getResult() != null && param.args[0] instanceof String) {
                                    if (param.args[1].getClass().getName().equals(getClassFor("AssetThemeHeader"))
                                            || param.args[1].getClass().getName().equals(getClassFor("DownloadedThemeHeader"))
                                            || param.args[1].getClass().getName().equals(getClassFor("PreInstalledThemeHeader"))) {
                                        String id = (String) param.args[0];
                                        if (!themesSet.containsKey(id)) {
                                            themesSet.put(id, param.args[1]);
                                            Log.d("Xposed", "xswiftkey IS MY THEME: " + isMyTheme(id));
                                            Log.d("Xposed", "xswiftkey THEMES MAPSET: " + themesSet.size() + "-" + Arrays.toString(param.args));
                                        } else if (!themesSet.get(id).equals(param.args[1])) {
                                            themesSet.put(id, param.args[1]);
                                            Log.d("Xposed", "xswiftkey IS MY THEME: " + isMyTheme(id));
                                            Log.d("Xposed", "xswiftkey THEMES MAPSET: " + themesSet.size() + "-" + Arrays.toString(param.args));
                                        }
                                    }
                                }
                            }
                        });
                    }

                    //* don't add my themes to downloaded themes set or discard them if missing from themes folder
                    findAndHookMethod(classThemeManager, "n", Context.class, new XC_MethodHook() {

                        XC_MethodHook.Unhook addThemes;

                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {

                            addThemes = findAndHookMethod(getClassFor("ImmutableMap_A"), lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args[0] instanceof String) {
                                        String id = (String) param.args[0];
                                        // File dir = new File(getMyThemesFolder(), id);
                                        if (isMyTheme(id) && param.args[1].getClass().getName().equals(getClassFor("DownloadedThemeHeader"))) {
                                            param.setResult(null);
                                        }
                                        File dir = new File(getMyThemesFolder(), id);
                                        if (!dir.exists()) {
                                            param.setResult(null);
                                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                                Log.e("Xposed", "xswiftkey THEME ID [" + id + "] exists in themelist.json but not found in themes folder!!! Remove its entry from themelist.");
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            addThemes.unhook();
                        }
                    });

                    //* don't add store themes to preinstalled themes set or discard them if missing from themes folder
                    findAndHookMethod(classThemeManager, "p", Context.class, new XC_MethodHook() {

                        XC_MethodHook.Unhook addThemes;

                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {

                            addThemes = findAndHookMethod(getClassFor("ImmutableMap_A"), lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args[0] instanceof String) {
                                        String id = (String) param.args[0];
                                        if (!isMyTheme(id) && param.args[1].getClass().getName().equals(getClassFor("PreInstalledThemeHeader"))) {
                                            param.setResult(null);
                                        }
                                        File dir = new File(getMyThemesFolder(), id);
                                        if (!dir.exists()) {
                                            param.setResult(null);
                                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                                Log.e("Xposed", "xswiftkey THEME ID [" + id + "] exists in themelist.json but not found in themes folder!!! Remove its entry from themelist.");
                                            }
                                        }
                                    }
                                }
                            });
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            addThemes.unhook();
                        }
                    });

                    //* don't add assets themes to themes set if we alredy have same theme in our theme collection
                    findAndHookMethod(classThemeManager, "q", Context.class, new XC_MethodHook() {

                        XC_MethodHook.Unhook addThemes;

                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {

                            addThemes = findAndHookMethod(getClassFor("ImmutableMap_A"), lpparam.classLoader, "b", Object.class, Object.class, new XC_MethodHook() {
                                @Override
                                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.args[0] instanceof String) {
                                        String id = (String) param.args[0];
                                        if ((isMyTheme(id) || id.contains("default")) && param.args[1].getClass().getName().equals(getClassFor("AssetThemeHeader"))) {
                                            param.setResult(null);
                                        }
                                    }
                                }
                            });
                        }

                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            addThemes.unhook();
                        }
                    });

                    //* blocking delete themes
                    hookMethod(methodDeleteThemes, XC_MethodReplacement.DO_NOTHING);

                    //* another one don't delete my stuff... calling only what is needed
                    hookMethod(methodDeleteThemelist, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            callMethod(param.thisObject, "l", param.args[0]);
                            return null;
                        }
                    });

                    //* blocking attempts to unzip and checksum verify of my themes, our themes don't have SHA-1 JSON element in themelist
                    findAndHookMethod(classThemeManager, "a",
                            findClass(getClassFor("DownloadedThemeHeader"), lpparam.classLoader), Context.class, new XC_MethodHook() {
                                String sha = null;

                                protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                    sha = (String) callMethod(param.args[0], "a");
                                    if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                        Log.d("Xposed", "xswiftkey THEME SHA-1 null? " + (sha == null));
                                    }
                                    if (sha == null) {
                                        param.setResult(null);
                                    }
                                }

                                protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                                    if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false) && param.getResult() == null) {
                                        Log.d("Xposed", "xswiftkey MY THEME -> No unzip and sumcheck...");
                                    }
                                }
                            });

                    //* blocking new empty themelist write if load theme failed
                    hookMethod(methodEmptyThemelist, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            String id = (String) callMethod(param.args[1], "b");
                            if (id.equals(selectedThemeId)) {
                                param.setResult(null);
                            }
                        }
                    });

                    //* get our theme folder name or "default" which is the name for store downloded themes
                    findAndHookMethod(getClassFor("DownloadedThemeHeader"), lpparam.classLoader, "f", Context.class, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            if (isMyTheme(selectedThemeId)) {
                                param.setResult(selectedThemeId);
                            }
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                Log.d("Xposed", "xswiftkey APPLY THEME: " + param.getResult());
                                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DUMP_LOGCAT, false)) {
                                    saveCurrentThemeId((Context) param.args[0], SaveThemeIdIntentService.SAVE_LOGCAT);
                                }
                            }
                        }
                    });

                    //* dealing with themes thumbnails... don't copy swiftkey themes thumbnails...
                    findAndHookMethod(getClassFor("DownloadedThemeHeader"), lpparam.classLoader, "a", Context.class,
                            findClass(getClassFor("ThemeStorage"), lpparam.classLoader), XC_MethodReplacement.DO_NOTHING);

                    //* ... get display density...
                    findAndHookMethod(getClassFor("ThemeStorage_A"), lpparam.classLoader, "a", DisplayMetrics.class, new XC_MethodHook() {
                        protected void afterHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            scrDensityFolder = (String) callMethod(param.getResult(), "b");
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                Log.d("Xposed", "xswiftkey DENSITY: " + scrDensityFolder);
                            }
                        }
                    });

                    //* ... and use my existing thumbnails (don't copy them, we alredy have thumbnails)
                    findAndHookMethod(getClassFor("DownloadedThemeHeader"), lpparam.classLoader, "d", Context.class, new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            String id = (String) callMethod(param.thisObject, "b");
                            if (isMyTheme(id)) {
                                File thumbnail = new File(getMyThemesFolder(), id + File.separator
                                        + scrDensityFolder + File.separator + "thumbnail.png");
                                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                    Log.d("Xposed", "xswiftkey IS MY THEME TMBNL: " + id + "->" + isMyTheme(id) + "->" + thumbnail + "<");
                                }
                                return new FileInputStream(thumbnail);
                            } else {
                                return null;
                            }
                        }
                    });

                    //* save applied theme to my preferences so we can alter swiftkey behavior based on the active theme
                    findAndHookMethod(getClassFor("ThemesListAdapter"), lpparam.classLoader, "onClick", View.class, new XC_MethodHook() {
                        protected void beforeHookedMethod(final XC_MethodHook.MethodHookParam param) throws Throwable {
                            BaseAdapter adapter = (BaseAdapter) getObjectField(param.thisObject, "c");
                            Object storeImageData = adapter.getItem(getIntField(param.thisObject, "a")); //get selected theme from adapter
                            selectedThemeId = (String) callMethod(storeImageData, "a"); // get selected theme ID
                            Activity activity = (Activity) getObjectField(getObjectField(param.thisObject, "c"), "a");
                            Context swiftContext = activity.getApplicationContext();
                            saveCurrentThemeId(swiftContext, SaveThemeIdIntentService.SAVE_CURRENT_THEME);
                            if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                                Log.d("Xposed", "xswiftkey ONCLICK THEME: " + selectedThemeId);
                            }
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

    private File getMyThemesFolder() {
        File list = new File(myPrefs.getString(XSwiftkeyActivity.MY_THEMES_LIST, ""));
        return list.getParentFile();
    }

    private void saveCurrentThemeId(Context context, String action) {
        Context myContext = null;
        try {
            myContext = context.createPackageContext(MY_PACKAGE_NAME, Context.CONTEXT_IGNORE_SECURITY);
        } catch (Throwable t) {
            XposedBridge.log(t);
        }
        if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
            Log.d("Xposed", "xswiftkey MODULE CONTEXT: " + ((myContext != null) ? myContext.getPackageName() : null));
        }
        Intent i = new Intent(myContext, SaveThemeIdIntentService.class);
        i.setAction(action);
        if (action.equals(SaveThemeIdIntentService.SAVE_CURRENT_THEME)) {
            i.removeExtra("lastBootTime");
            i.putExtra("saveTheme", selectedThemeId);
        }
        if (action.equals(SaveThemeIdIntentService.SAVE_LOGCAT)) {
            i.removeExtra("saveTheme");
            i.putExtra("lastBootTime", lastBootTime);
        }
        assert myContext != null;
        myContext.startService(i);
    }

    private boolean isMyTheme(String theme) {
        if (theme == null) {
            return false;
        }
        File file = new File(getMyThemesFolder(), theme);
        return file.exists() && file.isDirectory() && !theme.contains("_resources") && !theme.equals("default");
    }

    @SuppressLint("SdCardPath")
    private String getPrefsTitle() {
        return getMyThemesFolder().getAbsolutePath().replace(Environment.getExternalStorageDirectory().getAbsolutePath(), "/sdcard");
    }

    private void logSaveToDeleteThemes() {
        FilenameFilter themes = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                return isMyTheme(filename);
            }
        };
        for (String theme : getMyThemesFolder().list(themes)) {
            if (themesSet != null && themesSet.size() > 0 && themesSet.size() != count && !themesSet.containsKey(theme)) {
                Log.w("Xposed", "xswiftkey THEME ID [" + theme + "] exists in your themes folder but missing in themelist.json!!! Delete it or add it to themelist if you intent to use it.");
            }
        }
        assert themesSet != null;
        count = themesSet.size();
    }

    private String whichSwiftkey() {
        return myPrefs.getBoolean(XSwiftkeyActivity.HANDLE_BETA, false) ? Swiftkey.PACKAGE_NAME_BETA : Swiftkey.PACKAGE_NAME;
    }

    private String getClassFor(String clazz) {
        return Swiftkey.getClassNameForPackage(clazz);
    }

    private String getMethodFor(String method) {
        return Swiftkey.getMethodNameForPackage(method);
    }

    //* scans Swiftkey "classes.dex" and grabs the names of the classes we need based on source_file.java, annotations and interfaces for the particular
    // class. Those search criteria are less likely to change whit Swiftkey updates so
    //* we are almost future proofed against Swiftkey updates (I hope...). Class name fields are assigned a value dynamically at every reboot.
    //* Using JesusFreke/smali dexlib2 library (https://github.com/JesusFreke/smali)(Thank you) for getting what we need from "classes.dex" file.
    private void enumClasses(String swiftkeyPkg) {
        DexBackedDexFile dex = null;
        try {
            dex = DexFileFactory.loadDexFile(new File(swiftkeyPkg), "classes.dex", Opcodes.forApi(Build.VERSION.SDK_INT, false));
        } catch (IOException e) {
            e.printStackTrace();
        }
        assert dex != null;
        Set classes = dex.getClasses();
        for (Object object : classes) {
            DexBackedClassDef dbcd = (DexBackedClassDef) object;
            String source = dbcd.getSourceFile();
            if (isMyClass(dbcd)) {
                String[] fieldValue = formatSourceClass(source, dbcd.getType());
                Swiftkey.setClassFieldFor(fieldValue);
                if (myPrefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                    Log.d("Xposed", "xswiftkey CLASS: " + fieldValue[0] + " = " + getClassFor(fieldValue[0]));
                }
            }
        }
    }

    private String[] formatSourceClass(String sourceJava, String className) {
        if (sourceJava.contains(".java")) {
            sourceJava = sourceJava.replace(".java", "");
        }
        if (className.contains("$")) {
            String[] atemp = className.split("\\$");
            for (int i = 0; i < atemp.length; i++) {
                if (i > 0) {
                    sourceJava = sourceJava + "_" + capitalizeFirstLetter(atemp[i]).replace(";", "");
                }
            }
        }
        className = className.replace("/", ".");
        className = className.replace("$", ".");
        className = className.substring(1, className.length() - 1);
        return new String[]{sourceJava, className};
    }

    private String capitalizeFirstLetter(String original) {
        if (original == null || original.length() == 0) {
            return original;
        }
        return original.substring(0, 1).toUpperCase() + original.substring(1);
    }

    private boolean isMyClass(DexBackedClassDef cd) {
        String source = cd.getSourceFile();
        if (source == null) {
            return false;
        }
        switch (source) {
            case "ImmutableMap.java":
                if (cd.getAnnotations().size() > 0) {
                    for (DexBackedAnnotation a : cd.getAnnotations()) {
                        if (a.getType().equals("Ldalvik/annotation/MemberClasses;")) {
                            return false;
                        }
                    }
                }
                return true;
            case "ThemeHeader.java":
                return cd.getInterfaces().size() <= 0;
            case "ThemeManager.java":
                if (cd.getAnnotations().size() > 0) {
                    for (DexBackedAnnotation a : cd.getAnnotations()) {
                        if (a.getType().equals("Ldalvik/annotation/MemberClasses;")) {
                            return true;
                        }
                    }
                }
                return false;
            case "AssetThemeHeader.java":
                return cd.getInterfaces().size() <= 0;
            case "PreInstalledThemeHeader.java":
                return true;
            case "DownloadedThemeHeader.java":
                return cd.getAnnotations().size() <= 0 && cd.getInterfaces().size() <= 0;
            case "ThemeStorage.java":
                return true;
            case "ThemesListAdapter.java":
                if (cd.getInterfaces().size() > 0) {
                    for (String s : cd.getInterfaces()) {
                        if (s.equals("Landroid/view/View$OnClickListener;")) {
                            return true;
                        }
                    }
                }
                return false;
            default:
                return false;
        }
    }

    /*private boolean hasMethod(DexBackedClassDef dbcd) {
        for (Object o : dbcd.getMethods()) {
            DexBackedMethod dbm = (DexBackedMethod) o;
            if (Arrays.asList(Swiftkey.getMethodsArgs()).contains(dbm.getName() + String.valueOf(dbm.getParameterTypes()))) {
                return true;
            }
        }
        return false;
    }*/
}
