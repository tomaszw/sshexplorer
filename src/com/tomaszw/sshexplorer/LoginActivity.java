package com.tomaszw.sshexplorer;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;

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
		l.host = ((EditText)findViewById(R.id.editHost)).getText().toString();
		l.user = ((EditText)findViewById(R.id.editUser)).getText().toString();
		l.pass = ((EditText)findViewById(R.id.editPassword)).getText().toString();
	}

	@Override
	public void onClick(View v) {
		if (v == m_btnLogin) {
			Intent data = new Intent();
			data.putExtra("login", getLogin());
			setResult(RESULT_OK, data);
			finish();
		}
	}
}
