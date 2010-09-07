package org.odk.clinic.android.database;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.odk.clinic.android.openmrs.Cohort;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;

public class ClinicAdapter {
	private final static String t = "PatientDbAdapter";

	// patient columns
	public static final String KEY_ID = "_id";
	public static final String KEY_PATIENT_ID = "patient_id";
	public static final String KEY_IDENTIFIER = "identifier";
	public static final String KEY_GIVEN_NAME = "given_name";
	public static final String KEY_FAMILY_NAME = "family_name";
	public static final String KEY_MIDDLE_NAME = "middle_name";
	public static final String KEY_BIRTH_DATE = "birth_date";
	public static final String KEY_GENDER = "gender";

	// observation columns
	public static final String KEY_VALUE_TEXT = "value_text";
	public static final String KEY_VALUE_NUMERIC = "value_numeric";
	public static final String KEY_VALUE_DATE = "value_date";
	public static final String KEY_VALUE_INT = "value_int";
	public static final String KEY_FIELD_NAME = "field_name";
	public static final String KEY_ENCOUNTER_DATE = "encounter_date";
	public static final String KEY_DATA_TYPE = "data_type";
	
	// cohort columns
	public static final String KEY_COHORT_ID = "cohort_id";
	public static final String KEY_COHORT_NAME = "name";

	private DateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private String mZeroDate = "0000-00-00 00:00:00";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_NAME = "clinic.sqlite3";
	private static final String PATIENTS_TABLE = "patients";
	private static final String OBSERVATIONS_TABLE = "observations";
	private static final String COHORTS_TABLE = "cohorts";
	private static final int DATABASE_VERSION = 6;
	private static final String DATABASE_PATH = Environment
			.getExternalStorageDirectory() + "/clinic";

	private static final String CREATE_PATIENTS_TABLE = "create table "
			+ PATIENTS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_PATIENT_ID + " integer not null, " + KEY_IDENTIFIER
			+ " text, " + KEY_GIVEN_NAME + " text, " + KEY_FAMILY_NAME
			+ " text, " + KEY_MIDDLE_NAME + " text, " + KEY_BIRTH_DATE
			+ " datetime, " + KEY_GENDER + " text);";

	private static final String CREATE_OBSERVATIONS_TABLE = "create table "
			+ OBSERVATIONS_TABLE + " (_id integer primary key autoincrement, "
			+ KEY_PATIENT_ID + " integer not null, " 
			+ KEY_DATA_TYPE + " integer not null, " + KEY_VALUE_TEXT
			+ " text, " + KEY_VALUE_NUMERIC + " double, " + KEY_VALUE_DATE
			+ " datetime, " + KEY_VALUE_INT + " integer, " + KEY_FIELD_NAME
			+ " text not null, " + KEY_ENCOUNTER_DATE + " datetime not null);";
	
	private static final String CREATE_COHORTS_TABLE = "create table "
		+ COHORTS_TABLE + " (_id integer primary key autoincrement, "
		+ KEY_COHORT_ID + " integer not null, " + KEY_COHORT_NAME + " text);";

	private static class DatabaseHelper extends ODKSQLiteOpenHelper {

		DatabaseHelper() {
			super(DATABASE_PATH, DATABASE_NAME, null, DATABASE_VERSION);
			createStorage();
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(CREATE_PATIENTS_TABLE);
			db.execSQL(CREATE_OBSERVATIONS_TABLE);
			db.execSQL(CREATE_COHORTS_TABLE);
		}

