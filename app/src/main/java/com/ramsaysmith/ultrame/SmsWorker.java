package com.ramsaysmith.ultrame;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.telephony.SmsManager;

import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class SmsWorker extends Worker {

    public SmsWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    private boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this.getApplicationContext(), Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public Result doWork() {

        // Check for permissions and network connections
        if (!isSmsPermissionGranted()) {
            return Result.failure();
        }
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        if (!activeNetworkInfo.isConnected()) { // If no connection
            return Result.retry();
        }
        if (activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI) { // Don't update if on Wifi
            return Result.success();
        }

        // Send SMS message to update data and balance information
        SmsManager manager = SmsManager.getDefault();
        manager.sendTextMessage("6700", null, "DATA", null, null);

        return Result.success();
    }
}
