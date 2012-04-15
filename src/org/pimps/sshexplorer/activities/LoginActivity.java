package org.pimps.sshexplorer.activities;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.UnknownHostException;

import org.pimps.sshexplorer.App;
import org.pimps.sshexplorer.ExchangeService;
import org.pimps.sshexplorer.Login;
import org.pimps.sshexplorer.R;
import org.pimps.sshexplorer.Util;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

public class LoginActivity extends Activity implements OnClickListener,
        ServiceConnection {
    private EditText m_editHost;
    private EditText m_editUser;
    private EditText m_editPassword;
    private EditText m_editRemotePath;
    private Button m_btnLogin;
    private ExchangeService m_exchangeService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);
        m_editHost = (EditText) findViewById(R.id.editHost);
        m_editUser = (EditText) findViewById(R.id.editUser);
        m_editPassword = (EditText) findViewById(R.id.editPassword);
        m_editRemotePath = (EditText) findViewById(R.id.editRemotePath);

        View[] views = { m_editHost, m_editUser, m_editPassword,
                m_editRemotePath };
        for (View v : views) {
            Util.focusKbHide(v);
        }
        m_btnLogin = (Button) findViewById(R.id.loginBtn);
        m_btnLogin.setOnClickListener(this);
        Login l = new Login();
        try {
            FileInputStream f = openFileInput("login.dat");
            l.readFromFile(f);
            f.close();
            App.d("login read");
        } catch (IOException e) {
            e.printStackTrace();
        }
        setLogin(l);
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
        m_editRemotePath.setText(l.path);
    }

    private void fillLogin(Login l) {
        l.host = ((EditText) findViewById(R.id.editHost)).getText().toString();
        l.user = ((EditText) findViewById(R.id.editUser)).getText().toString();
        l.pass = ((EditText) findViewById(R.id.editPassword)).getText()
                .toString();
        l.path = m_editRemotePath.getText().toString();
    }

    @Override
    public void onClick(View v) {
        if (v == m_btnLogin) {
            Login l = getLogin();
            FileOutputStream f;
            try {
                f = openFileOutput("login.dat", Context.MODE_PRIVATE);
                try {
                    l.writeToFile(f);
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        f.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (FileNotFoundException ex) {
                // TODO Auto-generated catch block
                ex.printStackTrace();
            }
            login(l);
        }
    }

    private void login(final Login data) {
        // TODO Auto-generated method stub
        Log.d(App.TAG, "Logging " + data.user + "@" + data.host);
        new LoginTask(data).execute(null);
    }

    private void error(final String m) {
        Util.errorText(this, m);
    }

    @Override
    protected void onStart() {
        // TODO Auto-generated method stub
        super.onStart();

        Intent intent = new Intent(LoginActivity.this, ExchangeService.class);
        bindService(intent, this, 0);// Service.BIND_AUTO_CREATE);
        startService(intent);
    }

    @Override
    protected void onStop() {
        // TODO Auto-generated method stub
        super.onStop();
        unbindService(this);
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
        // TODO Auto-generated method stub
        m_exchangeService = ((ExchangeService.ExchangeBinder) service)
                .service();
        Log.d(App.TAG, "login activity: service connected");
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        // TODO Auto-generated method stub
        m_exchangeService = null;

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
            try {
                m_exchangeService.login(m_data, null);
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
            if (m_progress != null && m_progress.isShowing()) {
                m_progress.dismiss();
            }
            if (m_connected) {
                Intent data = new Intent();
                // data.putExtra("login", getLogin());
                data.putExtra("path", getLogin().path);
                setResult(RESULT_OK, data);
                finish();
            }
        }
    }
}
