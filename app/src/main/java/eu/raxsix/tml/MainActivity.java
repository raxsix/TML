package eu.raxsix.tml;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import eu.raxsix.tml.helper.Helper;
import eu.raxsix.tml.network.Network;

public class MainActivity extends AppCompatActivity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Helper.isTablet(this)) {
            // stop screen rotation on phones
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == R.id.action_settings) {

            Intent settingsIntent = new Intent(this, SettingsActivity.class);
            startActivity(settingsIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * OnClick button listener method for Create button
     */
    public void create(View view) {

        if (Network.isNetworkAvailable()) {

            Intent createIntent = new Intent(this, CreateGroupActivity.class);
            startActivity(createIntent);

        } else {
            Toast.makeText(this, R.string.network_not_available, Toast.LENGTH_LONG).show();
        }
    }


    public void join(View view) {

        if (Network.isNetworkAvailable()) {

            Intent joinIntent = new Intent(this, RoomListActivity.class);
            startActivity(joinIntent);
        } else {
            Toast.makeText(this, R.string.network_not_available, Toast.LENGTH_LONG).show();
        }
    }
}
