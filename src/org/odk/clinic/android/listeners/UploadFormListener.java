package org.odk.clinic.android.listeners;

public interface UploadFormListener {
    void uploadComplete(String result);
    void progressUpdate(String message, int progress, int max);
}
