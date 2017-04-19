package com.ericchee.bboyairwreck.piemessage;

import android.annotation.TargetApi;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.RemoteInput;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.media.RingtoneManager;
import android.os.Build;
import android.util.Log;

/**
 * Created by david on 4/17.
 *
 * @author david
 */
class Notification {
    private static PieMessageApplication pma = PieMessageApplication.getInstance();
    private static int lastNotifId = 0;

    @TargetApi(Build.VERSION_CODES.M)
    static void postNotification(String text, String sender, String chatId, String chatName) {
        int notifId = getNotifId();

        Intent resultIntent = new Intent(pma, MessageActivity.class);
        resultIntent.putExtra(Constants.Col.CHAT_ID, chatId);
        resultIntent.putExtra(Constants.Col.CHAT_NAME, chatName);
        resultIntent.putExtra(Constants.Col.ID, notifId);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(pma);

        stackBuilder.addParentStack(MessageActivity.class);

        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        String replyLabel = pma.getResources().getString(R.string.reply_label);
        Log.i("Notification", replyLabel);
        RemoteInput remoteInput = new RemoteInput.Builder(Constants.KEY_TEXT_REPLY)
                .setLabel(replyLabel)
                .build();

        android.app.Notification.Action action = new android.app.Notification.Action.Builder(Icon.createWithResource("", R.mipmap.notif_icon), replyLabel, resultPendingIntent)
                .addRemoteInput(remoteInput)
                .build();

        android.app.Notification.Builder mBuilder = new android.app.Notification.Builder(pma)
                .setSmallIcon(R.mipmap.notif_icon)
                .setContentTitle(chatName)
                .setContentText(sender.equals(chatName) ? text : sender + ": " + text)
                .setContentIntent(resultPendingIntent)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .addAction(action);

        NotificationManager mNotificationManager = (NotificationManager) pma.getSystemService(Context.NOTIFICATION_SERVICE);

        mNotificationManager.notify(notifId, mBuilder.build());
    }

    private synchronized static int getNotifId() {
        return lastNotifId++;
    }
}
