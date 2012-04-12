package com.tomaszw.sshexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.jcraft.jsch.Session;

public class App extends Application {
    public static Session session;
    public static final String TAG = "ssh-explorer";

    public static final void d(String m) {
        Log.d(TAG, m);
    }

    public static void error(final Activity activity, final String message) {
        activity.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                AlertDialog.Builder alert = new AlertDialog.Builder(activity);
                alert.setTitle("Error");
                alert.setMessage(message);
                alert.show();
            }
        });
    }

    public static void kbhide(View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) v.getContext().getSystemService(INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

            }
        });
    }
}
