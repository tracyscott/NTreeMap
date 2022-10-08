package art.lookingup.ntreemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    // creating a variable for our relative layout
    private LinearLayout linearLayout;

    private Toolbar myToolbar;
    private ActionBar myActionBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myActionBar = getSupportActionBar();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String lxIP = sharedPref.getString("lxip", "192.168.1.100");

        // initializing our view.
        linearLayout = findViewById(R.id.idRLView);

        // calling our  paint view class and adding
        // its view to our relative layout.
        PaintView paintView = new PaintView(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(700, 700);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        paintView.setLayoutParams(lp);
        linearLayout.addView(paintView);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
                /*
            case R.id.action_one:
                // Connect to RainbowStudio 1
                myActionBar.setTitle("RainbowCtrl - 1");
                if (whichRainbowStudio != RAINBOW_STUDIO_ONE) {
                    whichRainbowStudio = RAINBOW_STUDIO_ONE;
                    rainbowStudioIP = rainbowStudio1IP;
                    if (oscPortOut != null) {
                        oscPortOut.close();
                        oscPortOut = null;
                    }
                }
                return true;
                */
            default:
                super.onOptionsItemSelected(item);
        }
        return false;
    }
}