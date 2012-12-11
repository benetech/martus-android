package org.martus.android;

import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

/**
 * @author roms
 *         Date: 12/10/12
 */
public class ServerActivity extends Activity {

    private EditText textIp;
    private EditText textCode;


    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_server);

        textIp = (EditText)findViewById(R.id.serverIpText);
        textCode = (EditText)findViewById(R.id.serverCodeText);
    }

    public void confirmServer(View view) {
        String serverIP = textIp.getText().toString().trim();
        if (serverIP.length() < 6) {
            MartusActivity.showMessage(this, "Invalid Server IP", getString(R.string.error_message));
            return;
        }

        String serverCode = textCode.getText().toString().trim();
        if (serverCode.length() < 8) {
            MartusActivity.showMessage(this, "Invalid Server Code", getString(R.string.error_message));
            return;
        }

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        String serverPublicKey;
        NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();


        final AsyncTask<Object, Void, String> keyTask = new PublicKeyTask().execute(server, martusCrypto);
        try {
            serverPublicKey = keyTask.get();
            if (null == serverPublicKey) {
                MartusActivity.showMessage(this, "Invalid Server IP", getString(R.string.error_message));
                return;
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem getting server public key", e);
            MartusActivity.showMessage(this, getString(R.string.error_getting_server_key), getString(R.string.error_message));
            return;
        }

        try {
            if (confirmServerPublicKey(serverCode, serverPublicKey)) {
                SharedPreferences.Editor editor = mySettings.edit();
                editor.putString(SettingsActivity.KEY_SERVER_IP, serverIP);
                editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
                editor.commit();
                Toast.makeText(this, "Server choice success!", Toast.LENGTH_SHORT).show();
            } else {
                MartusActivity.showMessage(this, getString(R.string.invalid_server_public_code), getString(R.string.error_message));
            }
        } catch (StreamableBase64.InvalidBase64Exception e) {
            Log.e(AppConfig.LOG_LABEL,"problem computing public code", e);
            MartusActivity.showMessage(this, getString(R.string.error_computing_public_code), getString(R.string.error_message));
            return;
        }

        this.finish();
    }

    private boolean confirmServerPublicKey(String serverCode, String serverPublicKey) throws StreamableBase64.InvalidBase64Exception {
        final String normalizedPublicCode = MartusCrypto.removeNonDigits(serverCode);
        final String computedCode;
        computedCode = MartusCrypto.computePublicCode(serverPublicKey);
        return normalizedPublicCode.equals(computedCode);
    }
}