package org.martus.android;

import java.io.File;
import java.util.Locale;

import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.client.core.ConfigInfo;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MiniLocalization;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.database.BulletinStreamer;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

public class MartusActivity extends Activity {

	//String serverIPNew = "50.112.118.184";
	public static final String defaultServerIP = "54.245.101.104"; //public QA server
    public static final String defaultServerPublicCode = "8338.1685.2173.3777.2823";
    public static final String defaultMagicWord = "spam";
    private String serverPublicKey;

    private MobileBulletinStore store;
    private MartusSecurity martusCrypto;
    private ConfigInfo configInfo;
    private Activity myActivity;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private String serverPublicCode;
    private String magicWord;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setTitle("Martus Android");
        myActivity = this;

        updateSettings();

        try {
            martusCrypto = new MartusSecurity();
            martusCrypto.createKeyPair();
            store = new MobileBulletinStore(martusCrypto);
            store.setTopSectionFieldSpecs(StandardFieldSpecs.getDefaultTopSetionFieldSpecs());
            store.setBottomSectionFieldSpecs(StandardFieldSpecs.getDefaultBottomSectionFieldSpecs());
            configInfo = new ConfigInfo();

            NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);

            //Network calls must be made in background task
            final AsyncTask<Object, Void, String> keyTask = new PublicKeyTask().execute(server, martusCrypto);
            serverPublicKey = keyTask.get();
            gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

        } catch (MartusCrypto.CryptoInitializationException e) {
            Log.e("martus", "Unable to initialize crypto", e);
        } catch (Exception e) {
            Log.e("martus", "Problem getting server public key", e);
        }
        
        final Button buttonServerInfo = (Button) findViewById(R.id.serverInfo);
        buttonServerInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
            	try {

                    //confirm serverPublicKey is correct
                    final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverPublicCode);
                    final String computedCode = MartusCrypto.computePublicCode(serverPublicKey);
                    if (! normalizedPublicCode.equals(computedCode)) {
                        showError(myActivity, "Invalid public server code!");
                        return;
                    }

                    //Network calls must be made in background task
                    final AsyncTask<ClientSideNetworkGateway, Void, NetworkResponse> infoTask = new ServerInfoTask().execute(gateway);
                    NetworkResponse response = infoTask.get();

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

                    final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask().execute(gateway, martusCrypto, magicWord);
                    final NetworkResponse response = rightsTask.get();
                    if (!response.getResultCode().equals("ok")) {
                        showError(myActivity, "Don't have upload rights!");
                        return;
                    }

                    final Bulletin sample = createBulletin();
                    final BulletinStreamer bs = new BulletinStreamer(sample);

                    final File cacheDir = getCacheDir();

                    //File in cache directory  - use getDir() for directory of permanent files private to this app
                    final File file = new File(cacheDir, "preUpload.zip");
                    BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(bs, sample.getDatabaseKey(), file, martusCrypto);

                    final AsyncTask<Object, Void, String> uploadTask = new UploadBulletinTask().execute(sample.getUniversalId(), file, gateway, martusCrypto);
                    String result = uploadTask.get();

                    final TextView responseView = (TextView)findViewById(R.id.bulletinResponseText);
                    responseView.setText(result);
                } catch (Exception e) {
                    Log.e("martus", "Failed uploading bulletin", e);
                    e.printStackTrace();
                }
            }
        });

        final Button buttonCheckBulletins = (Button) findViewById(R.id.check_bulletins_button);
        buttonCheckBulletins.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    //Network calls must be made in background task
                    final AsyncTask<Object, Void, String> getIdsTask = new GetDraftBulletinsTask().execute(gateway, martusCrypto, martusCrypto.getPublicKeyString());
                    String response = getIdsTask.get();

                    final TextView responseView = (TextView)findViewById(R.id.check_bulletins_text);
                    responseView.setText(response);
                } catch (Exception e) {
                    Log.e("martus", "Failed getting bulletin ids", e);
                    e.printStackTrace();
                }
            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.settings_menu_item:
                intent = new Intent(MartusActivity.this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.ping_menu_item:
                intent = new Intent(MartusActivity.this, PingServer.class);
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateSettings() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, defaultServerIP);
        serverPublicCode = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_CODE, defaultServerPublicCode);
        magicWord = mySettings.getString(SettingsActivity.KEY_MAGIC_WORD, defaultMagicWord);
    }

    private Bulletin createBulletin() throws Exception
    {
        Bulletin b = store.createEmptyBulletin();
        configInfo.setAuthor("Test User");
        configInfo.setOrganization("Benetech");

        b.set(Bulletin.TAGAUTHOR, configInfo.getAuthor());
        b.set(Bulletin.TAGORGANIZATION, configInfo.getOrganization());
        b.set(Bulletin.TAGPUBLICINFO, configInfo.getTemplateDetails());
        b.set(Bulletin.TAGLANGUAGE, getDefaultLanguageForNewBulletin());
        b.set(Bulletin.TAGTITLE, "Sample bulletin from Android");
        setDefaultHQKeysInBulletin(b);
        b.setDraft();
        b.setAllPrivate(true);
        return b;
    }

    private String getDefaultLanguageForNewBulletin()
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

    private void setDefaultHQKeysInBulletin(Bulletin b)
    {
        HQKeys hqKeys = getDefaultHQKeysWithFallback();
        b.setAuthorizedToReadKeys(hqKeys);
    }

    private HQKeys getDefaultHQKeysWithFallback()
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

    private HQKeys getDefaultHQKeys() throws HQKeys.HQsException
    {
        return new HQKeys(configInfo.getDefaultHQKeysXml());
    }

    private String getLegacyHQKey()
    {
        return configInfo.getLegacyHQKey();
    }

    private static void showError( Context context, String msg){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle("Error")
             .setMessage(msg)
             .show();
    }
    
}