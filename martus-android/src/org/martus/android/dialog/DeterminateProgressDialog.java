package org.martus.android.dialog;

import org.martus.android.AppConfig;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

/**
 * @author roms
 *         Date: 1/17/13
 */
public class DeterminateProgressDialog extends DialogFragment {

    private Activity myActivity;

    public interface DeterminateProgressDialogListener {
        String getDeterminateDialogMessage();
    }

	public static DeterminateProgressDialog newInstance() {
        return new DeterminateProgressDialog();
	}

    public DeterminateProgressDialog() {
        // Empty constructor required for DialogFragment
    }

    public ProgressDialog getProgressDialog() {
        return (ProgressDialog)getDialog();
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
        myActivity = getActivity();
		final ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(((DeterminateProgressDialogListener) getActivity()).getDeterminateDialogMessage());
        dialog.setIndeterminate(false);
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(true);
        dialog.setMax(100);
        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setProgress(0);
		return dialog;
	}

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        myActivity.finish();
    }
}
