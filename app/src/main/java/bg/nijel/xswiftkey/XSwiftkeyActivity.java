package bg.nijel.xswiftkey;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.text.Html;
import android.text.InputType;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;

/**
 * Created by Nick on 2/7/2016.
 */
public class XSwiftkeyActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener, DialogChooseDirectory.Result {

    public static final String MY_THEMES_LIST = "my_themes_list";
    public static final String RESIZE_KEYBOARD = "resize_keyboard";
    public static final String KEY_LETTER_SCALE = "letter_key_bottom_text_scale";
    public static final String KEY_LETTER_HEIGHT = "letter_key_main_text_height";
    public static final String KEY_POPUP_LETTER_SCALE = "letter_preview_popup_text_scale";
    private static SharedPreferences prefs;

    @SuppressLint({"WorldReadableFiles", "SetWorldReadable"})
    @SuppressWarnings("deprecation")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String MY_PACKAGE_NAME = XSwiftkeyActivity.class.getPackage().getName();
        File prefsFile = new File(Environment.getDataDirectory(),
                "data/" + MY_PACKAGE_NAME + "/shared_prefs/" + MY_PACKAGE_NAME + "_preferences.xml");
        //noinspection ResultOfMethodCallIgnored
        prefsFile.setReadable(true, false);
        prefs = getSharedPreferences(MY_PACKAGE_NAME + "_preferences", Context.MODE_WORLD_READABLE);
        PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(this);
        setPreferenceScreen(prefScreen);
        setTitle(getString(R.string.app_name) + " Settings");
        {
            final Preference pr = new Preference(this);
            pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new DialogChooseDirectory(XSwiftkeyActivity.this, XSwiftkeyActivity.this, prefs.getString(XSwiftkeyActivity.MY_THEMES_LIST
                            , null));
                    return false;
                }
            });
            pr.setKey(MY_THEMES_LIST);
            pr.setTitle(R.string.pref_my_themes_location_title);
            pr.setSummary(getString(R.string.pref_my_themes_location_summary) + " " + prefs.getString(XSwiftkeyActivity.MY_THEMES_LIST
                    , "Not set"));
            prefScreen.addPreference(pr);
        }
        PreferenceCategory category = new PreferenceCategory(this);
        category.setTitle(R.string.pref_category_adjust_keyboard_size_title);
        prefScreen.addPreference(category);
        {
            final CheckBoxPreference pr = new CheckBoxPreference(this);
            pr.setKey(RESIZE_KEYBOARD);
            pr.setTitle(R.string.pref_adjust_size_title);
            pr.setSummary(R.string.pref_adjust_size_summary);
            pr.setChecked(prefs.getBoolean(RESIZE_KEYBOARD, false));
            category.addPreference(pr);
        }
        {
            final FriendlyEditTextPreference pr = new FriendlyEditTextPreference(this);
            pr.setKey(KEY_LETTER_SCALE);
            pr.setTitle(R.string.pref_letter_key_bottom_text_scale_title);
            pr.setSummary(R.string.pref_letter_key_bottom_text_scale_summary);
            pr.setDialogTitle(R.string.pref_letter_key_bottom_text_scale_title);
            pr.getEditText().setHint("0.8");
            pr.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            category.addPreference(pr);
        }
        {
            final FriendlyEditTextPreference pr = new FriendlyEditTextPreference(this);
            pr.setKey(KEY_LETTER_HEIGHT);
            pr.setTitle(R.string.pref_letter_key_main_text_height_title);
            pr.setSummary(R.string.pref_letter_key_main_text_height_summary);
            pr.setDialogTitle(R.string.pref_letter_key_main_text_height_title);
            pr.getEditText().setHint("0.5");
            pr.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            category.addPreference(pr);
        }
        {
            final FriendlyEditTextPreference pr = new FriendlyEditTextPreference(this);
            pr.setKey(KEY_POPUP_LETTER_SCALE);
            pr.setTitle(R.string.pref_letter_preview_popup_text_scale_title);
            pr.setSummary(R.string.pref_letter_preview_popup_text_scale_summary);
            pr.setDialogTitle(R.string.pref_letter_preview_popup_text_scale_title);
            pr.getEditText().setHint("0.7");
            pr.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
            category.addPreference(pr);
        }
        getPreferenceScreen().findPreference(KEY_LETTER_SCALE).setDependency(RESIZE_KEYBOARD);
        getPreferenceScreen().findPreference(KEY_LETTER_HEIGHT).setDependency(RESIZE_KEYBOARD);
        getPreferenceScreen().findPreference(KEY_POPUP_LETTER_SCALE).setDependency(RESIZE_KEYBOARD);
        PreferenceCategory apply = new PreferenceCategory(this);
        apply.setTitle(R.string.pref_category_apply_title);
        prefScreen.addPreference(apply);
        {
            final Preference pr = new Preference(this);
            pr.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(XSwiftkeyActivity.this)
                            .setTitle("Soft reboot now?")
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    Process suProcess;
                                    try {
                                        suProcess = Runtime.getRuntime().exec("su");
                                        DataOutputStream os = new DataOutputStream(suProcess.getOutputStream());
                                        os.writeBytes("killall system_server" + "\n");
                                        os.flush();
                                        os.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    // do nothing
                                }
                            })
                            .show();
                    return false;
                }
            });
            pr.setTitle(R.string.pref_category_apply_title);
            pr.setSummary(getString(R.string.pref_apply_changes_summary));
            apply.addPreference(pr);
        }
        //noinspection ConstantConditions
        if (!isModuleRunning()) {
            setTitle(Html.fromHtml("<font color=\"#FF4400\">" + getString(R.string.app_name) + " Module NOT ACTIVE</font>"));
            Toast.makeText(this, "Module not loaded or disabled...", Toast.LENGTH_LONG).show();
        }

    }


    @SuppressWarnings("SameReturnValue")
    private boolean isModuleRunning() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        //noinspection ConstantConditions,ConstantConditions
        if (!isModuleRunning()) {
            setTitle(Html.fromHtml("<font color=\"#FF4400\">" + getString(R.string.app_name) + " Module NOT ACTIVE</font>"));
            Toast.makeText(XSwiftkeyActivity.this, "Module not loaded or disabled...", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onChooseDirectory(String location) {
        prefs.edit().putString(MY_THEMES_LIST, location).apply();
        onSharedPreferenceChanged(prefs, MY_THEMES_LIST);
        getPreferenceScreen().findPreference(MY_THEMES_LIST).setSummary(getString(R.string.pref_my_themes_location_summary) + " "
                + prefs.getString(MY_THEMES_LIST, "Not set"));
    }
}
