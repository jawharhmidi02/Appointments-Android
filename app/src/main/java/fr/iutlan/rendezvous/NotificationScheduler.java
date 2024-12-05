package fr.iutlan.rendezvous;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class NotificationScheduler {

    @SuppressLint("ScheduleExactAlarm")
    public static void scheduleNotification(Context context, String title, String message, String appointmentDate, String appointmentTime) {
        try {
            Log.v("Time","Context: " + context);
            Log.v("Time","Title: " + title);
            Log.v("Time","message: " + message);
            Log.v("Time","appointmentDate: " + appointmentDate);
            Log.v("Time","appointmentTime: " + appointmentTime);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dateFormat.parse(appointmentDate + " " + appointmentTime));

            // Subtract 1 day
            calendar.add(Calendar.DAY_OF_YEAR, -1);
            Log.v("Time","Calender: " + calendar);


            // Create the intent for the notification
            Intent intent = new Intent(context, ReminderBroadcastReceiver.class);
            intent.putExtra("title", title);
            intent.putExtra("message", message);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            if (alarmManager != null) {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
