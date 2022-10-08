package art.lookingup.ntreemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    public static final int NUM_RUNS = 32;
    public static final int MAX_LEAVES_PER_RUN = 30;

    public Point3D[][] leaves = new Point3D[NUM_RUNS][];

    // creating a variable for our relative layout
    private LinearLayout linearLayout;

    private Toolbar myToolbar;
    private ActionBar myActionBar;

    public void updateLeafPosition(int run, int leaf, int x, int y, int z) {
        if (run < 0 || run >= NUM_RUNS) return;
        if (leaf < 0 || leaf >= MAX_LEAVES_PER_RUN) return;
        Point3D leafPos = leaves[run][leaf];
        leafPos.x = x;
        leafPos.y = y;
        leafPos.z = z;
        storeLeafPos(run, leaf, leafPos);
    }

    public void storeLeafPos(int run, int leaf, Point3D leafPos) {
        SharedPreferences appPrefs = ((MyApplication)getApplicationContext()).getSharedPrefs();
        appPrefs.edit().putInt("" + run + "." + leaf + "." + "x", leafPos.x).commit();
        appPrefs.edit().putInt("" + run + "." + leaf + "." + "y", leafPos.y).commit();
        appPrefs.edit().putInt("" + run + "." + leaf + "." + "z", leafPos.z).commit();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myActionBar = getSupportActionBar();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String lxIP = sharedPref.getString("lxip", "192.168.1.100");

        SharedPreferences appPrefs = ((MyApplication)getApplicationContext()).getSharedPrefs();

        // Initialize leaf positions with 0.  We will load the stored values from
        // the application context shared preferences.  The values will be stored
        // as integers where the key is run#.leaf#.x, run#.leaf#.y, run#.leaf#.z
        for (int i = 0; i < NUM_RUNS; i++) {
            leaves[i] = new Point3D[MAX_LEAVES_PER_RUN];
            for (int j = 0; j < MAX_LEAVES_PER_RUN; j++) {
                int x = appPrefs.getInt("" + i + "." + j + ".x", 5000);
                int y = appPrefs.getInt("" + i + "." + j + ".y", 5000);
                int z = appPrefs.getInt("" + i + "." + j + ".z", 5000);
                if (x != 5000) {
                    leaves[i][j] = new Point3D(x, y, z);
                } else {
                    leaves[i][j] = new Point3D(0, 0, 0);
                }
            }
        }

        PaintView paintView = new PaintView(this);
        // calling our  paint view class and adding
        // its view to our relative layout.

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(700, 700);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        paintView.setLayoutParams(lp);
        paintView.leaves = leaves;

        ImageButton runUpBtn = (ImageButton) findViewById(R.id.runUpBtn);
        ImageButton runDownBtn = (ImageButton) findViewById(R.id.runDownBtn);
        EditText runNumber = (EditText) findViewById(R.id.runView);

        ImageButton leafUpBtn = (ImageButton) findViewById(R.id.leafUpBtn);
        ImageButton leafDownBtn = (ImageButton) findViewById(R.id.leafDownBtn);
        EditText leafNumber = (EditText) findViewById(R.id.leafView);

        EditText xCoord = (EditText) findViewById(R.id.xCoordView);
        EditText yCoord = (EditText) findViewById(R.id.yCoordView);
        EditText zCoord = (EditText) findViewById(R.id.zCoordView);

        ImageButton updateLeafBtn = (ImageButton) findViewById(R.id.updateLeafBtn);
        updateLeafBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Update leaf clicked");
                if ("".equals(xCoord.getText()) ||
                        "".equals(yCoord.getText()) ||
                        "".equals(zCoord.getText()) ||
                        "".equals(runNumber.getText()) ||
                        "".equals(leafNumber.getText()))
                    return;
                int x = Integer.parseInt(xCoord.getText().toString());
                int y = Integer.parseInt(yCoord.getText().toString());
                int z = Integer.parseInt(zCoord.getText().toString());
                int run = Integer.parseInt(runNumber.getText().toString());
                int leaf = Integer.parseInt(leafNumber.getText().toString());
                updateLeafPosition(run, leaf, x, y, z);
                paintView.invalidate();
                Log.d(TAG, "updated leaf position");
            }
        });

        Log.d(TAG, "Initializing NTreeMap app........");
        // initializing our view.
        linearLayout = findViewById(R.id.idRLView);
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