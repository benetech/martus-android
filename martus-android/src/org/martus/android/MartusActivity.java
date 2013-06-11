package org.martus.android;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.martus.android.dialog.CreateAccountDialog;
import org.martus.android.dialog.LoginDialog;
import org.martus.android.dialog.MagicWordDialog;
import org.martus.android.dialog.ModalConfirmationDialog;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkInterfaceXmlRpcConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.util.StreamableBase64;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class MartusActivity extends BaseActivity implements LoginDialog.LoginDialogListener,
        CreateAccountDialog.CreateAccountDialogListener, MagicWordDialog.MagicWordDialogListener, OrbotHandler {

    public final static String PROXY_HOST = "127.0.0.1"; //test the local device proxy provided by Orbot/Tor
    public final static int PROXY_HTTP_PORT = 8118; //default for Orbot/Tor
    public final static int PROXY_SOCKS_PORT = 9050; //default for Orbot/Tor
	public static final String ACCOUNT_ID_FILENAME = "Mobile_Public_Account_ID.mpi";

    private static final String PACKETS_DIR = "packets";
    private static final String SERVER_COMMAND_PREFIX = "MartusServer.";
    private static final int CONFIRMATION_TYPE_RESET = 0;
    private static final int CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE = 1;

	public static final String NEW_ACCOUNT_DIALOG_TAG = "dlg_new_account";

    public static final int MAX_LOGIN_ATTEMPTS = 3;
    private String serverPublicKey;

    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private int invalidLogins;
    private CheckBox torCheckbox;

    static final int ACTIVITY_DESKTOP_KEY = 2;
    public static final int ACTIVITY_BULLETIN = 3;
    public static final String RETURN_TO = "return_to";
    private final static String pingPath = "/RPC2";
    private int confirmationType;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        PreferenceManager.setDefaultValues(this, R.xml.settings, false);

        torCheckbox = (CheckBox)findViewById(R.id.checkBox_use_tor);
        updateSettings();
        confirmationType = CONFIRMATION_TYPE_RESET;

    }

    protected void onStart() {
        super.onStart();
        invalidLogins = 0;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (martusCrypto.hasKeyPair()) {
            if (!checkDesktopKey()) {
                return;
            }
            if (!confirmServerPublicKey()) {
                Intent intent = new Intent(MartusActivity.this, ServerActivity.class);
                startActivityForResult(intent, EXIT_REQUEST_CODE);
                return;
            }

            boolean canUpload = mySettings.getBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, false);
            if (!canUpload) {
                showMagicWordDialog();
            }
            OrbotHelper oc = new OrbotHelper(this);
            if (!oc.isOrbotInstalled() || !oc.isOrbotRunning()) {
                torCheckbox.setChecked(false);
            }
            verifySetupInfo();
        } else {
            if (isAccountCreated()) {
                showLoginDialog();
            } else {
                showCreateAccountDialog();
            }
        }
        updateSettings();

    }

    @Override
        protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == EXIT_REQUEST_CODE && resultCode == EXIT_RESULT_CODE) {
            AppConfig.getInstance().getCrypto().clearKeyPair();
            finish();
        }
    }

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
	    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
	        if (event.getAction() == KeyEvent.ACTION_UP &&
	            keyCode == KeyEvent.KEYCODE_MENU) {
	            openOptionsMenu();
	            return true;
	        }
	    }
	    return super.onKeyUp(keyCode, event);
	}

    public void sendBulletin(View view) {
        Intent intent = new Intent(MartusActivity.this, BulletinActivity.class);
        startActivityForResult(intent, EXIT_REQUEST_CODE) ;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
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
            case R.id.quit_menu_item:
                if (!MartusApplication.isIgnoreInactivity()) {
                    logout();
                    finish();
                } else {
                    showMessage(this, getString(R.string.logout_while_sending_message),
		                    getString(R.string.logout_while_sending_title));
                }
                return true;
            case R.id.server_menu_item:
                intent = new Intent(MartusActivity.this, ServerActivity.class);
                startActivityForResult(intent, EXIT_REQUEST_CODE);
                return true;
            case R.id.ping_server_menu_item:
                pingServer();
                return true;
            case R.id.view_public_code_menu_item:
                try {
                    String publicCode = MartusCrypto.getFormattedPublicCode(martusCrypto.getPublicKeyString());
                    showMessage(this, publicCode, getString(R.string.view_public_code_dialog_title));
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "couldn't get public code", e);
                    showMessage(this, getString(R.string.view_public_code_dialog_error),
		                    getString(R.string.view_public_code_dialog_title));
                }
                return true;
            case R.id.reset_install_menu_item:
                if (!MartusApplication.isIgnoreInactivity()) {
                    showConfirmationDialog();
                } else {
                    showMessage(this, getString(R.string.logout_while_sending_message),
		                    getString(R.string.reset_while_sending_title));
                }
                return true;
	        case R.id.show_version_menu_item:
		        PackageInfo pInfo;
		        String version;
		        try {
			        pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			        version = pInfo.versionName;
		        } catch (PackageManager.NameNotFoundException e) {
			        version = "?";
		        }
		        Toast.makeText(this, version, Toast.LENGTH_LONG).show();
		        return true;
	        case R.id.export_mpi_menu_item:
				File mpiFile = getMpiFile();
		        showMessage(this, mpiFile.getAbsolutePath(), getString(R.string.exported_account_id_file_confirmation));
		         return true;
	        case R.id.email_mpi_menu_item:
		        showHowToSendDialog(this, getString(R.string.send_dialog_title));
		        return true;
	        case R.id.feedback_menu_item:
		        showContactUs();
		        return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

	private void showContactUs()
	{
		LayoutInflater li = LayoutInflater.from(this);
		View view = li.inflate(R.layout.contact_us, null);
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setIcon(android.R.drawable.ic_dialog_email)
		     .setTitle(R.string.feedback_dialog_title)
		     .setView(view)
		     .setPositiveButton(R.string.alert_dialog_ok, new SimpleOkayButtonListener())
		     .show();
	}

	private File getMpiFile()
	{
		File externalDir;
		File mpiFile;
		externalDir = Environment.getExternalStorageDirectory();
		mpiFile = new File(externalDir, ACCOUNT_ID_FILENAME);
		try {
		    exportPublicInfo(mpiFile);


		} catch (Exception e) {
		    Log.e(AppConfig.LOG_LABEL, "couldn't export public id", e);
		    showMessage(this, getString(R.string.export_public_account_id_dialog_error),
		            getString(R.string.export_public_account_id_dialog_title));
		}
		return mpiFile;
	}

	private void exportPublicInfo(File exportFile) throws IOException,
			StreamableBase64.InvalidBase64Exception,
			MartusCrypto.MartusSignatureException {
			MartusUtilities.exportClientPublicKey(getSecurity(), exportFile);
		}

    private void pingServer() {
        if (! isNetworkAvailable()) {
            Toast.makeText(this, getString(R.string.no_network_connection), Toast.LENGTH_LONG).show();
            return;
        }
        showProgressDialog(getString(R.string.progress_connecting_to_server));
        try {
            String pingUrl = "http://" + serverIP + pingPath;
            XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
            config.setServerURL(new URL(pingUrl));
            XmlRpcClient client = new XmlRpcClient();
            client.setConfig(config);

            final AsyncTask<XmlRpcClient, Void, String> pingTask = new PingTask();
            pingTask.execute(client);
        } catch (MalformedURLException e) {
            // do nothing
        }
    }

    private void clearPrefsDir() {
        File prefsDirFile = new File(getCacheDir().getParent(), PREFS_DIR);
        clearDirectory(prefsDirFile);
    }

    private void clearFailedBulletinsDir() {
            File prefsDirFile = new File(getCacheDir(), UploadBulletinTask.FAILED_BULLETINS_DIR);
            clearDirectory(prefsDirFile);
            prefsDirFile.delete();
        }

    private void removePacketsDir() {
        File packetsDirFile = new File(getCacheDir(), PACKETS_DIR);
        clearDirectory(packetsDirFile);
        packetsDirFile.delete();
    }

    public static void logout() {
        AppConfig.getInstance().getCrypto().clearKeyPair();
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

    public void onTorChecked(View view) {
        boolean checked = ((CheckBox) view).isChecked();

        if  (checked) {
            System.setProperty("proxyHost", PROXY_HOST);
            System.setProperty("proxyPort", String.valueOf(PROXY_HTTP_PORT));

            System.setProperty("socksProxyHost", PROXY_HOST);
            System.setProperty("socksProxyPort", String.valueOf(PROXY_SOCKS_PORT));

            try {

                OrbotHelper oc = new OrbotHelper(this);

                if (!oc.isOrbotInstalled())
                {
                    oc.promptToInstall(this);
                }
                else if (!oc.isOrbotRunning())
                {
                    oc.requestOrbotStart(this);
                }
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "Tor check failed", e);
            }

        } else {
            System.clearProperty("proxyHost");
            System.clearProperty("proxyPort");

            System.clearProperty("socksProxyHost");
            System.clearProperty("socksProxyPort");
        }
    }

    private boolean confirmServerPublicKey() {
        updateSettings();
        if (serverPublicKey.isEmpty()) {
            return false;
        }
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey, ((MartusApplication)getApplication()).getTransport());
        return true;
    }

    private void verifySetupInfo() {
        try {
            verifySavedDesktopKeyFile();
            verifyServerIPFile();
        } catch (MartusUtilities.FileVerificationException e) {
            Log.e(AppConfig.LOG_LABEL, "Desktop key file corrupted in checkDesktopKey");
            confirmationType = CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE;
            showModalConfirmationDialog();
        }
    }

    private void showModalConfirmationDialog() {
        ModalConfirmationDialog confirmationDialog = ModalConfirmationDialog.newInstance();
        confirmationDialog.show(getSupportFragmentManager(), "dlg_confirmation");
    }

    private boolean checkDesktopKey() {
        SharedPreferences HQSettings = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
        String desktopPublicKeyString = HQSettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, "");

        if (desktopPublicKeyString.length() < 1) {
            Intent intent = new Intent(MartusActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, ACTIVITY_DESKTOP_KEY);
            return false;
        }
        return true;
    }

    private boolean isAccountCreated() {
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
        return keyPairString.length() > 1;
    }



    private void createAccount(char[] password)  {
        martusCrypto.createKeyPair();

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            martusCrypto.writeKeyPair(out, password);
            out.close();
            byte[] keyPairData = out.toByteArray();

            // write keypair to prefs
            // need to first base64 encode so we can write to prefs
            String encodedKeyPair = Base64.encodeToString(keyPairData, Base64.NO_WRAP);

            // write to prefs
            SharedPreferences.Editor editor = mySettings.edit();
            editor.putString(SettingsActivity.KEY_KEY_PAIR, encodedKeyPair);
            editor.commit();
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem creating account", e);
            showMessage(MartusActivity.this, getString(R.string.error_create_account), getString(R.string.error_message));
        }
    }

    private void updateSettings() {
        SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        serverPublicKey = serverSettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        serverIP = serverSettings.getString(SettingsActivity.KEY_SERVER_IP, "");
    }





    @Override
    public void onFinishPasswordDialog(TextView passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        boolean confirmed = (password.length >= MIN_PASSWORD_SIZE) && confirmAccount(password);
        if (!confirmed) {
            if (++invalidLogins == MAX_LOGIN_ATTEMPTS) {
                finish();
            }
            Toast.makeText(this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
            showLoginDialog();
            return;
        }

        SharedPreferences serverSettings = getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE);
        serverPublicKey = serverSettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey, ((MartusApplication)getApplication()).getTransport());

        Intent resendService = new Intent(MartusActivity.this, ResendService.class);
        resendService.putExtra(SettingsActivity.KEY_SERVER_IP, serverIP);
        resendService.putExtra(SettingsActivity.KEY_SERVER_PUBLIC_KEY, serverPublicKey);
        startService(resendService);

        Intent intent = getIntent();
        int returnTo = intent.getIntExtra(RETURN_TO, 0);
        if (returnTo == ACTIVITY_BULLETIN) {
            Intent destination = new Intent(MartusActivity.this, BulletinActivity.class);
            destination.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            destination.putExtras(intent);
            startActivity(destination);
        }
        onResume();
    }




    void showCreateAccountDialog() {
        CreateAccountDialog newAccountDialog = CreateAccountDialog.newInstance();
        newAccountDialog.show(getSupportFragmentManager(), NEW_ACCOUNT_DIALOG_TAG);
    }

    public void onFinishNewAccountDialog(TextView passwordText, TextView confirmPasswordText) {

        boolean failed = false;
        char[] password = passwordText.getText().toString().trim().toCharArray();
        char[] confirmPassword = confirmPasswordText.getText().toString().trim().toCharArray();
        if (password.length < MIN_PASSWORD_SIZE) {
            Toast.makeText(MartusActivity.this,
            R.string.invalid_password, Toast.LENGTH_SHORT).show();
            failed = true;
        }
        if (!Arrays.equals(password, confirmPassword)) {
            Toast.makeText(MartusActivity.this,
            R.string.settings_pwd_not_equal, Toast.LENGTH_SHORT).show();
            failed = true;
        }

	    DialogFragment dialogFragment = (DialogFragment)getSupportFragmentManager().findFragmentByTag(NEW_ACCOUNT_DIALOG_TAG);
        if (dialogFragment != null) {
            dialogFragment.dismiss();
        }

        if (failed) {
            showCreateAccountDialog();
        } else {
            createAccount(password);
	        onResume();
        }
    }

    public void onCancelNewAccountDialog() {
        this.finish();
    }

    private void showMagicWordDialog() {
        MagicWordDialog magicWordDialog = MagicWordDialog.newInstance();
        magicWordDialog.show(getSupportFragmentManager(), "dlg_magicWord");
    }

    public void onFinishMagicWordDialog(TextView magicWordText) {
        String magicWord = magicWordText.getText().toString().trim();
        if (magicWord.isEmpty()) {
            Toast.makeText(this, getString(R.string.invalid_magic_word), Toast.LENGTH_SHORT).show();
            showMagicWordDialog();
            return;
        }
        showProgressDialog(getString(R.string.progress_confirming_magic_word));

        final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask();
        rightsTask.execute(gateway, martusCrypto, magicWord);
    }

    private void processMagicWordResponse(NetworkResponse response) {
        dialog.dismiss();
        try {
             if (!response.getResultCode().equals(NetworkInterfaceConstants.OK)) {
                 Toast.makeText(this, getString(R.string.no_upload_rights), Toast.LENGTH_SHORT).show();
                 showMagicWordDialog();
             } else {
                 Toast.makeText(this, getString(R.string.success_magic_word), Toast.LENGTH_SHORT).show();
                 SharedPreferences.Editor editor = mySettings.edit();
                 editor.putBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, true);
                 editor.commit();
             }
        } catch (Exception e) {
             Log.e(AppConfig.LOG_LABEL, "Problem verifying upload rights", e);
             Toast.makeText(this, getString(R.string.problem_confirming_magic_word), Toast.LENGTH_SHORT).show();
        }
    }

    private static void clearDirectory(final File dir) {
        if (dir!= null && dir.isDirectory()) {
            try {
                for (File child:dir.listFiles()) {
                    if (child.isDirectory()) {
                        clearDirectory(child);
                    }
                    child.delete();
                }
            }
            catch(Exception e) {
                Log.e(AppConfig.LOG_LABEL, String.format("Failed to clean the cache, error %s", e.getMessage()));
            }
        }
    }

    @Override
    public void onOrbotInstallCanceled() {
        torCheckbox.setChecked(false);
    }

    @Override
    public void onOrbotStartCanceled() {
        torCheckbox.setChecked(false);
    }

    @Override
    public String getConfirmationTitle() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            return getString(R.string.confirm_tamper_reset_title);
        } else
            return getString(R.string.confirm_reset_install);
    }

    @Override
    public String getConfirmationMessage() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            return getString(R.string.confirm_tamper_reset_message);
        } else {
            int count = getNumberOfUnsentBulletins();
            if (count == 0) {
              return getString(R.string.confirm_reset_install_extra_no_pending);
            } else {
                Resources res = getResources();
                return res.getQuantityString(R.plurals.confirm_reset_install_extra, count, count);
            }
        }
    }

    private int getNumberOfUnsentBulletins() {
        int pendingBulletins;
        final File cacheDir = getCacheDir();
        final String[] sendingBulletinNames = cacheDir.list(new ZipFileFilter());
        pendingBulletins = sendingBulletinNames.length;

        File failedDir = new File (cacheDir, UploadBulletinTask.FAILED_BULLETINS_DIR);
        if (failedDir.exists()) {
            final String[] failedBulletins = failedDir.list(new ZipFileFilter());
            pendingBulletins += failedBulletins.length;
        }
        return pendingBulletins;
    }

    @Override
    public void onConfirmationAccepted() {
        removePacketsDir();
        clearPreferences(mySettings.edit());
        clearPreferences(getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE).edit());
        clearPreferences(getSharedPreferences(PREFS_SERVER_IP, MODE_PRIVATE).edit());
        logout();
        clearPrefsDir();
        clearFailedBulletinsDir();
        final File cacheDir = getCacheDir();
        final String[] names = cacheDir.list(new ZipFileFilter());
        for (String name : names) {
            File zipFile = new File(cacheDir, name);
            zipFile.delete();
        }
        finish();
    }

    private void removePrefsFile(String prefName) {
        File serverIpFile = getPrefsFile(prefName);
        serverIpFile.delete();
    }

    private void clearPreferences(SharedPreferences.Editor editor) {
        editor.clear();
        editor.commit();
    }

    @Override
    public void onConfirmationCancelled() {
        if (confirmationType == CONFIRMATION_TYPE_TAMPERED_DESKTOP_FILE) {
            martusCrypto.clearKeyPair();
            finish();
        }
    }

    private void processPingResult(String result) {
        dialog.dismiss();
        Toast.makeText(this, result, Toast.LENGTH_LONG).show();
    }

	protected void showHowToSendDialog(Context context, String title) {
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_info)
             .setTitle(title)
             .setPositiveButton(R.string.send_dialog_email, new SendEmailButtonListener())
             .setNegativeButton(R.string.password_dialog_cancel, new CancelSendButtonListener())
             .setNeutralButton(R.string.send_dialog_bulletin, new SendBulletinButtonListener())
             .show();
    }

    public class SendEmailButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
	        File mpiFile = getMpiFile();
            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/plain");
            Uri uri = Uri.parse("file://" + mpiFile.getAbsolutePath());
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            startActivity(Intent.createChooser(emailIntent, "Send email..."));
        }
    }

	public class SendBulletinButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
	        File mpiFile = getMpiFile();
            String filePath = mpiFile.getPath();
            Intent bulletinIntent = new Intent(MartusActivity.this, BulletinActivity.class);
            bulletinIntent.putExtra(BulletinActivity.EXTRA_ATTACHMENT, filePath);
            startActivity(bulletinIntent);
        }
    }

	public class CancelSendButtonListener implements DialogInterface.OnClickListener {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            //do nothing
        }
    }

    private class UploadRightsTask extends AsyncTask<Object, Void, NetworkResponse> {
        @Override
        protected NetworkResponse doInBackground(Object... params) {

            final ClientSideNetworkGateway gateway = (ClientSideNetworkGateway)params[0];
            final MartusSecurity signer = (MartusSecurity)params[1];
            final String magicWord = (String)params[2];

            NetworkResponse result = null;

            try {
                result = gateway.getUploadRights(signer, magicWord);
            } catch (MartusCrypto.MartusSignatureException e) {
                Log.e(AppConfig.LOG_LABEL, "problem getting upload rights", e);
            }

            return result;
        }

        @Override
        protected void onPostExecute(NetworkResponse result) {
            super.onPostExecute(result);
            processMagicWordResponse(result);
        }
    }

    private class PingTask extends AsyncTask<XmlRpcClient, Void, String> {
        @Override
        protected String doInBackground(XmlRpcClient... clients) {

            final Vector params = new Vector();
            final XmlRpcClient client = clients[0];
            String result = getString(R.string.ping_result_ok);
            try {
                client.execute(SERVER_COMMAND_PREFIX + NetworkInterfaceXmlRpcConstants.CMD_PING, params);
            } catch (XmlRpcException e) {
                Log.e(AppConfig.LOG_LABEL, "Ping failed", e);
                result = getString(R.string.ping_result_down);
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            processPingResult(result);
        }
    }
}