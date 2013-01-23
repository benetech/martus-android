package org.martus.android;

import org.martus.common.crypto.MartusSecurity;

import android.util.Log;

/**
 * @author roms
 *         Date: 1/9/13
 */
public class LogOutProcess implements Runnable {

    private BaseActivity myActivity;

    public LogOutProcess(BaseActivity myActivity) {
        this.myActivity = myActivity;
    }

    @Override
    public void run() {
        if (MartusApplication.isIgnoreInactivity()) {
            return;
        }

        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();
        if (null != martusCrypto) {
            Log.i(AppConfig.LOG_LABEL, "!!!! About to clear keypair !!!!!");
            martusCrypto.clearKeyPair();
        }
        if (null != myActivity) {
            myActivity.close();
        }
    }
}
