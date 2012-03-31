package com.tomaszw.sshexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import com.jcraft.jsch.UserInfo;

public class SSHExplorerActivity extends Activity {
    public static final String TAG = "ssh-explorer";
    public static final int REQ_LOGIN = 0;
    private ListView m_fileListView;
    private EditText m_fileFilterEdit;
    private FileSystem m_fs;
    private String m_currentPath;
    private Session m_session;

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
        m_fileListView
                .setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> parent,
                            View view, int position, long id) {
                        doItemLongClick(position);
                        return false;
                    }
                });
        m_fileListView.setOnItemSelectedListener(new ListView.OnItemSelectedListener() {

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
        m_fileListView.setOnItemClickListener(new ListView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position,
                    long arg3) {
                doItemClick(position);
                // TODO Auto-generated method stub
                
            }
        });
        
        startActivityForResult(new Intent(this, LoginActivity.class), REQ_LOGIN);
    }

    private void doItemClick(int position) {
        Log.d(TAG, "click " + position);
    }
    
    private void doItemSelected(int position) {
        Log.d(TAG, "selected " + position);
    }
    
    private void doItemLongClick(int position) {
        // TODO Auto-generated method stub
        FileEntry e = (FileEntry) m_fileListView.getAdapter()
                .getItem(position);
        if (!e.dir) {
            download(e);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_LOGIN) {
            Login l = data.getExtras().getParcelable("login");
            login(l);
        }
    }

    private void login(final Login data) {
        // TODO Auto-generated method stub
        Log.d(TAG, "Logging " + data.user + "@" + data.host);
        new LoginTask(data).execute(null);
    }

    private void error(final String m) {

        runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        SSHExplorerActivity.this);
                alert.setTitle("Error");
                alert.setMessage(m);
                alert.show();
            }
        });
    }

    private void download(FileEntry e) {
        new DownloadTask(e).execute(null);
    }

    class LoginTask extends AsyncTask<Void, Void, Void> {
        private Login m_data;
        private ProgressDialog m_progress;

        public LoginTask(Login data) {
            m_data = data;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            JSch jsch = new JSch();
            try {
                m_session = jsch.getSession(m_data.user, m_data.host, 22);
                m_session.setUserInfo(new UserInfo() {

                    @Override
                    public void showMessage(String arg0) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public boolean promptYesNo(String arg0) {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    @Override
                    public boolean promptPassword(String arg0) {
                        // TODO Auto-generated method stub
                        return true;
                    }

                    @Override
                    public boolean promptPassphrase(String arg0) {
                        // TODO Auto-generated method stub
                        return false;
                    }

                    @Override
                    public String getPassword() {
                        // TODO Auto-generated method stub
                        return m_data.pass;
                    }

                    @Override
                    public String getPassphrase() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                });
                Log.d(TAG, "establishing session");
                m_session.connect();
                m_currentPath = "";
                m_fs = new SSHFileSystem(m_session);
            } catch (JSchException e) {
                e.printStackTrace();
                error(e.getMessage());
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            m_progress = new ProgressDialog(SSHExplorerActivity.this);
            m_progress.setMessage("Connecting...");
            m_progress.setIndeterminate(true);
            m_progress.setCancelable(false);
            m_progress.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            m_progress.dismiss();
            new LsTask(m_currentPath).execute(null);
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

        public DownloadTask(FileEntry e) {
            m_entry = e;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            try {
                scpFrom(m_entry.fullname(),
                        new File(Environment.getExternalStorageDirectory(),
                                m_entry.name).getAbsolutePath());
            } catch (IOException e) {
                error("Transfer error: " + e.getMessage());
            }
            return null;
        }

        private void scpFrom(String srcPath, String dstPath) throws IOException {
            long totalSize = m_entry.size;
            InputStream in = m_fs.input(srcPath);
            /* better size estimate if supported */
            if (in instanceof KnownSize) {
                totalSize = ((KnownSize) in).knownSize();
            }

            byte[] buf = new byte[4096];
            publishProgress((double) 0);

            FileOutputStream fos = new FileOutputStream(dstPath);
            long totalRead = 0;
            try {
                for (;;) {
                    int r = in.read(buf);
                    if (r < 0) {
                        break;
                    }
                    totalRead += r;
                    fos.write(buf, 0, r);
                    if (totalSize != 0) {
                        publishProgress((double) totalRead / (double) totalSize);
                    }
                }
            } finally {
                fos.close();
                fos = null;
            }
            if (totalRead < totalSize) {
                Log.e(TAG, "only " + totalRead + " bytes read out of "
                        + totalSize);
            }
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            // TODO Auto-generated method stub
            setProgress((int) Math.round(values[0] * 10000));
        }
    }
}