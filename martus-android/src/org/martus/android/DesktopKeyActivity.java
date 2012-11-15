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
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author roms
 *         Date: 10/24/12
 */
public class DesktopKeyActivity extends Activity {

    final int ACTIVITY_CHOOSE_FILE = 1;

    private EditText editText_code;
    private Activity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.desktop_sync);
        activity = this;

        editText_code = (EditText)findViewById(R.id.desktopCodeText);

        final Button buttonChoosePublicKeyFile = (Button) findViewById(R.id.desktopKeyChooseFile);
        buttonChoosePublicKeyFile.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String code = editText_code.getText().toString().trim();
                if ("".equals(code)) {
                    editText_code.requestFocus();
                    MartusActivity.showMessage(activity, "Public code can't be empty", "Error");
                    return;
                }
                try {
                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("file/*");
                    Intent intent = Intent.createChooser(chooseFile, "Choose a file picker");
                    startActivityForResult(intent, ACTIVITY_CHOOSE_FILE);
                } catch (Exception e) {
                    Log.e("martus", "Failed choosing file", e);
                    e.printStackTrace();
                }
            }
        });
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
                        Log.e("martus", "problem getting HQ key", e);
                    }
                }
                break;
            }
        }
    }



    public void setPublicKey(File importFile) throws Exception {
        String publicKeyString = extractPublicInfo(importFile);

        String publicCode = MartusCrypto.computePublicCode(publicKeyString);
        if(!confirmPublicCode(publicCode, editText_code.getText().toString().trim())) {
            MartusActivity.showMessage(activity, "Public code didn't match file", "Error");
            return;
        }
        SharedPreferences mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = mySettings.edit();
        editor.putString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, publicKeyString);
        editor.commit();
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

    public MartusCrypto getSecurity()
    {
        return AppConfig.getInstance().getStore().getSignatureGenerator();
    }

    boolean confirmPublicCode(String rawPublicCode, String userEnteredPublicCode)
    {
        String normalizedPublicCode = MartusCrypto.removeNonDigits(userEnteredPublicCode);
        return rawPublicCode.equals(normalizedPublicCode);
    }
}
