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
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author roms
 *         Date: 12/23/12
 */
public class MagicWordDialog extends DialogFragment implements DialogInterface.OnClickListener,
        TextView.OnEditorActionListener {

    private EditText magicWordText;

    public interface MagicWordDialogListener {
        void onFinishMagicWordDialog(TextView inputText);
    }

    public MagicWordDialog() {
        // Empty constructor required for DialogFragment
    }

    public static MagicWordDialog newInstance() {
        MagicWordDialog frag = new MagicWordDialog();
        Bundle args = new Bundle();
        frag.setArguments(args);
        frag.setCancelable(false);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        LayoutInflater factory = LayoutInflater.from(getActivity());
        final View magicWordView = factory.inflate(R.layout.magic_word_dialog, null);
        magicWordText = (EditText) magicWordView.findViewById(R.id.password_edit);
        magicWordText.setOnEditorActionListener(this);
        return new AlertDialog.Builder(getActivity())
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.magic_word_dialog_title)
            .setView(magicWordView)
            .setPositiveButton(R.string.alert_dialog_ok,this)
            .create();
    }

    public void onClick(DialogInterface dialog, int whichButton) {
        ((MagicWordDialogListener) getActivity()).onFinishMagicWordDialog(magicWordText);
    }

    @Override
    public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
        if (actionId == EditorInfo.IME_ACTION_DONE) {
            ((MagicWordDialogListener) getActivity()).onFinishMagicWordDialog(magicWordText);
            this.dismiss();
            return true;
        }
        return false;
    }
}
