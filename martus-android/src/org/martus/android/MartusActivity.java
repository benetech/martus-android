package org.martus.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

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

    private MartusSecurity martusCrypto;
    private Activity myActivity;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private String serverPublicCode;

    private TextView responseView;

    final int ACTIVITY_CHOOSE_ATTACHMENT = 2;

	/** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        //checkDesktopKey();

        //setTitle("Martus Android");
        myActivity = this;
        responseView = (TextView)findViewById(R.id.bulletinResponseText);
        updateSettings();

        martusCrypto = AppConfig.getInstance().getCrypto();

         if (null == martusCrypto) {


                //martusCrypto = new MartusSecurity();

                // if key doesn't exist
                //martusCrypto.createKeyPair();
    /*            ByteArrayOutputStream out = new ByteArrayOutputStream();
                martusCrypto.writeKeyPair(out, "password".toCharArray());
                out.close();
                String keyPair = out.toString();

                InputStream is = new ByteArrayInputStream(keyPair.getBytes());

                martusCrypto.readKeyPair(is, "password".toCharArray());*/

         }
        final Button buttonServerInfo = (Button) findViewById(R.id.serverInfo);
        buttonServerInfo.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {

                    //Network calls must be made in background task
                    final AsyncTask<ClientSideNetworkGateway, Void, NetworkResponse> infoTask = new ServerInfoTask().execute(gateway);
                    NetworkResponse response1 = infoTask.get();

                    //todo: move to upload bulletin screen
                    final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask().execute(gateway, martusCrypto, defaultMagicWord);
                    final NetworkResponse response = rightsTask.get();
                    if (!response.getResultCode().equals("ok")) {
                        showMessage(myActivity, "Don't have upload rights!", "Error");
                        return;
                    }

                    Object[] resultArray = response1.getResultArray();
                    final TextView responseView = (TextView)findViewById(R.id.response_server);
                    responseView.setText("ServerInfo: " + response1.getResultCode() + ", " + resultArray[0]);
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "Failed getting server info", e);
                    e.printStackTrace();
                }
            }
        });

        final Button createScreenBtn = (Button) findViewById(R.id.createScreen);
        createScreenBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MartusActivity.this, BulletinActivity.class);
                startActivity(intent);
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
                    Log.e(AppConfig.LOG_LABEL, "Failed getting bulletin ids", e);
                    e.printStackTrace();
                }
            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();
        checkDesktopKey();



        //Not sure if this is the best place to get/set Server Public Key
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        if (serverPublicKey.length() < 1) {
            //Network calls must be made in background task
            NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
            final AsyncTask<Object, Void, String> keyTask = new PublicKeyTask().execute(server, martusCrypto);
            try {
                serverPublicKey = keyTask.get();
                SharedPreferences.Editor editor = mySettings.edit();
                editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
                editor.commit();
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "Problem getting server public key", e);
                showMessage(this, "Problem getting server public key", "Error");
                return;
            }
        }

        //confirm serverPublicKey is correct
        final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverPublicCode);
        final String computedCode;
        try {
            computedCode = MartusCrypto.computePublicCode(serverPublicKey);
            if (! normalizedPublicCode.equals(computedCode)) {
                showMessage(myActivity, "Invalid public server code! Please fix in Settings screen.", "Error");
            }
        } catch (StreamableBase64.InvalidBase64Exception e) {
            Log.e(AppConfig.LOG_LABEL,"problem computing public code", e);
            showMessage(myActivity, "Problem computing public code", "Error");
            return;
        }

        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);
    }

    private void checkDesktopKey() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        String desktopPublicKeyString = mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, "");
        if (desktopPublicKeyString.length() < 1) {
            Intent intent = new Intent(MartusActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, ACTIVITY_CHOOSE_ATTACHMENT);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateSettings();
    }

    @Override
    protected void onNewIntent(Intent intent) {

        boolean shouldDelete = false;
        ClipData clipData = intent.getClipData();
        ClipData.Item item = clipData.getItemAt(0);
        Uri uri = item.getUri();
        if (uri != null) {

            String scheme = uri.getScheme();
            FileInputStream inputStream = null;
            File attachment = null;

            try {
                File outputDir = getExternalCacheDir();
                if ("file".equalsIgnoreCase(scheme)) {
                    String filePath = uri.getPath();
                    attachment = new File(filePath);
                } else {

                    attachment = File.createTempFile("tmp_", "jpg", outputDir);
                    // Ask for a stream of the desired type.
                    AssetFileDescriptor descr = getContentResolver()
                            .openTypedAssetFileDescriptor(uri, "image/*", null);
                    inputStream = descr.createInputStream();

                    BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(attachment));
                    int read;
                    byte bytes[] = new byte[1024];

                    while ((read = inputStream.read(bytes)) != -1) {
                        outputStream.write(bytes, 0, read);
                    }
                    shouldDelete = true;
                    outputStream.flush();
                    outputStream.close();
                }

                //todo : need to capture file and send to BulletinActivity screen
/*                sample = createBulletin();
                AttachmentProxy attProxy = new AttachmentProxy(attachment);
                sample.addPublicAttachment(attProxy);

                sendBulletin(sample);*/

            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "problem accepting attachment", e);
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                    }
                }
            }

            //Send attachment to BulletinActivity
            intent = new Intent(MartusActivity.this, BulletinActivity.class);
            // do not keep this intent in history
            //intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            intent.putExtra(BulletinActivity.EXTRA_ATTACHEMENT, attachment.getAbsolutePath());
            intent.putExtra(BulletinActivity.EXTRA_SHOULD_DELETE, shouldDelete);
            startActivity(intent);
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
    }

    public static void showMessage(Context context, String msg, String title){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle(title)
             .setMessage(msg)
             .show();
    }
    
}