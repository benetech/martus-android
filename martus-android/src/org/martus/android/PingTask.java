package org.martus.android;

import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class PingTask extends AsyncTask<XmlRpcClient, Void, String> {
    @Override
    protected String doInBackground(XmlRpcClient... clients) {

        final Vector params = new Vector();
        final XmlRpcClient client = clients[0];
        String result = null;
        try {
            result = (String) client.execute("MartusServer.ping", params);
        } catch (XmlRpcException e) {
            Log.e("martus", "xmlrpc ping failed", e);
        }
        return result;
    }
}
