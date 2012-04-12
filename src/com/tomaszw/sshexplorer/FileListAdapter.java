package com.tomaszw.sshexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class FileListAdapter extends ArrayAdapter<FileEntry> {
    private List<FileEntry> m_values;
    private List<Integer> m_checked = new ArrayList<Integer>();
    private List<Integer> m_filtered;
    private Map<Integer, CheckBox> m_boxes = new HashMap<Integer, CheckBox>();

    private Context m_context;

    public FileListAdapter(Context c, List<FileEntry> values) {
        super(c, android.R.layout.simple_list_item_checked, android.R.id.text1);
        m_values = new ArrayList<FileEntry>(values);
        Collections.sort(m_values, new FileEntryComparator());
        m_context = c;
        m_filtered = new ArrayList<Integer>();
        for (int i = 0; i < m_values.size(); ++i) {
            m_filtered.add(i);
        }
    }

    @Override
    public int getCount() {
        return m_filtered.size();
    }

    public void pattern(CharSequence pat) {
        String p = pat.toString().toLowerCase();
        List<Integer> f = new ArrayList<Integer>();
        for (int i = 0; i < m_values.size(); ++i) {
            if (m_values.get(i).name.toLowerCase().contains(p))
                f.add(i);
        }
        m_filtered = f;
        notifyDataSetChanged();
    }

    @Override
    public FileEntry getItem(int position) {
        // TODO Auto-generated method stub
        return m_values.get(m_filtered.get(position));
    }

    public List<FileEntry> getCheckedEntries() {
        List<FileEntry> entries = new ArrayList<FileEntry>();
        for (int p : m_checked) {
            entries.add(m_values.get(p));
        }
        return entries;
    }

    public void clearCheck(FileEntry e) {
        int i = m_values.indexOf(e);
        if (i != -1) {
            CheckBox box = m_boxes.get(i);
            if (box != null) {
                box.setChecked(false);
            }
        }
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        FileEntry e = getItem(position);

        RelativeLayout l = new RelativeLayout(m_context);
        RelativeLayout.LayoutParams params;
        l.setFocusable(false);

        final CheckBox box = new CheckBox(m_context);
        box.setFocusable(false);
        box.setFocusableInTouchMode(false);
        box.setId(1);
        if (e.dir) {
            box.setEnabled(false);
        }
        l.addView(box);
        l.setClickable(false);
        TextView v = new TextView(m_context);
        v.setFocusable(false);

        params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.RIGHT_OF, 1);
        params.addRule(RelativeLayout.CENTER_VERTICAL);
        l.addView(v, params);
        v.setText(e.name);
        //v.setClickable(true);
        if (e.dir) {
            v.setTextColor(Color.BLUE);
            box.setButtonDrawable(android.R.color.transparent);
        } else {
            int p = e.perms;
            if ((p & 0111) != 0) {
                v.setTextColor(Color.GREEN);
            }
        }

        /* checkbox listener */
        final int idx = m_filtered.get(position);
        m_boxes.put(idx, box);
        box.setChecked(m_checked.contains(idx));
        /*
        if (!e.dir) {
            v.setOnClickListener(new CheckBox.OnClickListener() {

                @Override
                public void onClick(View v) {
                    App.d("item click!");
                    // TODO Auto-generated method stub
                    boolean isChecked = !m_checked.contains(idx);
                    box.setChecked(isChecked);
                    if (isChecked) {
                        m_checked.add(idx);
                    } else {
                        boolean removed = m_checked.remove((Object) idx);
                    }

                }
            });
        }*/

        box.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {

            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                if (isChecked) {
                    m_checked.add(idx);
                } else {
                    boolean removed = m_checked.remove((Object) idx);
                }
            }
        });
        return l;
    }
}