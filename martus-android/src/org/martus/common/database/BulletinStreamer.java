package org.martus.common.database;

import java.io.IOException;
import java.io.StringWriter;

import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;
import org.martus.util.inputstreamwithseek.StringInputStreamWithSeek;

/**
 * @author roms
 *         Date: 10/8/12
 */
public class BulletinStreamer implements PacketStreamOpener{

    private Bulletin bulletin;


    public BulletinStreamer(Bulletin bulletin) {
        this.bulletin = bulletin;
    }

    @Override
    public InputStreamWithSeek openInputStream(DatabaseKey key, MartusCrypto decrypter) throws IOException, MartusCrypto.CryptoException {
        StringWriter writer = new StringWriter();

        if (key.getUniversalId().equals(bulletin.getBulletinHeaderPacket().getUniversalId())) {
            bulletin.getBulletinHeaderPacket().writeXml(writer, decrypter);
        } else if (key.getUniversalId().equals(bulletin.getFieldDataPacket().getUniversalId())) {
            bulletin.getFieldDataPacket().writeXml(writer, decrypter);
        } else if (key.getUniversalId().equals(bulletin.getPrivateFieldDataPacket().getUniversalId())) {
            bulletin.getPrivateFieldDataPacket().writeXml(writer, decrypter);
        }

        return new StringInputStreamWithSeek(writer.toString());
    }

}
