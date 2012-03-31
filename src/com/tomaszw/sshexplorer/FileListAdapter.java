package com.tomaszw.sshexplorer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;

import com.jcraft.jsch.ChannelSftp.LsEntry;

public class FileListAdapter extends ArrayAdapter<FileEntry> {
    private List<FileEntry> m_values;
    private List<Integer> m_filtered;
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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // TODO Auto-generated method stub
        CheckedTextView v = new CheckedTextView(m_context);
        v.setFocusable(false);
        FileEntry e = getItem(position);
        v.setText(e.name);
        if (e.dir) {
            v.setTextColor(Color.BLUE);
        } else {
            int p = e.perms;
            if ((p & 0111) != 0) {
                v.setTextColor(Color.GREEN);
            }
        }
        ;
        return v;
    }
}