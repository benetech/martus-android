package org.martus.android;

import java.io.File;
import java.io.FilenameFilter;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipImporter;
import org.martus.common.crypto.MartusSecurity;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author roms
 *         Date: 11/1/12
 */
public class ResendService extends IntentService implements ProgressUpdater {
    private NotificationHelper mNH;

    public ResendService() {
        super("ResendService");
    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        final String serverIP = intent.getStringExtra(SettingsActivity.KEY_SERVER_IP);
        final String serverPublicKey = intent.getStringExtra(SettingsActivity.KEY_SERVER_PUBLIC_KEY);
        final ClientSideNetworkGateway mGateway = ClientSideNetworkGateway.buildGateway(serverIP, serverPublicKey);

        final File cacheDir = getApplicationContext().getCacheDir();
        File failedDir = new File (cacheDir, UploadBulletinTask.FAILED_BULLETINS_DIR);
        if (failedDir.exists()) {
            final String[] names = failedDir.list(new ZipFileFilter());
            for (String name : names) {
                try {
                    final MartusSecurity mCrypto = BaseActivity.createKeyPairCopy(AppConfig.getInstance().getCrypto());
                    Bulletin tempBulletin = new Bulletin(mCrypto);
                    File zipFile = new File(failedDir, name);
                    BulletinZipImporter.loadFromFile(tempBulletin, zipFile, mCrypto);
                    mNH = new NotificationHelper(getApplicationContext(), tempBulletin.getUniversalId().hashCode());
                    UploadBulletinTask.createInitialNotification(mNH, getApplicationContext());
                    String result = UploadBulletinTask.doSend(tempBulletin.getUniversalId(), zipFile, mGateway, mCrypto, this);
                    mNH.completed(result);
                } catch (Exception e) {
                    Log.e(AppConfig.LOG_LABEL, "problem reading zipped bulletin", e);
                }
            }
        }
    }

    @Override
    public void showProgress(int value) {
        mNH.updateProgress(getApplicationContext().getString(R.string.starting_send_notification), value);
    }

}
