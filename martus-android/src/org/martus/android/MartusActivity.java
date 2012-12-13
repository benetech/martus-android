package org.martus.android;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkResponse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import info.guardianproject.onionkit.ui.OrbotHelper;

public class MartusActivity extends Activity {

    public final static String PROXY_HOST = "127.0.0.1"; //test the local device proxy provided by Orbot/Tor
    public final static int PROXY_HTTP_PORT = 8118; //default for Orbot/Tor
    public final static int PROXY_SOCKS_PORT = 9050; //default for Orbot/Tor

    public static final int MAX_LOGIN_ATTEMPTS = 3;
    private String serverPublicKey;

    private MartusSecurity martusCrypto;
    private static Activity myActivity;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private int invalidLogins;

    DialogFragment newAccountDialog;
    DialogFragment magicWordDialog;

    static final int ACTIVITY_DESKTOP_KEY = 2;
    public static final int ACTIVITY_BULLETIN = 3;
    public static final String RETURN_TO = "return_to";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        myActivity = this;
        updateSettings();

        martusCrypto = AppConfig.getInstance().getCrypto();
        if (!martusCrypto.hasKeyPair()) {
            if (isAccountCreated()) {
                invalidLogins = 0;
                showLoginDialog();
            } else {
                showCreateAccountDialog();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (martusCrypto.hasKeyPair()) {
            checkDesktopKey();
            if (!confirmServerPublicKey()) {
                Intent intent = new Intent(MartusActivity.this, ServerActivity.class);
                startActivity(intent);
                return;
            }

            SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
            boolean canUpload = mySettings.getBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, false);
            if (!canUpload) {
                showMagicWordDialog();
            }

        }
        updateSettings();
    }

