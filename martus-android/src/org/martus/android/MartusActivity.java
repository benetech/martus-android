package org.martus.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MartusActivity extends Activity {

	public static final String defaultServerIP = "54.245.101.104"; //public QA server
    public static final String defaultServerPublicCode = "8714.7632.8884.7614.8217";
    public static final String defaultMagicWord = "spam";
    private String serverPublicKey;
    private String password;

    private static final int PASSWORD_DIALOG = 4;

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

        myActivity = this;
        responseView = (TextView)findViewById(R.id.bulletinResponseText);
        updateSettings();

        martusCrypto = AppConfig.getInstance().getCrypto();
        if (!martusCrypto.hasKeyPair()) {
            if (isAccountCreated()) {
                showDialog(PASSWORD_DIALOG);
            } else {
                try {
                    createAccount("password");
                } catch (Exception e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
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

    @Override
    protected void onNewIntent(Intent intent) {

        String filePath = intent.getStringExtra(BulletinActivity.EXTRA_ATTACHMENT);
        if (null != filePath) {
            Intent bulletinIntent = new Intent(MartusActivity.this, BulletinActivity.class);
            bulletinIntent.putExtra(BulletinActivity.EXTRA_ATTACHMENT, filePath);
            startActivity(bulletinIntent);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PASSWORD_DIALOG:
                // This example shows how to add a custom layout to an AlertDialog
                LayoutInflater factory = LayoutInflater.from(this);
                final View passwordEntryView = factory.inflate(R.layout.password_dialog, null);
                final EditText passwordText = (EditText) passwordEntryView.findViewById(R.id.password_edit);

                return new AlertDialog.Builder(MartusActivity.this)
                    .setIconAttribute(android.R.attr.alertDialogIcon)
                    .setTitle(R.string.password_dialog_title)
                    .setView(passwordEntryView)
                    .setPositiveButton(R.string.password_dialog_ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            password = passwordText.getText().toString().trim();
                            boolean confirmed = confirmAccount(password);
                            if (!confirmed) {
                                MartusActivity.this.finish();
                            }
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.password_dialog_cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {

                            MartusActivity.this.finish();
                        }
                    })
                    .create();
        }

        return null;
    }

    private boolean isAccountCreated() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);

        // attempt to read keypair from prefs
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
        return keyPairString.length() > 1;
    }

    private boolean confirmAccount(String password)  {

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");

        // construct keypair from value read from prefs
        byte[] decodedKeyPair = Base64.decode(keyPairString, Base64.NO_WRAP);
        InputStream is = new ByteArrayInputStream(decodedKeyPair);
        try {
            martusCrypto.readKeyPair(is, password.toCharArray());
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem confirming password", e);
            return false;
        }
        return true;
    }

    private void createAccount(String password) throws Exception {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        // create new keypair and store in prefs
        martusCrypto.createKeyPair();

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        martusCrypto.writeKeyPair(out, "password".toCharArray());
        out.close();
        byte[] keyPairData = out.toByteArray();

        // write keypair to prefs
        // need to first base64 encode so we can write to prefs
        String encodedKeyPair = Base64.encodeToString(keyPairData, Base64.NO_WRAP);

        // write to prefs
        SharedPreferences.Editor editor = mySettings.edit();
        editor.putString(SettingsActivity.KEY_KEY_PAIR, encodedKeyPair);
        editor.commit();
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