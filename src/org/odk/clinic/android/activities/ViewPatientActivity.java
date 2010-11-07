package org.odk.clinic.android.activities;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.ObservationAdapter;
import org.odk.clinic.android.application.Clinic;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


// TODO if no obs, don't crash when viewing patients

public class ViewPatientActivity extends ListActivity {

	private Patient mPatient;

	private ArrayAdapter<Observation> mObservationAdapter;
	private ArrayList<Observation> mObservations = new ArrayList<Observation>();

	// Menu ID's
	private static final int MENU_FORMS = Menu.FIRST;

	private static final int FORM_CHOOSER = 0;


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.view_patient);

		if (!ClinicAdapter.storageReady()) {
			showCustomToast(getString(R.string.error, R.string.storage_error));
			finish();
		}

		// TODO Check for invalid patient IDs
		String patientIdStr = getIntent().getStringExtra(Constants.KEY_PATIENT_ID);
		Integer patientId = Integer.valueOf(patientIdStr);
		mPatient = getPatient(patientId);
		
		Clinic.getInstance().setSelectedPatient(mPatient);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.view_patient));

		View patientView = (View) findViewById(R.id.patient_info);
		patientView.setBackgroundResource(R.drawable.search_gradient);

		TextView textView = (TextView) findViewById(R.id.identifier_text);
		if (textView != null) {
			textView.setText(mPatient.getIdentifier());
		}

		textView = (TextView) findViewById(R.id.name_text);
		if (textView != null) {
			StringBuilder nameBuilder = new StringBuilder();
			nameBuilder.append(mPatient.getGivenName());
			nameBuilder.append(' ');
			nameBuilder.append(mPatient.getMiddleName());
			nameBuilder.append(' ');
			nameBuilder.append(mPatient.getFamilyName());
			textView.setText(nameBuilder.toString());
		}

		DateFormat df = new SimpleDateFormat("MMM dd, yyyy");
		textView = (TextView) findViewById(R.id.birthdate_text);
		if (textView != null) {
			textView.setText(df.format(mPatient.getBirthdate()));
		}

		ImageView imageView = (ImageView) findViewById(R.id.gender_image);
		if (imageView != null) {
			if (mPatient.getGender().equals("M")) {
				imageView.setImageResource(R.drawable.male);
			} else if (mPatient.getGender().equals("F")) {
				imageView.setImageResource(R.drawable.female);
			}
		}

	}

	private Patient getPatient(Integer patientId) {

		Patient p = null;
		ClinicAdapter ca = new ClinicAdapter();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ca.open();
		Cursor c = ca.fetchPatient(patientId);

		if (c != null && c.getCount() > 0) {
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

			p = new Patient();
			p.setPatientId(c.getInt(patientIdIndex));
			p.setIdentifier(c.getString(identifierIndex));
			p.setGivenName(c.getString(givenNameIndex));
			p.setFamilyName(c.getString(familyNameIndex));
			p.setMiddleName(c.getString(middleNameIndex));
			try {
				p.setBirthDate(df.parse(c.getString(birthDateIndex)));
			} catch (ParseException e) {
				e.printStackTrace();
			}
			p.setGender(c.getString(genderIndex));
		}

		if (c != null) {
			c.close();
		}
		ca.close();

		return p;
	}

	private void getAllObservations(Integer patientId) {

		ClinicAdapter ca = new ClinicAdapter();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

		ca.open();
		Cursor c = ca.fetchPatientObservations(patientId);

		if (c != null && c.getCount() >= 0) {
			mObservations.clear();

			int valueTextIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_TEXT);
			int valueIntIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_INT);
			int valueDateIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_DATE);
			int valueNumericIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_NUMERIC);
			int fieldNameIndex = c.getColumnIndex(ClinicAdapter.KEY_FIELD_NAME);
			int encounterDateIndex = c.getColumnIndex(ClinicAdapter.KEY_ENCOUNTER_DATE);
			int dataTypeIndex = c.getColumnIndex(ClinicAdapter.KEY_DATA_TYPE);

			Observation obs;
			String prevFieldName = null;
			do {
				String fieldName = c.getString(fieldNameIndex);

				// We only want most recent observation, so only get first observation
				if (!fieldName.equals(prevFieldName)) {

					obs = new Observation();
					obs.setFieldName(fieldName);
					try {
						obs.setEncounterDate(df.parse(c
								.getString(encounterDateIndex)));
					} catch (ParseException e) {
						e.printStackTrace();
					}

					int dataType = c.getInt(dataTypeIndex);
					obs.setDataType((byte) dataType);
					switch (dataType) {
					case Constants.TYPE_INT:
						obs.setValueInt(c.getInt(valueIntIndex));
						break;
					case Constants.TYPE_FLOAT:
						obs.setValueNumeric(c.getFloat(valueNumericIndex));
						break;
					case Constants.TYPE_DATE:
						try {
							obs.setValueDate(df.parse(c
									.getString(valueDateIndex)));
						} catch (ParseException e) {
							e.printStackTrace();
						}
						break;
					default:
						obs.setValueText(c.getString(valueTextIndex));
					}

					mObservations.add(obs);

					prevFieldName = fieldName;
				}

			} while(c.moveToNext());
		}

		refreshView();

		if (c != null) {
			c.close();
		}
		ca.close();
	}

	//TODO on long press, graph
	//TODO if you have only one value, don't display next level
	@Override
	protected void onListItemClick(ListView listView, View view, int position,
			long id) {

		if (mPatient != null) {
			// Get selected observation
			Observation obs = (Observation) getListAdapter().getItem(position);

			Intent ip;
			int dataType = obs.getDataType();
			if (dataType == Constants.TYPE_INT
					|| dataType == Constants.TYPE_FLOAT) {
				ip = new Intent(getApplicationContext(),
						ObservationChartActivity.class);
				ip.putExtra(Constants.KEY_PATIENT_ID, mPatient.getPatientId()
						.toString());
				ip.putExtra(Constants.KEY_OBSERVATION_FIELD_NAME, obs
						.getFieldName());
				startActivity(ip);
			} else {
				ip = new Intent(getApplicationContext(),
						ObservationTimelineActivity.class);
				ip.putExtra(Constants.KEY_PATIENT_ID, mPatient.getPatientId()
						.toString());
				ip.putExtra(Constants.KEY_OBSERVATION_FIELD_NAME, obs
						.getFieldName());
				startActivity(ip);
			}
		}
	}

	private void refreshView() {

		mObservationAdapter = new ObservationAdapter(this, R.layout.observation_list_item,
				mObservations);
		setListAdapter(mObservationAdapter);

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		super.onResume();

		if (mPatient != null) {
			// TODO Create more efficient SQL query to get only the latest observation values
			getAllObservations(mPatient.getPatientId());
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);
		menu.add(0, MENU_FORMS, 0, getString(R.string.forms))
		.setIcon(R.drawable.ic_menu_invite);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case MENU_FORMS:
			Intent i = new Intent(getApplicationContext(), FormChooserList.class);
			startActivityForResult(i, FORM_CHOOSER);
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (resultCode == RESULT_CANCELED) {
			return; 
		}

		String formPath = null;
		Intent i = null;
		switch (requestCode) {
		// returns with a form path, start entry
		case FORM_CHOOSER:
			formPath = intent.getStringExtra(FormEntryActivity.KEY_FORMPATH);
			i = new Intent("org.odk.clinic.android.action.FormEntry");
			i.putExtra(FormEntryActivity.KEY_FORMPATH, formPath);
			startActivity(i);
			break;
		}

		super.onActivityResult(requestCode, resultCode, intent);
	}
}
