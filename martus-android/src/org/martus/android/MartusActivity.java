package org.martus.android;

import java.util.Locale;
import java.util.Vector;

import org.apache.xmlrpc.client.XmlRpcClient;
import org.martus.client.android.PingTask;
import org.martus.client.android.ServerInfoTask;
import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.client.core.ConfigInfo;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpc;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MiniLocalization;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.network.NetworkInterface;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MartusActivity extends Activity {

	//String serverIPNew = "50.112.118.184";
	String serverIPNew = "54.245.101.104"; //public QA server
    private final String serverPublicCode = "8338.1685.2173.3777.2823";
    private MobileBulletinStore store;
    private MartusSecurity martusCrypto;
    private ConfigInfo configInfo;
    private Activity myActivity;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setTitle("Mardus Android");
        myActivity = this;

        try {
            martusCrypto = new MartusSecurity();
            martusCrypto.createKeyPair();
            store = new MobileBulletinStore(martusCrypto);
            store.setTopSectionFieldSpecs(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
            store.setBottomSectionFieldSpecs(StandardFieldSpecs.getDefaultBottomSectionFieldSpecs());
            configInfo = new ConfigInfo();
        } catch (MartusCrypto.CryptoInitializationException e) {
            Log.e("martus", "Unable to initialize", e);
        }

 	    
        final Button button = (Button) findViewById(R.id.gotoPing);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
            		Intent intent = new Intent(MartusActivity.this, PingServer.class);
                    startActivity(intent);
                    } catch (Exception e) {
					Log.e("martus", "Failed starting PingServer activity");
					e.printStackTrace();
				}
            }
        });
        
        final Button buttonServerInfo = (Button) findViewById(R.id.serverInfo);
        buttonServerInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {
        			MartusSecurity security = new MartusSecurity();
        			security.createKeyPair();

                    NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIPNew);
                    String serverPublicKey = server.getServerPublicKey(security);

                    //confirm serverPublicKey is correct
                    final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverPublicCode);
                    final String computedCode = MartusCrypto.computePublicCode(serverPublicKey);
                    if (! normalizedPublicCode.equals(computedCode)) {
                        showError(myActivity, "Invalid public server code!");
                        return;
                    }

                    ClientSideNetworkGateway gateway = ClientSideNetworkGateway.buildGateway(serverIPNew, serverPublicKey);

                    //Network calls must be made in background task
                    //final AsyncTask<ClientSideNetworkGateway, Void, NetworkResponse> infoTask = new ServerInfoTask().execute(gateway);
                    //NetworkResponse response = infoTask.get();

                    NetworkResponse response = gateway.getServerInfo();


                    Object[] resultArray = response.getResultArray();
                    final TextView responseView = (TextView)findViewById(R.id.response_server);
                    responseView.setText("ServerInfo: " + response.getResultCode() + ", " + resultArray[0]);
            	} catch (Exception e) {
        			Log.e("martus", "Failed getting server info", e);
					e.printStackTrace();
				}
            }
        });

        final Button uploadSampleButton = (Button) findViewById(R.id.uploadSampleBulletin);
        uploadSampleButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    MartusSecurity security = new MartusSecurity();
                    security.createKeyPair();

                    //security.writeKeyPair();
                    //security.readKeyPair();

                    NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIPNew);
                    String serverPublicKey = server.getServerPublicKey(security);
                    ClientSideNetworkGateway gateway = ClientSideNetworkGateway.buildGateway(serverIPNew, serverPublicKey);



                    Bulletin sample = createBulletin();

                    //NetworkResponse response = gateway.getServerInfo();
                    //Object[] resultArray = response.getResultArray();

                    final TextView responseView = (TextView)findViewById(R.id.bulletinResponseText);
                    responseView.setText("bulletin created successfully");
                    //responseView.setText("ServerInfo: " + response.getResultCode() + ", " + resultArray[0]);
                } catch (Exception e) {
                    Log.e("martus", "Failed uploading bulletin", e);
                    e.printStackTrace();
                }
            }
        });
    }

    public Bulletin createBulletin() throws Exception
    {
        Bulletin b = store.createEmptyBulletin();
        configInfo.setAuthor("Test User");
        configInfo.setOrganization("Benetech");

        b.set(Bulletin.TAGAUTHOR, configInfo.getAuthor());
        b.set(Bulletin.TAGORGANIZATION, configInfo.getOrganization());
        b.set(Bulletin.TAGPUBLICINFO, configInfo.getTemplateDetails());
        b.set(Bulletin.TAGLANGUAGE, getDefaultLanguageForNewBulletin());
        setDefaultHQKeysInBulletin(b);
        b.setDraft();
        b.setAllPrivate(true);
        return b;
    }

    public String getDefaultLanguageForNewBulletin()
    {
        final String preferredLanguage = getCurrentLanguage();
        MiniLocalization localization = new MiniLocalization();
        ChoiceItem[] availableLanguages = localization.getLanguageNameChoices();
        for (ChoiceItem item : availableLanguages) {
            if (item.getCode().equals(preferredLanguage))
                return preferredLanguage;
        }

        return MiniLocalization.LANGUAGE_OTHER;
    }

    private String getCurrentLanguage()
    {
        return Locale.getDefault().getLanguage();
    }

    public void setDefaultHQKeysInBulletin(Bulletin b)
    {
        HQKeys hqKeys = getDefaultHQKeysWithFallback();
        b.setAuthorizedToReadKeys(hqKeys);
    }

    public HQKeys getDefaultHQKeysWithFallback()
    {
        try
        {
            return getDefaultHQKeys();
        }
        catch (HQKeys.HQsException e)
        {
            e.printStackTrace();
            HQKey legacyKey = new HQKey(getLegacyHQKey());
            return new HQKeys(legacyKey);
        }
    }

    public HQKeys getDefaultHQKeys() throws HQKeys.HQsException
    {
        return new HQKeys(configInfo.getDefaultHQKeysXml());
    }

    public String getLegacyHQKey()
    {
        return configInfo.getLegacyHQKey();
    }

    public static void showError( Context context, String msg){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle("Error")
             .setMessage(msg)
             .show();
    }
    
}