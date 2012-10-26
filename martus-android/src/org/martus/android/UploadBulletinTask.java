package org.martus.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.MartusUtilities;
import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.BulletinZipUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.database.BulletinStreamer;
import org.martus.common.database.Database;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.Packet;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class UploadBulletinTask extends AsyncTask<Object, Void, String> {

    private NotificationHelper mNotificationHelper;
    private Bulletin bulletin;
    private BulletinSender sender;

    public UploadBulletinTask(Context context, Bulletin bulletin, BulletinSender sender) {
        mNotificationHelper = new NotificationHelper(context, bulletin.getUniversalId().hashCode());
        this.bulletin = bulletin;
        this.sender = sender;
    }

    @Override
    protected String doInBackground(Object... params) {

        final UniversalId uid = (UniversalId)params[0];
        final File cacheDir = (File)params[1];
        final ClientSideNetworkGateway gateway = (ClientSideNetworkGateway)params[2];
        final MartusSecurity signer = (MartusSecurity)params[3];

        String result = null;
        final BulletinStreamer bs = new BulletinStreamer(bulletin);
        final File file = new File(cacheDir, "toUpload.zip");

        try {
            BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(bs, bulletin.getDatabaseKey(), file, signer);
            result = uploadBulletinZipFile(uid, file, gateway, signer);
        } catch (MartusUtilities.FileTooLargeException e) {
            Log.e("martus", "file too large to upload", e);
            result = e.getMessage();
        } catch (IOException e) {
            Log.e("martus", "io problem uploading file", e);
            result = e.getMessage();
        } catch (MartusCrypto.MartusSignatureException e) {
            Log.e("martus", "crypto problem uploading file", e);
            result = e.getMessage();
        } catch (Packet.WrongPacketTypeException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (MartusCrypto.DecryptionException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (Packet.SignatureVerificationException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (MartusCrypto.NoKeyPairException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (Packet.InvalidPacketException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (MartusCrypto.CryptoException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        } catch (Database.RecordHiddenException e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        }

        //file.delete();
        return result;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        mNotificationHelper.createNotification("Uploading bulletin", bulletin.get(Bulletin.TAGTITLE));
        Log.e("martus","!!!!!! PreExecute   !!!!!!!");
    }

    @Override
    protected void onPostExecute(String s) {
        Log.e("martus", "!!!!!!!! PostExecute   !!!!!!!!!!");
        mNotificationHelper.completed(s, bulletin.get(Bulletin.TAGTITLE));
        if (null != sender) {
            sender.onSent();
        }
        super.onPostExecute(s);
    }

    private String uploadBulletinZipFile(UniversalId uid, File tempFile, ClientSideNetworkGateway gateway, MartusCrypto crypto)
        		throws
                    MartusUtilities.FileTooLargeException, IOException, MartusCrypto.MartusSignatureException
    {
        int totalSize = MartusUtilities.getCappedFileLength(tempFile);
        int offset = 0;
        byte[] rawBytes = new byte[NetworkInterfaceConstants.CLIENT_MAX_CHUNK_SIZE];
        FileInputStream inputStream = new FileInputStream(tempFile);
        String result = null;
        while(true)
        {
            int chunkSize = inputStream.read(rawBytes);
            if(chunkSize <= 0)
                break;
            byte[] chunkBytes = new byte[chunkSize];
            System.arraycopy(rawBytes, 0, chunkBytes, 0, chunkSize);

            String authorId = uid.getAccountId();
            String bulletinLocalId = uid.getLocalId();
            String encoded = StreamableBase64.encode(chunkBytes);

            NetworkResponse response = gateway.putBulletinChunk(crypto,
                                authorId, bulletinLocalId, totalSize, offset, chunkSize, encoded);
            result = response.getResultCode();
            if(!result.equals(NetworkInterfaceConstants.CHUNK_OK) && !result.equals(NetworkInterfaceConstants.OK))
                break;
            offset += chunkSize;
        }
        inputStream.close();
        return result;
    }
}
