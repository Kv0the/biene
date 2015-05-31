package angelhack.biene;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
	
	private static final String PREFS_NAME = "yoloswag420blazeit";

	@Override
    protected void onStart() {
		super.onStart();

		SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); 
		String uname= prefs.getString("username", null);
		if (uname == null) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString("username", "BIENE");
			editor.putInt("idTravel", 1);
			editor.putInt("idJourney", 1);
			editor.commit();
		}
	}

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final ActionBarActivity context = this;

        CircleButton st_journey = (CircleButton) context.findViewById(R.id.btn_start_journey);

        st_journey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Journey.class);
                startActivity(intent);
            }
        });

        TextView txt_journey = (TextView) context.findViewById(R.id.button_start_text);

        txt_journey.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CircleButton st_journey = (CircleButton) context.findViewById(R.id.btn_start_journey);
                st_journey.animate();
                st_journey.performClick();
            }
        });

        Button rev_journeys = (Button) this.findViewById(R.id.btn_review_journeys);

        rev_journeys.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, Review.class);
                startActivity(intent);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
}
