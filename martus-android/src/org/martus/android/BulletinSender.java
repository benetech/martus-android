package org.martus.android;

/**
 * @author roms
 *         Date: 10/25/12
 */
public interface BulletinSender {

    public void onSent();
    public void onProgressUpdate(int progress);
    public void onZipped();
}
