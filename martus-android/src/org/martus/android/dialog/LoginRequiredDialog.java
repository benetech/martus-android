package org.martus.android.dialog;

import org.martus.android.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TextView;

/**
 * @author roms
 *         Date: 12/23/12
 */
public class LoginRequiredDialog extends DialogFragment {

    public interface LoginRequiredDialogListener {
        void onFinishLoginRequiredDialog();
    }

    public LoginRequiredDialog() {
        // Empty constructor required for DialogFragment
    }

    public static LoginRequiredDialog newInstance() {
        LoginRequiredDialog frag = new LoginRequiredDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle("You must first login!")
            .setMessage("Before sending this bulletin")
            .setPositiveButton(R.string.alert_dialog_ok,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            ((LoginRequiredDialogListener) getActivity()).onFinishLoginRequiredDialog();
                        }
                    }
            )
            .create();
    }
}
