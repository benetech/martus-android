package org.martus.client.android;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.Exceptions;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class UploadRightsTask extends AsyncTask<Object, Void, NetworkResponse> {
    @Override
    protected NetworkResponse doInBackground(Object... params) {

        final ClientSideNetworkGateway gateway = (ClientSideNetworkGateway)params[0];
        final MartusSecurity signer = (MartusSecurity)params[1];
        final String magicWord = (String)params[2];

        NetworkResponse result = null;

        try {
            result = gateway.getUploadRights(signer, magicWord);
        } catch (MartusCrypto.MartusSignatureException e) {
            Log.e("martus", "problem getting upload rights", e);
        }

        return result;
    }
}
