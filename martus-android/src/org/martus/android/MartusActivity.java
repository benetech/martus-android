package org.martus.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.martus.client.android.PublicKeyTask;
import org.martus.client.android.ServerInfoTask;
import org.martus.client.android.UploadBulletinTask;
import org.martus.client.android.UploadRightsTask;
import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.client.core.ConfigInfo;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.MartusUtilities;
import org.martus.common.MiniLocalization;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.database.BulletinStreamer;
import org.martus.common.fieldspec.ChoiceItem;
import org.martus.common.fieldspec.StandardFieldSpecs;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.common.packet.BulletinHeaderPacket;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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
    private String serverPublicCode = "8338.1685.2173.3777.2823";
    private String magicWord = "spam";
    private String serverPublicKey;

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

            NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIPNew);

            //Network calls must be made in background task
            final AsyncTask<Object, Void, String> keyTask = new PublicKeyTask().execute(server, martusCrypto);
            serverPublicKey = keyTask.get();

        } catch (MartusCrypto.CryptoInitializationException e) {
            Log.e("martus", "Unable to initialize crypto", e);
        } catch (Exception e) {
            Log.e("martus", "Problem getting server public key", e);
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

                    //confirm serverPublicKey is correct
                    final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverPublicCode);
                    final String computedCode = MartusCrypto.computePublicCode(serverPublicKey);
                    if (! normalizedPublicCode.equals(computedCode)) {
                        showError(myActivity, "Invalid public server code!");
                        return;
                    }

                    ClientSideNetworkGateway gateway = ClientSideNetworkGateway.buildGateway(serverIPNew, serverPublicKey);

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

                    ClientSideNetworkGateway gateway = ClientSideNetworkGateway.buildGateway(serverIPNew, serverPublicKey);

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
                    final File file = new File(cacheDir, "preUpload");
                    BulletinZipUtilities.exportPublicBulletinPacketsFromDatabaseToZipFile(bs, sample.getDatabaseKey(), file, martusCrypto);

                    //uploadBulletinZipFile(sample.getUniversalId(), file, gateway, martusCrypto);

                    final AsyncTask<Object, Void, String> uploadTask = new UploadBulletinTask().execute(sample.getUniversalId(), file, gateway, martusCrypto);
                    String result = uploadTask.get();

                    final TextView responseView = (TextView)findViewById(R.id.bulletinResponseText);
                    responseView.setText(result);
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
        b.set(Bulletin.TAGTITLE, "Sample bulletin from Android");
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

    private String uploadBulletinZipFile(UniversalId uid, File tempFile, ClientSideNetworkGateway gateway, MartusCrypto crypto)
    		throws
                MartusUtilities.FileTooLargeException, IOException, MartusCrypto.MartusSignatureException
    {
        int totalSize = MartusUtilities.getCappedFileLength(tempFile);
        int offset = 0;
        byte[] rawBytes = new byte[NetworkInterfaceConstants.CLIENT_MAX_CHUNK_SIZE];
        FileInputStream inputStream = new FileInputStream(tempFile);
        String result = null;
        while(true)
        {
            int chunkSize = inputStream.read(rawBytes);
            if(chunkSize <= 0)
                break;
            byte[] chunkBytes = new byte[chunkSize];
            System.arraycopy(rawBytes, 0, chunkBytes, 0, chunkSize);

            String authorId = uid.getAccountId();
            String bulletinLocalId = uid.getLocalId();
            String encoded = StreamableBase64.encode(chunkBytes);

            NetworkResponse response = gateway.putBulletinChunk(crypto,
                                authorId, bulletinLocalId, totalSize, offset, chunkSize, encoded);
            result = response.getResultCode();
            if(!result.equals(NetworkInterfaceConstants.CHUNK_OK) && !result.equals(NetworkInterfaceConstants.OK))
                break;
            offset += chunkSize;
        }
        inputStream.close();
        return result;
    }
    
}