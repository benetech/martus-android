package org.martus.android;

import android.R;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;

/**
 * @author roms
 *         Date: 10/23/12
 */
public class NotificationHelper {
    private Context mContext;
    private NotificationManager mNotificationManager;
    private int mNotificationId;
    private String mTitle;

    public NotificationHelper(Context context, int notificationId)
    {
        mContext = context;
        mNotificationId = notificationId;
    }

    /**
     * Put the notification into the status bar
     */
    public void createNotification(String title, String subject) {
        mTitle = title;
        //get the notification manager
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //create the content which is shown in the notification pulldown
        final Notification notification = new Notification.Builder(mContext)
                 .setContentTitle(title)
                 .setContentText(subject)
                 .setSmallIcon(R.drawable.stat_notify_sync)
                 .setOngoing(true)
                 .build();

        //show the notification
        mNotificationManager.notify(mNotificationId, notification);
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void completed(String resultMsg, String title)    {
        //update notification to indicate completion
        int icon = R.drawable.stat_sys_download_done;
        if (!resultMsg.equals("ok")) {
            icon = R.drawable.stat_notify_error;
        }
        final Notification notification = new Notification.Builder(mContext)
                .setContentTitle(mTitle)
                .setContentText(title + " | sent with result: " + resultMsg)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .build();

        mNotificationManager.notify(mNotificationId, notification);
    }
}
