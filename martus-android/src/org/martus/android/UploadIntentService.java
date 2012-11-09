package org.martus.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64;

import android.app.IntentService;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

/**
 * @author roms
 *         Date: 11/1/12
 */
public class UploadIntentService extends IntentService {
    private NotificationHelper mNH;
    private ClientSideNetworkGateway mGateway;
    private MartusCrypto mCrypto;

    public UploadIntentService() {
        super("UploadIntentService");
    }

    public UploadIntentService(ClientSideNetworkGateway gateway, MartusCrypto crypto) {
        super("UploadIntentService");

        mGateway = gateway;
        mCrypto = crypto;
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

        String filePath = intent.getStringExtra(BulletinActivity.EXTRA_ATTACHMENT);
        File zippedBulletin = new File(filePath);
        String accountId = intent.getStringExtra(BulletinActivity.EXTRA_ACCOUNT_ID);
        String localId = intent.getStringExtra(BulletinActivity.EXTRA_LOCAL_ID);
        UniversalId uid = UniversalId.createFromAccountAndLocalId(accountId, localId);
        String bulletinTitle = intent.getStringExtra(BulletinActivity.EXTRA_BULLETIN_TITLE);

        mNH = new NotificationHelper(this, bulletinTitle.hashCode());
        mNH.createNotification("Martus - " + bulletinTitle, "sending ...");
        try {
            String result = uploadBulletinZipFile(uid, zippedBulletin, mGateway, mCrypto, bulletinTitle);
            mNH.completed(result);
        } catch (MartusUtilities.FileTooLargeException e) {
            Log.e(AppConfig.LOG_LABEL, "bulletin too large", e);
        } catch (IOException e) {
            Log.e(AppConfig.LOG_LABEL, "io problem sending bulletin", e);
        } catch (MartusCrypto.MartusSignatureException e) {
            Log.e(AppConfig.LOG_LABEL, "crypto problem sending bulletin", e);
        }
        zippedBulletin.delete();
    }

    private String uploadBulletinZipFile(UniversalId uid, File tempFile, ClientSideNetworkGateway gateway, MartusCrypto crypto, String title)
            throws MartusUtilities.FileTooLargeException, IOException, MartusCrypto.MartusSignatureException {
        int totalSize = MartusUtilities.getCappedFileLength(tempFile);
        int offset = 0;
        byte[] rawBytes = new byte[NetworkInterfaceConstants.CLIENT_MAX_CHUNK_SIZE];
        FileInputStream inputStream = new FileInputStream(tempFile);
        String result = null;
        while (true) {
            int chunkSize = inputStream.read(rawBytes);
            if (chunkSize <= 0)
                break;
            byte[] chunkBytes = new byte[chunkSize];
            System.arraycopy(rawBytes, 0, chunkBytes, 0, chunkSize);

            String authorId = uid.getAccountId();
            String bulletinLocalId = uid.getLocalId();
            String encoded = StreamableBase64.encode(chunkBytes);

            NetworkResponse response = gateway.putBulletinChunk(crypto,
                    authorId, bulletinLocalId, totalSize, offset, chunkSize, encoded);
            result = response.getResultCode();
            if (!result.equals(NetworkInterfaceConstants.CHUNK_OK) && !result.equals(NetworkInterfaceConstants.OK))
                break;
            offset += chunkSize;

            mNH.updateProgress("sending ...", offset * 100 / totalSize);
        }
        inputStream.close();
        return result;
    }
}
