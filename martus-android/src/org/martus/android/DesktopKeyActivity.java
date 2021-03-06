package org.martus.android;

import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.util.StreamableBase64;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class DesktopKeyActivity extends BaseActivity {

    final int ACTIVITY_CHOOSE_FILE = 1;

    private EditText editText_code;
    private Activity activity;
    private boolean shouldShowInstallExplorer = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desktop_sync);
        activity = this;

        editText_code = (EditText)findViewById(R.id.desktopCodeText);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    public void chooseKeyFile(View view) {
        shouldShowInstallExplorer = false;
        String code = editText_code.getText().toString().trim();
        if ("".equals(code)) {
            editText_code.requestFocus();
            showMessage(activity, getString(R.string.public_code_validation_empty), getString(R.string.error_message));
            return;
        }
        try {
            Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
            chooseFile.setType("file/*");
            Intent intent = Intent.createChooser(chooseFile, getString(R.string.select_file_picker));
            startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
        } catch (Exception e) {
            Log.e("martus", "Failed choosing file", e);
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_CHOOSE_FILE: {
                if (resultCode == RESULT_OK){
                    Uri uri = data.getData();
                    String filePath = uri.getPath();

                    try {
                        setPublicKey(new File(filePath));
                    } catch (Exception e) {
                        showMessage(activity, getString(R.string.invalid_public_account_file), getString(R.string.error_message));
                        Log.e("martus", "problem getting HQ key", e);
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    shouldShowInstallExplorer = true;
                }
                break;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (shouldShowInstallExplorer) {
            showInstallExplorerDialog();
            shouldShowInstallExplorer = false;
        }
    }

    public void setPublicKey(File importFile) throws Exception {
        String publicKeyString = extractPublicInfo(importFile);

        String publicCode = MartusCrypto.computePublicCode(publicKeyString);
        if(!confirmPublicCode(publicCode, editText_code.getText().toString().trim())) {
            showMessage(activity, getString(R.string.invalid_public_code), getString(R.string.error_message));
            return;
        }

        SharedPreferences HQSettings = getSharedPreferences(PREFS_DESKTOP_KEY, MODE_PRIVATE);
        SharedPreferences.Editor editor = HQSettings.edit();

        editor.putString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, publicKeyString);
        editor.commit();


        File desktopKeyFile = getPrefsFile(PREFS_DESKTOP_KEY);
        MartusUtilities.createSignatureFileFromFile(desktopKeyFile, getSecurity());

        finish();
    }

    public String extractPublicInfo(File file) throws
            IOException,
            StreamableBase64.InvalidBase64Exception,
                MartusUtilities.PublicInformationInvalidException
    {
        Vector importedPublicKeyInfo = MartusUtilities.importClientPublicKeyFromFile(file);
        String publicKey = (String) importedPublicKeyInfo.get(0);
        String signature = (String) importedPublicKeyInfo.get(1);
        MartusUtilities.validatePublicInfo(publicKey, signature, getSecurity());
        return publicKey;
    }

    boolean confirmPublicCode(String rawPublicCode, String userEnteredPublicCode)
    {
        String normalizedPublicCode = MartusCrypto.removeNonDigits(userEnteredPublicCode);
        return rawPublicCode.equals(normalizedPublicCode);
    }
}
