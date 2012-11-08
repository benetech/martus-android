package org.martus.android;

import android.app.Application;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {

    @Override
    public void onCreate()
    {
        super.onCreate();

        // Initialize the singletons so their instances
        // are bound to the application process.
        initSingletons();
    }

    protected void initSingletons()
    {
        // Initialize the instance of MySingleton
        AppConfig.initInstance(this.getCacheDir());
    }

    public void customAppMethod()
    {
        // Custom application method
    }

}
