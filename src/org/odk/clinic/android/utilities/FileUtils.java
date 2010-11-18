package org.odk.clinic.android.utilities;

import android.os.Environment;

public final class FileUtils {
    // Storage paths
    public static final String ODK_CLINIC_ROOT = Environment.getExternalStorageDirectory() + "/odk/clinic/";
    public static final String FORMS_PATH = ODK_CLINIC_ROOT + "forms/";
    public static final String INSTANCES_PATH = ODK_CLINIC_ROOT + "instances/";
    public static final String DATABASE_PATH = ODK_CLINIC_ROOT + "databases/";
}
