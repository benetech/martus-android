package org.martus.android;

import java.util.Vector;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class GetDraftBulletinsTask extends AsyncTask<Object, Void, String> {
    @Override
    protected String doInBackground(Object... params) {

        final ClientSideNetworkGateway gateway = (ClientSideNetworkGateway)params[0];
        final MartusSecurity signer = (MartusSecurity)params[1];
        final String accountId = (String)params[2];

        NetworkResponse response;
        String result = "";

        Vector bulletinIds = new Vector();
        try {
            response = gateway.getDraftBulletinIds(signer, accountId, bulletinIds);
            result = response.getResultArray().length + "";
        } catch (MartusCrypto.MartusSignatureException e) {
            Log.e("martus", "problem getting draft bulletin ids", e);
        }

        return result;
    }
}
