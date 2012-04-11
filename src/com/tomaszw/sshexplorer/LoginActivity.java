package com.tomaszw.sshexplorer;

import java.net.UnknownHostException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class LoginActivity extends Activity implements OnClickListener {
    private EditText m_editHost;
    private EditText m_editUser;
    private EditText m_editPassword;
    private Button m_btnLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        m_editHost = (EditText) findViewById(R.id.editHost);
        m_editUser = (EditText) findViewById(R.id.editUser);
        m_editPassword = (EditText) findViewById(R.id.editPassword);
        m_btnLogin = (Button) findViewById(R.id.loginBtn);
        m_btnLogin.setOnClickListener(this);
        setLogin(new Login());
    }

    public Login getLogin() {
        Login l = new Login();
        fillLogin(l);
        return l;
    }

    public void setLogin(Login l) {
        m_editHost.setText(l.host);
        m_editUser.setText(l.user);
        m_editPassword.setText(l.pass);
    }

    private void fillLogin(Login l) {
        l.host = ((EditText) findViewById(R.id.editHost)).getText().toString();
        l.user = ((EditText) findViewById(R.id.editUser)).getText().toString();
        l.pass = ((EditText) findViewById(R.id.editPassword)).getText()
                .toString();
    }

    @Override
    public void onClick(View v) {
        if (v == m_btnLogin) {
            login(getLogin());
        }
    }

    private void login(final Login data) {
        // TODO Auto-generated method stub
        Log.d(App.TAG, "Logging " + data.user + "@" + data.host);
        new LoginTask(data).execute(null);
    }

    private void error(final String m) {
        App.error(this, m);
    }

    class LoginTask extends AsyncTask<Void, Void, Void> {
        private Login m_data;
        private ProgressDialog m_progress;
        private boolean m_connected = false;

        public LoginTask(Login data) {
            m_data = data;
        }

        @Override
        protected Void doInBackground(Void... params) {
            // TODO Auto-generated method stub
            JSch jsch = new JSch();
            try {
                Session session = jsch.getSession(m_data.user, m_data.host, 22);
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
                        return m_data.pass;
                    }

                    @Override
                    public String getPassphrase() {
                        // TODO Auto-generated method stub
                        return null;
                    }
                });
                
                Log.d(App.TAG, "establishing session");
                session.setConfig("compression.s2c", "none");
                session.setConfig("compression.c2s", "none");
                session.connect();
                // rekey for scp performance
                session.setConfig("cipher.s2c", "arcfour,aes128-cbc,blowfish-cbc,3des-cbc");
                session.setConfig("cipher.c2s", "arcfour,aes128-cbc,blowfish-cbc,3des-cbc");
                session.rekey();
                App.session = session;
                m_connected = true;
            } catch (Exception e) {
                e.printStackTrace();
                if (e.getCause() instanceof UnknownHostException) {
                    error(" Unknown host: " + m_data.host);
                } else {
                    error(e.getMessage());
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            m_progress = new ProgressDialog(LoginActivity.this);
            m_progress.setMessage("Connecting...");
            m_progress.setIndeterminate(true);
            m_progress.setCancelable(false);
            m_progress.show();
        }

        @Override
        protected void onPostExecute(Void result) {
            if (m_progress != null) {
                m_progress.dismiss();
            }
            if (m_connected) {
                Intent data = new Intent();
                // data.putExtra("login", getLogin());
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }
}
