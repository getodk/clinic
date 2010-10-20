package org.odk.clinic.android.activities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.PatientAdapter;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Patient;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

// TODO Display patient view data
// TODO Display ages instead of dates
// TODO Optimize download patient task

public class ListPatientActivity extends ListActivity {

    // Menu ID's
	private static final int MENU_DOWNLOAD = Menu.FIRST;
	private static final int MENU_PREFERENCES = MENU_DOWNLOAD + 1;
	
	// Request codes
	public static final int DOWNLOAD_PATIENT = 1;
	public static final int BARCODE_CAPTURE = 2;
	
	private static final String DOWNLOAD_PATIENT_CANCELED_KEY = "downloadPatientCanceled";

	private ImageButton mBarcodeButton;
	private EditText mSearchText;
	private TextWatcher mFilterTextWatcher;

	private ArrayAdapter<Patient> mPatientAdapter;
	private ArrayList<Patient> mPatients = new ArrayList<Patient>();
	private boolean mDownloadPatientCanceled = false;

	/*
	 * (non-Javadoc)
	 * 
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		if (savedInstanceState != null) {
		    if (savedInstanceState.containsKey(DOWNLOAD_PATIENT_CANCELED_KEY)) {
		        mDownloadPatientCanceled = savedInstanceState.getBoolean(DOWNLOAD_PATIENT_CANCELED_KEY);
		    }
		}
		
		setContentView(R.layout.find_patient);
		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.find_patient));
		
		if (!ClinicAdapter.storageReady()) {
			showCustomToast(getString(R.string.error, R.string.storage_error));
			finish();
		}
		
		mFilterTextWatcher = new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (mPatientAdapter != null) {
					mPatientAdapter.getFilter().filter(s);
				}
			}

			@Override
			public void afterTextChanged(Editable s) {

			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}
		};

		mSearchText = (EditText) findViewById(R.id.search_text);
		mSearchText.addTextChangedListener(mFilterTextWatcher);

		mBarcodeButton = (ImageButton) findViewById(R.id.barcode_button);
		mBarcodeButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				Intent i = new Intent("com.google.zxing.client.android.SCAN");
				try {
					startActivityForResult(i, BARCODE_CAPTURE);
				} catch (ActivityNotFoundException e) {
					Toast t = Toast.makeText(getApplicationContext(),
							getString(R.string.error, R.string.barcode_error),
							Toast.LENGTH_LONG);
					t.setGravity(Gravity.CENTER_VERTICAL, 0, 0);
					t.show();
				}
			}
		});

	}

	@Override
	protected void onListItemClick(ListView listView, View view, int position,
			long id) {
		// Get selected patient
		Patient p = (Patient) getListAdapter().getItem(position);
		String patientIdStr = p.getPatientId().toString();

		Intent ip = new Intent(getApplicationContext(),
				ViewPatientActivity.class);
		ip.putExtra(Constants.KEY_PATIENT_ID, patientIdStr);
		startActivity(ip);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_DOWNLOAD, 0, getString(R.string.download_patients))
				.setIcon(R.drawable.ic_menu_invite);
		menu.add(0, MENU_PREFERENCES, 0, getString(R.string.server_preferences))
				.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_DOWNLOAD:
			Intent id = new Intent(getApplicationContext(),
					DownloadPatientActivity.class);
			startActivityForResult(id, DOWNLOAD_PATIENT);
			return true;
		case MENU_PREFERENCES:
			Intent ip = new Intent(getApplicationContext(),
					PreferencesActivity.class);
			startActivity(ip);

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
	    
		if (resultCode == RESULT_CANCELED) {
		    if (requestCode == DOWNLOAD_PATIENT) {
		        mDownloadPatientCanceled = true;
		    }
			return;
		}

		if (requestCode == BARCODE_CAPTURE && intent != null) {
			String sb = intent.getStringExtra("SCAN_RESULT");
			if (sb != null && sb.length() > 0) {
				mSearchText.setText(sb);
			}
		}
		super.onActivityResult(requestCode, resultCode, intent);

	}

	private void getPatients() {
		getPatients(null);
	}

	private void getPatients(String searchStr) {

		ClinicAdapter ca = new ClinicAdapter();
		DateFormat mdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ca.open();
		Cursor c = null;
		if (searchStr != null) {
			c = ca.fetchPatients(searchStr, searchStr);
		} else {
			c = ca.fetchAllPatients();
		}

		if (c != null && c.getCount() >= 0) {

			mPatients.clear();

			int patientIdIndex = c
					.getColumnIndex(ClinicAdapter.KEY_PATIENT_ID);
			int identifierIndex = c
					.getColumnIndex(ClinicAdapter.KEY_IDENTIFIER);
			int givenNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_GIVEN_NAME);
			int familyNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_FAMILY_NAME);
			int middleNameIndex = c
					.getColumnIndex(ClinicAdapter.KEY_MIDDLE_NAME);
			int birthDateIndex = c
					.getColumnIndex(ClinicAdapter.KEY_BIRTH_DATE);
			int genderIndex = c.getColumnIndex(ClinicAdapter.KEY_GENDER);

			if (c.getCount() > 0) {
				
				Patient p;
				do {
					p = new Patient();
					p.setPatientId(c.getInt(patientIdIndex));
					p.setIdentifier(c.getString(identifierIndex));
					p.setGivenName(c.getString(givenNameIndex));
					p.setFamilyName(c.getString(familyNameIndex));
					p.setMiddleName(c.getString(middleNameIndex));
					try {
						p.setBirthDate(mdf.parse(c.getString(birthDateIndex)));
					} catch (ParseException e) {
						e.printStackTrace();
					}
					p.setGender(c.getString(genderIndex));
					mPatients.add(p);
				} while (c.moveToNext());
			}
		
		}

		refreshView();

		if (c != null) {
			c.close();
		}
		ca.close();

	}

	private void refreshView() {

		mPatientAdapter = new PatientAdapter(this, R.layout.patient_list_item,
				mPatients);
		setListAdapter(mPatientAdapter);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mSearchText.removeTextChangedListener(mFilterTextWatcher);

	}

	@Override
	protected void onResume() {
		super.onResume();
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
	    boolean firstRun = settings.getBoolean(PreferencesActivity.KEY_FIRST_RUN, true);
	    
		if (firstRun) {
		    // Save first run status
		    SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(PreferencesActivity.KEY_FIRST_RUN, false);
            editor.commit();
            
            // Start preferences activity
		    Intent ip = new Intent(getApplicationContext(),
                    PreferencesActivity.class);
            startActivity(ip);
            
		} else {
            getPatients();

            if (mPatients.isEmpty() && !mDownloadPatientCanceled) {
                // Download patients if no patients are in db
                Intent id = new Intent(getApplicationContext(),
                        DownloadPatientActivity.class);
                startActivityForResult(id, DOWNLOAD_PATIENT);
            } else {
                // hack to trigger refilter
                mSearchText.setText(mSearchText.getText().toString());
            }
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putBoolean(DOWNLOAD_PATIENT_CANCELED_KEY, mDownloadPatientCanceled);
	}

	private void showCustomToast(String message) {
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View view = inflater.inflate(R.layout.toast_view, null);

		// set the text in the view
		TextView tv = (TextView) view.findViewById(R.id.message);
		tv.setText(message);

		Toast t = new Toast(this);
		t.setView(view);
		t.setDuration(Toast.LENGTH_LONG);
		t.setGravity(Gravity.CENTER, 0, 0);
		t.show();
	}
}
