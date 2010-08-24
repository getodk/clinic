package org.odk.clinic.android.listeners;

import java.util.HashMap;

public interface DownloadPatientListener {
	void downloadComplete(HashMap<String, Object> result);
	void progressUpdate(String message, int progress, int max);
}
