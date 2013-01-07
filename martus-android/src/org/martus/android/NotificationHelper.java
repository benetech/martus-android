package org.martus.android;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.support.v4.app.NotificationCompat;

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
        mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

        //create the content which is shown in the notification pulldown
        final Notification notification = new NotificationCompat.Builder(mContext)
                 .setContentTitle(mTitle)
                 .setContentText(subject)
                 .setSmallIcon(android.R.drawable.stat_notify_sync)
                 .setOngoing(true)
                 .setProgress(100, 0, false)
                 .build();

        //show the notification
        mNotificationManager.notify(mNotificationId, notification);
    }

    public void updateProgress(String subject, int progress) {
        //create the content which is shown in the notification pulldown
        final Notification notification = new NotificationCompat.Builder(mContext)
                 .setContentTitle(mTitle)
                 .setContentText(subject)
                 .setSmallIcon(android.R.drawable.stat_notify_sync)
                 .setOngoing(true)
                 .setProgress(100, progress, false)
                 .build();

        //show the notification
        mNotificationManager.notify(mNotificationId, notification);
    }

    /**
     * called when the background task is complete, this removes the notification from the status bar.
     * We could also use this to add a new ‘task complete’ notification
     */
    public void  completed(String resultMsg)    {
        //update notification to indicate completion
        int icon;
        if (null != resultMsg) {
            icon = android.R.drawable.stat_sys_download_done;
            if (!resultMsg.equals("ok")) {
                icon = android.R.drawable.stat_notify_error;
            }
        } else {
            resultMsg = mContext.getString(R.string.failure_send_notification);
            icon = android.R.drawable.stat_notify_error;
        }
        final Notification notification = new NotificationCompat.Builder(mContext)
                .setContentTitle(mTitle)
                .setContentText(mContext.getString(R.string.successful_send_notification) + resultMsg)
                .setSmallIcon(icon)
                .setAutoCancel(true)
                .build();

        mNotificationManager.notify(mNotificationId, notification);
    }
}
