package org.odk.clinic.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.PatientDbAdapter;
import org.odk.clinic.android.listeners.DownloadPatientListener;
import org.odk.clinic.android.openmrs.Obs;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.openmrs.ServerConstants;
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
					+ ServerConstants.USER_DOWNLOAD_URL;
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
			mProgressDialog.setButton(getString(R.string.cancel),
					loadingButtonListener);
			return mProgressDialog;

		}
		return null;
	}

	public void downloadComplete(HashMap<String, Object> result) {

		dismissDialog(PROGRESS_DIALOG);

		if (result.containsKey(DownloadPatientTask.KEY_ERROR)) {
			String error = (String) result.get(DownloadPatientTask.KEY_ERROR);
			Toast t = Toast.makeText(getApplicationContext(),
					getString(R.string.error, error), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			t.show();
		} else {
			@SuppressWarnings("unchecked")
			ArrayList<Patient> foundPatients = (ArrayList<Patient>) result
			.get(DownloadPatientTask.KEY_PATIENTS);

			if (foundPatients != null) {

				// Update patient database
				PatientDbAdapter pda = new PatientDbAdapter(this);
				pda.open();
				// Clear all previous patient entries
				pda.deleteAllPatients();
				for (Patient p : foundPatients) {
					pda.createPatient(p);
				}

				List<Obs> obslist = (List<Obs>) result.get(DownloadPatientTask.KEY_MEDICAL_HISTOTY);
				if(obslist != null){
					for(Obs obs : obslist){
						pda.createObs(obs);
					}
				}

				pda.close();
			}

		}

		finish();
	}

	@Override
	public void progressUpdate(String message, int progress, int max) {
		mProgressDialog.setMax(max);
		mProgressDialog.setProgress(progress);
		mProgressDialog.setTitle(getString(R.string.downloading_patients,message));
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
