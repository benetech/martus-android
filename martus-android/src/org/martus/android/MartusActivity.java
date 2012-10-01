package org.martus.android;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.crypto.MockMartusSecurity;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MartusActivity extends Activity {
	    
	//String serverIPNew = "66.201.46.82";
	//String serverIPNew = "50.112.118.184";
	//String serverIPNew = "10.10.220.114";
	String serverIPNew = "54.245.101.104"; //public QA server

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setTitle("Mardus Android");
 	    
        final Button button = (Button) findViewById(R.id.gotoPing);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		Intent intent = new Intent(MartusActivity.this, pingme.class);
                    startActivity(intent);
                    } catch (Exception e) {
					Log.e("error", "Failed starting pingme activity");
					e.printStackTrace();
				}
            }
        });
        
        final Button buttonServerInfo = (Button) findViewById(R.id.serverInfo);
        buttonServerInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
        			MockMartusSecurity security = new MockMartusSecurity();
        			security.createKeyPair();
            		NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIPNew);
            		String serverPublicKey = server.getServerPublicKey(security);
            		ClientSideNetworkGateway gateway = ClientSideNetworkGateway.buildGateway(serverIPNew, serverPublicKey);
                	NetworkResponse response = gateway.getServerInfo();
                    Object[] resultArray = response.getResultArray();

                    final TextView responseView = (TextView)findViewById(R.id.response_server);
                    responseView.setText("ServerInfo: " + response.getResultCode() + ", " + resultArray[0]);
            	} catch (Exception e) {
        			Log.e("Martus", "Failed starting MockMartusSecurity", e);
					e.printStackTrace();
				}
            }
        });
    }
    
}