package org.martus.common.database;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.martus.common.bulletin.Bulletin;
import org.martus.common.crypto.MartusCrypto;
import org.martus.util.inputstreamwithseek.ByteArrayInputStreamWithSeek;
import org.martus.util.inputstreamwithseek.InputStreamWithSeek;

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
        Writer writer = new StringWriter();
        byte[] xml = new byte[0];

        if (key.getUniversalId().equals(bulletin.getBulletinHeaderPacket().getUniversalId())) {
            xml = bulletin.getBulletinHeaderPacket().writeXml(writer, decrypter);
        } else if (key.getUniversalId().equals(bulletin.getFieldDataPacket().getUniversalId())) {
            xml = bulletin.getFieldDataPacket().writeXml(writer, decrypter);
        } else if (key.getUniversalId().equals(bulletin.getPrivateFieldDataPacket().getUniversalId())) {
            xml = bulletin.getPrivateFieldDataPacket().writeXml(writer, decrypter);
        }

        return new ByteArrayInputStreamWithSeek(xml);
    }
}
