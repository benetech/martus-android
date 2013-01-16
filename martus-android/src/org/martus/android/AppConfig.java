package org.martus.android;

import java.io.File;

import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.fieldspec.StandardFieldSpecs;

import android.content.Context;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class AppConfig {

    public static final String LOG_LABEL = "martus";

    private static AppConfig instance;
    private ClientBulletinStore store;
    private MartusSecurity martusCrypto;

    public static void initInstance(File cacheDir, Context context ) {
        if (instance == null) {
            instance = new AppConfig(cacheDir, context);
        }
    }

    public static AppConfig getInstance() {
        return instance;
    }

    private AppConfig(File cacheDir, Context context) {
        // Constructor hidden because this is a singleton
        try {
            martusCrypto = new MartusSecurity();
        } catch (Exception e) {
            Log.e(LOG_LABEL, "unable to initialize crypto", e);
        }

        store = new ClientBulletinStore(martusCrypto);
        try {
            store.doAfterSigninInitialization(cacheDir);
        } catch (Exception e) {
            Log.e(LOG_LABEL, "unable to initialize store", e);
        }

        //store = new MobileBulletinStore(martusCrypto);
        store.setTopSectionFieldSpecs(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
        store.setBottomSectionFieldSpecs(StandardFieldSpecs.getDefaultBottomSectionFieldSpecs());

    }

    public MartusSecurity getCrypto() {
        return martusCrypto;
    }

    public ClientBulletinStore getStore() {
        return store;
    }

}
