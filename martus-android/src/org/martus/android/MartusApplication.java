package org.martus.android;

import org.martus.common.network.TorTransportWrapper;

import android.app.Application;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class MartusApplication extends Application {

	private TorTransportWrapper transport;
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
	    transport = TorTransportWrapper.create();
    }

	public TorTransportWrapper getTransport()
	{
		return transport;
	}
}
