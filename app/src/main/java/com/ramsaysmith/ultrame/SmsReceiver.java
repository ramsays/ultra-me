package com.ramsaysmith.ultrame;

import android.app.Activity;
import android.app.PendingIntent;
import android.arch.persistence.room.Room;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.provider.Telephony;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.List;

public class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        if (intent.getAction().equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            SmsMessage[] smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent);
            for (SmsMessage message : smsMessages) {
                if (message.getOriginatingAddress().equals("6700")) { // Only messages from ultra mobile
                    String text = message.getMessageBody();
                    if (text.startsWith("4G LTE:")) { // update data status
                        String used = text.substring(text.indexOf(" ", 18)+1, text.indexOf("B used", 14)+1),
                            available = text.substring(8, text.indexOf("B remaining")+1);
                        Log.i("Ultra.me", "Data usage is " + used + " with " + available + " available");

                        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = app_preferences.edit();
                        editor.putInt("dataUsed", getMbValue(used));
                        editor.putInt("dataAvailable", getMbValue(available));
                        editor.putLong("dataUpdate", Calendar.getInstance().getTimeInMillis());
                        editor.apply();
                        this.abortBroadcast();
                    }
                    else if (text.startsWith("Ultra Wallet")) { // update balances
                        String wallet = text.substring(text.indexOf("$")+1, text.indexOf(System.lineSeparator(), 15)-1);
                        String intl = text.substring(text.indexOf("$", text.indexOf("INTL"))+1, text.indexOf(System.lineSeparator(), text.indexOf("INTL")+14)-1);
                        String roaming = text.substring(text.indexOf("$", text.indexOf("Roaming"))+1, text.indexOf(System.lineSeparator(), text.indexOf("Roaming")+10)-1);
                        String amount = text.substring(text.indexOf("$", text.indexOf("Your "))+1, text.indexOf(" ", text.indexOf("Your ")+7));
                        String expiry = text.substring(text.indexOf("expires on")+11, text.indexOf("expires on")+17);
                        Log.i("Ultra.me", "Balances, wallet = $" + wallet + ", international = $" + intl + ", roaming = $" + roaming);
                        Log.i("Ultra.me", "Plan is $" + amount + " per month and expires on " + expiry);

                        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = app_preferences.edit();
                        editor.putFloat("balanceWallet", Float.valueOf(wallet));
                        editor.putFloat("balanceIntl", Float.valueOf(intl));
                        editor.putFloat("balanceRoaming", Float.valueOf(roaming));
                        editor.putInt("planAmount", Integer.valueOf(amount));
                        editor.putString("planExpires", expiry);
                        editor.apply();
                        this.abortBroadcast();
                    }
                    else if (text.startsWith("Thank you from Ultra")) { // new billing cycle
                        String expiry = text.substring(text.indexOf("will end on")+12, text.indexOf("will end on")+18);
                        Log.i("Ultra.me", "Plan has renewed and expires on " + expiry);

                        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(context);
                        SharedPreferences.Editor editor = app_preferences.edit();
                        editor.putInt("dataUsed", 0);
                        editor.putLong("dataUpdate", Calendar.getInstance().getTimeInMillis());
                        editor.putString("planExpires", expiry);
                        editor.apply();
                        this.abortBroadcast();
                    }
                    else if (text.startsWith("System error")) {
                        Log.e("Ultra.me", "Ultra Mobile system returned an error.");
                        this.abortBroadcast();
                    }
                    else if (text.startsWith("Invalid command")) {
                        Log.e("Ultra.me", "Ultra Mobile system denied request due to invalid command.");
                        this.abortBroadcast();
                    }
                    else { // add to notification database
                        if (text.length() < 30) {
                            this.abortBroadcast();
                            continue;
                        }

                        new InsertAsyncTask(context, text).execute();

                        // build android notification
                        Intent notifyIntent = new Intent(context, MainActivity.class);
                        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = PendingIntent.getActivity(context.getApplicationContext(), 0, notifyIntent, 0);
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(context.getApplicationContext(), "main")
                                .setSmallIcon(R.drawable.ic_notify_main)
                                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                                .setContentTitle(context.getString(R.string.app_name))
                                .setContentText(text)
                                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                                .setContentIntent(pendingIntent)
                                .setAutoCancel(true);

                        // send notification
                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context.getApplicationContext());
                        notificationManager.notify(0, builder.build());
                    }
                }
            }
        }

    }

    private int getMbValue(String textValue) {
        int value;
        if (textValue.substring(textValue.length()-2).equals("GB")) {
            value = Integer.valueOf(textValue.replaceAll("\\.","").substring(0, textValue.indexOf("G")-1).trim());
        }
        else {
            value = Integer.valueOf(textValue.substring(0, textValue.indexOf(".")).trim());
        }
        return value;
    }

    private static class InsertAsyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<Context> weakActivity;
        private String notificationItem;

        public InsertAsyncTask(Context activity, String notification) {
            weakActivity = new WeakReference<>(activity);
            notificationItem = notification;
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // Insert notifications
            AppDatabase db = AppDatabase.getDatabase(weakActivity.get());
            NotificationContent.NotificationItem item = new NotificationContent.NotificationItem(
                    db.notificationDao().getNotificationCount(),
                    notificationItem,
                    Calendar.getInstance().getTime().getTime());
            db.notificationDao().insert(item);
            return 1;
        }
    }
}
