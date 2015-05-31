package angelhack.biene;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.ShareActionProvider;
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

import java.util.ArrayList;
import java.util.HashMap;

public class MapDetail extends ActionBarActivity {
    private GoogleMap mMap;
    private HashMap<Marker, Bitmap> mHash;

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

    private ArrayList<LatLng> getFakeData() {
        ArrayList<LatLng> points = new ArrayList<LatLng>();
        points.add(new LatLng(0.0, 0.0));
        points.add(new LatLng(10.0, 0.0));
        points.add(new LatLng(0.0, 20.0));
        points.add(new LatLng(0.0, 30.0));
        points.add(new LatLng(10.0, 35.0));
        points.add(new LatLng(15.0, 35.0));
        return points;
    }

    private void drawRoute() {

        // TODO get data from DB
        ArrayList<LatLng> points = getFakeData();

        Polyline line = mMap.addPolyline(new PolylineOptions()
                //.add(new LatLng(51.5, -0.1), new LatLng(40.7, -74.0))
                .width(5)
                .color(Color.RED));

        // Remove previous roads and marks
        mMap.clear();
        // Print map and all markers
        mMap.addPolyline(new PolylineOptions().addAll(points).width(5).color(Color.RED));

        ArrayList<LatLng> photoLocations = new ArrayList<LatLng>();
        ArrayList<Bitmap> photos = new ArrayList<Bitmap>();
        ArrayList<String> descriptions = new ArrayList<String>();
        mHash = new HashMap<Marker, Bitmap>();
        // TODO query DB for the photos and store them in the arrays
        for (int i = 0; i < 10; i++) {
            photoLocations.add(new LatLng(0.0, 0.0));
            photos.add((Bitmap) BitmapFactory.decodeResource(getResources(), R.drawable.alessio));
            descriptions.add("BIENE");
        }
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
}
