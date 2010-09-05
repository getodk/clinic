package org.odk.clinic.android.activities;

import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.PatientDbAdapter;
import org.odk.clinic.android.listeners.DownloadListener;
import org.odk.clinic.android.openmrs.Cohort;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.preferences.ServerPreferences;
import org.odk.clinic.android.tasks.DownloadCohortTask;
import org.odk.clinic.android.tasks.DownloadPatientTask;
import org.odk.clinic.android.tasks.DownloadTask;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.widget.Toast;

// TODO Merge this activity into FindPatientActivity

public class DownloadPatientActivity extends Activity implements
		DownloadListener {

	private final static int COHORT_DIALOG = 1;
	private final static int COHORTS_PROGRESS_DIALOG = 2;
	private final static int PATIENTS_PROGRESS_DIALOG = 3;
	
	private AlertDialog mCohortDialog;
	private ProgressDialog mProgressDialog;

	private DownloadTask mDownloadTask;
	
	private ArrayList<Cohort> mCohorts = new ArrayList<Cohort>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.download_patients));

		if (!PatientDbAdapter.storageReady()) {
			Toast t = Toast.makeText(getApplicationContext(),
					getString(R.string.error, R.string.storage_error),
					Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			t.show();
			finish();
		}
		
		// get the task if we've changed orientations. If it's null, open up the
		// cohort selection dialog
		mDownloadTask = (DownloadTask) getLastNonConfigurationInstance();
		if (mDownloadTask == null) {
			getAllCohorts();
			showDialog(COHORT_DIALOG);
		}
	}
	
	private void downloadCohorts()
	{
		if (mDownloadTask != null)
			return;
		
		// setup dialog and upload task
		showDialog(COHORTS_PROGRESS_DIALOG);

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

		mDownloadTask = new DownloadCohortTask();
		mDownloadTask
				.setServerConnectionListener(DownloadPatientActivity.this);
		mDownloadTask.execute(url, username, password);
	}
	
	private void downloadPatients()
	{
		if (mDownloadTask != null)
			return;
		
		// setup dialog and upload task
		showDialog(PATIENTS_PROGRESS_DIALOG);

		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());

		String url = settings.getString(ServerPreferences.KEY_SERVER,
				getString(R.string.default_server))
				+ Constants.USER_DOWNLOAD_URL;
		String username = settings.getString(ServerPreferences.KEY_USERNAME,
				getString(R.string.default_username));
		String password = settings.getString(ServerPreferences.KEY_PASSWORD,
				getString(R.string.default_password));
		int cohortId = settings.getInt(ServerPreferences.KEY_COHORT, -1);

		mDownloadTask = new DownloadPatientTask();
		mDownloadTask.setServerConnectionListener(DownloadPatientActivity.this);
		mDownloadTask.execute(url, username, password, Integer
				.toString(cohortId));
	}
	
	private void getAllCohorts() {

		PatientDbAdapter pda = new PatientDbAdapter();

		pda.open();
		Cursor c = pda.fetchAllCohorts();

		if (c != null && c.getCount() >= 0) {

			mCohorts.clear();

			int cohortIdIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_COHORT_ID);
			int nameIndex = c
					.getColumnIndex(PatientDbAdapter.KEY_COHORT_NAME);

			Cohort cohort;
			if (c.getCount() > 0) {
				do {
					cohort = new Cohort();
					cohort.setCohortId(c.getInt(cohortIdIndex));
					cohort.setName(c.getString(nameIndex));
					mCohorts.add(cohort);
				} while (c.moveToNext());
			}
		}

		if (c != null)
			c.close();

		pda.close();

	}

	private AlertDialog createCohortDialog() {
		
		DialogInterface.OnClickListener refreshButtonListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// Remove dialog to get a fresh instance next time we call showDialog()
				removeDialog(COHORT_DIALOG);
				mCohortDialog = null;
				downloadCohorts();
			}
		};
		
		DialogInterface.OnClickListener okayButtonListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				removeDialog(COHORT_DIALOG);
				mCohortDialog = null;
				downloadPatients();
			}
		};
		
		DialogInterface.OnClickListener itemClickListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				Cohort c = mCohorts.get(which);
				
				SharedPreferences settings = PreferenceManager
						.getDefaultSharedPreferences(getBaseContext());
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt(ServerPreferences.KEY_COHORT, c.getCohortId().intValue());
				editor.commit();
			}
		};
		
		DialogInterface.OnCancelListener cancelListener = new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				finish();
			}
		};
		
		SharedPreferences settings = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		int cohortId = settings.getInt(ServerPreferences.KEY_COHORT, -1);

		// TODO Move strings into strings.xml
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a cohort to download");

		if (!mCohorts.isEmpty()) {
			
			int selectedCohortIndex = -1;
			String[] cohortNames = new String[mCohorts.size()];
			for (int i = 0; i < mCohorts.size(); i++) {
				Cohort c = mCohorts.get(i);
				cohortNames[i] = c.getName();
				if (cohortId == c.getCohortId()) {
					selectedCohortIndex = i;
				}
			}
			builder.setSingleChoiceItems(cohortNames, selectedCohortIndex,
					itemClickListener);
			builder.setPositiveButton("OK", okayButtonListener);
		} else {
			builder.setMessage("No cohorts available.");
		}
		builder.setNeutralButton("Refresh", refreshButtonListener);
		builder.setOnCancelListener(cancelListener);

		return builder.create();
	}
	
	private ProgressDialog createProgressDialog() {
		
		ProgressDialog dialog = new ProgressDialog(this);
		DialogInterface.OnClickListener loadingButtonListener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				mDownloadTask.setServerConnectionListener(null);
				finish();
			}
		};
		dialog.setTitle(getString(R.string.connecting_server));
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setIndeterminate(false);
		dialog.setCancelable(false);
		dialog.setMax(100);
		dialog.setButton(getString(R.string.cancel),
				loadingButtonListener);
		
		return dialog;
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		
		if (id == COHORT_DIALOG) {
			mCohortDialog = createCohortDialog();
			return mCohortDialog;
		} else if (id == COHORTS_PROGRESS_DIALOG || id == PATIENTS_PROGRESS_DIALOG) {
			mProgressDialog = createProgressDialog();
			return mProgressDialog;
		}

		return null;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		
		if (id == COHORTS_PROGRESS_DIALOG || id == PATIENTS_PROGRESS_DIALOG) {
			ProgressDialog progress = (ProgressDialog) dialog;
			progress.setTitle(getString(R.string.connecting_server));
			progress.setMax(100);
			progress.setProgress(0);
		}
	}

	public void downloadComplete(String result) {

		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		
		if (result != null) {
			Toast t = Toast.makeText(getApplicationContext(),
					getString(R.string.error, result), Toast.LENGTH_LONG);
			t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
			t.show();
			
			showDialog(COHORT_DIALOG);
			
		} else if (mDownloadTask instanceof DownloadCohortTask) {
			getAllCohorts();
			showDialog(COHORT_DIALOG);
			
		} else {
			finish();
		}
		
		mDownloadTask = null;
	}

	@Override
	public void progressUpdate(String message, int progress, int max) {
		mProgressDialog.setMax(max);
		mProgressDialog.setProgress(progress);
		mProgressDialog.setTitle(getString(R.string.downloading, message));
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		return mDownloadTask;
	}

	@Override
	protected void onDestroy() {
		if (mDownloadTask != null) {
			mDownloadTask.setServerConnectionListener(null);
		}
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		if (mDownloadTask != null) {
			mDownloadTask.setServerConnectionListener(this);
		}
		super.onResume();
	}

	@Override
	protected void onPause() {
		if (mCohortDialog != null && mCohortDialog.isShowing()) {
			mCohortDialog.dismiss();
		}
		if (mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
		super.onPause();
	}

}
