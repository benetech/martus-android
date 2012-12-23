package org.martus.android;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author roms
 *         Date: 12/22/12
 */
public class LoginDialog  extends DialogFragment {

    public interface LoginDialogListener {
        void onFinishPasswordDialog(TextView inputText);
        void onCancelPasswordDialog();
    }

    public LoginDialog() {
        // Empty constructor required for DialogFragment
    }

    public static LoginDialog newInstance() {
        LoginDialog frag = new LoginDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        frag.setCancelable(false);
        return frag;
    }


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        LayoutInflater factory = LayoutInflater.from(getActivity());
        View passwordEntryView = factory.inflate(R.layout.password_dialog, null);
        final EditText passwordText = (EditText) passwordEntryView.findViewById(R.id.password_edit);

        return new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.password_dialog_title)
            .setView(passwordEntryView)
            .setPositiveButton(R.string.alert_dialog_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LoginDialogListener) getActivity()).onFinishPasswordDialog(passwordText);
                        }
                    }
            )
            .setNegativeButton(R.string.password_dialog_cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LoginDialogListener) getActivity()).onCancelPasswordDialog();
                        }
                    }
            )
            .create();
    }

}
