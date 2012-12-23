package org.martus.android.dialog;

import org.martus.android.ConfirmationDialogHandler;
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
public class ConfirmationDialog extends DialogFragment {

    public interface ConfirmationDialogListener {
        void onConfirmationAccepted();
        void onConfirmationCancelled();
    }

    public ConfirmationDialog() {
        // Empty constructor required for DialogFragment
    }

    public static ConfirmationDialog newInstance() {
        ConfirmationDialog frag = new ConfirmationDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle("Are you sure?")
                .setPositiveButton(R.string.yes,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((ConfirmationDialogListener) getActivity()).onConfirmationAccepted();
                            }
                        }
                )
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                ((ConfirmationDialogListener) getActivity()).onConfirmationCancelled();
                            }
                        }
                )
                .create();
    }
}
