package org.odk.clinic.android.tasks;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.odk.clinic.android.database.ClinicAdapter;
import org.odk.clinic.android.listeners.UploadFormListener;

import android.os.AsyncTask;
import android.util.Log;

public class UploadFormTask extends AsyncTask<String, String, String> {
    private static String tag = "UploadFormTask";
    
    protected UploadFormListener mStateListener;
    String mUrl;
    protected ClinicAdapter mClinicAdapter = new ClinicAdapter();
    
    public void setUploadServer(String newServer) {
        mUrl = newServer;
    }
    
    @Override
    protected String doInBackground(String... params) {
        String instancePath = params[0];
        
        try {
            File f = new File(instancePath);
            FileInputStream fis = new FileInputStream(f);
            
            // setup http url connection
            URL u = new URL(mUrl);
            Log.d(tag, "Connecting to " + mUrl);
            HttpURLConnection c = (HttpURLConnection) u.openConnection();
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.addRequestProperty("Content-type", "text/xml");
            
            // write xml to connection
            int bytesAvailable = fis.available();
            int maxBufferSize = 1024;
            int bufferSize = Math.min(bytesAvailable, maxBufferSize);
            byte[] buffer = new byte[bufferSize];

            // copy file contents
            DataOutputStream dos = new DataOutputStream(c.getOutputStream());
            int bytesRead = fis.read(buffer, 0, bufferSize);
            
            // FIXME writing out?
            while (bytesRead > 0)
            {
                System.out.write(buffer, 0, bufferSize);
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fis.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fis.read(buffer, 0, bufferSize);
            }

            // close streams
            fis.close();
            dos.flush();
            dos.close();
            
            // TODO check response
            
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        } catch (IOException e) {
            e.printStackTrace();
            return e.getLocalizedMessage();
        }

        return null;
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
    protected void onPostExecute(String result) {
        synchronized (this) {
            if (mStateListener != null)
                mStateListener.uploadComplete(result);
        }
    }

    public void setUploadListener(UploadFormListener sl) {
        synchronized (this) {
            mStateListener = sl;
        }
    }
}
