package org.idempotentimplements.sshexplorer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.jcraft.jsch.JSchException;

public class SSHExplorerActivity extends Activity {
    public static final int REQ_LOGIN = 0;
    private ListView m_fileListView;
    private EditText m_fileFilterEdit;
    private FileSystem m_fs;
    private String m_currentPath = "";
    private ExchangeService m_exchangeService;
    private ExchangeBridge m_exchangeBridge;
    private TextView m_filePathText;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // TODO Auto-generated method stub
        super.onSaveInstanceState(outState);
        outState.putString("path", getCurrentPath());
    }

    private void restore(Bundle s) {
        // TODO Auto-generated method stub
        setCurrentPath(s.getString("path"));
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_filePathText = (TextView) findViewById(R.id.filePathText);
        m_fileFilterEdit = (EditText) findViewById(R.id.fileFilterEdit);
        Util.focusKbHide(m_fileFilterEdit);
        /*
         * m_fileFilterEdit.setOnFocusChangeListener(new
         * EditText.OnFocusChangeListener() {
         * 
         * @Override public void onFocusChange(View v, boolean hasFocus) { //
         * TODO Auto-generated method stub if (!hasFocus) { InputMethodManager
         * imm = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
         * imm.hideSoftInputFromWindow(m_fileFilterEdit.getWindowToken(), 0); }
         * 
         * } });
         */
        m_fileFilterEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                FileListAdapter adapter = (FileListAdapter) m_fileListView
                        .getAdapter();
                if (adapter != null) {
                    Log.d(App.TAG, "setting filter " + s);
                    adapter.pattern(s);
                }

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count,
                    int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        m_fileListView = (ListView) findViewById(R.id.fileListView);
        //m_fileListView.setSelector(android.R.color.transparent);
        registerForContextMenu(m_fileListView);
        /*
         * m_fileListView .setOnItemLongClickListener(new
         * ListView.OnItemLongClickListener() {
         * 
         * @Override public boolean onItemLongClick(AdapterView<?> parent, View
         * view, int position, long id) { doItemLongClick(position); return
         * false; } });
         */
        m_fileListView
                .setOnItemSelectedListener(new ListView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(AdapterView<?> arg0, View arg1,
                            int arg2, long arg3) {
                        // TODO Auto-generated method stub
                        doItemSelected(arg2);

                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> arg0) {
                        // TODO Auto-generated method stub

                    }
                });
        m_fileListView
                .setOnItemClickListener(new ListView.OnItemClickListener() {

                    @Override
                    public void onItemClick(AdapterView<?> arg0, View arg1,
                            int position, long arg3) {
                        doItemClick(position);
                        // TODO Auto-generated method stub

                    }
                });

        if (savedInstanceState != null) {
            restore(savedInstanceState);
        }
        if (App.session == null || !App.session.isConnected()) {
            startActivityForResult(new Intent(this, LoginActivity.class),
                    REQ_LOGIN);
        } else {
            onLogged();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // TODO Auto-generated method stub
        super.onCreateOptionsMenu(menu);
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.appmenu, menu);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
            ContextMenuInfo menuInfo) {
        // TODO Auto-generated method stub
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inf = getMenuInflater();
        inf.inflate(R.menu.exploreitemmenu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // TODO Auto-generated method stub
        if (item.getItemId() == R.id.quit) {
            Intent intent = new Intent(SSHExplorerActivity.this,
                    ExchangeService.class);
            unbindService(m_exchangeBridge);
            m_exchangeBridge = null;
            stopService(intent);
            finish();
            return true;
        }
        if (item.getItemId() == R.id.login) {
            startActivityForResult(new Intent(this, LoginActivity.class),
                    REQ_LOGIN);

        }
        return false;
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.downloadSelected) {
            FileListAdapter adapter = (FileListAdapter) m_fileListView
                    .getAdapter();
            List<FileEntry> entries = adapter.getCheckedEntries();
            Log.d(App.TAG, entries.size() + " items for download");
            for (FileEntry fileE : entries) {
                if (!fileE.dir) {
                    download(fileE);
                }
                adapter.clearCheck(fileE);
            }
            return true;
        } else if (item.getItemId() == R.id.cancelAllDownloads) {
            m_exchangeService.cancelAllDownloads();
        }
        return false;
    }

    private void doItemClick(int position) {
        Log.d(App.TAG, "click " + position);
        FileEntry e = (FileEntry) m_fileListView.getAdapter().getItem(position);
        if (e.dir) {
            pushPath(e.name);
            ls();
        } else {
            FileListAdapter a = (FileListAdapter)m_fileListView.getAdapter();
            a.toggle(position);
        }
    }

    public void onFileHomeClick(View v) {
        setCurrentPath("");
        ls();
    }

    public void onFileUpClick(View v) {
        if (m_fs == null)
            return;

        try {
            setCurrentPath(m_fs.upPath(getCurrentPath()));
            ls();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            error(e);
        }
    }

    private void doItemSelected(int position) {
        Log.d(App.TAG, "selected " + position);
    }

    /*
     * private void doItemLongClick(int position) { Log.d(TAG, "long click " +
     * position); // TODO Auto-generated method stub
     * 
     * FileEntry e = (FileEntry) m_fileListView.getAdapter().getItem(position);
     * if (!e.dir) { download(e); } }
     */

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (!currentPathEmpty()) {
                popPath();
                ls();
                return true;
            }
        }
        // TODO Auto-generated method stub
        return super.onKeyDown(keyCode, event);
    }

    private void pushPath(String seg) {
        if (currentPathEmpty()) {
            setCurrentPath(seg);
        } else if (getCurrentPath().endsWith("/")) {
            setCurrentPath(getCurrentPath() + seg);
        } else {
            setCurrentPath(getCurrentPath() + ("/" + seg));
        }
    }

    private void popPath() {
        if (currentPathEmpty())
            return;
        int i = getCurrentPath().lastIndexOf('/');
        if (i != -1) {
            setCurrentPath(getCurrentPath().substring(0, i));
        } else {
            setCurrentPath("");
        }
    }

    private boolean currentPathEmpty() {
        return getCurrentPath() == null || getCurrentPath().equals("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_LOGIN) {
            setCurrentPath(data.getStringExtra("path"));
            onLogged();
        }
    }

    private void onLogged() {
        try {
            m_fs = new SSHFileSystem(App.session);
            ls();
        } catch (JSchException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            error(e);
        }
    }

    private void error(Throwable e) {
        Util.error(this, e);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

        m_exchangeBridge = new ExchangeBridge();
        Intent intent = new Intent(SSHExplorerActivity.this,
                ExchangeService.class);
        bindService(intent, m_exchangeBridge, 0);// Service.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if (m_exchangeBridge != null) {
            unbindService(m_exchangeBridge);
        }
    }

    private void download(FileEntry e) {
        if (m_exchangeService != null) {
            m_exchangeService.download(m_fs, e.fullname(), e.size);
        } else {
            Log.w(App.TAG, "download request but service is NULL");
        }
    }

    private void ls() {
        new LsTask(getCurrentPath()).execute(null);
    }

    private void setCurrentPath(String currentPath) {
        //if (m_fs == null) {
            m_currentPath = currentPath;
        /*} else {
            try {
                m_currentPath = m_fs.normPath(currentPath);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                error(e);
                e.printStackTrace();
            }
        }*/
        m_filePathText.setText(currentPath);
    }

    private String getCurrentPath() {
        return m_currentPath;
    }

    class ExchangeBridge implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            m_exchangeService = ((ExchangeService.ExchangeBinder) service)
                    .service();
            Log.d(App.TAG, "service connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // TODO Auto-generated method stub
            m_exchangeService = null;

        }
    }

    class LsTask extends AsyncTask<Void, Integer, List<FileEntry>> {
        private String m_path;
        private ProgressDialog m_progress;

        public LsTask(String path) {
            m_path = path;
        }

        @Override
        protected List<FileEntry> doInBackground(Void... params) {
            try {
                return m_fs.entries(m_path);
            } catch (IOException e) {
                e.printStackTrace();
                error(e);
                return new ArrayList<FileEntry>();
            }
        }

        @Override
        protected void onPreExecute() {
            m_progress = new ProgressDialog(SSHExplorerActivity.this);
            m_progress.setMessage("Listing files...");
            m_progress.setIndeterminate(true);
            m_progress.setCancelable(false);
            m_progress.show();
        }

        @Override
        protected void onPostExecute(List<FileEntry> values) {
            m_progress.dismiss();
            FileListAdapter adapter = new FileListAdapter(
                    SSHExplorerActivity.this, values);
            m_fileListView.setAdapter(adapter);
            m_fileFilterEdit.setText("");

        }
    }
}