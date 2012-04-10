package com.tomaszw.sshexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class ExchangeService extends Service {
    public static final String TAG = "exchange-service";
    public static final int CHUNK_SIZE = 0x1000;

    private List<DownloadEntry> m_entries = new ArrayList<DownloadEntry>();
    private DownloadTask m_dltask = null;

    private IBinder m_binder = new ExchangeBinder();

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(TAG, "created");
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "destroyed");
        // TODO Auto-generated method stub
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return m_binder;
    }

    public void download(FileSystem fs, String path, long estimatedSize) {
        Log.d(TAG, "download " + path);
        DownloadEntry e = new DownloadEntry();
        e.filesystem = fs;
        e.filePath = path;
        e.downloaded = 0;
        e.size = estimatedSize;
        synchronized (m_entries) {
            m_entries.add(e);
            if (m_dltask == null) {
                m_dltask = new DownloadTask();
                m_dltask.execute(null);
            }
        }
    }

    class DownloadEntry {
        public FileSystem filesystem;
        public String filePath;
        public long downloaded;
        public long size;
    }

    private String cutFileName(String path) {
        int i = path.lastIndexOf('/');
        if (i < 0) {
            return path;
        }
        return path.substring(i + 1);
    }

    class DownloadTask extends AsyncTask<Void, Double, Void> {
        private boolean m_running;

        public DownloadTask() {
            m_running = true;
        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            m_running = false;
        }

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress((double) 0);
            try {
                while (m_running) {
                    DownloadEntry e = null;
                    synchronized (m_entries) {
                        if (m_entries.isEmpty()) {
                            m_dltask = null;
                            break;
                        }
                        e = m_entries.get(0);
                    }
                    Log.d(TAG, "start downloading " + e.filePath + " ("
                            + e.size + " bytes)");
                    try {
                        String dst = new File(
                                Environment.getExternalStorageDirectory(),
                                cutFileName(e.filePath)).getAbsolutePath();
                        scpFrom(e, dst);
                        synchronized (m_entries) {
                            m_entries.remove(0);
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                        // FIXME: error handling
                        // error("Transfer error: " + ex.getMessage());
                    }
                }
            } finally {
                publishProgress((double) 1);
            }
            return null;
        }

        private void scpFrom(DownloadEntry entry, String dstPath)
                throws IOException {
            String srcPath = entry.filePath;
            FileSystem fs = entry.filesystem;

            Log.d(TAG, "size (estimate 1) = " + entry.size);
            InputStream in = fs.input(srcPath);
            /* better size estimate if supported */
            if (in instanceof KnownSize) {
                entry.size = ((KnownSize) in).knownSize();
                Log.d(TAG, "size (estimate 2) = " + entry.size);
            }
            long totalSize = entry.size;

            byte[] buf = new byte[CHUNK_SIZE];
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
                    entry.downloaded = totalRead;
                    if (totalSize != 0) {
                        publishProgress(computeProgress());
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
        }

        private double computeProgress() {
            long total = 0;
            long done = 0;
            synchronized (m_entries) {
                for (DownloadEntry e : m_entries) {
                    total += e.size;
                    done += e.downloaded;
                }
                return (double) done / (double) total;
            }
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            // TODO Auto-generated method stub
            long p = Math.round(values[0] * 100);
            Log.d(TAG, "progress " + p + "%");
        }
    }

    public class ExchangeBinder extends Binder {
        public ExchangeService service() {
            return ExchangeService.this;
        }
    }

}
