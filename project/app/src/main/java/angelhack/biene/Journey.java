package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
    private ClusterpointTask mClusterpoint;

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
        mClusterpoint = new ClusterpointTask();
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
            startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
        }
    }

    private String showRenameDialog () {
        alertDialog.setTitle("Name your picture!");
        final EditText name = new EditText(SecondScan.this);

        LinearLayout ll = new LinearLayout(this);
        ll.setOrientation(LinearLayout.VERTICAL);
        ll.addView(name);
        alertDialog.setView(ll);

        alertDialog.setCancelable(false);
        String rename = null;
        alertDialog.setPositiveButton("OK",  new DialogInterface.OnClickListener() { 
            public void onClick(DialogInterface dialog, int id) {
                rename = name.getText();
                dialog.dismiss();
            }
        });

        AlertDialog alert = alertDialog.create();
        alert.show();

        while (rename == null);
        return rename;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        System.out.println("RESULT");
        try {
            if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
                //File photoFile = createImageFile();
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                showRenameDialog();
                mClusterpoint.execute(imageBitmap);
                // TODO save in file
            }
        }
        catch (Exception e) {
            System.out.println("wololo");
            e.printStackTrace();
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
        return image;
    }

    private void storeImage(Bitmap image) {
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d("storeImage",
                    "Error creating media file, check storage permissions: ");// e.getMessage());
            return;
        }
        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            image.compress(Bitmap.CompressFormat.PNG, 90, fos);
            fos.close();
        } catch (FileNotFoundException e) {
            Log.d("storeImage", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("storeImage", "Error accessing file: " + e.getMessage());
        }
    }

    private  File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted
        // using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory()
                + "/Android/data/"
                + getApplicationContext().getPackageName()
                + "/Files");


        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.

        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        // Create a media file name
        String timeStamp = new SimpleDateFormat("ddMMyyyy_HHmm").format(new Date());
        File mediaFile;
        String mImageName="MI_"+ timeStamp +".jpg";
        mediaFile = new File(mediaStorageDir.getPath() + File.separator + mImageName);
        return mediaFile;
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

    public class ClusterpointTask extends AsyncTask<Bitmap, Void, Void> {

        @Override
        protected Void doInBackground(Bitmap... image) {
            try {

                SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                int idPhoto = prefs.getInt("idJourney", 0);
                String uname = prefs.getString("username", "alessio");

                if (idPhoto == 0) throw new Exception("The app did not get initialized correctly");
                else {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putInt("idJourney", idPhoto+1);
                    editor.commit();
                }
            
                final String BASE_URL = "https://api-eu.clusterpoint.com/843/DB_Test.json";

                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("id", idPhoto);
                    jsonObject.put("username", uname);
                    Log.d("output", jsonObject.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } catch (Exception e)  {
                //Toast.makeText(getApplicationContext(), "ERROOOOOOR :(", Toast.LENGTH_LONG).show();

                System.out.println("guidoguidoguido");
                e.printStackTrace();
            }
            return null;
        }
    }
}

