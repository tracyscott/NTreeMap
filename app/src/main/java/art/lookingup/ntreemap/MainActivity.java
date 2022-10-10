package art.lookingup.ntreemap;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.Formatter;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.illposed.osc.OSCBundle;
import com.illposed.osc.OSCListener;
import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCPortIn;
import com.illposed.osc.OSCPortOut;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    public static final int NUM_RUNS = 32;
    public static final int MAX_LEAVES_PER_RUN = 55;

    public Point3D[][] leaves = new Point3D[NUM_RUNS][];

    EditText runNumber;
    EditText leafNumber;
    EditText xCoord;
    EditText yCoord;
    EditText zCoord;
    PaintView paintView;
    EditText txtMsg;
    Button txtMsgBtn;
    CheckBox bgLeaves;

    // creating a variable for our relative layout
    private LinearLayout linearLayout;

    private Toolbar myToolbar;
    private ActionBar myActionBar;

    //
    // OSC Communication
    //
    private String lxIP = "192.168.4.41";
    private String myIP = "192.168.2.136";
    private int lxPort = 7979;
    // For 2-way communication. (currently unused).
    private int myRecvPort = 7980;
    private OSCPortOut oscPortOut;
    private OSCPortIn oscReceiver;

    // Wrapper class for communicating with the message queue shared with OSC sending.
    public static class LeafPos {
        public Point3D pos;
        public int runNum;
        public int leafNum;
        public LeafPos(int runNum, int leafNum, Point3D pos) {
            this.runNum = runNum; this.leafNum = leafNum; this.pos = pos;
        }
    }

    // Wrapper class for queue messages
    public static class QueueMsg {
        // 0 = leafPos, 1 = request run data, 2 = test txt msg, 3 = leaf sel, 4 = bg leaves
        public int msgType;
        public LeafPos leafPos;
        public int runNum;
        public String txtMessage;
        public int leafNum;
        public boolean bgLeaves;
    }

    public void updateLeafPosition(int run, int leaf, int x, int y, int z) {
        if (run < 1 || run > NUM_RUNS) return;
        if (leaf < 0 || leaf >= MAX_LEAVES_PER_RUN) return;
        Point3D leafPos = leaves[run-1][leaf];
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

    public void focusCurrentLeaf(int run, int leaf) {
        focusCurrentLeaf(run, leaf, true);
    }

    /**
     * Called when a new run/leaf number is selected.  We need to update the values in
     * the xCoord, yCoord, and zCoord text fields.
     * @param run
     * @param leaf
     */
    public void focusCurrentLeaf(int run, int leaf, boolean updateLX) {
        if (run < 1 || run > NUM_RUNS) return;
        if (leaf < 0 || leaf >= MAX_LEAVES_PER_RUN) return;
        Point3D leafPos = leaves[run-1][leaf];
        xCoord.setText("" + leafPos.x);
        yCoord.setText("" + leafPos.y);
        zCoord.setText("" + leafPos.z);
        paintView.currentRun = run;
        paintView.currentLeaf = leaf;
        MainActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                paintView.invalidate();
            }
        });
        // Sometimes, we want to update LX.
        if (updateLX) {
            QueueMsg qMsg = new QueueMsg();
            qMsg.msgType = 3;
            qMsg.runNum = run;
            qMsg.leafNum = leaf;
            synchronized (messageQueue) {
                messageQueue.add(qMsg);
            }
        }
    }

    public int getDisplayedLeafNum() {
        if (leafNumber.getText().toString().equals(""))
            return 0;
        return Integer.parseInt(leafNumber.getText().toString());
    }

    public int getDisplayedRunNum() {
        if (runNumber.getText().toString().equals(""))
            return 0;
        return Integer.parseInt(runNumber.getText().toString());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        myActionBar = getSupportActionBar();

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        lxIP = sharedPref.getString("lx_ip", "192.168.1.100");

        SharedPreferences appPrefs = ((MyApplication)getApplicationContext()).getSharedPrefs();

        // Initialize leaf positions with 0.  We will load the stored values from
        // the application context shared preferences.  The values will be stored
        // as integers where the key is run#.leaf#.x, run#.leaf#.y, run#.leaf#.z
        for (int runNum = 1; runNum <= NUM_RUNS; runNum++) {
            leaves[runNum-1] = new Point3D[MAX_LEAVES_PER_RUN];
            for (int j = 0; j < MAX_LEAVES_PER_RUN; j++) {
                int x = appPrefs.getInt("" + runNum + "." + j + ".x", 5000);
                int y = appPrefs.getInt("" + runNum + "." + j + ".y", 5000);
                int z = appPrefs.getInt("" + runNum + "." + j + ".z", 5000);
                if (x != 5000) {
                    leaves[runNum-1][j] = new Point3D(x, y, z);
                } else {
                    leaves[runNum-1][j] = new Point3D(0, 0, 0);
                }
            }
        }

        Context context = getApplicationContext();
        WifiManager wm = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        myIP = Formatter.formatIpAddress(wm.getConnectionInfo().getIpAddress());

        paintView = new PaintView(this);
        // calling our  paint view class and adding
        // its view to our relative layout.

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(700, 700);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        paintView.setLayoutParams(lp);
        paintView.leaves = leaves;

        runNumber = (EditText) findViewById(R.id.runView);
        TextWatcher runWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                String newStr = s.toString();
                if ("".equals(newStr))
                    return;
                int runNum = Integer.parseInt(s.toString());
                focusCurrentLeaf(runNum, getDisplayedLeafNum());
            }
        };
        runNumber.addTextChangedListener(runWatcher);
        ImageButton runUpBtn = (ImageButton) findViewById(R.id.runUpBtn);
        runUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int runNum = 1;
                if (runNumber.getText().toString().equals(""))
                    runNumber.setText("1");
                else {
                    runNum = Integer.parseInt(runNumber.getText().toString());
                    runNum = (runNum + 1)%(NUM_RUNS+1);
                    if (runNum == 0) runNum = 1;
                    runNumber.setText("" + runNum);
                }
                focusCurrentLeaf(runNum, getDisplayedLeafNum());
            }
        });
        ImageButton runDownBtn = (ImageButton) findViewById(R.id.runDownBtn);
        runDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int runNum = 1;
                if (runNumber.getText().toString().equals(""))
                    runNumber.setText("1");
                else {
                    runNum = Integer.parseInt(runNumber.getText().toString());
                    runNum = runNum - 1;
                    if (runNum < 1) runNum = NUM_RUNS;
                    runNumber.setText("" + runNum);
                }
                focusCurrentLeaf(runNum, getDisplayedLeafNum());
            }
        });

        leafNumber = (EditText) findViewById(R.id.leafView);
        TextWatcher leafWatcher = new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {
                String newStr = s.toString();
                if ("".equals(newStr))
                    return;
                int leafNum = Integer.parseInt(s.toString());
                focusCurrentLeaf(getDisplayedRunNum(), leafNum);
            }
        };
        leafNumber.addTextChangedListener(leafWatcher);
        ImageButton leafUpBtn = (ImageButton) findViewById(R.id.leafUpBtn);
        leafUpBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int leafNum = 0;
                if (leafNumber.getText().toString().equals(""))
                    leafNum = 0;
                else {
                    leafNum = Integer.parseInt(leafNumber.getText().toString()) + 1;
                    if (leafNum >= MAX_LEAVES_PER_RUN)
                        leafNum = 0;
                }
                leafNumber.setText("" + leafNum);
                focusCurrentLeaf(getDisplayedRunNum(), leafNum);
            }
        });
        ImageButton leafDownBtn = (ImageButton) findViewById(R.id.leafDownBtn);
        leafDownBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                int leafNum = 0;
                if (leafNumber.getText().toString().equals(""))
                    leafNum = 0;
                else {
                    leafNum = Integer.parseInt(leafNumber.getText().toString()) - 1;
                    if (leafNum < 0)
                        leafNum = MAX_LEAVES_PER_RUN - 1;
                }
                leafNumber.setText("" + leafNum);
                focusCurrentLeaf(getDisplayedRunNum(), leafNum);
            }
        });

        xCoord = (EditText) findViewById(R.id.xCoordView);
        yCoord = (EditText) findViewById(R.id.yCoordView);
        zCoord = (EditText) findViewById(R.id.zCoordView);

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

        txtMsg = (EditText) findViewById(R.id.txtMsgView);
        txtMsgBtn = (Button) findViewById(R.id.txtMsgBtn);
        txtMsgBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                Log.d(TAG, "TXT button clicked!");
                String txt = txtMsg.getText().toString();
                Log.d(TAG, "TXT msg= " + txt);
                if ("".equals(txt)) return;
                QueueMsg qMsg = new QueueMsg();
                qMsg.msgType = 2;
                qMsg.txtMessage = txt;
                synchronized (messageQueue) {
                    messageQueue.add(qMsg);
                }
            }
        });
        bgLeaves = (CheckBox) findViewById(R.id.bgLeavesChk);

        bgLeaves.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,boolean isChecked) {
                paintView.currentRunOnly = !isChecked;
                paintView.invalidate();
                QueueMsg qMsg = new QueueMsg();
                qMsg.msgType = 4;
                qMsg.bgLeaves = isChecked;
                synchronized (messageQueue) {
                    messageQueue.add(qMsg);
                }
            }
        });

        Log.d(TAG, "Initializing NTreeMap app........");
        // initializing our view.
        linearLayout = findViewById(R.id.idRLView);
        linearLayout.addView(paintView);
    }

    protected void onResume() {
        Log.d(TAG, "resuming!");
        super.onResume();
        oscRecvThread.start();
        oscSendThread.start();
    }

    protected void onPause() {
        Log.d(TAG, "pausing!");
        super.onPause();
        if (oscReceiver != null) {
            oscReceiver.stopListening();
            oscReceiver.close();
        }
        if (oscPortOut != null) {
            oscPortOut.close();
            oscPortOut = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.my_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int run;
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_push:
                run = getDisplayedRunNum();
                for (int leafNum = 0; leafNum < leaves[run-1].length; leafNum++) {
                    Point3D p = leaves[run-1][leafNum];
                    if (p.x == 0 && p.y == 0 && p.z == 0)
                        continue;
                    LeafPos lpos = new LeafPos(run, leafNum, p);
                    QueueMsg qMsg = new QueueMsg();
                    qMsg.msgType = 0;
                    qMsg.leafPos = lpos;
                    synchronized (messageQueue) {
                        messageQueue.add(qMsg);
                    }
                }
                return true;
            case R.id.action_pull:
                run = getDisplayedRunNum();
                QueueMsg qMsg = new QueueMsg();
                qMsg.msgType = 1;
                qMsg.runNum = run;
                synchronized (messageQueue) {
                    messageQueue.add(qMsg);
                }
                return true;
            default:
                super.onOptionsItemSelected(item);
        }
        return false;
    }

    Queue<QueueMsg> messageQueue = new LinkedList<QueueMsg>();

    private Thread oscSendThread = new Thread() {

        public void sendOscMsg(OSCMessage oscMsg) {
            try {
                Log.d(TAG, "Sending message.");
                oscPortOut.send(oscMsg);
            } catch (UnknownHostException e) {
                // Error handling when your IP isn't found
                Log.d(TAG, "UnknownHostException");
                return;
            } catch (Exception e) {
                // Error handling for any other errors
                Log.d(TAG, "some other error: " + e.getMessage());
                Log.e(TAG, "error", e);
                return;
            }
        }

        @Override
        public void run() {
            // We need to open and close these ports as we switch between 1 and 2.
            while (true) {
                if (oscPortOut == null) {
                    try {
                        // Connect to some IP address and port
                        Log.d(TAG, "opening OSC port to send to: " + lxIP);
                        oscPortOut = new OSCPortOut(InetAddress.getByName(lxIP), lxPort);
                        Log.d(TAG, "port opened.");
                    } catch (UnknownHostException unhex) {
                        Log.e(TAG, "UnknownHostException: " + unhex.getMessage());
                    } catch (SocketException sex) {
                        Log.e(TAG, "SocketException: " + sex.getMessage());
                    }
                }
                if (oscPortOut != null) {
                    synchronized(messageQueue) {
                        if (!messageQueue.isEmpty()) {
                            QueueMsg qMsg = messageQueue.remove();
                            if (qMsg.msgType == 0) {
                                LeafPos leafPos = qMsg.leafPos;
                                List<Object> args = new ArrayList<Object>();
                                args.add("" + leafPos.runNum);
                                args.add("" + leafPos.leafNum);
                                args.add("" + leafPos.pos.x);
                                args.add("" + leafPos.pos.y);
                                args.add("" + leafPos.pos.z);
                                OSCMessage message = new OSCMessage("/ntree/leafpos", args);
                                sendOscMsg(message);
                            } else if (qMsg.msgType == 1) {
                                // Request LX send us leaf positions for a run
                                List<Object> args = new ArrayList<Object>();
                                args.add(myIP);
                                args.add("" + qMsg.runNum);
                                OSCMessage message = new OSCMessage("/ntree/sendrun", args);
                                sendOscMsg(message);
                            } else if (qMsg.msgType == 2) {
                                // txt msg update
                                List<Object> args = new ArrayList<Object>();
                                args.add(qMsg.txtMessage);
                                OSCMessage message = new OSCMessage("/ntree/txtmsg", args);
                                sendOscMsg(message);
                            } else if (qMsg.msgType == 3) {
                                // Set the selected leaf in LX
                                List<Object> args = new ArrayList<Object>();
                                args.add("" + qMsg.runNum);
                                args.add("" + qMsg.leafNum);
                                OSCMessage message = new OSCMessage("/ntree/leafsel", args);
                                sendOscMsg(message);
                            } else if (qMsg.msgType == 4) {
                                // Turn on/off background leaves
                                List<Object> args = new ArrayList<Object>();
                                args.add((qMsg.bgLeaves)?"1":"0");
                                OSCMessage message = new OSCMessage("/ntree/bgleaves", args);
                                sendOscMsg(message);
                            }
                        }
                    }
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException iex) {
                    // When we switch between RAINBOW_STUDIO_ONE and RAINBOW_STUDIO_TWO, we close
                    // our oscOutputPort, set it to null, and then interrupt this thread.  This
                    // Thread should re-initialize oscOutputPort with the correct IP based on our
                    // preferences.
                }
            }

        }
    };


    private Thread oscRecvThread = new Thread() {
        @Override
        public void run() {
            try {
                Log.d(TAG, "Initializing OSC Receiver");
                oscReceiver = new OSCPortIn(myRecvPort);
                OSCListener listener = new OSCListener() {
                    public void acceptMessage(java.util.Date time, OSCMessage message) {
                        Log.d(TAG, "Message received!");
                        List<Object> args = message.getArguments();
                        Log.d(TAG, "received: " + message.getAddress());
                        Log.d(TAG, "# args=" + args.size());
                        int run =  Integer.parseInt((String)args.get(0));
                        Log.d(TAG, " run= " + run);
                        int leaf = Integer.parseInt((String)args.get(1));
                        Log.d(TAG, " leaf= " + leaf);
                        int x = Integer.parseInt((String)args.get(2));
                        Log.d(TAG, " x=" + x);
                        int y = Integer.parseInt((String)args.get(3));
                        Log.d(TAG, " y=" + y);
                        int z = Integer.parseInt((String)args.get(4));
                        Log.d(TAG, " z=" + z);
                        Log.d(TAG, "" + run + "." + leaf + "=" + x + "," + y + "," + z);
                        updateLeafPosition(run, leaf, x, y, z);
                        //gammaRedBar.setProgress((int)(100.0f * (value-1.0f)/¡™
                    }
                };
                oscReceiver.addListener("//*", listener);
                oscReceiver.startListening();
                Log.d(TAG, "Done listening!");
            } catch (SocketException sex) {
                Log.e(TAG, "SocketException receiving: " + sex.getMessage());
            }
        }
    };
}