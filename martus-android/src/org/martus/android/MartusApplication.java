package org.martus.android;

import org.martus.common.crypto.MartusSecurity;

import android.app.Application;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {

    public static final long DISCONNECT_TIMEOUT = 120000; // 2 min = 5 * 60 * 1000 ms

    private Handler disconnectHandler = new Handler(){
        public void handleMessage(Message msg) {
        }
    };

    private Runnable disconnectCallback = new Runnable() {
        @Override
        public void run() {
            Log.e(AppConfig.LOG_LABEL, " SHOULD  Auto Session Logout !!!!!!!!");
            // Perform any required operation on disconnect
            MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();
            if (null != martusCrypto) {
                //martusCrypto.clearKeyPair();
                Log.e(AppConfig.LOG_LABEL, "Auto Session Logout !!!!!!!!");
            }
        }
    };

    public void resetDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
        disconnectHandler.postDelayed(disconnectCallback, DISCONNECT_TIMEOUT);
    }

    public void stopDisconnectTimer(){
        disconnectHandler.removeCallbacks(disconnectCallback);
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
        resetDisconnectTimer();
    }

    protected void initSingletons()
    {
        // Initialize the instance of MySingleton
        AppConfig.initInstance(this.getCacheDir(), this.getApplicationContext());
    }

    public void customAppMethod()
    {
        // Custom application method
    }

}
