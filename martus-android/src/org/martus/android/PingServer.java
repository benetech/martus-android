package org.martus.android;

import java.net.URL;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class PingServer extends Activity {

	//String serverIPNew = "http://50.112.118.184/RPC2";
	//String serverIPNew = "http://66.201.46.82:988/RPC2";
	private final static String pingPath = "/RPC2";
	TextView textview;
    private String serverIP;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ping);

        ActionBar actionBar = getActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);

        textview = new TextView(this); 
    	textview=(TextView)findViewById(R.id.response);

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverIP = "http://" + mySettings.getString(SettingsActivity.KEY_SERVER_IP, MartusActivity.defaultServerIP + pingPath);
	    
        final Button button = (Button) findViewById(R.id.buttonPing);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            		config.setServerURL(new URL(serverIP));
            		XmlRpcClient client = new XmlRpcClient();
            		client.setConfig(config);

                    //Network calls must be made in background task
                    final AsyncTask<XmlRpcClient, Void, String> pingTask = new PingTask().execute(client);
                    String result = pingTask.get();

                  	textview.setText("response: " +result);
				} catch (Exception e) {
					Log.e("martus-xmlrpc", "xmlrpc call failed", e);
					e.printStackTrace();
				}
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MartusActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.settings_menu_item:
                intent = new Intent(PingServer.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
}