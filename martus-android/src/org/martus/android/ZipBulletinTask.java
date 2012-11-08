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
import org.martus.common.database.ReadableDatabase;
import org.martus.common.network.NetworkInterfaceConstants;
import org.martus.common.network.NetworkResponse;
import org.martus.common.packet.Packet;
import org.martus.common.packet.UniversalId;
import org.martus.util.StreamableBase64;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

/**
 * @author roms
 *         Date: 10/3/12
 */
public class ZipBulletinTask extends AsyncTask<Object, Integer, File> {

    private Bulletin bulletin;
    private BulletinSender sender;

    public ZipBulletinTask(Bulletin bulletin, BulletinSender sender) {
        this.bulletin = bulletin;
        this.sender = sender;
    }

    @Override
    protected File doInBackground(Object... params) {

        final File cacheDir = (File)params[0];
        final MartusSecurity signer = (MartusSecurity)params[1];
        final ReadableDatabase db = (ReadableDatabase)params[2];

        File tmpBulletin = null;

        File file = null;

        try {
            file = File.createTempFile("tmp_send_", ".zip", cacheDir);
            //tmpBulletin = File.createTempFile("tmp_", ".bull", cacheDir);
            //final BulletinStreamer bs = new BulletinStreamer(bulletin, tmpBulletin);
            //BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(bs, bulletin.getDatabaseKey(), file, signer);

            BulletinZipUtilities.exportBulletinPacketsFromDatabaseToZipFile(db, bulletin.getDatabaseKey(), file, signer);
        } catch (Exception e) {
            Log.e("martus", "problem serializing bulletin to zip", e);
        }

        if (null != tmpBulletin) tmpBulletin.delete();
        return file;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
    }

    @Override
    protected void onPostExecute(File result) {
        if (null != sender) {
            sender.onZipped(result);
        }
        super.onPostExecute(result);
    }

}
