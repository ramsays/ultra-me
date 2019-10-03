package com.ramsaysmith.ultrame;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DashboardFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DashboardFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DashboardFragment extends Fragment {
    // TODO: Rename parameter arguments, choose names that match

    public DashboardFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment DashboardFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static DashboardFragment newInstance(String param1, String param2) {
        DashboardFragment fragment = new DashboardFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        new RefreshAsyncTask(this.getView()).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View content = inflater.inflate(R.layout.fragment_dashboard, container, false);
        new RefreshAsyncTask(content).execute();

        return content;
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name

    }

    private static class RefreshAsyncTask extends AsyncTask<Void, Void, Integer> {

        private WeakReference<View> weakView;
        private Integer dataUsed, dataTotal, dataValue;
        private String dateString;

        public RefreshAsyncTask(View view) {
            weakView = new WeakReference<>(view);
        }

        private boolean isUsagePermissionGranted() {
            AppOpsManager appOps = (AppOpsManager) weakView.get().getContext().getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,  android.os.Process.myUid(), weakView.get().getContext().getPackageName());
            return (mode == AppOpsManager.MODE_ALLOWED);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ProgressBar dataUsageCircle = weakView.get().findViewById(R.id.dataUsageCircle);
            dataValue = dataUsageCircle.getProgress();
            dataUsageCircle.setProgress(0);
        }

        @Override
        protected Integer doInBackground(Void... params) {
            View view = weakView.get();

            // Load cached information
            SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());
            Long ticks = Calendar.getInstance().getTimeInMillis() - app_preferences.getLong("dataUpdate", Calendar.getInstance().getTimeInMillis());
            if (ticks/1000 <= 60*1.5 || isUsagePermissionGranted()) { dateString = "Just now"; } // if less than one minute
            else if (ticks/1000 <= 60*60*1.5) { dateString = Math.round((float)ticks/1000/60) + " mins. ago"; } // if less than one hour
            else if (ticks/1000 <= 60*60*24*1.5) { dateString = Math.round((float)ticks/1000/60/60) + " hrs. ago"; } // if less than one day
            else { dateString = Math.round((float)ticks/1000/60/60/24) + " days ago"; }

            // Data usage circle
            dataUsed = app_preferences.getInt("dataUsed", 0);
            Integer available = app_preferences.getInt("dataAvailable", 100);
            dataTotal = dataUsed + available;

            if (isUsagePermissionGranted()) { // Add current system usage if allowed
                try {
                    NetworkStatsManager networkStatsManager = (NetworkStatsManager) view.getContext().getSystemService(Context.NETWORK_STATS_SERVICE);
                    TelephonyManager manager = (TelephonyManager) view.getContext().getSystemService(Context.TELEPHONY_SERVICE);
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
            View view = weakView.get();
            SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());

            // Update date of update
            TextView lastUpdated = view.findViewById(R.id.timeStatusUpdated);
            lastUpdated.setText(dateString);

            // Update usage circle
            TextView usedData = view.findViewById(R.id.dataUsed);
            usedData.setText(String.valueOf(dataUsed));
            TextView totalData = view.findViewById(R.id.dataTotal);
            if (dataTotal > 1000) {
                DecimalFormat decimalFormat = new DecimalFormat(".#");
                totalData.setText("of " + decimalFormat.format(dataTotal / 1000) + " GB");
            }
            else {
                totalData.setText("of " + String.valueOf(dataTotal) + " MB");
            }
            ProgressBar dataUsageCircle = view.findViewById(R.id.dataUsageCircle);
            dataUsageCircle.setMax(dataTotal);
            dataUsageCircle.setSecondaryProgress(dataTotal);

            // Animate data circle
            if (Math.abs(dataValue - dataUsed) >= 5) {
                ObjectAnimator animation = ObjectAnimator.ofInt(dataUsageCircle, "progress", dataValue, dataUsed);
                animation.setDuration(dataUsed * 5000 / dataTotal); // in milliseconds
                animation.setInterpolator(new DecelerateInterpolator());
                animation.setStartDelay(100);
                animation.start();
            }
            else {
                dataUsageCircle.setProgress(dataUsed);
            }
        }
    }

}
