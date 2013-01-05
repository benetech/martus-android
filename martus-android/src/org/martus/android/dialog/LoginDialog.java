package org.martus.android.dialog;

import org.martus.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author roms
 *         Date: 12/22/12
 */
public class LoginDialog extends DialogFragment implements DialogInterface.OnClickListener, TextView.OnEditorActionListener {

    private EditText passwordText;



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
        passwordText = (EditText) passwordEntryView.findViewById(R.id.password_edit);
        passwordText.setOnEditorActionListener(this);

        return new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.password_dialog_title)
            .setView(passwordEntryView)
            .setPositiveButton(R.string.alert_dialog_ok, this)
            .setNegativeButton(R.string.password_dialog_cancel, this)
            .create();
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            ((LoginDialogListener) getActivity()).onFinishPasswordDialog(passwordText);
            this.dismiss();
            return true;
        }
        return false;
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        switch (whichButton) {
            case -1:    ((LoginDialogListener) getActivity()).onFinishPasswordDialog(passwordText);
                        break;
            case -2:    ((LoginDialogListener) getActivity()).onCancelPasswordDialog();
                        break;
        }
    }

}
