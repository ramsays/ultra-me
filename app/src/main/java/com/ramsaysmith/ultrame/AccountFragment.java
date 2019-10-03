package com.ramsaysmith.ultrame;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.app.Fragment;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.telephony.SmsManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.text.NumberFormat;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link AccountFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link AccountFragment} factory method to
 * create an instance of this fragment.
 */
public class AccountFragment extends Fragment {

    private Button actionRecharge;
    private EditText rechargePin;

    private OnFragmentInteractionListener mListener;

    public AccountFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_account, container, false);
        SharedPreferences app_preferences = PreferenceManager.getDefaultSharedPreferences(view.getContext());

        // Plan information
        TextView planAmount = view.findViewById(R.id.planAmount);
        planAmount.setText(String.format("$%s", app_preferences.getInt("planAmount", 19)));
        TextView planDataAmount = view.findViewById(R.id.planDataAmount);
        int dataAmount = app_preferences.getInt("dataUsed", 500) + app_preferences.getInt("dataAvailable", 0);
        if (dataAmount > 950) { // If 1 GB or more
            dataAmount = Math.round(dataAmount / 1000f);
            planDataAmount.setText(String.format("%s GB of 4G LTE data", dataAmount));
        } else
            planDataAmount.setText(String.format("%s MB of 4G LTE data", dataAmount));
        TextView planExpires = view.findViewById(R.id.planExpires);
        planExpires.setText(String.format("Expires on %s at 2359 PST", app_preferences.getString("planExpires", "1 Jan")));

        // Wallet and balances
        NumberFormat formatter = NumberFormat.getCurrencyInstance();
        TextView balanceWallet = view.findViewById(R.id.balanceWallet);
        balanceWallet.setText(formatter.format(app_preferences.getFloat("balanceWallet", 0.00f)));
        TextView balanceIntl = view.findViewById(R.id.balanceIntl);
        balanceIntl.setText(formatter.format(app_preferences.getFloat("balanceIntl", 0.00f)));
        TextView balanceRoaming = view.findViewById(R.id.balanceRoaming);
        balanceRoaming.setText(formatter.format(app_preferences.getFloat("balanceRoaming", 0.00f)));

        // Action buttons and editors
        rechargePin = view.findViewById(R.id.actionRecharge);
        actionRecharge = view.findViewById(R.id.actionRechargeButton);
        rechargePin.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                EditText editor = (EditText) v;
                if (editor.getText().length() >= 8)
                    actionRecharge.setEnabled(true);
                else
                    actionRecharge.setEnabled(false);
                return false;
            }
        });
        actionRecharge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send SMS message to update data and balance information
                SmsManager manager = SmsManager.getDefault();
                manager.sendTextMessage("6700", null, String.format("RECHARGE + %s", rechargePin.getText()), null, null);
                rechargePin.setText("");
                v.setEnabled(false);
                v.requestFocus();
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(v.getContext())
                        .setTitle("Recharge request sent")
                        .setMessage("Your recharge PIN has been sent to Ultra Mobile. You should recieve a notification momentarily to confirm the recharge is complete.")
                        .setNegativeButton("Got it", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                alertDialog.show();
            }
        });

        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
