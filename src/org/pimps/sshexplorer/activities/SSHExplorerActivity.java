package org.pimps.sshexplorer.activities;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.pimps.sshexplorer.App;
import org.pimps.sshexplorer.ExchangeService;
import org.pimps.sshexplorer.FileEntry;
import org.pimps.sshexplorer.FileListAdapter;
import org.pimps.sshexplorer.FileSystem;
import org.pimps.sshexplorer.R;
import org.pimps.sshexplorer.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
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

public class SSHExplorerActivity extends Activity {
    public static final int REQ_LOGIN = 0;
    private ListView m_fileListView;
    private EditText m_fileFilterEdit;
    private ExchangeService m_exchangeService;
    private ExchangeBridge m_exchangeBridge;
    private TextView m_filePathText;
    private View m_contentView;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        App.d("create");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_contentView = this.findViewById(android.R.id.content);
        m_contentView.setVisibility(View.INVISIBLE);

        m_filePathText = (TextView) findViewById(R.id.filePathText);
        m_fileFilterEdit = (EditText) findViewById(R.id.fileFilterEdit);
        Util.focusKbHide(m_fileFilterEdit);
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
        m_fileListView.requestFocus();
        // m_fileListView.setSelector(android.R.color.transparent);
        registerForContextMenu(m_fileListView);
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

        m_exchangeBridge = new ExchangeBridge();
        Intent intent = new Intent(SSHExplorerActivity.this,
                ExchangeService.class);
        bindService(intent, m_exchangeBridge, 0);// Service.BIND_AUTO_CREATE);
        startService(intent);

    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // TODO Auto-generated method stub
        super.onConfigurationChanged(newConfig);
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
        // if (position < m_fileListView.getCount()) {
        Log.d(App.TAG, "click " + position);
        FileEntry e = (FileEntry) m_fileListView.getAdapter().getItem(position);
        if (e.dir) {
            cdInCurrent(e.name);
        } else {
            FileListAdapter a = (FileListAdapter) m_fileListView.getAdapter();
            a.toggle(position);
        }
        // }
    }

    public void onFileHomeClick(View v) {
        cd("");
    }

    public void onFileUpClick(View v) {
        cdUp();
    }

    private void doItemSelected(int position) {
        Log.d(App.TAG, "selected " + position);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            String p = getCurrentPath();
            if (p == null || p.equals("/")) {
                //
            } else {
                cdUp();
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    private void cdInCurrent(String seg) {
        if (currentPathEmpty()) {
            cd(seg);
        } else if (getCurrentPath().endsWith("/")) {
            cd(getCurrentPath() + seg);
        } else {
            cd(getCurrentPath() + ("/" + seg));
        }
    }

    private FileSystem fs() {
        return m_exchangeService.filesystem();
    }

    private void cdUp() {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    cd(fs().upPath(getCurrentPath()));
                } catch (IOException e) {
                    error(e);
                }
                return null;
            }
        }.execute(new Void[] {});
    }

    private boolean currentPathEmpty() {
        return getCurrentPath() == null || getCurrentPath().equals("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_LOGIN) {
            if (resultCode == RESULT_OK) {
                App.d("login OK");
                if (m_contentView != null) {
                    m_contentView.setVisibility(View.VISIBLE);
                }
                setCurrentPath(data.getStringExtra("path"));
                onLogged();
            } else {
                if (m_exchangeService != null
                        && m_exchangeService.isRemoteConnected()) {
                    onLogged();
                } else {
                    finish();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        App.d("resume");
    }

    private void onLogged() {
        cd(getCurrentPath());
    }

    private void error(Throwable e) {
        Util.error(this, e);
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        App.d("destroy");
        if (m_exchangeBridge != null) {
            unbindService(m_exchangeBridge);
        }
        super.onDestroy();
    }

    private void download(FileEntry e) {
        if (m_exchangeService != null) {
            m_exchangeService.download(fs(), e.fullname(), e.size);
        } else {
            Log.w(App.TAG, "download request but service is NULL");
        }
    }

    private void ls() {
        new LsTask(getCurrentPath()).execute(null);
    }

    private void cd(final String path) {
        new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                // TODO Auto-generated method stub
                try {
                    String p = fs().normPath(path);
                    App.d("cd " + p);
                    setCurrentPath(p);
                } catch (IOException e) {
                    error(e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                App.d("cd done, listing");
                ls();
            }
        }.execute(new Void[] {});
    }

    private String getCurrentPath() {
        if (m_exchangeService == null)
            return null;
        return m_exchangeService.currentPath;
    }

    private void setCurrentPath(final String currentPath) {
        m_exchangeService.currentPath = currentPath;
        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                m_filePathText.setText(currentPath);
            }
        });
    }

    class ExchangeBridge implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            m_exchangeService = ((ExchangeService.ExchangeBinder) service)
                    .service();
            App.d("ssh activity: service connected");
            if (!m_exchangeService.isRemoteConnected()) {
                App.d("remote is disconnected");
                startActivityForResult(new Intent(SSHExplorerActivity.this,
                        LoginActivity.class), REQ_LOGIN);
            } else {
                m_contentView.setVisibility(View.VISIBLE);
                App.d("remote is connected");
                onLogged();
            }

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            App.d("warning: service disconnected!");
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
                return fs().entries(m_path);
            } catch (IOException e) {
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
            if (m_progress.isShowing()) {
                m_progress.dismiss();
            }
            FileListAdapter adapter = new FileListAdapter(
                    SSHExplorerActivity.this, values);
            m_fileListView.setAdapter(adapter);
            m_fileFilterEdit.setText("");

        }
    }
}