package org.martus.common.database;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.martus.common.bulletin.Bulletin;
import org.martus.common.bulletin.PendingAttachmentList;
import org.martus.common.crypto.MartusCrypto;
import org.martus.common.packet.Packet;
import org.martus.util.UnicodeStringWriter;
import org.martus.util.UnicodeWriter;
import org.martus.util.inputstreamwithseek.FileInputStreamWithSeek;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;
import org.martus.util.inputstreamwithseek.StringInputStreamWithSeek;

/**
 * @author roms
 *         Date: 10/8/12
 */
public class BulletinStreamer implements PacketStreamOpener{

    private Bulletin bulletin;
    private File bulletinFile;


    public BulletinStreamer(Bulletin bulletin, File bulletinFile) {
        this.bulletin = bulletin;
        this.bulletinFile = bulletinFile;
    }

    @Override
    public InputStreamWithSeek openInputStream(DatabaseKey key, MartusCrypto crypto) throws IOException, MartusCrypto.CryptoException {
        Writer writer = new FileWriter(bulletinFile);

        if (key.getUniversalId().equals(bulletin.getBulletinHeaderPacket().getUniversalId())) {
            bulletin.getBulletinHeaderPacket().writeXml(writer, crypto);
        } else if (key.getUniversalId().equals(bulletin.getFieldDataPacket().getUniversalId())) {
            bulletin.getFieldDataPacket().writeXml(writer, crypto);
        } else if (key.getUniversalId().equals(bulletin.getPrivateFieldDataPacket().getUniversalId())) {
            bulletin.getPrivateFieldDataPacket().writeXml(writer, crypto);
        } else {
            //write attachments
            writePendingAttachments(bulletin.getPendingPublicAttachments(), writer, crypto);
            writePendingAttachments(bulletin.getPendingPrivateAttachments(), writer, crypto);
        }

        return new FileInputStreamWithSeek(bulletinFile);
    }

    private static void writePendingAttachments(PendingAttachmentList pendingAttachments, Writer writer, MartusCrypto crypto) throws IOException, MartusCrypto.CryptoException
    	{
    		for(int i = 0; i < pendingAttachments.size(); ++i)
    		{
    			Packet packet = pendingAttachments.get(i);
                packet.writeXml(writer, crypto);
    		}
    	}

}