    public void sendBulletin(View view) {
        Intent intent = new Intent(MartusActivity.this, BulletinActivity.class);
        startActivity(intent);
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
            case R.id.quit_menu_item:
                martusCrypto.clearKeyPair();
                deleteCache(this);
                finish();
                return true;
            case R.id.server_menu_item:
                intent = new Intent(MartusActivity.this, ServerActivity.class);
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
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);
        return true;
    }

    private void checkDesktopKey() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        String desktopPublicKeyString = mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, "");
        if (desktopPublicKeyString.length() < 1) {
            Intent intent = new Intent(MartusActivity.this, DesktopKeyActivity.class);
            startActivityForResult(intent, ACTIVITY_DESKTOP_KEY);
        }
    }

    private boolean isAccountCreated() {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);

        // attempt to read keypair from prefs
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");
        return keyPairString.length() > 1;
    }

    private boolean confirmAccount(char[] password)  {

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        String keyPairString = mySettings.getString(SettingsActivity.KEY_KEY_PAIR, "");

        // construct keypair from value read from prefs
        byte[] decodedKeyPair = Base64.decode(keyPairString, Base64.NO_WRAP);
        InputStream is = new ByteArrayInputStream(decodedKeyPair);
        try {
            martusCrypto.readKeyPair(is, password);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Problem confirming password", e);
            return false;
        }
        return true;
    }

    private void createAccount(char[] password)  {
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        // create new keypair and store in prefs
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
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, "");
    }

    public static void showMessage(Context context, String msg, String title){
        AlertDialog.Builder alert = new AlertDialog.Builder(context);
        alert.setIcon(android.R.drawable.ic_dialog_alert)
             .setTitle(title)
             .setMessage(msg)
             .show();
    }

    public static void deleteCache(Context context) {
        File dir = context.getCacheDir();
        if (dir != null && dir.isDirectory()) {
            deleteDir(dir);
        }
    }

    public static boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        return dir.delete();
    }

    void showLoginDialog() {
        DialogFragment loginDialog = LoginDialogFragment.newInstance();
        loginDialog.show(getFragmentManager(), "login");
    }

    public void doLoginPositiveClick(EditText passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        boolean confirmed = confirmAccount(password);
        if (!confirmed) {
            if (++invalidLogins == MAX_LOGIN_ATTEMPTS) {
                finish();
            }
            Toast.makeText(this, getString(R.string.incorrect_password), Toast.LENGTH_SHORT).show();
            showLoginDialog();
        }

        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(MartusActivity.this);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

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

    public void doLoginNegativeClick() {
        this.finish();
    }

    public static class LoginDialogFragment extends DialogFragment {

        public static LoginDialogFragment newInstance() {
            LoginDialogFragment frag = new LoginDialogFragment();
            Bundle args = new Bundle();
            frag.setArguments(args);
            frag.setCancelable(false);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater factory = LayoutInflater.from(myActivity);
            final View passwordEntryView = factory.inflate(R.layout.password_dialog, null);
            final EditText passwordText = (EditText) passwordEntryView.findViewById(R.id.password_edit);
            return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.password_dialog_title)
                .setView(passwordEntryView)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doLoginPositiveClick(passwordText);
                            }
                        }
                )
                .setNegativeButton(R.string.password_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doLoginNegativeClick();
                            }
                        }
                )
                .create();
        }
    }

    void showCreateAccountDialog() {
        newAccountDialog = CreateAccountDialogFragment.newInstance();
        newAccountDialog.show(getFragmentManager(), "create");
    }

    public void doCreateAccountPositiveClick(EditText passwordText) {
        char[] password = passwordText.getText().toString().trim().toCharArray();
        createAccount(password);
        checkDesktopKey();
        newAccountDialog.dismiss();
    }

    public void doCreateAccountNegativeClick() {
        this.finish();
    }

    public static class CreateAccountDialogFragment extends DialogFragment {

        public static CreateAccountDialogFragment newInstance() {
            CreateAccountDialogFragment frag = new CreateAccountDialogFragment();
            Bundle args = new Bundle();
            frag.setArguments(args);
            frag.setCancelable(false);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater factory = LayoutInflater.from(myActivity);
            final View createAccountDialog = factory.inflate(R.layout.create_account, null);
            final EditText newPasswordText = (EditText) createAccountDialog.findViewById(R.id.new_password_field);
            return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.create_account_dialog_title)
                .setView(createAccountDialog)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doCreateAccountPositiveClick(newPasswordText);
                            }
                        }
                )
                .setNegativeButton(R.string.password_dialog_cancel,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doCreateAccountNegativeClick();
                            }
                        }
                )
                .create();
        }
    }

    private void showMagicWordDialog() {
        magicWordDialog = MagicWordDialogFragment.newInstance();
        magicWordDialog.show(getFragmentManager(), "magicWord");
    }

    public void doMagicWordPositiveClick(EditText magicWordText) {
        String magicWord = magicWordText.getText().toString().trim();
        if (magicWord.isEmpty()) {
            Toast.makeText(this, "Invalid Magic Word!", Toast.LENGTH_SHORT).show();
            showMagicWordDialog();
            return;
        }
        try {
             final AsyncTask<Object, Void, NetworkResponse> rightsTask = new UploadRightsTask().execute(gateway, martusCrypto, magicWord);
             final NetworkResponse response = rightsTask.get();
             if (!response.getResultCode().equals("ok")) {
                 Toast.makeText(this, getString(R.string.no_upload_rights), Toast.LENGTH_SHORT).show();
                 showMagicWordDialog();
             } else {
                 Toast.makeText(this, "Success - can now upload bulletins!", Toast.LENGTH_SHORT).show();
                 SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
                 SharedPreferences.Editor editor = mySettings.edit();
                 editor.putBoolean(SettingsActivity.KEY_HAVE_UPLOAD_RIGHTS, true);
                 editor.commit();
             }
        } catch (Exception e) {
             Log.e(AppConfig.LOG_LABEL, "Problem verifying upload rights", e);
             Toast.makeText(this, "Problem confirming magic word", Toast.LENGTH_SHORT).show();
        }
    }

    public static class MagicWordDialogFragment extends DialogFragment {

        public static MagicWordDialogFragment newInstance() {
            MagicWordDialogFragment frag = new MagicWordDialogFragment();
            Bundle args = new Bundle();
            frag.setArguments(args);
            frag.setCancelable(false);
            return frag;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            LayoutInflater factory = LayoutInflater.from(myActivity);
            final View magicWordView = factory.inflate(R.layout.magic_word_dialog, null);
            final EditText magicWordText = (EditText) magicWordView.findViewById(R.id.password_edit);
            return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(R.string.magic_word_dialog_title)
                .setView(magicWordView)
                .setPositiveButton(R.string.alert_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((MartusActivity) getActivity()).doMagicWordPositiveClick(magicWordText);
                            }
                        }
                )
                .create();
        }
    }
    
}