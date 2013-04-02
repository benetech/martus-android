package org.martus.android;

import java.io.File;

import org.martus.clientside.ClientSideNetworkHandlerUsingXmlRpcForNonSSL;
import org.martus.common.Exceptions;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NonSSLNetworkAPI;
import org.martus.util.StreamableBase64;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * @author roms
 *         Date: 12/10/12
 */
public class ServerActivity extends BaseActivity implements TextView.OnEditorActionListener {

    private static final int MIN_SERVER_CODE = 20;
    private static final int MIN_SERVER_IP = 7;

    private EditText textIp;
    private EditText textCode;
    private Activity myActivity;
    private String serverIP;
    private String serverCode;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.choose_server);

        if ((Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) && haveVerifiedServerInfo()) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        myActivity = this;
        textIp = (EditText)findViewById(R.id.serverIpText);
        textCode = (EditText)findViewById(R.id.serverCodeText);
        textCode.setOnEditorActionListener(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            confirmServer(textCode);
            return true;
        }
        return false;
    }

    @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            switch (item.getItemId()) {
                case android.R.id.home:
                    finish();
                    return true;
                default:
                    return super.onOptionsItemSelected(item);
            }
        }

    public void confirmServer(View view) {
        serverIP = textIp.getText().toString().trim();
        if (serverIP.length() < MIN_SERVER_IP) {
            showErrorMessage(getString(R.string.invalid_server_ip), getString(R.string.error_message));
            return;
        }

        serverCode = textCode.getText().toString().trim();
        if (serverCode.length() < MIN_SERVER_CODE) {
            showErrorMessage(getString(R.string.invalid_server_code), getString(R.string.error_message));
            return;
        }

        showProgressDialog(getString(R.string.progress_connecting_to_server));

        NonSSLNetworkAPI server = new ClientSideNetworkHandlerUsingXmlRpcForNonSSL(serverIP);
        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();

        final AsyncTask <Object, Void, String> keyTask = new PublicKeyTask();
        keyTask.execute(server, martusCrypto);
    }

    private void processResult(String serverPublicKey) {
        dialog.dismiss();
        try {
            if (null == serverPublicKey) {
                showErrorMessage(getString(R.string.invalid_server_ip), getString(R.string.error_message));
                return;
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem getting server public key", e);
            showErrorMessage(getString(R.string.error_getting_server_key), getString(R.string.error_message));
            return;
        }

        try {
            if (confirmServerPublicKey(serverCode, serverPublicKey)) {
                SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
                SharedPreferences.Editor editor = serverSettings.edit();
                editor.putString(SettingsActivity.KEY_SERVER_IP, serverIP);
                editor.putString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
                editor.commit();

                SharedPreferences.Editor magicWordEditor = mySettings.edit();
                magicWordEditor.putBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, false);
                magicWordEditor.commit();
                Toast.makeText(this, getString(R.string.successful_server_choice), Toast.LENGTH_SHORT).show();

                File serverIpFile = getPrefsFile(PREFS_SERVER_IP);
                MartusUtilities.createSignatureFileFromFile(serverIpFile, getSecurity());


            } else {
                showErrorMessage(getString(R.string.invalid_server_code), getString(R.string.error_message));
                return;
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL,"problem processing server IP", e);
            showErrorMessage(getString(R.string.error_computing_public_code), getString(R.string.error_message));
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

    private void showErrorMessage(String msg, String title){
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(msg)
                .setPositiveButton(R.string.retry_server, new RetryButtonHandler())
                .setNegativeButton(R.string.cancel_server, new CancelButtonHandler())
                .show();
    }

    private class RetryButtonHandler implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int whichButton) {
            /* Do nothing */
        }
    }

    private boolean haveVerifiedServerInfo() {
	    SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        return serverSettings.getString(SettingsActivity.KEY_SERVER_IP, "").length() > 1;
    }

    private class CancelButtonHandler implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int whichButton) {

            if (!haveVerifiedServerInfo()) {
                myActivity.setResult(EXIT_RESULT_CODE);
            }
            myActivity.finish();
        }
    }

    private class PublicKeyTask extends AsyncTask<Object, Void, String> {
        @Override
        protected String doInBackground(Object... params) {

            final NonSSLNetworkAPI server = (NonSSLNetworkAPI)params[0];
            final MartusSecurity security = (MartusSecurity)params[1];
            String result = null;

            try {
                result = server.getServerPublicKey(security);
            } catch (Exceptions.ServerNotAvailableException e) {
                Log.e(AppConfig.LOG_LABEL, "server not available", e);
            } catch (MartusUtilities.PublicInformationInvalidException e) {
                Log.e(AppConfig.LOG_LABEL, "invalid public info", e);
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            processResult(result);
        }
    }
}