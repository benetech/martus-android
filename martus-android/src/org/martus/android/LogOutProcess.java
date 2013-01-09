package org.martus.android;

import org.martus.common.crypto.MartusSecurity;

import android.util.Log;

/**
 * @author roms
 *         Date: 1/9/13
 */
public class LogOutProcess implements Runnable {
    @Override
    public void run() {
        Log.i(AppConfig.LOG_LABEL, "!!!! About to clear keypair !!!!!");
        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();
        if (null != martusCrypto) {
            martusCrypto.clearKeyPair();
        }
    }
}
