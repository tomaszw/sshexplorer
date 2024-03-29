package org.pimps.sshexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.pimps.sshexplorer.activities.SSHExplorerActivity;
import org.pimps.sshexplorer.stream.ProvidesStreamSize;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class ExchangeService extends Service {
    public static final int BUFFER_SIZE = 32768 * 4;
    private Session m_session;
    private FileSystem m_fs;
    public String currentPath = "";

    private List<DownloadEntry> m_entries = new ArrayList<DownloadEntry>();
    private DownloadTask m_dltask = null;
    private NotificationManager m_notifyManager;
    private static int DOWNLOAD_ID = 1;
    private IBinder m_binder = new ExchangeBinder();

    public interface PasswordPrompter {
        public String promptPassword();
    }

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(App.TAG, "created");
        m_notifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
    }

    public boolean isRemoteConnected() {
        return m_session != null && m_session.isConnected();
    }

    public FileSystem filesystem() {
        return m_fs;
    }

    public void login(final Login data, final PasswordPrompter pp)
            throws Exception {
        // TODO Auto-generated method stub
        JSch jsch = new JSch();
        Session session = jsch.getSession(data.user, data.host, 22);
        session.setUserInfo(new UserInfo() {

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
                if (data.pass != null) {
                    return data.pass;
                }
                return pp.promptPassword();
            }

            @Override
            public String getPassphrase() {
                // TODO Auto-generated method stub
                return null;
            }
        });

        Log.d(App.TAG, "establishing session");
        session.connect();
        // rekey for scp performance
        session.setConfig("cipher.s2c",
                "arcfour,aes128-cbc,blowfish-cbc,3des-cbc");
        session.setConfig("cipher.c2s",
                "arcfour,aes128-cbc,blowfish-cbc,3des-cbc");
        session.setConfig("compression.s2c", "none");
        session.setConfig("compression.c2s", "none");
        session.rekey();
        m_session = session;
        App.d("logged in");
        m_fs = new SSHFileSystem(m_session);
        App.d("filesystem created");
    }

    private void downloadNotification(String tickerText, String contentText) {
        int icon = R.drawable.ic_launcher;
        long when = System.currentTimeMillis();
        Notification notification = new Notification(icon, tickerText, when);
        Context context = getApplicationContext();
        CharSequence contentTitle = "SSH Explorer";
        Intent notificationIntent = new Intent(this, SSHExplorerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0);
        notification.setLatestEventInfo(context, contentTitle, contentText,
                contentIntent);
        m_notifyManager.notify(DOWNLOAD_ID, notification);
    }

    private void cancelDownloadNotification() {
        m_notifyManager.cancel(DOWNLOAD_ID);
    }

    @Override
    public void onDestroy() {
        Log.d(App.TAG, "destroyed");
        // TODO Auto-generated method stub
        cancelAllDownloads();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return m_binder;
    }

    public boolean alreadyDownloading(DownloadEntry a) {
        for (DownloadEntry b : m_entries) {
            if (b.filesystem == a.filesystem && b.filePath.equals(a.filePath)) {
                return true;
            }
        }
        return false;
    }

    public void cancelAllDownloads() {
        if (m_dltask != null) {
            m_dltask.cancel(true);
        }
    }

    public void download(FileSystem fs, String path, long estimatedSize) {
        Log.d(App.TAG, "download " + path);
        DownloadEntry e = new DownloadEntry();
        e.filesystem = fs;
        e.filePath = path;
        e.downloaded = 0;
        e.size = estimatedSize;
        synchronized (m_entries) {
            if (alreadyDownloading(e)) {
                Log.d(App.TAG, "already downloading " + path);
                return;
            }
            m_entries.add(e);
            if (m_dltask == null) {
                m_dltask = new DownloadTask();
                downloadNotification("Transferring", "");
                m_dltask.execute(null);
            }
        }
    }

    private String cutFileName(String path) {
        int i = path.lastIndexOf('/');
        if (i < 0) {
            return path;
        }
        return path.substring(i + 1);
    }

    class DownloadEntry {
        public FileSystem filesystem;
        public String filePath;
        public long downloaded;
        public long size;
    }

    class DownloadTask extends AsyncTask<Void, Double, Void> {
        private boolean m_running;
        private int m_ptr = -1;
        private long m_lastDownloaded = -1;
        private double m_lastKbps = -1;
        private int m_speedBacklog = 5;

        public DownloadTask() {
            m_running = true;
        }

        @Override
        protected void onCancelled() {
            // TODO Auto-generated method stub
            App.d("cancelling download task");
            m_running = false;
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... params) {
            publishProgress((double) 0);
            m_ptr = 0;
            for (;;) {
                DownloadEntry e = null;
                synchronized (m_entries) {
                    if (m_ptr >= m_entries.size() || !m_running) {
                        m_ptr = -1;
                        m_entries.clear();
                        m_dltask = null;
                        cancelDownloadNotification();
                        break;
                    }
                    e = m_entries.get(m_ptr);
                }
                Log.d(App.TAG, "start downloading " + e.filePath + " ("
                        + e.size + " bytes)");
                try {
                    String dst = new File(
                            Environment.getExternalStorageDirectory(),
                            cutFileName(e.filePath)).getAbsolutePath();
                    scpFrom(e, dst);
                    ++m_ptr;
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    synchronized (m_entries) {
                        App.d("xfer error");
                        downloadNotification("Transfer error", ex.getMessage());
                        m_ptr = -1;
                        m_entries.clear();
                        m_dltask = null;
                    }
                    break;
                    // FIXME: error handling
                    // error("Transfer error: " + ex.getMessage());
                }
            }
            if (isCancelled()) {
                downloadNotification("Transfer Cancelled", "Transfer Cancelled");
                cancelDownloadNotification();
            }
            return null;
        }

        private void scpFrom(DownloadEntry entry, String dstPath)
                throws IOException {
            String srcPath = entry.filePath;
            FileSystem fs = entry.filesystem;
            Log.d(App.TAG, "size (estimate 1) = " + entry.size);
            InputStream in = fs.input(srcPath);
            try {
                /* better size estimate if supported */
                if (in instanceof ProvidesStreamSize) {
                    entry.size = ((ProvidesStreamSize) in).streamSize();
                    Log.d(App.TAG, "size (estimate 2) = " + entry.size);
                }

                long totalSize = entry.size;
                byte[] buf = new byte[BUFFER_SIZE];
                OutputStream fos = new FileOutputStream(dstPath);
                long totalDone = 0;
                long timeLoBound = System.currentTimeMillis();
                long time = timeLoBound;
                try {
                    while (totalDone < totalSize) {
                        if (!m_running) {
                            break;
                        }
                        int r = in.read(buf);
                        if (r < 0) {
                            // eof
                            break;
                        }
                        fos.write(buf, 0, r);

                        // Log.d(App.TAG, "read " + r + " bytes");
                        totalDone += r;
                        entry.downloaded = totalDone;
                        time = System.currentTimeMillis();
                        double dt = (double) (time - timeLoBound) / 1000;
                        if (dt >= 1) {
                            timeLoBound = time;
                            publishProgress(dt);
                        }
                    }
                } catch (InterruptedIOException ex) {
                    // cancel likely
                } finally {
                    try {
                        fos.close();
                    } catch (Throwable e) {
                    }

                    fos = null;
                }
                if (totalDone < totalSize) {
                    Log.e(App.TAG, "only " + totalDone + " bytes read out of "
                            + totalSize);
                    // remove partial file
                    new File(dstPath).delete();
                } else {
                    Log.d(App.TAG, "done " + totalDone + " bytes");
                }
            } finally {
                in.close();
            }
        }

        private long totalDownloaded() {
            long done = 0;
            synchronized (m_entries) {
                for (DownloadEntry e : m_entries) {
                    done += e.downloaded;
                }
            }
            return done;
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
            double dt = values[0];
            if (dt <= 0)
                return;
            double progress = computeProgress();
            long done = totalDownloaded();
            double kbps = 0;
            if (m_lastDownloaded > 0) {
                long doneDelta = done - m_lastDownloaded;
                double bps = doneDelta / dt;
                kbps = bps / 1024;
            }
            if (m_lastKbps >= 0) {
                kbps = (m_lastKbps + kbps) / 2;
                m_lastKbps = kbps;
            }

            long p = Math.round(progress * 100);
            downloadNotification("", String.format(
                    "Transferred files (%d/%d): %d%%, %.1f kB/s", m_ptr,
                    m_entries.size(), p, kbps));
            m_lastDownloaded = done;
            Log.d(App.TAG, "progress " + p + "%");
        }
    }

    public class ExchangeBinder extends Binder {
        public ExchangeService service() {
            return ExchangeService.this;
        }
    }

}
