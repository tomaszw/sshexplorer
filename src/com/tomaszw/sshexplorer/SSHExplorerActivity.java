package com.tomaszw.sshexplorer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
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
	private Session m_session;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	requestWindowFeature(Window.FEATURE_PROGRESS);
    	
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        m_fileListView = (ListView) findViewById(R.id.fileListView);
        m_fileListView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				// TODO Auto-generated method stub
				LsEntry e = (LsEntry) m_fileListView.getAdapter().getItem(position);
				if (!e.getAttrs().isDir()) {
					download(e);
				}
				return false;
			}
		});
        
        startActivityForResult(new Intent(this, LoginActivity.class), REQ_LOGIN);
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
				AlertDialog.Builder alert = new AlertDialog.Builder(SSHExplorerActivity.this);
				alert.setTitle("Error");
				alert.setMessage(m);
				alert.show();
			}
		});
	}
	
	private void sftp(ChannelSftp c) {
		new LsTask(c, ".").execute(null);
	}
	
	private void download(LsEntry e) {
		new DownloadTask(m_session, e).execute(null);
	}

	class LoginTask extends AsyncTask<Void, Void, Void> {
		private Login m_data;
		private ChannelSftp m_sftpChannel;
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
				}
				);
				Log.d(TAG, "establishing session");
				m_session.connect();
				Channel channel = m_session.openChannel("sftp");
				channel.connect();
				m_sftpChannel = (ChannelSftp) channel;
				Log.d(TAG, "connected!");
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
			sftp(m_sftpChannel);
		}
	}
	
	class LsTask extends AsyncTask<Void, Integer, List<LsEntry> > {
		private ChannelSftp m_channel;
		private String m_path;
		private ProgressDialog m_progress;

		public LsTask(ChannelSftp c, String path) {
			m_channel = c;
			m_path = path;
		}
		
		@Override
		protected List<LsEntry> doInBackground(Void... params) {
			// TODO Auto-generated method stub
			ArrayList<LsEntry> values = new ArrayList<LsEntry>();
			try {
				Vector v = m_channel.ls(".");
				Log.d(TAG, "received file list");
				for (Object o : v) {
					if (o instanceof LsEntry) {
						LsEntry e = (LsEntry)o;
						values.add(e);
					}
				}
			} catch (SftpException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				error(e.getMessage());
			}
			//Collections.sort(values, new FileListComparator());
			return values;
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
		protected void onPostExecute(List<LsEntry> values) {
			m_progress.dismiss();
			m_fileListView.setAdapter(new FileListAdapter(SSHExplorerActivity.this, values));
		}
	}
	
	class FileListAdapter extends ArrayAdapter<LsEntry> {
		private List<LsEntry> m_values;
		private Context m_context;

		public FileListAdapter(Context c, List<LsEntry> values) {
			super(c, android.R.layout.simple_list_item_checked, android.R.id.text1);
			m_values = values;
			m_context = c;
		}
		
		@Override
		public int getCount() {
			// TODO Auto-generated method stub
			return m_values.size();
		}
		
		@Override
		public LsEntry getItem(int position) {
			// TODO Auto-generated method stub
			return m_values.get(position);
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			// TODO Auto-generated method stub
			CheckedTextView v = new CheckedTextView(m_context);
			LsEntry e = m_values.get(position);
			v.setText(e.getFilename());
			if (e.getAttrs().isDir()) {
				v.setTextColor(Color.BLUE);
			} else {
				int p = e.getAttrs().getPermissions();
				if ((p & 0111) != 0) {
					v.setTextColor(Color.GREEN);
				}
			};
			return v;
		}
	}
	
	static int checkAck(InputStream in) throws IOException {
		int b = in.read();
		// b may be 0 for success,
		// 1 for error,
		// 2 for fatal error,
		// -1
		if (b == 0)
			return b;
		if (b == -1)
			return b;

		if (b == 1 || b == 2) {
			StringBuffer sb = new StringBuffer();
			int c;
			do {
				c = in.read();
				sb.append((char) c);
			} while (c != '\n');
			if (b == 1) { // error
				System.out.print(sb.toString());
			}
			if (b == 2) { // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
	
	class DownloadTask extends AsyncTask<Void, Double, Void> {
		private LsEntry m_entry;
		public DownloadTask(Session s, LsEntry e) {
			m_session = s;
			m_entry = e;
		}
		@Override
		protected Void doInBackground(Void... params) {
			// TODO Auto-generated method stub
			try {
				Log.d(TAG, "download " + m_entry.getFilename());
				ChannelExec channel = (ChannelExec)m_session.openChannel("exec");
				channel.setCommand("scp -f " + m_entry.getFilename());
				OutputStream out = channel.getOutputStream();
				InputStream in = channel.getInputStream();
				channel.connect();
				Log.d(TAG, "connected");
				byte[] buf = new byte[1024];
				// send '\0'
				buf[0] = 0;
				out.write(buf, 0, 1);
				out.flush();

				while (true) {
					int c = checkAck(in);
					if (c != 'C') {
						Log.e(TAG, "bad ack");
						break;
					}

					// read '0644 '
					in.read(buf, 0, 5);

					long filesize = 0L;
					while (true) {
						if (in.read(buf, 0, 1) < 0) {
							// error
							break;
						}
						if (buf[0] == ' ')
							break;
						filesize = filesize * 10L + (long) (buf[0] - '0');
					}

					String file = null;
					for (int i = 0;; i++) {
						in.read(buf, i, 1);
						if (buf[i] == (byte) 0x0a) {
							file = new String(buf, 0, i);
							break;
						}
					}

					Log.d(TAG, "starting");
					publishProgress((double)0);
					
					// send '\0'
					buf[0] = 0;
					out.write(buf, 0, 1);
					out.flush();
					// read a content of lfile
					error("file-size = " + filesize);
					FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), m_entry.getFilename()));
					
					int r;
					long total_read = 0;
					long total_filesize = filesize;
					
					while (true) {
						if (buf.length < filesize)
							r = buf.length;
						else
							r = (int) filesize;
						r = in.read(buf, 0, r);
						if (r < 0) {
							error("transfer error " + r);
							// error
							break;
						}
						fos.write(buf, 0, r);
						filesize -= r;
						total_read += r;
						if (total_filesize != 0) {
							publishProgress((double) total_read / (double)total_filesize);
						}
						Log.d(TAG, "read " + total_read + " bytes");
						if (filesize == 0L)
							break;
					}
					fos.close();
					fos = null;

					if (checkAck(in) != 0) {
						error("transfer error - no ack");
						return null;
					}

					// send '\0'
					buf[0] = 0;
					out.write(buf, 0, 1);
					out.flush();
				}
				
			} catch (final JSchException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				error(e.getMessage());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				error(e.getMessage());
			}
			return null;
		}
		@Override
		protected void onProgressUpdate(Double... values) {
			// TODO Auto-generated method stub
			setProgress((int)Math.round(values[0] * 10000));
		}
	}
	
}