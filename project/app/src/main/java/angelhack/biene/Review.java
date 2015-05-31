package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;

import android.text.format.Time;
import android.util.Base64;
import android.util.Log;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;


import java.util.ArrayList;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
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
import java.util.Iterator;
import java.util.List;
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
                        list.add(docs.getJSONObject(key).getString("journey")+": "+docs.getJSONObject(key).getString("title"));
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

                SharedPreferences prefs = getActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String uname = prefs.getString("username", null);

                Map<String, String> qq = new HashMap<String, String>();
                qq.put("query", "<user>"+uname+"</user>");
                String json = new GsonBuilder().create().toJson(qq, Map.class);
                HttpResponse resp = makeRequest("https://api-eu.clusterpoint.com/854/DB_test/_search.json", json);
                return getDataFromResponse(resp);
            }

            protected void onPostExecute(String[] data) {
                if (data != null) {
                    mAdapter.clear();
                    for (String dt : data) {
                        if (mAdapter.getPosition(dt) < 0) mAdapter.add(dt);
                    }
                }
            }
        }
    }
}
