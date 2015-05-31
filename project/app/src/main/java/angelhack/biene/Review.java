package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.text.format.Time;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class Review extends ActionBarActivity {

    private static final String PREFS_NAME = "yoloswag420blazeit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_review, menu);
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

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        private ArrayAdapter mAdapter;

        @Override
        public void onStart() {
            super.onStart();
            updateAdapter();
        }

        private void updateAdapter() {
            FetchListTask fetch = new FetchListTask();
            fetch.execute();
        }


        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            View rootView = inflater.inflate(R.layout.fragment_review, container, false);

            mAdapter =
                    new ArrayAdapter<String>(
                            // The current context (this fragment's parent activity)
                            getActivity(),
                            // ID of list item layout
                            R.layout.list_item,
                            // ID of the textview to populate
                            R.id.list_item_textview,
                            // Forecast data
                            new ArrayList<String>()
                    );

            // Get a reference to the ListView, and attach this adapter it it
            ListView listView = (ListView) rootView.findViewById(
                    R.id.listview
            );

            listView.setAdapter(mAdapter);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String journey = mAdapter.getItem(position).toString();
                    // Toast.makeText(getActivity(), forecast, Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(getActivity(), MapDetail.class).
                            putExtra(Intent.EXTRA_TEXT, journey);
                    startActivity(intent);
                }
            });

            return rootView;
        }

        public class FetchListTask extends AsyncTask<String, Void, String[]> {

            private final String LOG_TAG = FetchListTask.class.getSimpleName();

            private String[] getDataFromResponse(HttpResponse resp)
                    throws JSONException {

                final String OWM_ = "list";
                final String OWM_WEATHER = "weather";
                final String OWM_TEMPERATURE = "temp";
                final String OWM_MAX = "max";
                final String OWM_MIN = "min";
                final String OWM_DESCRIPTION = "main";

                JSONObject forecastJson = new JSONObject(forecastJsonStr);
                JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);

                // OWM returns daily forecasts based upon the local time of the city that is being
                // asked for, which means that we need to know the GMT offset to translate this data
                // properly.

                // Since this data is also sent in-order and the first day is always the
                // current day, we're going to take advantage of that to get a nice
                // normalized UTC date for all of our weather.

                Time dayTime = new Time();
                dayTime.setToNow();

                // we start at the day returned by local time. Otherwise this is a mess.
                int julianStartDay = Time.getJulianDay(System.currentTimeMillis(), dayTime.gmtoff);

                // now we work exclusively in UTC
                dayTime = new Time();

                String[] resultStrs = new String[numDays];
                for(int i = 0; i < weatherArray.length(); i++) {
                    // For now, using the format "Day, description, hi/low"
                    String day;
                    String description;
                    String highAndLow;

                    // Get the JSON object representing the day
                    JSONObject dayForecast = weatherArray.getJSONObject(i);

                    // The date/time is returned as a long.  We need to convert that
                    // into something human-readable, since most people won't read "1400356800" as
                    // "this saturday".
                    long dateTime;
                    // Cheating to convert this to UTC time, which is what we want anyhow
                    dateTime = dayTime.setJulianDay(julianStartDay+i);
                    day = getReadableDateString(dateTime);

                    // description is in a child array called "weather", which is 1 element long.
                    JSONObject weatherObject = dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
                    description = weatherObject.getString(OWM_DESCRIPTION);

                    // Temperatures are in a child object called "temp".  Try not to name variables
                    // "temp" when working with temperature.  It confuses everybody.
                    JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
                    double high = temperatureObject.getDouble(OWM_MAX);
                    double low = temperatureObject.getDouble(OWM_MIN);

                    highAndLow = formatHighLows(high, low);
                    resultStrs[i] = day + " - " + description + " - " + highAndLow;
                }

                return resultStrs;

            }

            public HttpResponse makeRequest(String uri, String json) {
                try {
                    HttpPost httpPost = new HttpPost(uri);
                    httpPost.setEntity(new StringEntity(json));
                    httpPost.setHeader("Accept", "application/json");
                    httpPost.setHeader("Content-type", "application/json");
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

                if (params.length == 0) return null;

                SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                int idJourney = prefs.getInt("idJourney", 0);
                String uname = prefs.getString("username", null);

                Map<String, String> qq = new HashMap<String, String>();
                qq.put("query", "<id>"+idJourney+"</id><title>"+uname+"</title>");
                qq.put("docs","20");
                qq.put("offset","0");
                qq.put("list", new GsonBuilder().create().toJson(new HashMap<String, String>(), Map.class));
                String json = new GsonBuilder().create().toJson(qq, Map.class);
                HttpResponse resp = makeRequest("https://api-eu.clusterpoint.com/854/DB_test/_lookup.json", json);
                String[] respStr =  getDataFromResponse(resp);
                return respStr;
            }

            @Override
            protected void onPostExecute(String[] result) {
                if (result != null) {
                    mAdapter.clear();
                    for (String itemStr : result) {
                        mAdapter.add(itemStr);
                    }
                }
            }
        }
    }
}
