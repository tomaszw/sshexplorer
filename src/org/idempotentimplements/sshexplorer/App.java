package org.idempotentimplements.sshexplorer;

import android.app.Application;
import android.util.Log;
import android.widget.EditText;

import com.jcraft.jsch.Session;

public class App extends Application {
    public static final String TAG = "ssh-explorer";

    public static final void d(String m) {
        Log.d(TAG, m);
    }
}
