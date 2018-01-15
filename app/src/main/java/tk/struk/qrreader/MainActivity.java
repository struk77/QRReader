package tk.struk.qrreader;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String cameraPerm = Manifest.permission.CAMERA;

    private final String LOGTAG = getClass().getSimpleName();

    // UI
    private TextView txtResult, resultView;

    // QREader
    private SurfaceView mySurfaceView;
    private QREader qrEader;

    boolean hasCameraPermission = false;
    private String lastScannedData = "";

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        hasCameraPermission = RuntimePermissionUtil.checkPermissonGranted(this, cameraPerm);

        txtResult = findViewById(R.id.scanText);
        resultView = findViewById(R.id.resultView);
        resultView.setMovementMethod(ScrollingMovementMethod.getInstance());


        final Button stateBtn = findViewById(R.id.btn_start_stop);
        // change of reader state in dynamic
        stateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (qrEader.isCameraRunning()) {
                    stateBtn.setText(R.string.start_qr_reader);
                    qrEader.stop();
                } else {
                    stateBtn.setText(R.string.stop_qr_reader);
                    qrEader.start();
                }
            }
        });

        stateBtn.setVisibility(View.VISIBLE);

        Button restartbtn = findViewById(R.id.btn_restart_activity);
        restartbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                restartActivity();
            }
        });

        // Setup SurfaceView
        // -----------------
        mySurfaceView = findViewById(R.id.camera_view);

        if (hasCameraPermission) {
            // Setup QREader
            setupQREader();
        } else {
            RuntimePermissionUtil.requestPermission(MainActivity.this, cameraPerm, 100);
        }
    }

    void restartActivity() {
        startActivity(new Intent(MainActivity.this, MainActivity.class));
        finish();
    }

    void setupQREader() {
        // Init QREader
        // ------------
        qrEader = new QREader.Builder(this, mySurfaceView, new QRDataListener() {
            @Override
            public void onDetected(final String data) {
                if (data.equals(lastScannedData)) return;
                lastScannedData = data;
                Log.d(LOGTAG, "Value : " + data);
                final long mills = 100L;
                final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                assert vibrator != null;
                vibrator.vibrate(mills);
                txtResult.post(new Runnable() {
                    @Override
                    public void run() {
                        txtResult.setAllCaps(true);
                        txtResult.setTextColor(getResources().getColor(android.R.color.black));
                        txtResult.setText(data);

                        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

                        String url = sharedPref.getString("url", "");
                        String login = sharedPref.getString("login", "");
                        String password = sharedPref.getString("password", "");
                        String list_id = sharedPref.getString("list_id", "");

                        JSONObject jsonPayload = new JSONObject();
                        try {
                            jsonPayload
                                    .put("login", login)
                                    .put("password", password)
                                    .put("list_id", list_id)
                                    .put("number", data);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        RequestQueue MyRequestQueue = Volley.newRequestQueue(getApplication());
                        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                (Request.Method.POST, url, jsonPayload, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        vibrator.vibrate(mills);
                                        String description = "", status = "";
                                        try {
                                            description = response.getString("description");
                                            status = response.getString("status");
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }
                                        if (status.equals("error")) {
                                            txtResult.setText(R.string.string_error);
                                            txtResult.setTextColor(getResources().getColor(android.R.color.holo_red_light));
                                        } else {
                                            txtResult.setText(R.string.string_ok);
                                            txtResult.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                                        }
                                        Spanned result = Html.fromHtml(description);
                                        resultView.setText(result);
                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // TODO Auto-generated method stub

                                    }
                                });


                        MyRequestQueue.add(jsObjRequest);


                    }
                });
            }
        }).facing(QREader.BACK_CAM)
                .enableAutofocus(true)
                .height(mySurfaceView.getHeight())
                .width(mySurfaceView.getWidth())
                .build();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (hasCameraPermission) {
            // Cleanup in onPause()
            // --------------------
            qrEader.releaseAndCleanup();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (hasCameraPermission) {

            // Init and Start with SurfaceView
            // -------------------------------
            qrEader.initAndStart(mySurfaceView);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull final String[] permissions,
                                           @NonNull final int[] grantResults) {
        if (requestCode == 100) {
            RuntimePermissionUtil.onRequestPermissionsResult(grantResults, new RPResultListener() {
                @Override
                public void onPermissionGranted() {
                    if ( RuntimePermissionUtil.checkPermissonGranted(MainActivity.this, cameraPerm)) {
                        restartActivity();
                    }
                }

                @Override
                public void onPermissionDenied() {
                    // do nothing
                }
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                finish();
                break;
            case R.id.action_exit:
                finish();
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
