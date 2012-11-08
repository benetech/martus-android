package org.martus.android;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.client.bulletinstore.MobileBulletinStore;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.bulletin.AttachmentProxy;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletinstore.BulletinStore;
import org.martus.common.packet.UniversalId;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
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
    public static final String EXTRA_ATTACHMENT = "org.martus.android.filePath";
    public static final String EXTRA_ACCOUNT_ID = "org.martus.android.accountId";
    public static final String EXTRA_LOCAL_ID = "org.martus.android.localId";
    public static final String EXTRA_BULLETIN_TITLE = "org.martus.android.title";

    private SharedPreferences mySettings;
    private ClientBulletinStore store;
    private HQKey hqKey;
    private String serverPublicKey;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;

    private Bulletin bulletin;
    private EditText titleText;
    private EditText summaryText;
    private ProgressDialog dialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.send_bulletin);


        mySettings = PreferenceManager.getDefaultSharedPreferences(this);
        hqKey = new HQKey(mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, ""));
        store = AppConfig.getInstance().getStore();
        updateSettings();
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

        if (null == bulletin) {
            try {
                bulletin = createBulletin();
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "problem creating bulletin", e);
                MartusActivity.showMessage(this, "couldn't create new bulletin", "Error");
            }
        }
        addAttachmentFromIntent();

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

                    zipBulletin(bulletin);
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "Failed sending bulletin", e);
                    e.printStackTrace();
                }
            }
        });

    }

    private void addAttachmentFromIntent() {
        String filePath;
        File attachment = null;
        FileInputStream inputStream = null;
        Intent intent = getIntent();
        filePath = intent.getStringExtra(EXTRA_ATTACHMENT);
        Uri uri;

        try {
            if (null != filePath) {
                //attachment passed directly from another app via EXTRA_ATTACHMENT parameter
                attachment = new File(filePath);
            } else {
                //check if file uri was passed via Android Send
                uri = intent.getData();
                Bundle bundle = intent.getExtras();
                if (null != bundle) {
                    if (bundle.containsKey(Intent.EXTRA_STREAM)) {
                        uri = (Uri)bundle.get(Intent.EXTRA_STREAM);
                    }
                }
                if (uri != null) {
                    String scheme = uri.getScheme();

                    if ("file".equalsIgnoreCase(scheme)) {
                        //uri is direct path of file
                        filePath = uri.getPath();
                        attachment = new File(filePath);
                    } else {
                        //attachment passed as media content
                        String fileName = getFileNameFromUri(uri);
                        attachment = new File(getCacheDir(), fileName);
                        // Ask for a stream of the desired type.
                        AssetFileDescriptor descr = getContentResolver()
                                .openTypedAssetFileDescriptor(uri, "*/*", null);
                        inputStream = descr.createInputStream();

                        BufferedOutputStream outputStream = new BufferedOutputStream(new FileOutputStream(attachment));
                        int read;
                        byte bytes[] = new byte[1024];

                        while ((read = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, read);
                        }

                        outputStream.flush();
                        outputStream.close();
                    }

                }
            }

            //attachment was passed via intent
            if (null != attachment) {
                AttachmentProxy attProxy = new AttachmentProxy(attachment);
                bulletin.addPublicAttachment(attProxy);
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem adding attachment to bulletin", e);
            MartusActivity.showMessage(this, "problem adding attachment to bulletin", "Error");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                }
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

        String filePath = intent.getStringExtra(EXTRA_ATTACHMENT);
        try {

            File attachment = new File(filePath);
            AttachmentProxy attProxy = new AttachmentProxy(attachment);
            bulletin.addPublicAttachment(attProxy);


        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem adding attachment to bulletin", e);
        }

    }

    private void zipBulletin(Bulletin bulletin)  {

        dialog = new ProgressDialog(this);
        dialog.setTitle("Packaging...");
        dialog.setIndeterminate(true);
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();

        String author = mySettings.getString(SettingsActivity.KEY_AUTHOR, "Unknown author");
        bulletin.set(Bulletin.TAGAUTHOR, author);
        String title = titleText.getText().toString().trim();
        String summary = summaryText.getText().toString().trim();
        bulletin.set(Bulletin.TAGTITLE, title);
        bulletin.set(Bulletin.TAGSUMMARY, summary);
        try {
            store.saveBulletin(bulletin);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem saving bulletin", e);
            dialog.dismiss();
            MartusActivity.showMessage(this, "problem saving bulletin, " + e.getMessage(), "Error");
        }

        final AsyncTask<Object, Integer, File> zipTask = new ZipBulletinTask(bulletin, this);
        zipTask.execute(getCacheDir(), AppConfig.getInstance().getCrypto(), store.getDatabase());

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
        //magicWord = mySettings.getString(SettingsActivity.KEY_MAGIC_WORD, MartusActivity.defaultMagicWord);
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
    }

    @Override
    public void onSent() {
        dialog.dismiss();
        finish();
    }

    @Override
    public void onZipped(File zippedFile) {
        dialog.dismiss();
        sendZippedBulletin(zippedFile);
    }

    private void sendZippedBulletin(File zippedFile) {
        dialog = new ProgressDialog(this);
        dialog.setTitle("Sending...");
        dialog.setIndeterminate(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
        dialog.show();

        String bulletinTitle = bulletin.get(Bulletin.TAGTITLE);
        UniversalId bulletinId = bulletin.getUniversalId();
        try {
            store.destroyBulletin(bulletin);
        } catch (IOException e) {
            Log.e(AppConfig.LOG_LABEL, "problem destroying bulletin", e);
        }
        final AsyncTask<Object, Integer, String> uploadTask = new UploadBulletinTask(getApplicationContext(), bulletinTitle, this, bulletinId);
        uploadTask.execute(bulletin.getUniversalId(), zippedFile, gateway, AppConfig.getInstance().getCrypto());
    }

    @Override
    public void onProgressUpdate(int progress) {
        dialog.setProgress(progress);
    }

    private String getFileNameFromUri(Uri uri) {
        Cursor cursor = managedQuery(uri,
                                   new String[] { MediaStore.Images.Media.DATA },
                                   null, null, null);
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();

        String path =  cursor.getString(column_index);
        return new File(path).getName();
    }
}
