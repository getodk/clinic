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
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Observation;
import org.odk.clinic.android.openmrs.Patient;

import android.os.AsyncTask;

import com.jcraft.jzlib.ZInputStream;

public class DownloadPatientTask extends
		AsyncTask<String, String, HashMap<String, Object>> {

	DownloadPatientListener mStateListener;
	public static final String KEY_ERROR = "error";
	public static final String KEY_PATIENTS = "patients";
	public static final String KEY_OBSERVATIONS = "observations";

	int mAction = Constants.ACTION_DOWNLOAD_PATIENTS;
	String mSerializer = Constants.DEFAULT_PATIENT_SERIALIZER;
	String mLocale = Locale.getDefault().getLanguage();

	@Override
	protected HashMap<String, Object> doInBackground(String... values) {

		String url = values[0];
		String username = values[1];
		String password = values[2];
		int cohort = Integer.valueOf(values[3]).intValue();

		HashMap<String, Object> result = new HashMap<String, Object>();

		try {
			DataInputStream zdis = connectToServer(url, username, password,
					cohort);
			if (zdis != null) {
				result.put(KEY_PATIENTS, downloadPatients(zdis));
				result.put(KEY_OBSERVATIONS, downloadObsevations(zdis));
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

		if (mAction == Constants.ACTION_DOWNLOAD_PATIENTS && cohort > 0) {
			dos.writeInt(cohort);
		}

		dos.flush();

		// read connection status
		DataInputStream zdis = null;
		DataInputStream dis = new DataInputStream(c.getInputStream());
		ZInputStream zis = new ZInputStream(dis);
		zdis = new DataInputStream(zis);

		int status = zdis.readByte();

		if (status == Constants.STATUS_FAILURE) {
			zdis = null;
			throw new IOException("Connection failed. Please try again.");
		} else if (status == Constants.STATUS_ACCESS_DENIED) {
			zdis = null;
			throw new IOException(
					"Access denied. Check your username and password.");
		} else {
			assert (status == Constants.STATUS_SUCCESS); // success
			return zdis;
		}
	}

	private List<Patient> downloadPatients(DataInputStream zdis)
			throws Exception {

		int c = zdis.readInt();

		List<Patient> patients = new ArrayList<Patient>(c);
		for (int i = 1; i < c + 1; i++) {
			Patient p = new Patient();
			if (zdis.readBoolean()) {
				p.setPatientId(zdis.readInt());
			}
			if (zdis.readBoolean()) {
				zdis.readUTF(); // ignore prefix
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
				p.setBirthDate(new Date(zdis.readLong()));
			}
			if (zdis.readBoolean()) {
				p.setIdentifier(zdis.readUTF());
			}

			zdis.readBoolean(); // ignore new patient

			patients.add(p);
			publishProgress(p.getPatientId().toString(), Integer.valueOf(i)
					.toString(), Integer.valueOf(c).toString());
		}

		return patients;
	}

	private List<Observation> downloadObsevations(DataInputStream zdis)
			throws Exception {

		List<Observation> obs = new ArrayList<Observation>();

		// patient table fields
		int count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			zdis.readInt(); // field id
			zdis.readUTF(); // field name
		}

		// Patient table field values
		count = zdis.readInt();
		for (int i = 0; i < count; i++) {
			zdis.readInt(); // field id
			zdis.readInt(); // patient id
			zdis.readUTF(); // value
		}

		// for every patient
		int icount = zdis.readInt();
		for (int i = 0; i < icount; i++) {
			
			// get patient id
			int patientId = zdis.readInt();

			// loop through list of obs
			int jcount = zdis.readInt();
			for (int j = 0; j < jcount; j++) {

				// get field name
				String fieldName = zdis.readUTF();
				
				// get ob values
				int kcount = zdis.readInt();
				for (int k = 0; k < kcount; k++) {
					
					Observation o = new Observation();
					o.setPatientId(patientId);
					o.setFieldName(fieldName);

					int type = zdis.readByte();
					if (type == Constants.TYPE_STRING) {
						o.setValueText(zdis.readUTF());
					} else if (type == Constants.TYPE_INT) {
						o.setValueInt(zdis.readInt());
					} else if (type == Constants.TYPE_FLOAT) {
						o.setValueNumeric(zdis.readFloat());
					} else if (type == Constants.TYPE_DATE) {
						o.setValueDate(new Date(zdis.readLong()));
					}

					o.setEncounterDate(new Date(zdis.readLong()));
					obs.add(o);
				}
			}
		}

		return obs;
	}
}
