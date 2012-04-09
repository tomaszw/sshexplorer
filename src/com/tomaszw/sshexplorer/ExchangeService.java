package com.tomaszw.sshexplorer;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.jcraft.jsch.Session;

public class ExchangeService extends Service {
    public static final String TAG = "exchange-service";
    private IBinder m_binder = new ExchangeBinder();
    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();
        Log.d(TAG, "created");
    }
    
    @Override
    public void onDestroy() {
        Log.d(TAG, "destroyed");
        // TODO Auto-generated method stub
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub
        return m_binder;
    }

    public void download(Session s, String path) {
        Log.d(TAG, "download " + path);
    }

    public class ExchangeBinder extends Binder {
        public ExchangeService service() {
            return ExchangeService.this;
        }
        
    }
    
}
