package org.martus.android;

import java.net.URL;
import java.util.Vector;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class pingme extends Activity {

	//String serverIPNew = "http://50.112.118.184/RPC2";
	//String serverIPNew = "http://66.201.46.82:988/RPC2";
	String serverIPNew = "http://54.245.101.104/RPC2";     //public QA server
	XmlRpcClient client = new XmlRpcClient();
	String response;
	TextView textview;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ping);
        //setTitle("Mardus Android");
        
        textview = new TextView(this); 
    	textview=(TextView)findViewById(R.id.response); 
	    
        final Button button = (Button) findViewById(R.id.buttonPing);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		Log.v("martus-xmlrpc", "calling: "+serverIPNew);
            		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            		config.setServerURL(new URL(serverIPNew));
            		XmlRpcClient client = new XmlRpcClient();
            		client.setConfig(config);
            		Vector params = new Vector();
            		String result = (String) client.execute("MartusServer.ping", params);
                  	textview.setText("response: " +result);
            		Log.v("martus-xmlrpc", "response: "+result);
				} catch (Exception e) {
					Log.e("martus-xmlrpc", "xmlrpc call failed", e);
					e.printStackTrace();
				}
            }
        });

        try {
//			MockMartusSecurity security = new MockMartusSecurity();
		} catch (Exception e) {
			Log.e("error", "MartusSecurity failed");
			e.printStackTrace();
		}
    }
    
}