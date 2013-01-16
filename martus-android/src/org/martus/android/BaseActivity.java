package org.martus.android;

import org.martus.android.dialog.ConfirmationDialog;
import org.martus.android.dialog.InstallExplorerDialog;
import org.martus.android.dialog.LoginRequiredDialog;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;

/**
 * @author roms
 *         Date: 12/19/12
 */
public class BaseActivity extends FragmentActivity implements ConfirmationDialog.ConfirmationDialogListener,
        LoginRequiredDialog.LoginRequiredDialogListener {

    public static final long INACTIVITY_TIMEOUT = 30000; // 1 min = 1 * 60 * 1000 ms

    public static final int EXIT_RESULT_CODE = 0;
    public static final int EXIT_REQUEST_CODE = 0;

    protected MartusApplication parentApp;
    private String confirmationDialogTitle;

    private Handler inactivityHandler = new Handler(){
        public void handleMessage(Message msg) {
        }
    };

    private Runnable inactivityCallback = new LogOutProcess(this);

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentApp = (MartusApplication) this.getApplication();
        confirmationDialogTitle = getString(R.string.confirm_default);
    }

    public void resetInactivityTimer(){
        inactivityHandler.removeCallbacks(inactivityCallback);
        if (!MartusApplication.isIgnoreInactivity()) {
            inactivityHandler.postDelayed(inactivityCallback, INACTIVITY_TIMEOUT);
        }
    }

    public void stopInactivityTimer(){
        inactivityHandler.removeCallbacks(inactivityCallback);
    }

    public void showLoginRequiredDialog() {
        LoginRequiredDialog loginRequiredDialog = LoginRequiredDialog.newInstance();
        loginRequiredDialog.show(getSupportFragmentManager(), "dlg_login");
    }


    public void onFinishLoginRequiredDialog() {
        BaseActivity.this.finish();
        Intent intent = new Intent(BaseActivity.this, MartusActivity.class);
        intent.putExtras(getIntent());
        intent.putExtra(MartusActivity.RETURN_TO, MartusActivity.ACTIVITY_BULLETIN);
        intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        startActivity(intent);
    }

    public void showInstallExplorerDialog() {
        InstallExplorerDialog explorerDialog = InstallExplorerDialog.newInstance();
        explorerDialog.show(getSupportFragmentManager(), "dlg_install");
    }

    public void showConfirmationDialog() {
        ConfirmationDialog confirmationDialog = ConfirmationDialog.newInstance();
        confirmationDialog.show(getSupportFragmentManager(), "dlg_confirmation");
    }

    public void onConfirmationAccepted() {
        //do nothing
    }

    public void onConfirmationCancelled() {
        //do nothing
    }

    @Override
    public String getConfirmationTitle() {
        return confirmationDialogTitle;
    }

    @Override
    public void onUserInteraction(){
        resetInactivityTimer();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetInactivityTimer();
    }

    @Override
    public void onStop() {
        super.onStop();
        stopInactivityTimer();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //No call for super(). Bug on API Level > 11.
    }

    public void close() {
        setResult(EXIT_RESULT_CODE);
        finish();
    }
}