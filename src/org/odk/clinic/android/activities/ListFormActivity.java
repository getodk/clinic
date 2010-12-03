package org.odk.clinic.android.activities;

import java.util.ArrayList;

import org.odk.clinic.android.R;
import org.odk.clinic.android.adapters.PatientAdapter;
import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.openmrs.Constants;
import org.odk.clinic.android.openmrs.Form;
import org.odk.clinic.android.utilities.FileUtils;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ListFormActivity extends ListActivity {
    
    // Menu ID's
    private static final int MENU_DOWNLOAD = Menu.FIRST;
    
    // Request codes
    public static final int DOWNLOAD_FORM = 1;
    public static final int COLLECT_FORM = 2;
    
    private String patientIdStr;
    
    private ArrayAdapter<Form> mFormAdapter;
    private ArrayList<Form> mForms = new ArrayList<Form>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.form_list);
        
        if (!FileUtils.storageReady()) {
            showCustomToast(getString(R.string.error, R.string.storage_error));
            finish();
        }

        // TODO Check for invalid patient IDs
        patientIdStr = getIntent().getStringExtra(Constants.KEY_PATIENT_ID);
        
        setTitle(getString(R.string.app_name) + " > "
                + getString(R.string.forms));

    }
    
    private void getDownloadedForms() {

        ClinicAdapter ca = new ClinicAdapter();

        ca.open();
        Cursor c = ca.fetchAllForms();

        if (c != null && c.getCount() >= 0) {

            mForms.clear();

            int formIdIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_ID);
            int nameIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_NAME);
            int pathIndex = c
                    .getColumnIndex(ClinicAdapter.KEY_FORM_PATH);

            Form f;
            if (c.getCount() > 0) {
                do {
                    if (!c.isNull(pathIndex)) {
                        f = new Form();
                        f.setFormId(c.getInt(formIdIndex));
                        f.setPath(c.getString(pathIndex));
                        f.setName(c.getString(nameIndex));
                        mForms.add(f);
                    }
                } while (c.moveToNext());
            }
        }

        refreshView();
        
        if (c != null)
            c.close();

        ca.close();

    }
    
    private void refreshView() {

        mFormAdapter = new ArrayAdapter<Form>(this, android.R.layout.simple_list_item_1,
                mForms);
        setListAdapter(mFormAdapter);

    }
    
    @Override
    protected void onListItemClick(ListView listView, View view, int position,
            long id) {
        // Get selected form
        Form f = (Form) getListAdapter().getItem(position);
        String formPath = f.getPath();

        Intent i = new Intent("org.odk.collect.android.action.FormEntry");
        i.putExtra("formpath", formPath);
        startActivityForResult(i, COLLECT_FORM);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, MENU_DOWNLOAD, 0, getString(R.string.download_forms))
                .setIcon(R.drawable.ic_menu_download);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case MENU_DOWNLOAD:
            Intent id = new Intent(getApplicationContext(),
                    DownloadFormActivity.class);
            startActivityForResult(id, DOWNLOAD_FORM);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode,
            Intent intent) {
        
        if (resultCode == RESULT_CANCELED) {
            return;
        }
        
        if (requestCode == COLLECT_FORM && intent != null) {
            // TODO Update ODK collect to return saved instance path
            String instancePath = intent.getStringExtra("instancepath");
            if (instancePath != null && instancePath.length() > 0) {
                // Update instance
            }
        }

        super.onActivityResult(requestCode, resultCode, intent);

    }

    @Override
    protected void onResume() {
        super.onResume();
        
        getDownloadedForms();
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
