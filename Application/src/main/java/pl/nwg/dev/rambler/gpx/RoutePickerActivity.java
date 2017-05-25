package pl.nwg.dev.rambler.gpx;

import android.Manifest;
import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.karambola.geo.Units;
import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.util.GpxUtils;

import static pl.nwg.dev.rambler.gpx.R.id.osmmap;

/**
 * Route Picker activity created by piotr on 02.05.17.
 */
public class RoutePickerActivity extends Utils
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "Picker";

    private final int MAX_ZOOM_LEVEL = 19;
    private final int MIN_ZOOM_LEVEL = 4;

    Button locationButton;
    Button fitButton;
    Button nextButton;
    Button previousButton;
    Button searchButton;

    TextView routePrompt;

    TextView routesSummary;

    private MapView mMapView;
    private IMapController mapController;

    private MapEventsReceiver mapEventsReceiver;

    private List<GeoPoint> mAllGeopoints;

    private Integer mSelectedRouteIdx = null;
    private int mRoutesListSize = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().requestFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#33ffffff")));
            actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#55ffffff")));
        }

        Context ctx = getApplicationContext();
        //important! set your user agent to prevent getting banned from the osm servers
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route_picker);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        setUpMap();

        refreshMap();
    }

    private void setUpMap() {

        mMapView = (MapView) findViewById(osmmap);

        mMapView.setTilesScaledToDpi(true);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        TilesOverlay tilesOverlay = mMapView.getOverlayManager().getTilesOverlay();
        tilesOverlay.setOvershootTileCache(tilesOverlay.getOvershootTileCache() * 2);

        mMapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
        mMapView.setMinZoomLevel(MIN_ZOOM_LEVEL);

        mMapView.setMultiTouchControls(true);

        mapController = mMapView.getController();

        mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                mSelectedRouteIdx = null;
                refreshMap(false);

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                return false;
            }
        };

        restoreMapPosition();

        setUpButtons();
        setButtonsState();
    }

    private void restoreMapPosition() {

        if (Data.sLastZoom == null && Data.sLastCenter == null && mAllGeopoints != null) {
            mMapView.zoomToBoundingBox(findBoundingBox(mAllGeopoints), true);
        } else {

            if (Data.sLastZoom == null) {
                mapController.setZoom(3);
            } else {
                mapController.setZoom(Data.sLastZoom);
            }

            if (Data.sLastCenter == null) {
                mapController.setCenter(new GeoPoint(0d, 0d));
            } else {
                mapController.setCenter(Data.sLastCenter);
            }
        }
    }

    private void refreshMap(boolean zoom_to_fit) {

        mMapView.getOverlays().clear();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mMapView.getOverlays().add(0, mapEventsOverlay);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlays().add(mScaleBarOverlay);
        // Scale bar tries to draw as 1-inch, so to put it in the top center, set x offset to
        // half screen width, minus half an inch.
        mScaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);


        /*
         * We'll create bounding box around this
         */
        mAllGeopoints = new ArrayList<>();

        final List<Route> routesList = Data.mRoutesGpx.getRoutes();

        mRoutesListSize = routesList.size();

        for(int i = 0; i < mRoutesListSize; i++) {

            final Route route = Data.mRoutesGpx.getRoutes().get(i);
            List<RoutePoint> routePoints = route.getRoutePoints();
            List<GeoPoint> geoPoints = new ArrayList<>();

            for(int j = 0; j < routePoints.size(); j++) {

                RoutePoint routePoint = routePoints.get(j);
                GeoPoint geoPoint = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());
                geoPoints.add(geoPoint);

                if (mSelectedRouteIdx == null) {
                    mAllGeopoints.add(geoPoint);
                } else {
                    if (i == mSelectedRouteIdx) {
                        mAllGeopoints.add(geoPoint);
                    }
                }
            }

            final Polyline routeOverlay = new Polyline();
            routeOverlay.setPoints(geoPoints);

            if (mSelectedRouteIdx != null) {

                if (i == mSelectedRouteIdx) {

                    routeOverlay.setColor(Color.parseColor("#0099ff"));

                } else {

                    routeOverlay.setColor(Color.parseColor("#11000000"));
                }

            } else {

                routeOverlay.setColor(typeColors[i % N_COLOURS]);
            }
            routeOverlay.setWidth(15);

            mMapView.getOverlays().add(routeOverlay);
        }

        if (mSelectedRouteIdx != null) {
            routePrompt.setText(GpxUtils.getRouteNameAnnotated(routesList.get(mSelectedRouteIdx), Units.METRIC));
            routesSummary.setText((mSelectedRouteIdx +1) + "/" + mRoutesListSize);
        }

        if(zoom_to_fit) {
            mMapView.zoomToBoundingBox(findBoundingBox(mAllGeopoints), false);
        }

        mMapView.invalidate();
        setButtonsState();
    }

    private void refreshMap() {
        refreshMap(true);
    }

    private void setUpButtons() {

        locationButton = (Button) findViewById(R.id.picker_location_button);
        locationButton.setEnabled(false);
        locationButton.getBackground().setAlpha(0);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.setZoom(18);
                mapController.setCenter(Data.sCurrentPosition);
                setButtonsState();
            }
        });

        fitButton = (Button) findViewById(R.id.picker_fit_button);
        fitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*
                if (Data.sCardinalGeoPoints != null && Data.sCardinalGeoPoints.size() > 1) {
                    mMapView.zoomToBoundingBox(findBoundingBox(Data.sCardinalGeoPoints), true);
                }
                setButtonsState();
                */
                if (mAllGeopoints != null) {
                    mMapView.zoomToBoundingBox(findBoundingBox(mAllGeopoints), true);
                }
            }
        });
        nextButton = (Button) findViewById(R.id.picker_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectedRouteIdx == null) {
                    mSelectedRouteIdx = 0;
                } else {
                    if (mSelectedRouteIdx < mRoutesListSize -1) {
                        mSelectedRouteIdx++;
                    } else {
                        mSelectedRouteIdx = 0;
                    }
                }
                refreshMap();
            }
        });
        previousButton = (Button) findViewById(R.id.picker_previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mSelectedRouteIdx == null) {
                    mSelectedRouteIdx = 0;
                } else {
                    if (mSelectedRouteIdx > 0) {
                        mSelectedRouteIdx--;
                    } else {
                        mSelectedRouteIdx = mRoutesListSize -1;
                    }
                }
                refreshMap();
            }
        });

        searchButton = (Button) findViewById(R.id.picker_search_button);

        routesSummary = (TextView) findViewById(R.id.routes_summary);

        routePrompt = (TextView) findViewById(R.id.picker_route_prompt);
    }

    private void setButtonsState() {

        if (mMapView.getProjection().getZoomLevel() < MAX_ZOOM_LEVEL) {
            nextButton.setEnabled(true);
            nextButton.getBackground().setAlpha(255);
        } else {
            nextButton.setEnabled(false);
            nextButton.getBackground().setAlpha(100);
        }

        if (mMapView.getProjection().getZoomLevel() > MIN_ZOOM_LEVEL) {
            previousButton.setEnabled(true);
            previousButton.getBackground().setAlpha(255);
        } else {
            previousButton.setEnabled(false);
            previousButton.getBackground().setAlpha(100);
        }

        /*
         * When the Route Manager main activity (picker) is ready, this button will be adding selected route
         * to Data.mRoutesGpx, and close the Creator.
         */
        if (Data.osrmRoutes != null && Data.osrmRoutes.size() > 0 && Data.sSelectedAlternative != null) {
            searchButton.setEnabled(true);
            searchButton.getBackground().setAlpha(255);
        } else {
            searchButton.setEnabled(false);
            searchButton.getBackground().setAlpha(100);
        }

    }

    public void onResume(){
        super.onResume();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        mGoogleApiClient.connect();
        restoreMapPosition();
        mSelectedRouteIdx = null;
        refreshMap(false);
    }

    @Override
    public void onLocationChanged(Location location) {

        try {

            Data.sCurrentPosition = new GeoPoint(location.getLatitude(), location.getLongitude());

            locationButton.setEnabled(true);
            locationButton.getBackground().setAlpha(255);

        } catch(Exception e) {

            locationButton.setEnabled(false);
            locationButton.getBackground().setAlpha(0);

            Log.d(TAG, "Error getting location: " + e);
        }
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnected: " + connectionHint);
        }

        try {
            LocationRequest locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(30000)
                    .setSmallestDisplacement(0);

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, locationRequest, this);
            }

        } catch (Exception e) {

            if (Log.isLoggable(TAG, Log.DEBUG)) {
                Log.d(TAG, "Error getting location: " + e);
            }
        }
    }

    @Override // GoogleApiClient.ConnectionCallbacks
    public void onConnectionSuspended(int cause) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionSuspended: " + cause);
        }
    }

    @Override // GoogleApiClient.OnConnectionFailedListener
    public void onConnectionFailed(ConnectionResult result) {
        if (Log.isLoggable(TAG, Log.DEBUG)) {
            Log.d(TAG, "onConnectionFailed: " + result);
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Data.sLastZoom = mMapView.getZoomLevel();
        Data.sLastCenter = new GeoPoint(mMapView.getMapCenter().getLatitude(), mMapView.getMapCenter().getLongitude());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_route_picker, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        /*
         * We'll enable/disable menu options here
         */
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent i;

        switch (item.getItemId()) {

            case R.id.routes_new_autorute:
                i = new Intent(RoutePickerActivity.this, RouteCreatorActivity.class);
                startActivity(i);
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }
}