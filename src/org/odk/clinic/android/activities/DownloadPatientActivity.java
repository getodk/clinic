package org.odk.clinic.android.activities;

import java.util.HashMap;
import java.util.List;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.PatientDbAdapter;
import org.odk.clinic.android.listeners.DownloadPatientListener;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.preferences.ServerPreferences;
import org.odk.clinic.android.tasks.DownloadPatientTask;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

// todo move inserts into task

public class DownloadPatientActivity extends Activity implements
		DownloadPatientListener {

	private final static int PROGRESS_DIALOG = 1;
	private ProgressDialog mProgressDialog;

	private DownloadPatientTask mDownloadPatientTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.download_patients));

		// get the task if we've changed orientations. If it's null it's a new
		// upload.
		mDownloadPatientTask = (DownloadPatientTask) getLastNonConfigurationInstance();
		if (mDownloadPatientTask == null) {
			// setup dialog and upload task
			showDialog(PROGRESS_DIALOG);
			mDownloadPatientTask = new DownloadPatientTask();

			SharedPreferences settings = PreferenceManager
					.getDefaultSharedPreferences(getBaseContext());

			String url = settings.getString(ServerPreferences.KEY_SERVER,
					getString(R.string.default_server))
					+ Constants.USER_DOWNLOAD_URL;
			String username = settings.getString(
					ServerPreferences.KEY_USERNAME,
					getString(R.string.default_username));
			String password = settings.getString(
					ServerPreferences.KEY_PASSWORD,
					getString(R.string.default_password));
			String cohort = settings.getString(ServerPreferences.KEY_COHORT,
					getString(R.string.default_cohort));

			mDownloadPatientTask = new DownloadPatientTask();
			mDownloadPatientTask
					.setServerConnectionListener(DownloadPatientActivity.this);
			mDownloadPatientTask.execute(url, username, password, cohort);
		}
	}

	// }

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case PROGRESS_DIALOG:
			mProgressDialog = new ProgressDialog(this);
			DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					dialog.dismiss();
					mDownloadPatientTask.setServerConnectionListener(null);
					finish();
				}
			};
			mProgressDialog.setTitle(getString(R.string.connecting_server));
			mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			mProgressDialog.setIndeterminate(false);
			mProgressDialog.setCancelable(false);
			mProgressDialog.setMax(0);
			mProgressDialog.setProgress(0);
			mProgressDialog.setButton(getString(R.string.cancel),
					loadingButtonListener);
			return mProgressDialog;

		}
		return null;
	}

	public void downloadComplete(String result) {

		dismissDialog(PROGRESS_DIALOG);
		if (result != null) {
			Toast t = Toast.makeText(getApplicationContext(),
					getString(R.string.error, result), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			t.show();
		}
		
		finish();
	}

	@Override
	public void progressUpdate(String message, int progress, int max) {
		mProgressDialog.setMax(max);
		mProgressDialog.setProgress(progress);
		mProgressDialog.setTitle(getString(R.string.downloading, message));
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mDownloadPatientTask;
	}

	@Override
	protected void onDestroy() {
		if (mDownloadPatientTask != null) {
			mDownloadPatientTask.setServerConnectionListener(null);
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		if (mDownloadPatientTask != null) {
			mDownloadPatientTask.setServerConnectionListener(this);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		super.onPause();
	}

}
