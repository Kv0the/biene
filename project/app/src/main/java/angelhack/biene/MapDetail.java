package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
import android.util.Base64;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MapDetail extends ActionBarActivity {
    private GoogleMap mMap;
    private HashMap<Marker, Bitmap> mHash;
    private ArrayList<LatLng> points = new ArrayList<LatLng>();
    private ArrayList<LatLng> photoLocations = new ArrayList<LatLng>();
    private ArrayList<Bitmap> photos = new ArrayList<Bitmap>();
    private ArrayList<String> descriptions = new ArrayList<String>();

    private static final String PREFS_NAME = "yoloswag420blazeit";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_detail);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_map, menu);

        MenuItem menuItem = menu.findItem(R.id.action_share);

        ShareActionProvider mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);

        if (mShareActionProvider != null) {
            mShareActionProvider.setShareIntent(createShareIntent());
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            // TODO
            case R.id.action_settings:

                break;
            case R.id.action_share:

                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }


    private void setUpMapIfNeeded() {
        if (mMap != null) {
            return;
        }
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        if (mMap == null) {
            return;
        }

        mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());

        // Initialize map options. For example:
        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(41.394801, -2.148309)));

        mMap.addMarker(new MarkerOptions()
                .position(new LatLng(46.394801, -2.148309)));

        drawRoute();

    }

    private Intent createShareIntent() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "#BIENE");
        return shareIntent;
    }

    private void drawRoute() {

        // TODO get data from DB
        Polyline line = mMap.addPolyline(new PolylineOptions()
                //.add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                .width(5)
                .color(Color.RED));

        // Remove previous roads and marks
        mMap.clear();
        // Print map and all markers
        mHash = new HashMap<Marker, Bitmap>();
        mMap.addPolyline(new PolylineOptions().addAll(photoLocations).width(5).color(Color.RED));
        for (int i = 0; i < photos.size(); i++) {
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(photoLocations.get(i))
                    .title(descriptions.get(i)));
            mHash.put(marker, photos.get(i));

        }


        focusCameraOnPath(points);
    }

    private void focusCameraOnPath(ArrayList<LatLng> path) {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        for (LatLng point : path) {
            builder.include(point);
        }

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        final int width = size.x;
        final int height = size.y;
        final int padding = 40;
        // avoid animateCamera if points is empty
        if (path.size() > 0) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(
                    builder.build(), width, height, padding));
        }
    }

    class MyInfoWindowAdapter implements GoogleMap.InfoWindowAdapter {

        private final View myContentsView;

        MyInfoWindowAdapter(){
            myContentsView = getLayoutInflater().inflate(R.layout.map_info, null);
        }

        @Override
        public View getInfoContents(Marker marker) {
            try {

                TextView title = ((TextView) myContentsView.findViewById(R.id.map_info_title));
                title.setText(getTitle());

                ImageView imageView = (ImageView) findViewById(R.id.marker_image);
                Bitmap bitmap = mHash.get(marker);
                imageView.setImageBitmap(bitmap);

            }
            catch (Exception e) {
                System.out.println("topkek");
                e.printStackTrace();
            }

            return myContentsView;
        }

        @Override
        public View getInfoWindow(Marker marker) {
            // TODO Auto-generated method stub
            return null;
        }
    }

    public class FetchListTask extends AsyncTask<String, Void, String[]> {

        private final String LOG_TAG = FetchListTask.class.getSimpleName();

        private String[] getDataFromResponse(HttpResponse resp) {

            String[] str = new String[0];
            Log.d("AAAAH", "ENTRAMOS");

            try {
                String json_string = EntityUtils.toString(resp.getEntity());
                System.out.printf(json_string);
                JSONObject tmp = new JSONObject(json_string);
                JSONObject docs = tmp.getJSONObject("documents");
                List<String> list = new ArrayList<String>();
                for (Iterator iterator = docs.keys(); iterator.hasNext();) {
                    String key = (String) iterator.next();
                    list.add(docs.getJSONObject(key).getString("lat")+" "
                            +docs.getJSONObject(key).getString("lon")+" "
                            +docs.getJSONObject(key).getString("title")+ " "
                            +docs.getJSONObject(key).getString("photo"));
                }
                str = list.toArray(str);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return str;

        }

        public HttpResponse makeRequest(String uri, String json) {
            try {
                String authStr = "elnombredelviento@gmail.com:bienealessio";
                String encoding = Base64.encodeToString(authStr.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);

                HttpPost httpPost = new HttpPost(uri);
                httpPost.setEntity(new StringEntity(json));
                httpPost.setHeader("Accept", "application/json");
                httpPost.setHeader("Content-type", "application/json");
                httpPost.setHeader("Authorization", "Basic " + encoding);
                return new DefaultHttpClient().execute(httpPost);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        protected String[] doInBackground(String... params) {

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            String uname = prefs.getString("username", null);
            String[] mstr = params[0].split(" ");
            String ms = mstr[2].substring(1);

            Map<String, String> qq = new HashMap<String, String>();
            qq.put("query", "<journey>"+ms+"</journey><user>"+uname+"</user>");
            String json = new GsonBuilder().create().toJson(qq, Map.class);
            HttpResponse resp = makeRequest("https://api-eu.clusterpoint.com/854/DB_test/_search.json", json);
            return getDataFromResponse(resp);
        }

        protected void onPostExecute(String[] data) {
            if (data != null) {
                for (String str : data) {
                    String[] tmp = str.split(" ");
                    points.add(new LatLng(Double.valueOf(tmp[0]), Double.valueOf(tmp[1])));
                    photoLocations.add(new LatLng(Double.valueOf(tmp[0]), Double.valueOf(tmp[1])));
                    photos.add(stringToBitmap(tmp[1]));
                    descriptions.add(tmp[2]);
                }

            }
        }

        public Bitmap stringToBitmap(String str) {
            byte[] bitmapdata = str.getBytes();
            return BitmapFactory.decodeByteArray(bitmapdata, 100, bitmapdata.length);
        }
    }
}
