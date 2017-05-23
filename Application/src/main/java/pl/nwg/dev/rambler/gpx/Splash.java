package pl.nwg.dev.rambler.gpx;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

/**
 * This activity just asks user for necessary permissions, and launches the main activity if given.
 */
public class Splash extends AppCompatActivity implements
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final String TAG = "Splash";

    private boolean mainActivityLaunched = false;

    /* Id to identify Location permission request. */
    private static final int PERMISSION_REQUEST_WRITE_STORAGE = 1;
    private static final int PERMISSION_REQUEST_ACCESS_FINE_LOCATION = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // If permissions granted, we start the main activity (shut this activity down).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startMainActivity();
            finish();
        }

        setContentView(R.layout.activity_permission_request);
    }

    public void onClickApprovePermissionRequest(View view) {
        Log.d(TAG, "onClickApprovePermissionRequest()");

        // On 23+ (M+) devices, External storage permission not granted. Request permission.

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_WRITE_STORAGE);
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSION_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    public void onClickDenyPermissionRequest(View view) {
        Log.d(TAG, "onClickDenyPermissionRequest()");
        finish();
    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        String permissionResult = "Request code: " + requestCode + ", Permissions: " + permissions
                + ", Results: " + grantResults;
        Log.d(TAG, "onRequestPermissionsResult(): " + permissionResult);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            if (!mainActivityLaunched) {
                startMainActivity();
                mainActivityLaunched = true;
            }
            finish();

        } else {

            if (requestCode == PERMISSION_REQUEST_WRITE_STORAGE &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), getString(R.string.permissions_insufficient) + " " +
                        getString(R.string.permissions_files), Toast.LENGTH_LONG).show();
            }

            if (requestCode == PERMISSION_REQUEST_ACCESS_FINE_LOCATION &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                Toast.makeText(getApplicationContext(), getString(R.string.permissions_insufficient) + " " +
                        getString(R.string.permissions_location), Toast.LENGTH_LONG).show();
            }

            finish();
        }
    }

    private void startMainActivity() {

        Log.d(TAG, "startMainActivity called");
        Intent mainActivityIntent = new Intent(this, MainActivity.class);

        startActivity(mainActivityIntent);
        finish();
    }

}
