package org.martus.android.dialog;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

/**
 * @author roms
 *         Date: 1/17/13
 */
public class IndeterminateProgressDialog extends DialogFragment {

    public interface IndeterminateProgressDialogListener {
        String getIndeterminateDialogMessage();
    }

	public static IndeterminateProgressDialog newInstance() {
        return new IndeterminateProgressDialog ();
	}

    public IndeterminateProgressDialog() {
        // Empty constructor required for DialogFragment
    }

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final ProgressDialog dialog = new ProgressDialog(getActivity());
		dialog.setTitle(((IndeterminateProgressDialogListener) getActivity()).getIndeterminateDialogMessage());
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
		return dialog;
	}

}
