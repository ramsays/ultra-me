package com.ramsaysmith.ultrame;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.app.FragmentManager;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

public class MainActivity extends AppCompatActivity {

    static final int SMS_PERMISSION_CODE = 134;
    private Fragment dashboardFragment, accountFragment, notificationFragment;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_dashboard:
                    replaceFragment(dashboardFragment);
                    return true;
                case R.id.navigation_account:
                    replaceFragment(accountFragment);
                    return true;
                case R.id.navigation_notifications:
                    replaceFragment(notificationFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dashboardFragment = (Fragment) new DashboardFragment();
        accountFragment = (Fragment) new AccountFragment();
        notificationFragment = (Fragment) new NotificationFragment();
        replaceFragment(dashboardFragment);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        createNotificationChannel();
        new DBAsyncTask(this).execute();

        if (!isSmsPermissionGranted()) { requestReadAndSendSmsPermission(); }
        if (!isUsagePermissionGranted()) {
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(this)
                    .setTitle("Usage access required")
                    .setMessage("For the best experience, you need to give Ultra.me access to your phone's data usage. You can do this in your system settings.")
                    .setNegativeButton("No Thanks", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        }
                    });
            alertDialog.show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings: /*Intent intent = new Intent(this, SettingsActivity.class); startActivity(intent);*/ break;
        }
        return super.onOptionsItemSelected(item);
    }

    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
    public void replaceFragment(Fragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.fragmentContainer, fragment);
        fragmentTransaction.addToBackStack(fragment.toString());
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        fragmentTransaction.commit();
    }

    // SMS permissions
    public boolean isSmsPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
    }
    public boolean isUsagePermissionGranted() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,  android.os.Process.myUid(), getPackageName());
        return (mode == AppOpsManager.MODE_ALLOWED);
    }
    private void requestReadAndSendSmsPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
            // You may display a non-blocking explanation here, read more in the documentation:
            // https://developer.android.com/training/permissions/requesting.html
        }
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS, Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE}, SMS_PERMISSION_CODE);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_CODE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Configure and queue the update worker
                    PeriodicWorkRequest.Builder smsBuilder =
                            new PeriodicWorkRequest.Builder(SmsWorker.class, 5, TimeUnit.DAYS);
                    // ...if you want, you can apply constraints to the builder here...

                    // Create the actual work object:
                    PeriodicWorkRequest smsUpdate = smsBuilder.build();
                    // Then enqueue the recurring task:
                    WorkManager.getInstance().cancelAllWork();
                    WorkManager.getInstance().enqueue(smsUpdate);

                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                    AlertDialog.Builder dialog = new AlertDialog.Builder(this);
                    dialog.setTitle("Permission denied").setMessage("Cannot send or receive SMS messages, so this app cannot keep status up-to-date.");
                    dialog.show();
                }
            }
        }
    }

    // Notifications
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = getSystemService(NotificationManager.class);

            // Main notification channel
            NotificationChannel channel = new NotificationChannel("main", getString(R.string.channel_main_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.channel_main_desc));
            notificationManager.createNotificationChannel(channel);

            // Data usage notifications
            channel = new NotificationChannel("data-usage", getString(R.string.channel_data_name),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(getString(R.string.channel_data_desc));
            notificationManager.createNotificationChannel(channel);
        }
    }
    private static class DBAsyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<Activity> weakActivity;

        public DBAsyncTask(Activity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            // Load notifications
            AppDatabase db = AppDatabase.getDatabase(weakActivity.get());
            if (!NotificationContent.ITEMS.isEmpty()) { return 1; }
            List<NotificationContent.NotificationItem> notificationItemList = db.notificationDao().getAll();
            for (NotificationContent.NotificationItem item : notificationItemList) {
                NotificationContent.addItem(item);
            }
            return 1;
        }
    }
}
