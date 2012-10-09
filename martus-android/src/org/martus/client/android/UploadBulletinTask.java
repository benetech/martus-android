package org.martus.client.android;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.martus.clientside.ClientSideNetworkGateway;
import org.martus.common.MartusUtilities;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.crypto.MartusSecurity;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64;

import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class UploadBulletinTask extends AsyncTask<Object, Void, String> {
    @Override
    protected String doInBackground(Object... params) {

        final UniversalId uid = (UniversalId)params[0];
        final File bulletinZip = (File)params[1];
        final ClientSideNetworkGateway gateway = (ClientSideNetworkGateway)params[2];
        final MartusSecurity signer = (MartusSecurity)params[3];

        String result;

        try {
            result = uploadBulletinZipFile(uid, bulletinZip, gateway, signer);
        } catch (MartusUtilities.FileTooLargeException e) {
            Log.e("martus", "file too large to upload", e);
            result = e.getMessage();
        } catch (IOException e) {
            Log.e("martus", "io problem uploading file", e);
            result = e.getMessage();
        } catch (MartusCrypto.MartusSignatureException e) {
            Log.e("martus", "crypto problem uploading file", e);
            result = e.getMessage();
        }

        return result;
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
