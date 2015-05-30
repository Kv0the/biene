package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.clusterpoint.api.CPSConnection;
import com.clusterpoint.api.request.CPSInsertRequest;
import com.clusterpoint.api.response.CPSModifyResponse;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class Journey extends ActionBarActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_TAKE_PHOTO = 1;
    private String mCurrentPhotoPath;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    private static final String PREFS_NAME = "yoloswag420blazeit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_journey);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new JourneyFragment())
                    .commit();
        }
        buildGoogleApiClient();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnected(Bundle bundle) {
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_journey, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void takePhoto(View view) {
        // take photo
        dispatchTakePictureIntent();

        // get location
        String location = getLocation();
        Toast.makeText(getApplicationContext(), location, Toast.LENGTH_SHORT).show();
    }

    public void endJourney(View view) {
        Toast.makeText(this, "SE FUE", Toast.LENGTH_LONG).show();
    }

    public static class JourneyFragment extends Fragment {


        public JourneyFragment() {
            // Required empty public constructor
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            // Inflate the layout for this fragment
            return inflater.inflate(R.layout.fragment_journey, container, false);
        }
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        Uri contentUri = Uri.fromFile(image);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
        storeImageInCP(image);
        return image;
    }

    protected synchronized void buildGoogleApiClient() {  // call it on create
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    private String getLocation() {
        String location = "";
        if (mLastLocation != null) {
            location += String.valueOf(mLastLocation.getLatitude());
            location += "-";
            location += String.valueOf(mLastLocation.getLongitude());
        }
        else return "null";
        return location;
    }

    private void storeImageInCP(File savedImage) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            int idPhoto = prefs.getInt("idJourney", 0);
            String uname = prefs.getString("username", null);

            if (idPhoto == 0) throw new Exception("The app did not get initialized correctly");
            else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("idJourney", idPhoto+1);
                editor.commit();
            }

            CPSConnection conn = new CPSConnection("tcps://cloud-eu-0.clusterpoint.com:9008", "DB_Test", "BIENE", "Alessio",
                                               "843", "document", "//document/id");

            List<String> docs = new ArrayList<String>();
            docs.add("<document><id>"+ idPhoto +"</id><user>" + uname + "</user></document>");
            
            //Create Insert request
            CPSInsertRequest insert_req = new CPSInsertRequest();
            //Add documents to request
            insert_req.setStringDocuments(docs);
            //Send request
            CPSModifyResponse insert_resp = (CPSModifyResponse) conn.sendRequest(insert_req);
            //Print out inserted document ids
            Log.d("GUIDO", "Inserted ids: " + Arrays.toString(insert_resp.getModifiedIds()));

            //Close connection
            conn.close();
        } catch (Exception e)  {
            e.printStackTrace();
        }   
    }
}
