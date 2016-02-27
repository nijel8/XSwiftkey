package bg.nijel.xswiftkey.service;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import bg.nijel.xswiftkey.XSwiftkeyActivity;

public class SaveThemeIdIntentService extends IntentService {

    public static final String CURRENT_THEME = "current_theme";
    public static final String SAVE_CURRENT_THEME = "bg.nijel.xswiftkey.SAVE_CURRENT_THEME";
    public static final String SAVE_LOGCAT = "bg.nijel.xswiftkey.SAVE_LOGCAT";

    public SaveThemeIdIntentService() {
        super("SaveThemeIdIntentService");
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(SAVE_CURRENT_THEME) && intent.hasExtra("saveTheme")) {
            String MY_PACKAGE_NAME = getApplicationContext().getPackageName();
            SharedPreferences prefs = getSharedPreferences(MY_PACKAGE_NAME + "_preferences", Context.MODE_WORLD_READABLE);
            String theme = intent.getStringExtra("saveTheme");
            prefs.edit().putString(CURRENT_THEME, theme).apply();
            if (prefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                Log.d("Xposed", "xswiftkey SAVING THEME: " + theme);
                Log.d("Xposed", "xswiftkey SAVED THEME: " + prefs.getString(CURRENT_THEME, "no theme"));
            }
        }
        if (intent.getAction().equals(SAVE_LOGCAT) && intent.hasExtra("lastBootTime")) {
            if (new File("/sdcard/Xswiftkey.logcat").exists()){
                return;
            }
            long lastBootTime = intent.getLongExtra("lastBootTime", 0l);
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm:ss.mmm", Locale.US);
            Log.d("Xposed", "xswiftkey Saving filtered logcat to /sdcard/Xswiftkey.logcat...");
            Process suProcess = null;
            try {
                suProcess = Runtime.getRuntime().exec("su");
                PrintStream ps = null;
                ps = new PrintStream(new BufferedOutputStream(suProcess.getOutputStream(), 32768));
                ps.print("logcat -b system -b events -b crash -b main -v time -t '" + sdf.format(lastBootTime - 30000l)
                        + "' | grep 'swiftkey' > /sdcard/Xswiftkey.logcat\n");
                ps.flush();
                ps.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