		@Override
		// upgrading will destroy all old data
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS " + PATIENTS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + OBSERVATIONS_TABLE);
			db.execSQL("DROP TABLE IF EXISTS " + COHORTS_TABLE);
			onCreate(db);
		}
	}

	public ClinicAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper();
		mDb = mDbHelper.getWritableDatabase();
		return this;
	}

	public void close() {
		mDbHelper.close();
	}

	/**
	 * Insert patient into the database.
	 * 
	 * @param patient
	 *            Patient object containing patient info
	 * @return database id of the new patient
	 */
	public long createPatient(Patient patient) {
		ContentValues cv = new ContentValues();

		cv.put(KEY_PATIENT_ID, patient.getPatientId());
		cv.put(KEY_IDENTIFIER, patient.getIdentifier());

		cv.put(KEY_GIVEN_NAME, patient.getGivenName());
		cv.put(KEY_FAMILY_NAME, patient.getFamilyName());
		cv.put(KEY_MIDDLE_NAME, patient.getMiddleName());

		cv.put(KEY_BIRTH_DATE,
				patient.getBirthdate() != null ? mDateFormat.format(patient
						.getBirthdate()) : mZeroDate);
		cv.put(KEY_GENDER, patient.getGender());

		long id = -1;
		try {
			id = mDb.insert(PATIENTS_TABLE, null, cv);
		} catch (SQLiteConstraintException e) {
			Log.e(t, "Caught SQLiteConstraitException: " + e);
		}

		return id;
	}

	public long createObservation(Observation obs) {
		ContentValues cv = new ContentValues();

		cv.put(KEY_PATIENT_ID, obs.getPatientId());
		cv.put(KEY_VALUE_TEXT, obs.getValueText());

		cv.put(KEY_VALUE_NUMERIC, obs.getValueNumeric());
		cv.put(KEY_VALUE_DATE,
				obs.getValueDate() != null ? mDateFormat.format(obs
						.getValueDate()) : mZeroDate);
		cv.put(KEY_VALUE_INT, obs.getValueInt());

		cv.put(KEY_FIELD_NAME, obs.getFieldName());
		
		cv.put(KEY_DATA_TYPE, obs.getDataType());
		
		cv.put(KEY_ENCOUNTER_DATE,
				obs.getEncounterDate() != null ? mDateFormat.format(obs
						.getEncounterDate()) : mZeroDate);

		long id = -1;
		try {
			id = mDb.insert(OBSERVATIONS_TABLE, null, cv);
		} catch (SQLiteConstraintException e) {
			Log.e(t, "Caught SQLiteConstraitException: " + e);
		}

		return id;
	}
	
	public long createCohort(Cohort cohort) {
		ContentValues cv = new ContentValues();

		cv.put(KEY_COHORT_ID, cohort.getCohortId());
		cv.put(KEY_COHORT_NAME, cohort.getName());

		long id = -1;
		try {
			id = mDb.insert(COHORTS_TABLE, null, cv);
		} catch (SQLiteConstraintException e) {
			Log.e(t, "Caught SQLiteConstraitException: " + e);
		}

		return id;
	}

	/**
	 * Remove all patients from the database.
	 * 
	 * @return number of affected rows
	 */
	public boolean deleteAllPatients() {
		return mDb.delete(PATIENTS_TABLE, null, null) > 0;
	}

	public boolean deleteAllObservations() {
		return mDb.delete(OBSERVATIONS_TABLE, null, null) > 0;
	}
	
	public boolean deleteAllCohorts() {
		return mDb.delete(COHORTS_TABLE, null, null) > 0;
	}
	
	/**
	 * Get a cursor to multiple patients from the database.
	 * 
	 * @param name
	 *            name matching a patient
	 * @param identifier
	 *            identifier matching a patient
	 * @return cursor to the file
	 * @throws SQLException
	 */
	public Cursor fetchPatients(String name, String identifier)
			throws SQLException {
		Cursor c = null;
		if (name != null) {
			// search using name

			// remove all wildcard characters
			name = name.replaceAll("\\*", "");
			name = name.replaceAll("%", "");
			name = name.replaceAll("_", "");

			name = name.replaceAll("  ", " ");
			name = name.replace(", ", " ");
			String[] names = name.split(" ");

			StringBuilder expr = new StringBuilder();

			for (int i = 0; i < names.length; i++) {
				String n = names[i];
				if (n != null && n.length() > 0) {
					expr.append(KEY_GIVEN_NAME + " LIKE '" + n + "%'");
					expr.append(" OR ");
					expr.append(KEY_FAMILY_NAME + " LIKE '" + n + "%'");
					expr.append(" OR ");
					expr.append(KEY_MIDDLE_NAME + " LIKE '" + n + "%'");
					if (i < names.length - 1)
						expr.append(" OR ");
				}
			}

			c = mDb.query(true, PATIENTS_TABLE, new String[] { KEY_ID,
					KEY_PATIENT_ID, KEY_IDENTIFIER, KEY_GIVEN_NAME,
					KEY_FAMILY_NAME, KEY_MIDDLE_NAME, KEY_BIRTH_DATE,
					KEY_GENDER }, expr.toString(), null, null, null, null, null);
		} else if (identifier != null) {
			// search using identifier

			// escape all wildcard characters
			identifier = identifier.replaceAll("\\*", "^*");
			identifier = identifier.replaceAll("%", "^%");
			identifier = identifier.replaceAll("_", "^_");

			c = mDb.query(true, PATIENTS_TABLE, new String[] { KEY_ID,
					KEY_PATIENT_ID, KEY_IDENTIFIER, KEY_GIVEN_NAME,
					KEY_FAMILY_NAME, KEY_MIDDLE_NAME, KEY_BIRTH_DATE,
					KEY_GENDER }, KEY_IDENTIFIER + " LIKE '" + identifier
					+ "%' ESCAPE '^'", null, null, null, null, null);
		}

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchPatient(Integer patientId) throws SQLException {
		Cursor c = null;
		c = mDb.query(true, PATIENTS_TABLE, new String[] { KEY_ID,
				KEY_PATIENT_ID, KEY_IDENTIFIER, KEY_GIVEN_NAME,
				KEY_FAMILY_NAME, KEY_MIDDLE_NAME, KEY_BIRTH_DATE, KEY_GENDER },
				KEY_PATIENT_ID + "=" + patientId, null, null, null, null, null);

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}
	
	public Cursor fetchAllPatients() throws SQLException {
		Cursor c = null;
		c = mDb.query(true, PATIENTS_TABLE, new String[] { KEY_ID,
				KEY_PATIENT_ID, KEY_IDENTIFIER, KEY_GIVEN_NAME,
				KEY_FAMILY_NAME, KEY_MIDDLE_NAME, KEY_BIRTH_DATE, KEY_GENDER },
				null, null, null, null, null, null);

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}
	
	public Cursor fetchAllCohorts() throws SQLException {
		Cursor c = null;
		c = mDb.query(true, COHORTS_TABLE, new String[] { KEY_ID,
				KEY_COHORT_ID, KEY_COHORT_NAME },
				null, null, null, null, null, null);

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	/**
	 * Update patient in the database.
	 * 
	 * @param patient
	 *            Patient object containing patient info
	 * @return number of affected rows
	 */
	public boolean updatePatient(Patient patient) {
		ContentValues cv = new ContentValues();

		cv.put(KEY_PATIENT_ID, patient.getPatientId());
		cv.put(KEY_IDENTIFIER, patient.getIdentifier());

		cv.put(KEY_GIVEN_NAME, patient.getGivenName());
		cv.put(KEY_FAMILY_NAME, patient.getFamilyName());
		cv.put(KEY_MIDDLE_NAME, patient.getMiddleName());

		cv.put(KEY_BIRTH_DATE,
				patient.getBirthdate() != null ? mDateFormat.format(patient
						.getBirthdate()) : mZeroDate);
		cv.put(KEY_GENDER, patient.getGender());

		return mDb.update(PATIENTS_TABLE, cv, KEY_PATIENT_ID + "='"
				+ patient.getPatientId() + "'", null) > 0;
	}

	public static boolean createStorage() {
		if (storageReady()) {
			File f = new File(DATABASE_PATH);
			if (f.exists()) {
				return true;
			} else {
				return f.mkdirs();
			}
		} else {
			return false;
		}

	}

	public static boolean storageReady() {
		String cardstatus = Environment.getExternalStorageState();
		if (cardstatus.equals(Environment.MEDIA_REMOVED)
				|| cardstatus.equals(Environment.MEDIA_UNMOUNTABLE)
				|| cardstatus.equals(Environment.MEDIA_UNMOUNTED)
				|| cardstatus.equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
			return false;
		} else {
			return true;
		}
	}

	public Cursor fetchPatientObservations(Integer patientId) throws SQLException {
		Cursor c = null;
		// TODO removing an extra KEY_VALUE_TEXT doesn't screw things up?
		c = mDb.query(OBSERVATIONS_TABLE, new String[] { KEY_VALUE_TEXT,
				KEY_DATA_TYPE, KEY_VALUE_NUMERIC, KEY_VALUE_DATE,
				KEY_VALUE_INT, KEY_FIELD_NAME, KEY_ENCOUNTER_DATE },
				KEY_PATIENT_ID + "=" + patientId, null, null, null,
				KEY_FIELD_NAME + "," + KEY_ENCOUNTER_DATE + " desc");

		if (c != null)
			c.moveToFirst();

		return c;
	}
	
	public Cursor fetchPatientObservation(Integer patientId, String fieldName)
			throws SQLException {
		Cursor c = null;

		c = mDb.query(OBSERVATIONS_TABLE, new String[] { KEY_VALUE_TEXT,
				KEY_DATA_TYPE, KEY_VALUE_NUMERIC, KEY_VALUE_DATE,
				KEY_VALUE_INT, KEY_ENCOUNTER_DATE },
				KEY_PATIENT_ID + "=" + patientId + " AND " + KEY_FIELD_NAME
						+ "='" + fieldName + "'", null, null, null,
				KEY_ENCOUNTER_DATE + " desc");

		if (c != null)
			c.moveToFirst();

		return c;
	}
}
