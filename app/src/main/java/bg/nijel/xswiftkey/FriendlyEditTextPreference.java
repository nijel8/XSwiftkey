package bg.nijel.xswiftkey;

/**
 * Created by Nick on 1/2/2016.
 */

import android.content.Context;
import android.preference.EditTextPreference;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;

class FriendlyEditTextPreference extends EditTextPreference {

    public FriendlyEditTextPreference(Context context) {
        super(context);
    }

    // According to ListPreference implementation
    @Override
    public CharSequence getSummary() {
        String text = getText();
        CharSequence summary = super.getSummary();
        if (TextUtils.isEmpty(text)) {
            return String.format(summary.toString(), text).replace("null", getEditText().getHint());
        } else {
            summary = super.getSummary();
            if (summary != null) {
                if (text.startsWith(".")){
                    text = "0" + text;
                }
                return String.format(summary.toString(), text);
            } else {
                return null;
            }
        }
    }

    @Override
    protected void onAddEditTextToDialogView(View dialogView, EditText editText) {
        super.onAddEditTextToDialogView(dialogView, editText);
        editText.setSelection(0, editText.length());
    }

}