package org.martus.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.bulletin.AttachmentProxy;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.database.Database;
import org.martus.common.packet.Packet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

/**
 * @author roms
 *         Date: 10/25/12
 */
public class BulletinActivity extends Activity implements BulletinSender{

    final int ACTIVITY_CHOOSE_ATTACHMENT = 2;
    public static final String EXTRA_ATTACHEMENT = "filePath";
    public static final String EXTRA_SHOULD_DELETE = "shouldDelete";

    private Activity myActivity;
    private SharedPreferences mySettings;
    private MobileBulletinStore store;
    private HQKey hqKey;
    private String serverPublicKey;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private String magicWord;

    private Bulletin bulletin;
    private EditText titleText;
    private EditText summaryText;
    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_bulletin);


        myActivity = this;
        mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        hqKey = new HQKey(mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, ""));
        store = AppConfig.getInstance().getStore();
        updateSettings();
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

        titleText = (EditText)findViewById(R.id.createBulletinTitle);
        summaryText = (EditText)findViewById(R.id.bulletinSummary);


        final Button buttonChooseAttachment = (Button) findViewById(R.id.addAttachment);
        buttonChooseAttachment.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                try {
                    Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                    chooseFile.setType("file/*");
                    Intent intent = Intent.createChooser(chooseFile, "Choose an attachment");
                    startActivityForResult(intent, ACTIVITY_CHOOSE_ATTACHMENT);
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "Failed choosing file", e);
                    e.printStackTrace();
                }
            }
        });

        final Button sendButton = (Button) findViewById(R.id.sendToMartus);
        sendButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    if (null == bulletin) {
                        bulletin = createBulletin();
                    }
                    sendBulletin(bulletin);
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "Failed sending bulletin", e);
                    e.printStackTrace();
                }
            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        int i = 1;

        Intent intent = getIntent();
        String filePath = intent.getStringExtra(EXTRA_ATTACHEMENT);
        if (null != filePath) {
            try {

                File attachment = new File(filePath);
                if (null == bulletin) {
                    bulletin = createBulletin();
                }
                AttachmentProxy attProxy = new AttachmentProxy(attachment);
                bulletin.addPublicAttachment(attProxy);


            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "problem adding attachment to bulletin", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_CHOOSE_ATTACHMENT: {
                if (resultCode == RESULT_OK) {
                    if (null != data) {
                        Uri uri = data.getData();
                        String filePath = uri.getPath();

                        AttachmentProxy attachment = new AttachmentProxy(new File(filePath));
                        try {
                            if (null == bulletin) {
                                bulletin = createBulletin();
                            }
                            bulletin.addPublicAttachment(attachment);
                        } catch (Exception e) {
                            Log.e(AppConfig.LOG_LABEL, "problem getting attachment", e);
                        }
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {

        String filePath = intent.getStringExtra(EXTRA_ATTACHEMENT);
        try {

            File attachment = new File(filePath);
            if (null == bulletin) {
                bulletin = createBulletin();
            }
            AttachmentProxy attProxy = new AttachmentProxy(attachment);
            bulletin.addPublicAttachment(attProxy);


        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem adding attachment to bulletin", e);
        }
    }

    private void sendBulletin(Bulletin bulletin) throws IOException, MartusCrypto.CryptoException, Packet.InvalidPacketException, Packet.WrongPacketTypeException, Packet.SignatureVerificationException, Database.RecordHiddenException, InterruptedException, ExecutionException {
        dialog = ProgressDialog.show(this, "Sending...", "", true, false);

        String author = mySettings.getString(SettingsActivity.KEY_AUTHOR, "Unknown author");
        bulletin.set(Bulletin.TAGAUTHOR, author);
        String title = titleText.getText().toString().trim();
        String summary = summaryText.getText().toString().trim();
        bulletin.set(Bulletin.TAGTITLE, title);
        bulletin.set(Bulletin.TAGSUMMARY, summary);

        final AsyncTask<Object, Void, String> uploadTask = new UploadBulletinTask(getApplicationContext(), bulletin, this);
        uploadTask.execute(bulletin.getUniversalId(), getCacheDir(), gateway, AppConfig.getInstance().getCrypto());
    }

    private Bulletin createBulletin() throws Exception
    {
        Bulletin b = store.createEmptyBulletin();
        b.set(Bulletin.TAGLANGUAGE, getDefaultLanguageForNewBulletin());
        b.setAuthorizedToReadKeys(new HQKeys(hqKey));
        b.setDraft();
        b.setAllPrivate(false);
        return b;
    }

    private String getDefaultLanguageForNewBulletin()
    {
        return mySettings.getString(SettingsActivity.KEY_DEFAULT_LANGUAGE, Locale.getDefault().getLanguage());
    }

    private void updateSettings() {
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, MartusActivity.defaultServerIP);
        magicWord = mySettings.getString(SettingsActivity.KEY_MAGIC_WORD, MartusActivity.defaultMagicWord);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
    }

    @Override
    public void onSent() {
        dialog.dismiss();
        finish();
    }
}
