package org.martus.android;

import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.fieldspec.StandardFieldSpecs;

import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class AppConfig {

    public static final String LOG_LABEL = "martus";

    private static AppConfig instance;
    private MobileBulletinStore store;
    private MartusSecurity martusCrypto;

    public String customVar;

    public static void initInstance() {
        if (instance == null) {
            // Create the instance
            instance = new AppConfig();
        }
    }

    public static AppConfig getInstance() {
        // Return the instance
        return instance;
    }

    private AppConfig() {
        // Constructor hidden because this is a singleton
        try {
            martusCrypto = new MartusSecurity();
        } catch (MartusCrypto.CryptoInitializationException e) {
            Log.e(LOG_LABEL, "unable to initialize crypto", e);
        }
        martusCrypto.createKeyPair();
        store = new MobileBulletinStore(martusCrypto);
        store.setTopSectionFieldSpecs(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
        store.setBottomSectionFieldSpecs(StandardFieldSpecs.getDefaultBottomSectionFieldSpecs());
    }

    public MartusSecurity getCrypto() {
        return martusCrypto;
    }

    public MobileBulletinStore getStore() {
        return store;
    }

}
