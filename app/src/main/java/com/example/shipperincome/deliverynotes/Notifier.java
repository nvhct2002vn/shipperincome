package com.example.shipperincome.deliverynotes;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import com.example.shipperincome.MainActivity;

final class Notifier {
    static final String MONITOR_CHANNEL = "delivery_notes_monitor";
    static final String NOTE_CHANNEL = "delivery_notes_notes";

    static final int FOREGROUND_ID = 1001;
    // Id thong bao cua tung ghi chu = offset + id trong DB, de khong bao gio trung FOREGROUND_ID.
    private static final int NOTE_ID_OFFSET = 100_000;

    private Notifier() {
    }

    static void ensureChannels(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                MONITOR_CHANNEL,
                "Theo dõi vị trí",
                NotificationManager.IMPORTANCE_LOW));
        manager.createNotificationChannel(new NotificationChannel(
                NOTE_CHANNEL,
                "Nhắc ghi chú địa điểm",
                NotificationManager.IMPORTANCE_HIGH));
    }

    static Notification monitorNotification(Context context) {
        Intent stopIntent = new Intent(context, LocationMonitorService.class)
                .setAction(LocationMonitorService.ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(
                context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return baseBuilder(context, MONITOR_CHANNEL)
                .setContentTitle("Đang theo dõi vị trí giao hàng")
                .setContentText("Ứng dụng sẽ nhắc khi bạn đến gần địa điểm đã lưu.")
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setOngoing(true)
                .addAction(new Notification.Action.Builder(null, "Tắt nhắc", stopPending).build())
                .build();
    }

    static void showNote(Context context, LocationNote note, float distanceMeters) {
        if (context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Notification notification = baseBuilder(context, NOTE_CHANNEL)
                .setContentTitle(note.name + " · " + Math.round(distanceMeters) + "m")
                .setContentText(note.note)
                .setStyle(new Notification.BigTextStyle().bigText(note.note))
                .setSmallIcon(android.R.drawable.ic_dialog_map)
                .setAutoCancel(true)
                .build();
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        int notifyId = (int) (NOTE_ID_OFFSET + note.id % (Integer.MAX_VALUE - NOTE_ID_OFFSET));
        manager.notify(notifyId, notification);
    }

    private static Notification.Builder baseBuilder(Context context, String channelId) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(context, channelId)
                .setContentIntent(pendingIntent)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
    }
}
