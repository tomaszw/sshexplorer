package com.tomaszw.sshexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.util.Log;

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
                AlertDialog.Builder alert = new AlertDialog.Builder(
                        activity);
                alert.setTitle("Error");
                alert.setMessage(message);
                alert.show();
            }
        });
    }

}
