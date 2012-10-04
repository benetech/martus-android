package org.martus.client.android;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.network.NetworkResponse;

import android.os.AsyncTask;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class ServerInfoTask extends AsyncTask<ClientSideNetworkGateway, Void, NetworkResponse> {
    @Override
    protected NetworkResponse doInBackground(ClientSideNetworkGateway... params) {

        final ClientSideNetworkGateway gateway = params[0];
        NetworkResponse result;
        result = gateway.getServerInfo();

        return result;
    }
}
