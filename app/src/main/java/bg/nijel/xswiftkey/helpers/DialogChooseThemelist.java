package bg.nijel.xswiftkey.helpers;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import bg.nijel.xswiftkey.R;

public class DialogChooseThemelist implements OnClickListener, OnItemClickListener {
    private final Context m_context;
    private File m_currentDir;
    private final List<File> m_entries = new ArrayList<>();
    private final ListView m_list;
    private Result mResult = null;
    private static String mBrowsingFolder;
    private static int[] mSavedScroll;
    private final Button mPositiveBtn;
    private String mResultJson;
    private AlertDialog m_alertDialog;
    private  boolean hasThemelist;

    public class DirAdapter extends ArrayAdapter<File> {
        public DirAdapter(int resid) {
            super(DialogChooseThemelist.this.m_context, resid, DialogChooseThemelist.this.m_entries);
        }

        @SuppressWarnings("deprecation")
        public View getView(final int position, View convertView, final ViewGroup parent) {
            TextView textview = (TextView) super.getView(position, convertView, parent);
            File file = DialogChooseThemelist.this.m_entries.get(position);
            if (file != null) {
                textview.setText(file.getName());
                if (!file.getName().equals("..")) {
                    if (file.isDirectory()) {
                        textview.setCompoundDrawablesWithIntrinsicBounds(DialogChooseThemelist.this.m_context.getResources().getDrawable(R.drawable.folder), null, null, null);
                    } else {
                        textview.setCompoundDrawablesWithIntrinsicBounds(DialogChooseThemelist.this.m_context.getResources().getDrawable(R.drawable.file), null, null, null);
                    }
                } else {
                    textview.setCompoundDrawablesWithIntrinsicBounds(DialogChooseThemelist.this.m_context.getResources().getDrawable(R.drawable.folder_up), null, null, null);
                }
                if (hasThemelist){
                    m_list.setSelection(0);
                }else {
                    if (mSavedScroll != null && mSavedScroll[0] > 0) {
                        m_list.setSelectionFromTop(mSavedScroll[0], mSavedScroll[1]);
                    }
                }
            }
            return textview;
        }
    }

    public interface Result {
        void onChooseDirectory(String str);
    }

    private void listJsonFiles() {
        this.m_entries.clear();
        if (this.m_currentDir.canRead()) {
            FileFilter themelist = new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().equals("themelist.json") && pathname.isFile();
                }
            };
            File[] files = this.m_currentDir.listFiles();
            if (this.m_currentDir.getParent() != null) {
                this.m_entries.add(new File(".."));
            }
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        this.m_entries.add(file);
                    }
                }
            }
            Collections.addAll(this.m_entries, this.m_currentDir.listFiles(themelist));
        } else {
            Toast.makeText(this.m_context, this.m_currentDir.getAbsolutePath() + ": " + this.m_context.getString(R.string.toast_unable_access), Toast.LENGTH_SHORT).show();
        }
        Collections.sort(this.m_entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                if (f1.isDirectory() && f2.isDirectory()) {
                    return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
                } else if (f1.isDirectory()) {
                    return 1;
                }else if(f2.getName().equals("..")){
                    return 1;
                } else {
                    return -1;
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    public DialogChooseThemelist(Context context, Result res, String startDir) {
        this.m_context = context;
        this.mResult = res;
        mBrowsingFolder = startDir;
        File themelist = new File(startDir);
        if (themelist.exists()) {
            this.m_currentDir = themelist.getParentFile();
        } else {
            this.m_currentDir = Environment.getExternalStorageDirectory();
        }
        listJsonFiles();
        DirAdapter adapter = new DirAdapter(R.layout.listitem_row_textview);
        final Builder builder = new Builder(this.m_context);
        builder.setAdapter(adapter, this);
        builder.setPositiveButton("SELECT", new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if (DialogChooseThemelist.this.mResult != null) {
                    DialogChooseThemelist.this.mResult.onChooseDirectory(DialogChooseThemelist.this.mResultJson);
                }
                dialog.dismiss();
                mBrowsingFolder = null;
            }
        });
        builder.setNegativeButton("CANCEL", new OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                mBrowsingFolder = null;
            }
        });
        builder.setTitle(DialogChooseThemelist.this.m_currentDir.getAbsolutePath());
        m_alertDialog = builder.create();
        this.m_list = m_alertDialog.getListView();
        this.m_list.setOnItemClickListener(this);
        m_alertDialog.show();
        mPositiveBtn = m_alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
        mPositiveBtn.setEnabled(false);
        this.m_list.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == SCROLL_STATE_IDLE) {
                    saveScrollView(view);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
            if (hasThemelist){
                this.m_list.setSelection(0);
            }else {
                if (mSavedScroll != null && mSavedScroll[0] > 0) {
                this.m_list.setSelectionFromTop(mSavedScroll[0], mSavedScroll[1]);
                }
            }
    }

    private static Drawable scaleDrawable(Drawable drawable, int width, int height) {
        int wi = drawable.getIntrinsicWidth();
        int hi = drawable.getIntrinsicHeight();
        int dimDiff = Math.abs(wi - width) - Math.abs(hi - height);
        float scale = (dimDiff > 0) ? width / (float) wi : height / (float) hi;
        Rect bounds = new Rect(0, 0, (int) (scale * wi), (int) (scale * hi));
        drawable.setBounds(bounds);
        return drawable;
    }

    public void onItemClick(AdapterView<?> adapterView, View textview, int pos, long id) {
        if (pos >= 0 && pos < this.m_entries.size()) {
            saveScrollView(adapterView);
            if (this.m_entries.get(pos).isFile()) {
                textview.setSelected(true);
                mResultJson = this.m_entries.get(pos).getAbsolutePath();
                mPositiveBtn.setEnabled(true);
            }else {
                if (this.m_entries.get(pos).getName().equals("..")) {
                    this.m_currentDir = this.m_currentDir.getParentFile();
                } else {
                    this.m_currentDir = this.m_entries.get(pos);
                }
                mBrowsingFolder = DialogChooseThemelist.this.m_currentDir.getAbsolutePath();
                m_alertDialog.setTitle(mBrowsingFolder);
                listJsonFiles();
                this.m_list.setAdapter(new DirAdapter(R.layout.listitem_row_textview));
                hasThemelist = this.m_entries.contains(new File(this.m_currentDir, "themelist.json"));
                mPositiveBtn.setEnabled(false);
            }
        }
    }

    private void saveScrollView(AdapterView<?> adapterView) {
        int first = adapterView.getFirstVisiblePosition();
        int top = (adapterView.getChildAt(0) == null) ? 0 : (adapterView.getChildAt(0).getTop() - adapterView.getPaddingTop());
        mSavedScroll = new int[]{first, top};
    }

    public void onClick(DialogInterface dialog, int which) {
    }

}
