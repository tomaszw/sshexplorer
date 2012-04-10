package com.tomaszw.sshexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;

import com.jcraft.jsch.JSchException;

public class SSHExplorerActivity extends Activity {
    public static final String TAG = "ssh-explorer";
    public static final int REQ_LOGIN = 0;
    private ListView m_fileListView;
    private EditText m_fileFilterEdit;
    private FileSystem m_fs;
    private String m_currentPath;
    private ExchangeService m_exchangeService;
    private ExchangeBridge m_exchangeBridge;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_PROGRESS);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_fileFilterEdit = (EditText) findViewById(R.id.fileFilterEdit);
        m_fileFilterEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before,
                    int count) {
                FileListAdapter adapter = (FileListAdapter) m_fileListView
                        .getAdapter();
                if (adapter != null) {
                    Log.d(TAG, "setting filter " + s);
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

        startActivityForResult(new Intent(this, LoginActivity.class), REQ_LOGIN);
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
    public boolean onContextItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.downloadSelected) {
            FileListAdapter adapter = (FileListAdapter) m_fileListView
                    .getAdapter();
            List<FileEntry> entries = adapter.getCheckedEntries();
            Log.d(TAG, entries.size() + " items for download");
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
        Log.d(TAG, "click " + position);
        FileEntry e = (FileEntry) m_fileListView.getAdapter().getItem(position);
        if (e.dir) {
            pushPath(e.name);
            ls();
        }
    }

    private void doItemSelected(int position) {
        Log.d(TAG, "selected " + position);
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
                return true;
            }
        }
        // TODO Auto-generated method stub
        return super.onKeyDown(keyCode, event);
    }

    private void pushPath(String seg) {
        if (currentPathEmpty()) {
            m_currentPath = seg;
        } else if (m_currentPath.endsWith("/")) {
            m_currentPath += seg;
        } else {
            m_currentPath += "/" + seg;
        }
    }

    private void popPath() {
        if (currentPathEmpty())
            return;
        int i = m_currentPath.lastIndexOf('/');
        if (i != -1) {
            m_currentPath = m_currentPath.substring(0, i);
        } else {
            m_currentPath = "";
        }
        ls();
    }

    private boolean currentPathEmpty() {
        return m_currentPath == null || m_currentPath.equals("");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_LOGIN) {
            m_currentPath = "";
            try {
                m_fs = new SSHFileSystem(App.session);
                ls();
            } catch (JSchException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                error(e.getMessage());
            }
        }
    }

    private void error(final String m) {
        App.error(this, m);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

        m_exchangeBridge = new ExchangeBridge();
        Intent intent = new Intent(SSHExplorerActivity.this,
                ExchangeService.class);
        bindService(intent, m_exchangeBridge, Service.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        if (m_exchangeService != null) {
            unbindService(m_exchangeBridge);
        }
    }

    private void download(FileEntry e) {
        if (m_exchangeService != null) {
            m_exchangeService.download(m_fs, e.fullname(), e.size);
        } else {
            Log.w(TAG, "download request but service is NULL");
        }
    }

    private void ls() {
        new LsTask(m_currentPath).execute(null);
    }

    class ExchangeBridge implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // TODO Auto-generated method stub
            m_exchangeService = ((ExchangeService.ExchangeBinder) service)
                    .service();
            Log.d(TAG, "service connected");
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
                error(e.getMessage());
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

    class DownloadTask extends AsyncTask<Void, Double, Void> {
        private FileEntry m_entry;
        private boolean m_running;

        public DownloadTask(FileEntry e) {
            m_entry = e;
            m_running = true;
        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            m_running = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            Log.d(TAG, "start download task of " + m_entry.name);
            try {
                scpFrom(m_entry.fullname(),
                        new File(Environment.getExternalStorageDirectory(),
                                m_entry.name).getAbsolutePath());
            } catch (IOException e) {
                e.printStackTrace();
                error("Transfer error: " + e.getMessage());
            }
            return null;
        }

        private void scpFrom(String srcPath, String dstPath) throws IOException {
            long totalSize = m_entry.size;
            Log.d(TAG, "size (estimate 1) = " + totalSize);
            InputStream in = m_fs.input(srcPath);
            /* better size estimate if supported */
            if (in instanceof KnownSize) {
                totalSize = ((KnownSize) in).knownSize();
                Log.d(TAG, "size (estimate 2) = " + totalSize);
            }

            byte[] buf = new byte[4096];
            publishProgress((double) 0);
            try {
                FileOutputStream fos = new FileOutputStream(dstPath);
                long totalRead = 0;
                try {
                    while (totalRead < totalSize) {
                        if (!m_running) {
                            break;
                        }
                        int r = in.read(buf);
                        if (r < 0) {
                            // eof
                            break;
                        }
                        Log.d(TAG, "read " + r + " bytes");
                        totalRead += r;
                        fos.write(buf, 0, r);
                        if (totalSize != 0) {
                            publishProgress((double) totalRead
                                    / (double) totalSize);
                        }
                    }
                } finally {
                    try {
                        fos.close();
                    } catch (Throwable e) {
                    }

                    fos = null;
                }
                if (totalRead < totalSize) {
                    Log.e(TAG, "only " + totalRead + " bytes read out of "
                            + totalSize);
                } else {
                    Log.d(TAG, "done " + totalRead + " bytes");
                }
            } finally {
                publishProgress((double) 1);
            }
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            // TODO Auto-generated method stub
            setProgress((int) Math.round(values[0] * 10000));
        }
    }
}