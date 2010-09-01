package org.odk.clinic.android.listeners;


public interface DownloadPatientListener {
	void downloadComplete(String result);
	void progressUpdate(String message, int progress, int max);
}
