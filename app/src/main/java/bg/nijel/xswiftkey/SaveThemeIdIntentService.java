package bg.nijel.xswiftkey;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class SaveThemeIdIntentService extends IntentService {

    public static final String CURRENT_THEME = "current_theme";
    public static final String SAVE_CURRENT_THEME = "bg.nijel.xswiftkey.SAVE_CURRENT_THEME";

    public SaveThemeIdIntentService() {
        super("SaveThemeIdIntentService");
    }

    @SuppressLint("WorldReadableFiles")
    @SuppressWarnings("deprecation")
    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent.getAction().equals(SAVE_CURRENT_THEME)) {
            String MY_PACKAGE_NAME = SaveThemeIdIntentService.class.getPackage().getName();
            SharedPreferences prefs = getSharedPreferences(MY_PACKAGE_NAME + "_preferences", Context.MODE_WORLD_READABLE);
            String theme = intent.getStringExtra("saveTheme");
            prefs.edit().putString(CURRENT_THEME, theme).apply();
            if (prefs.getBoolean(XSwiftkeyActivity.KEY_DEBUG, false)) {
                Log.d("Xposed","xswiftkey SAVING THEME: " + theme);
                Log.d("Xposed","xswiftkey SAVED THEME: " + prefs.getString(CURRENT_THEME, "no theme"));
            }
        }
    }

}
