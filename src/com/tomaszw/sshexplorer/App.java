package com.tomaszw.sshexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;

import com.jcraft.jsch.Session;

public class App extends Application {
    public static Session session;
    public static final String TAG = "ssh-explorer";
    
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
