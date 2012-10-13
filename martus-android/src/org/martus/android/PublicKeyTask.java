package org.martus.android;

import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.martus.common.Exceptions;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NonSSLNetworkAPI;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class PublicKeyTask extends AsyncTask<Object, Void, String> {
    @Override
    protected String doInBackground(Object... params) {

        final NonSSLNetworkAPI server = (NonSSLNetworkAPI)params[0];
        final MartusSecurity security = (MartusSecurity)params[1];
        String result = null;

        try {
            result = server.getServerPublicKey(security);
        } catch (Exceptions.ServerNotAvailableException e) {
            Log.e("martus", "server not available", e);
        } catch (MartusUtilities.PublicInformationInvalidException e) {
            Log.e("martus", "invalid public info", e);
        }
        return result;
    }
}
