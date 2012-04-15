package org.pimps.sshexplorer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Util {

    public static void focusKbHide(View v) {
        v.setOnFocusChangeListener(new View.OnFocusChangeListener() {

            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                // TODO Auto-generated method stub
                if (!hasFocus) {
                    InputMethodManager imm = (InputMethodManager) v
                            .getContext().getSystemService(
                                    Application.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }

            }
        });
    }

    public static void error(final Activity activity, Throwable t) {
        while (t.getCause() != null) t = t.getCause();
        t.printStackTrace();
        errorText(activity, t.getMessage());
    }
    
    public static void errorText(final Activity activity, final String message) {
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

}
