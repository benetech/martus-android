package org.martus.android;

import android.app.Application;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {


    public static boolean ignoreInactivity = false;

    public void setIgnoreInactivity(boolean ignore) {
        ignoreInactivity = ignore;
    }

    public static boolean isIgnoreInactivity() {
        return ignoreInactivity;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();

        initSingletons();
    }

    protected void initSingletons()
    {
        AppConfig.initInstance(this.getCacheDir(), this.getApplicationContext());
    }


}
