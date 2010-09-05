package org.odk.clinic.android.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.odk.clinic.android.R;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.openmrs.Constants;

import android.app.ExpandableListActivity;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

public class ShowPatientActivity extends ExpandableListActivity {
	private static final String FIELD_NAME = "FIELD_NAME";
	private static final String ENCOUNTER_DATE = "ENCOUNTER_DATE";

	private ExpandableListAdapter mAdapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		String name = getIntent().getStringExtra(Constants.KEY_PATIENT_NAME);
		String identifier = getIntent().getStringExtra(Constants.KEY_PATIENT_IDENTIFIER);
		
		setTitle(getString(R.string.app_name) + " > "
				+ name + " - " + identifier /*getString(R.string.show_patient)*/);

		if (!ClinicAdapter.storageReady()) {
			showCustomToast(getString(R.string.error, R.string.storage_error));
			finish();
		}
		

		String patientId = getIntent().getStringExtra(Constants.KEY_PATIENT_ID);
		ClinicAdapter pda = new ClinicAdapter();
		pda.open();
		Cursor c = pda.fetchPatientObservations(patientId);

		if(c != null){

			int valueTextIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_TEXT);
			int valueIntIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_INT);
			int valueDateIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_DATE);
			int valueNumericIndex = c.getColumnIndex(ClinicAdapter.KEY_VALUE_NUMERIC);
			int fieldNameIndex = c.getColumnIndex(ClinicAdapter.KEY_FIELD_NAME);
			int encounterDateIndex = c.getColumnIndex(ClinicAdapter.KEY_ENCOUNTER_DATE);
			int dataTypeIndex = c.getColumnIndex(ClinicAdapter.KEY_DATA_TYPE);

			List<Map<String, String>> groupData = new ArrayList<Map<String, String>>();
			List<List<Map<String, String>>> childData = new ArrayList<List<Map<String, String>>>();
			List<Map<String, String>> children = null;
			
			String prevFieldName = null;

			do{
				String fieldName = c.getString(fieldNameIndex);
				String valueText = c.getString(valueTextIndex);
				String valueDate = c.getString(valueDateIndex);
				Integer valueInt = c.getInt(valueIntIndex);
				Float valueNumeric = c.getFloat(valueNumericIndex);
				String encounterDate = c.getString(encounterDateIndex);
				Integer dataType = c.getInt(dataTypeIndex);

				if(!fieldName.equals(prevFieldName)){
					Map<String, String> curGroupMap = new HashMap<String, String>();
					groupData.add(curGroupMap);
					curGroupMap.put(FIELD_NAME, fieldName);
					
					prevFieldName = fieldName;
					
					children = new ArrayList<Map<String, String>>();
					childData.add(children);
				}

				String value = valueText;
				if(dataType == Constants.TYPE_DATE)
					value = valueDate;
				else if(dataType == Constants.TYPE_INT)
					value = valueInt.toString();
				else if(dataType == Constants.TYPE_FLOAT)
					value = valueNumeric.toString();

				Map<String, String> curChildMap = new HashMap<String, String>();
				children.add(curChildMap);
				curChildMap.put(FIELD_NAME, value);
				curChildMap.put(ENCOUNTER_DATE, encounterDate);

			}while(c.moveToNext());
						

			// Set up our adapter
			mAdapter = new SimpleExpandableListAdapter(
					this,
					groupData,
					android.R.layout.simple_expandable_list_item_1,
					new String[] { FIELD_NAME },
					new int[] { android.R.id.text1, android.R.id.text2 },
					childData,
					android.R.layout.simple_expandable_list_item_2,
					new String[] { FIELD_NAME, ENCOUNTER_DATE },
					new int[] { android.R.id.text1, android.R.id.text2 }
			);

			setListAdapter(mAdapter);
		}

		if(c != null)
			c.close();

		pda.close();
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
