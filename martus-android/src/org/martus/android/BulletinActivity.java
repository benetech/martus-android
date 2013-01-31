package org.martus.android;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.martus.android.dialog.ConfirmationDialog;
import org.martus.android.dialog.DeterminateProgressDialog;
import org.martus.android.dialog.IndeterminateProgressDialog;
import org.martus.client.bulletinstore.ClientBulletinStore;
import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.HQKey;
import org.martus.common.HQKeys;
import org.martus.common.bulletin.AttachmentProxy;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.packet.UniversalId;

import android.app.ActionBar;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;
import com.bugsense.trace.BugSenseHandler;
import com.ipaulpro.afilechooser.utils.FileUtils;

/**
 * @author roms
 *         Date: 10/25/12
 */
public class BulletinActivity extends BaseActivity implements BulletinSender,
        ConfirmationDialog.ConfirmationDialogListener, IndeterminateProgressDialog.IndeterminateProgressDialogListener,
        DeterminateProgressDialog.DeterminateProgressDialogListener, AdapterView.OnItemLongClickListener {

    final int ACTIVITY_CHOOSE_ATTACHMENT = 2;
    public static final String EXTRA_ATTACHMENT = "org.martus.android.filePath";
    public static final String EXTRA_ATTACHMENTS = "org.martus.android.filePaths";
    public static final String EXTRA_ACCOUNT_ID = "org.martus.android.accountId";
    public static final String EXTRA_LOCAL_ID = "org.martus.android.localId";
    public static final String EXTRA_BULLETIN_TITLE = "org.martus.android.title";

    private static final int CONFIRMATION_TYPE_CANCEL_BULLETIN = 0;
    private static final int CONFIRMATION_TYPE_DELETE_ATTACHMENT = 1;

    private ClientBulletinStore store;
    private HQKey hqKey;
    private String serverPublicKey;
    private ClientSideNetworkGateway gateway = null;
    private String serverIP;
    private boolean autoLogout;

    private Bulletin bulletin;
    private Map<String, File> bulletinAttachments;
    private int confirmationType;
    private int attachmentToRemoveIndex;
    private String attachmentToRemoveName;
    private EditText titleText;
    private EditText summaryText;
    private ArrayAdapter<String> attachmentAdapter;
    private boolean shouldShowInstallExplorer = false;
    private IndeterminateProgressDialog indeterminateDialog;
    private DeterminateProgressDialog determinateDialog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        BugSenseHandler.initAndStartSession(BulletinActivity.this, ExternalKeys.BUGSENSE_KEY);
        setContentView(R.layout.send_bulletin_linear);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.GINGERBREAD_MR1) {
            ActionBar actionBar = getActionBar();
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        MartusSecurity martusCrypto = AppConfig.getInstance().getCrypto();
        if (!martusCrypto.hasKeyPair()) {
            showLoginRequiredDialog();
        }

        hqKey = new HQKey(mySettings.getString(SettingsActivity.KEY_DESKTOP_PUBLIC_KEY, ""));
        store = AppConfig.getInstance().getStore();
        updateSettings();
        gateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

        if (null == bulletin) {
            try {
                bulletin = createBulletin();
                bulletinAttachments = new HashMap<String, File>(2);
            } catch (Exception e) {
                Log.e(AppConfig.LOG_LABEL, "problem creating bulletin", e);
                showMessage(this, getString(R.string.problem_creating_bulletin), getString(R.string.error_message));
            }
        }

        titleText = (EditText)findViewById(R.id.createBulletinTitle);
        summaryText = (EditText)findViewById(R.id.bulletinSummary);

        attachmentAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        ListView list = (ListView)findViewById(android.R.id.list);
        list.setTextFilterEnabled(true);
        list.setAdapter(attachmentAdapter);
        list.setLongClickable(true);
        list.setOnItemLongClickListener(this);

        addAttachmentFromIntent();
    }

    private void chooseAttachment() {
        shouldShowInstallExplorer = false;
        try {
            Intent chooseFile = FileUtils.createGetContentIntent();
            Intent intent = Intent.createChooser(chooseFile, "Choose an attachment");
            startActivityForResult(intent, ACTIVITY_CHOOSE_ATTACHMENT);
        } catch (ActivityNotFoundException e) {
            Log.e(AppConfig.LOG_LABEL, "Failed choosing file", e);
            Toast.makeText(this, getString(R.string.failure_choosing_file), Toast.LENGTH_LONG).show();
        }
    }

    private void sendBulletin() {
        try {
            for (File file : bulletinAttachments.values()) {
                addAttachmentToBulletin(file);
            }
            zipBulletin(bulletin);
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "Failed zipping bulletin", e);
            Toast.makeText(this, getString(R.string.failure_zipping_bulletin), Toast.LENGTH_LONG).show();
        }
    }

    private void addAttachmentFromIntent() {

        Intent intent = getIntent();
        ArrayList<File> attachments = getFilesFromIntent(intent);

        try {
            for (File attachment : attachments) {
                addAttachmentToMap(attachment);
            }
        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem adding attachment to bulletin", e);
            showMessage(this, getString(R.string.problem_adding_attachment), getString(R.string.error_message));
        }
    }

    private void addAttachmentToBulletin(File attachment) throws IOException, MartusCrypto.EncryptionException {
        AttachmentProxy attProxy = new AttachmentProxy(attachment);
        bulletin.addPublicAttachment(attProxy);
    }

    private void addAttachmentToMap(File attachment) {
        attachmentAdapter.add(attachment.getName());
        bulletinAttachments.put(attachment.getName(), attachment);
    }

    private ArrayList<File> getFilesFromIntent(Intent intent) {
        ArrayList<File> attachments = new ArrayList<File>(1);
        String filePath;
        String[] filePaths;
        filePath = intent.getStringExtra(EXTRA_ATTACHMENT);
        filePaths = intent.getStringArrayExtra(EXTRA_ATTACHMENTS);

        try {
            if (null != filePath) {
                attachments.add(new File(filePath));
            } else if (null != filePaths) {
                for (String path : filePaths) {
                    attachments.add(new File(path));
                }
            } else {
                //check if file uri was passed via Android Send
                Bundle bundle = intent.getExtras();
                if (null != bundle) {
                    if (bundle.containsKey(Intent.EXTRA_STREAM)) {
                        ArrayList<Uri> uris;
                        Object payload = bundle.get(Intent.EXTRA_STREAM);
                        if (payload instanceof Uri) {
                            uris = new ArrayList<Uri>(1);
                            uris.add((Uri)payload);
                        } else {
                            uris = (ArrayList<Uri>)payload;
                        }
                        for (Uri uri : uris) {
                            attachments.add(getFileFromUri(uri));
                        }
                    }
                }
            }


        } catch (Exception e) {
            Log.e(AppConfig.LOG_LABEL, "problem getting files for attachments", e);
            showMessage(this, getString(R.string.problem_getting_files_for_attachments), getString(R.string.error_message));
        }

        return attachments;
    }

    private File getFileFromUri(Uri uri) throws URISyntaxException {
        String filePath = FileUtils.getPath(this, uri);
        return new File(filePath);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case ACTIVITY_CHOOSE_ATTACHMENT: {
                if (resultCode == RESULT_OK) {
                    if (null != data) {
                        Uri uri = data.getData();
                        try {
                            String filePath = FileUtils.getPath(this, uri);
                            File file = new File(filePath);
                            addAttachmentToMap(file);
                        } catch (Exception e) {
                            Log.e(AppConfig.LOG_LABEL, "problem getting attachment", e);
                            Toast.makeText(this, getString(R.string.problem_getting_attachment), Toast.LENGTH_SHORT).show();
                        }
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    shouldShowInstallExplorer = true;
                }
                break;
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.send_bulletin, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; go home
                Intent intent = new Intent(this, MartusActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;
            case R.id.send_bulletin_menu_item:
                sendBulletin();
                return true;
            case R.id.cancel_bulletin_menu_item:
                setConfirmationType(CONFIRMATION_TYPE_CANCEL_BULLETIN);
                showConfirmationDialog();
                return true;
            case R.id.add_attachment_menu_item:
                chooseAttachment();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        if (shouldShowInstallExplorer) {
            showInstallExplorerDialog();
            shouldShowInstallExplorer = false;
        }
        if (! isNetworkAvailable()) {
            showMessage(this, getString(R.string.no_network_create_bulletin_warning),
                    getString(R.string.no_network_connection));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        BugSenseHandler.closeSession(BulletinActivity.this);
    }

    @Override
    public String getIndeterminateDialogMessage() {
        return getString(R.string.bulletin_packaging_progress);
    }

    @Override
    public String getDeterminateDialogMessage() {
        return getString(R.string.bulletin_sending_progress);
    }

    private void zipBulletin(Bulletin bulletin)  {
        indeterminateDialog = IndeterminateProgressDialog.newInstance();
        indeterminateDialog.show(getSupportFragmentManager(), "dlg_zipping");

        String author = mySettings.getString(SettingsActivity.KEY_AUTHOR, getString(R.string.default_author));
        bulletin.set(Bulletin.TAGAUTHOR, author);
        String title = titleText.getText().toString().trim();
        String summary = summaryText.getText().toString().trim();
        bulletin.set(Bulletin.TAGTITLE, title);
        bulletin.set(Bulletin.TAGSUMMARY, summary);
        stopInactivityTimer();
        parentApp.setIgnoreInactivity(true);

        final AsyncTask<Object, Integer, File> zipTask = new ZipBulletinTask(bulletin, this);
        zipTask.execute(getCacheDir(), store);

    }

    private Bulletin createBulletin() throws Exception
    {
        Bulletin b = store.createEmptyBulletin();
        b.set(Bulletin.TAGLANGUAGE, getDefaultLanguageForNewBulletin());
        b.setAuthorizedToReadKeys(new HQKeys(hqKey));
        b.setDraft();
        b.setAllPrivate(true);
        return b;
    }

    private String getDefaultLanguageForNewBulletin()
    {
        return mySettings.getString(SettingsActivity.KEY_DEFAULT_LANGUAGE, Locale.getDefault().getLanguage());
    }

    private void updateSettings() {
        serverIP = mySettings.getString(SettingsActivity.KEY_SERVER_IP, "");
        serverPublicKey = mySettings.getString(SettingsActivity.KEY_SERVER_PUBLIC_KEY, "");
        autoLogout = mySettings.getBoolean(SettingsActivity.KEY_AUTO_LOGOUT, false);
    }

    @Override
    public void onSent(String result) {
        try {
            determinateDialog.dismissAllowingStateLoss();
        } catch (IllegalStateException e) {
            //this is okay as the user may have closed this screen
        }
        String message = getResultMessage(result, this);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        if (autoLogout) {
            MartusActivity.logout();
            setResult(EXIT_RESULT_CODE);
        }
        finish();
    }

    public static String getResultMessage(String result, Context context) {
        String message;
        if (result != null && result.equals(NetworkInterfaceConstants.OK)) {
            message = context.getString(R.string.successful_send_notification);
        } else {
            message = context.getString(R.string.failed_send_notification, result);
        }
        return message;
    }

    @Override
    public void onZipped(File zippedFile) {
        try {
            indeterminateDialog.dismissAllowingStateLoss();
        } catch (IllegalStateException e) {
            //this is okay as the user may have closed this screen
        }
        sendZippedBulletin(zippedFile);
    }

    private void sendZippedBulletin(File zippedFile) {
        determinateDialog = DeterminateProgressDialog.newInstance();
        try {
            determinateDialog.show(getSupportFragmentManager(), "dlg_sending");
        } catch (IllegalStateException e) {
            // just means user has left app - do nothing
        }

        UniversalId bulletinId = bulletin.getUniversalId();
        try {
            removeCachedUriAttachments();
            store.destroyBulletin(bulletin);
        } catch (IOException e) {
            Log.e(AppConfig.LOG_LABEL, "problem destroying bulletin", e);
        }
        AsyncTask<Object, Integer, String> uploadTask = new UploadBulletinTask((MartusApplication)getApplication(),
                this, bulletinId);
        uploadTask.execute(bulletin.getUniversalId(), zippedFile, gateway, AppConfig.getInstance().getCrypto());
    }

    private void removeCachedUriAttachments() {
        AttachmentProxy[] attachmentProxies = bulletin.getPublicAttachments();
        for (AttachmentProxy proxy : attachmentProxies) {
            String label = proxy.getLabel();
            File file = new File(getCacheDir(), label);
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Override
    public void onProgressUpdate(int progress) {
        if (null != determinateDialog.getProgressDialog()) {
            determinateDialog.getProgressDialog().setProgress(progress);
        }
    }

    @Override
    public void onConfirmationAccepted() {
        switch (confirmationType) {
            case CONFIRMATION_TYPE_CANCEL_BULLETIN :
                this.finish();
                break;
            case CONFIRMATION_TYPE_DELETE_ATTACHMENT :
                String fileName = attachmentAdapter.getItem(attachmentToRemoveIndex);
                bulletinAttachments.remove(fileName);
                attachmentAdapter.remove(fileName);
                break;
        }
    }

    private void setConfirmationType(int type) {
        confirmationType = type;
    }

    @Override
    public String getConfirmationTitle() {
        if (confirmationType == CONFIRMATION_TYPE_CANCEL_BULLETIN) {
            return getString(R.string.confirm_cancel_bulletin);
        } else {
            return getString(R.string.confirm_remove_attachment, attachmentToRemoveName);
        }
    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapterView, View view, int i, long l) {
        Object item = adapterView.getItemAtPosition(i);
        attachmentToRemoveIndex = i;
        attachmentToRemoveName = item.toString();
        showRemoveDialog();
        return false;
    }

    public void showRemoveDialog() {
        setConfirmationType(CONFIRMATION_TYPE_DELETE_ATTACHMENT);
        ConfirmationDialog confirmationDialog = ConfirmationDialog.newInstance();
        confirmationDialog.show(getSupportFragmentManager(), "dlg_delete_attachment");
    }
}
