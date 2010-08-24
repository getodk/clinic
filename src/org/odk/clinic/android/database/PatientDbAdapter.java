package org.odk.clinic.android.database;

import org.odk.clinic.android.openmrs.Patient;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class PatientDbAdapter {
	private final static String t = "PatientDbAdapter";

	// database columns
	public static final String KEY_ID = "_id";
	public static final String KEY_PATIENT_ID = "patientid";
	public static final String KEY_IDENTIFIER = "identifier";
	public static final String KEY_GIVEN_NAME = "givenname";
	public static final String KEY_FAMILY_NAME = "familyname";
	public static final String KEY_MIDDLE_NAME = "middlename";
	public static final String KEY_BIRTHDATE = "birthdate";
	public static final String KEY_GENDER = "gender";

	private DatabaseHelper mDbHelper;
	private SQLiteDatabase mDb;

	private static final String DATABASE_CREATE = "create table patients (_id integer primary key autoincrement, "
			+ "patientid integer not null, "
			+ "identifier text, "
			+ "givenname text, "
			+ "familyname text, "
			+ "middlename text, "
			+ "birthdate text, " + "gender text);";

	private static final String DATABASE_NAME = "patient";
	private static final String DATABASE_TABLE = "patients";
	private static final int DATABASE_VERSION = 1;

	private final Context mCtx;

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL(DATABASE_CREATE);
		}

		@Override
		// upgrading will destroy all old data
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			db.execSQL("DROP TABLE IF EXISTS patients");
			onCreate(db);
		}
	}

	public PatientDbAdapter(Context ctx) {
		this.mCtx = ctx;
	}

	public PatientDbAdapter open() throws SQLException {
		mDbHelper = new DatabaseHelper(mCtx);
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

		cv.put(KEY_BIRTHDATE, patient.getBirthdate());
		cv.put(KEY_GENDER, patient.getGender());

		long id = -1;
		try {
			id = mDb.insert(DATABASE_TABLE, null, cv);
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
		return mDb.delete(DATABASE_TABLE, null, null) > 0;
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

			c = mDb.query(true, DATABASE_TABLE,
					new String[] { KEY_ID, KEY_PATIENT_ID, KEY_IDENTIFIER,
							KEY_GIVEN_NAME, KEY_FAMILY_NAME, KEY_MIDDLE_NAME,
							KEY_BIRTHDATE, KEY_GENDER }, expr.toString(), null,
					null, null, null, null);
		} else if (identifier != null) {
			// search using identifier

			// escape all wildcard characters
			identifier = identifier.replaceAll("*", "^*");
			identifier = identifier.replaceAll("%", "^%");
			identifier = identifier.replaceAll("_", "^_");

			c = mDb.query(true, DATABASE_TABLE,
					new String[] { KEY_ID, KEY_PATIENT_ID, KEY_IDENTIFIER,
							KEY_GIVEN_NAME, KEY_FAMILY_NAME, KEY_MIDDLE_NAME,
							KEY_BIRTHDATE, KEY_GENDER }, KEY_IDENTIFIER
							+ " LIKE '" + identifier + "%' ESCAPE '^'", null,
					null, null, null, null);
		}

		if (c != null) {
			c.moveToFirst();
		}
		return c;
	}

	public Cursor fetchAllPatients() throws SQLException {
		Cursor c = null;
		c = mDb.query(true, DATABASE_TABLE, new String[] { KEY_ID,
				KEY_PATIENT_ID, KEY_IDENTIFIER, KEY_GIVEN_NAME,
				KEY_FAMILY_NAME, KEY_MIDDLE_NAME, KEY_BIRTHDATE, KEY_GENDER },
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

		cv.put(KEY_BIRTHDATE, patient.getBirthdate());
		cv.put(KEY_GENDER, patient.getGender());

		return mDb.update(DATABASE_TABLE, cv, KEY_PATIENT_ID + "='"
				+ patient.getPatientId().toString() + "'", null) > 0;
	}

}
