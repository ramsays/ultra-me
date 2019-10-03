package com.ramsaysmith.ultrame;

import android.animation.ObjectAnimator;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

/**
 * Implementation of App Widget functionality.
 */
public class DataUsageWidget extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {

        // Construct the RemoteViews object
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.data_usage_widget);
        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 1, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.dataWidgetUsageCircle, pendingIntent);

        new RefreshAsyncTask(appWidgetId, context, appWidgetManager).execute();

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // There may be multiple widgets active, so update all of them
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    private static class RefreshAsyncTask extends AsyncTask<Void, Void, Integer> {

        private int weakWidgetId;
        private WeakReference<Context> weakContext;
        private WeakReference<AppWidgetManager> weakManager;
        private Integer dataUsed, dataTotal;

        public RefreshAsyncTask(int appWidgetId, Context context, AppWidgetManager manager) {
            weakWidgetId = appWidgetId;
            weakContext = new WeakReference<>(context);
            weakManager = new WeakReference<>(manager);
        }

        private boolean isUsagePermissionGranted() {
            AppOpsManager appOps = (AppOpsManager) weakContext.get().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,  android.os.Process.myUid(), weakContext.get().getPackageName());
            return (mode == AppOpsManager.MODE_ALLOWED);
        }

        @Override
        protected Integer doInBackground(Void... params) {

            // Load cached information
            SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(weakContext.get());

            // Data usage circle
            dataUsed = app_preferences.getInt("dataUsed", 0);
            Integer available = app_preferences.getInt("dataAvailable", 100);
            dataTotal = dataUsed + available;

            if (isUsagePermissionGranted()) { // Add current system usage if allowed
                try {
                    NetworkStatsManager networkStatsManager = (NetworkStatsManager) weakContext.get().getSystemService(Context.NETWORK_STATS_SERVICE);
                    TelephonyManager manager = (TelephonyManager) weakContext.get().getSystemService(Context.TELEPHONY_SERVICE);
                    String subscriberId = manager.getSubscriberId();
                    NetworkStats.Bucket bucket = networkStatsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, subscriberId,
                            app_preferences.getLong("dataUpdate", Calendar.getInstance().getTimeInMillis()),
                            Calendar.getInstance().getTimeInMillis());
                    long extraData = (bucket.getRxBytes() + bucket.getTxBytes()) / (1024*1024);
                    dataUsed += Integer.parseInt(String.valueOf(extraData));
                    Log.i("Ultra.me", "System indicated " + extraData + "MB extra data used since last system sync.");
                }
                catch (SecurityException e) {
                    Log.e("Ultra.me", "Could not access subscriber id to get live data.");
                }
                catch (Exception e) {
                    Log.e("Ultra.me", "Error occurred: " + e.getMessage());
                }
            }

            return 1;
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            RemoteViews remoteView = new RemoteViews(weakContext.get().getPackageName(), R.layout.data_usage_widget);

            // Update usage circle
            remoteView.setTextViewText(R.id.dataWidgetUsed, String.valueOf(dataUsed));
            if (dataTotal > 1000) {
                DecimalFormat decimalFormat = new DecimalFormat(".#");
                remoteView.setTextViewText(R.id.dataWidgetTotal, "of " + decimalFormat.format(dataTotal / 1000) + " GB");
            }
            else {
                remoteView.setTextViewText(R.id.dataWidgetTotal, "of " + String.valueOf(dataTotal) + " MB");
            }
            remoteView.setProgressBar(R.id.dataWidgetUsageCircle, dataTotal, dataUsed, false);
            weakManager.get().partiallyUpdateAppWidget(weakWidgetId, remoteView);
        }
    }
}
