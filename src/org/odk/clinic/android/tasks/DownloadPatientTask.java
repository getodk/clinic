package org.odk.clinic.android.tasks;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.odk.clinic.android.listeners.DownloadPatientListener;
import org.odk.clinic.android.openmrs.Obs;
import org.odk.clinic.android.openmrs.Patient;
import org.odk.clinic.android.openmrs.ServerConstants;

import android.os.AsyncTask;

import com.jcraft.jzlib.ZInputStream;

public class DownloadPatientTask extends
AsyncTask<String, String, HashMap<String, Object>> {

	DownloadPatientListener mStateListener;
	public static final String KEY_ERROR = "error";
	public static final String KEY_PATIENTS = "patients";
	public static final String KEY_MEDICAL_HISTOTY = "medical_history";

	int mAction = ServerConstants.ACTION_DOWNLOAD_PATIENTS;
	String mSerializer = ServerConstants.DEFAULT_PATIENT_SERIALIZER;
	String mLocale = Locale.getDefault().getLanguage();
	SimpleDateFormat mDateFormat = new SimpleDateFormat("MMM dd, yyyy");
	ArrayList<Patient> mPatients = null;

	@Override
	protected HashMap<String, Object> doInBackground(String... values) {

		String url = values[0];
		String username = values[1];
		String password = values[2];
		int cohort = Integer.valueOf(values[3]).intValue();

		HashMap<String, Object> result = new HashMap<String, Object>();

		ArrayList<Patient> patients = null;
		try {
			DataInputStream zdis = connectToServer(url, username, password,
					cohort);
			if (zdis != null) {
				patients = downloadPatients(zdis);
				result.put(KEY_PATIENTS, patients);
				result.put(KEY_MEDICAL_HISTOTY, downloadMedicalHistory(zdis));
				
				zdis.close();
			}
		} catch (Exception e) {
			result.put(KEY_ERROR, e.getLocalizedMessage());
			e.printStackTrace();
		}

		return result;
	}

	@Override
	protected void onProgressUpdate(String... values) {
		synchronized (this) {
			if (mStateListener != null) {
				// update progress and total
				mStateListener.progressUpdate(values[0], new Integer(values[1])
				.intValue(), new Integer(values[2]).intValue());
			}
		}

	}

	@Override
	protected void onPostExecute(HashMap<String, Object> result) {
		synchronized (this) {
			if (mStateListener != null)
				mStateListener.downloadComplete(result);
		}
	}

	public void setServerConnectionListener(DownloadPatientListener sl) {
		synchronized (this) {
			mStateListener = sl;
		}
	}

	// url, username, password, serializer, locale, action, cohort
	private DataInputStream connectToServer(String url, String username,
			String password, int cohort) throws Exception {

		// compose url
		URL u = null;
		u = new URL(url);

		// setup http url connection
		HttpURLConnection c = null;
		c = (HttpURLConnection) u.openConnection();
		c.setDoOutput(true);
		c.setRequestMethod("POST");
		c.addRequestProperty("Content-type", "application/octet-stream");
		// write auth details to connection
		DataOutputStream dos = null;
		dos = new DataOutputStream(c.getOutputStream());
		dos.writeUTF(username); // username
		dos.writeUTF(password); // password

		dos.writeUTF(mSerializer); // serializer
		dos.writeUTF(mLocale); // locale
		dos.writeByte(Integer.valueOf(mAction).byteValue());

		if (mAction == ServerConstants.ACTION_DOWNLOAD_PATIENTS && cohort > 0) {
			dos.writeInt(cohort);
		}

		dos.flush();

		// read connection status
		DataInputStream zdis = null;
		DataInputStream dis = new DataInputStream(c.getInputStream());
		ZInputStream zis = new ZInputStream(dis);
		zdis = new DataInputStream(zis);

		int status = zdis.readByte();

		if (status == ServerConstants.STATUS_FAILURE) {
			zdis = null;
			throw new IOException("Connection failed. Please try again.");
		} else if (status == ServerConstants.STATUS_ACCESS_DENIED) {
			zdis = null;
			throw new IOException(
			"Access denied. Check your username and password.");
		} else {
			assert (status == ServerConstants.STATUS_SUCCESS); // success
			return zdis;
		}
	}

	private ArrayList<Patient> downloadPatients(DataInputStream zdis)
	throws Exception {

		int c = zdis.readInt();

		ArrayList<Patient> patients = new ArrayList<Patient>(c);
		for (int i = 0; i < c; i++) {
			Patient p = new Patient();
			if (zdis.readBoolean()) {
				p.setPatientId(zdis.readInt());
			}
			if (zdis.readBoolean()) {
				// ignore prefix
				zdis.readUTF();
			}
			if (zdis.readBoolean()) {
				p.setFamilyName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setMiddleName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setGivenName(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setGender(zdis.readUTF());
			}
			if (zdis.readBoolean()) {
				p.setBirthdate(mDateFormat.format((new Date(zdis.readLong()))));
			}
			if (zdis.readBoolean()) {
				p.setIdentifier(zdis.readUTF());
			}

			// ignore new patient
			zdis.readBoolean();

			patients.add(p);
			publishProgress(p.getPatientId().toString(), Integer.valueOf(i)
					.toString(), Integer.valueOf(c).toString());
		}
		
		return patients;
	}

	private List<Obs> downloadMedicalHistory(DataInputStream zdis) throws Exception {

		List<Obs> obslist = new ArrayList<Obs>();

		//Patient table fields
		int count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			System.out.println("Field Id=" + zdis.readInt());
			System.out.println("Field Name=" + zdis.readUTF());
		}

		//Patient table field values
		count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			System.out.println("Field Id=" + zdis.readInt());
			System.out.println("Patient Id=" + zdis.readInt());
			System.out.println("Value=" + zdis.readUTF());
		}

		//Patient medical history
		count = zdis.readInt();
		for (int i = 0; i < count; i++) 
			downloadPatientMedicalHistory(obslist, zdis.readInt(), zdis);

		return obslist;
	}

	
	private static void downloadPatientMedicalHistory(List<Obs> obslist, int patientId, DataInputStream zdis) throws Exception {

		int count = zdis.readInt();
		for (int i = 0; i < count; i++) 		
			downloadPatientMedicalHistoryFieldValues(obslist, patientId, zdis.readUTF(), zdis);
	}
	

	private static void downloadPatientMedicalHistoryFieldValues(List<Obs> obslist, int patientId, String fieldName, DataInputStream zdis) throws Exception {

		int count = zdis.readInt();

		for (int i = 0; i < count; i++) {
			Obs obs = new Obs();
			obs.setPatientId(patientId);
			obs.setFieldName(fieldName);

			byte type = zdis.readByte();

			if(type == 1)
				obs.setValueText(zdis.readUTF());
			else if(type == 2)
				obs.setValueInt(zdis.readInt());
			else if(type == 3)
				obs.setValueNumeric(zdis.readFloat());
			else if(type == 4)
				obs.setValueDate(new Date(zdis.readLong()));

			obs.setEncounterDate(new Date(zdis.readLong()));

			obslist.add(obs);
		}
	}
}
