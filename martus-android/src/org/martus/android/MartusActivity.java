package org.martus.android;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.client.core.ConfigInfo;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MiniLocalization;
import org.martus.common.bulletin.AttachmentProxy;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.database.BulletinStreamer;
import org.martus.common.database.Database;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.common.packet.Packet;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
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
    public static final String defaultServerPublicCode = "8714.7632.8884.7614.8217";
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

   private TextView responseView;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        //setTitle("Martus Android");
        myActivity = this;

        responseView = (TextView)findViewById(R.id.bulletinResponseText);

        updateSettings();

        try {
            martusCrypto = new MartusSecurity();

            // if key doesn't exist
            martusCrypto.createKeyPair();
/*            ByteArrayOutputStream out = new ByteArrayOutputStream();
            martusCrypto.writeKeyPair(out, "password".toCharArray());
            out.close();
            String keyPair = out.toString();

            InputStream is = new ByteArrayInputStream(keyPair.getBytes());

            martusCrypto.readKeyPair(is, "password".toCharArray());*/


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
                    String result = sendBulletin(sample);

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

    private String sendBulletin(Bulletin sample) throws IOException, MartusCrypto.CryptoException, Packet.InvalidPacketException, Packet.WrongPacketTypeException, Packet.SignatureVerificationException, Database.RecordHiddenException, InterruptedException, ExecutionException {
        final File cacheDir = getCacheDir();
        final BulletinStreamer bs = new BulletinStreamer(sample);

        //File in cache directory  - use getDir() for directory of permanent files private to this app
        final File file = new File(cacheDir, "preUpload.zip");
        BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(bs, sample.getDatabaseKey(), file, martusCrypto);

        final AsyncTask<Object, Void, String> uploadTask = new UploadBulletinTask().execute(sample.getUniversalId(), file, gateway, martusCrypto);
        String result = uploadTask.get();
        file.deleteOnExit();
        return result;
    }


    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
    }

    @Override
    protected void onNewIntent(Intent intent) {

        responseView.setText("");

        Bundle bundle = intent.getExtras();
        ClipData clipData = intent.getClipData();

        Bulletin sample;
        File tmpAttachment = null;

        ClipData.Item item = clipData.getItemAt(0);
        Uri uri = item.getUri();
        if (uri != null) {

            String scheme = uri.getScheme();
            FileInputStream inputStream = null;
            File attachment;

            try {
                File outputDir = getCacheDir();
                if ("file".equalsIgnoreCase(scheme)) {
                    String filePath = uri.getPath();
                    attachment = new File(filePath);
                } else {

                    tmpAttachment = File.createTempFile("tmp_", "jpg", outputDir);
                    // Ask for a stream of the desired type.
                    AssetFileDescriptor descr = getContentResolver()
                            .openTypedAssetFileDescriptor(uri, "image/*", null);
                    inputStream = descr.createInputStream();

                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tmpAttachment));
                    int read;
                    byte bytes[] = new byte[1024];

                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }

                    outputStream.flush();
                    outputStream.close();
                    attachment = tmpAttachment;
                }
                sample = createBulletin();
                AttachmentProxy attProxy = new AttachmentProxy(attachment);
                sample.addPrivateAttachment(attProxy);

                String result = sendBulletin(sample);


                responseView.setText(result);

            } catch (Exception e) {
                Log.e("martus", "problem sending bulletin with attachment", e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
                if (tmpAttachment != null) {
                    tmpAttachment.deleteOnExit();
                }
            }
        }
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