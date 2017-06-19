package pl.nwg.dev.rambler.gpx;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.karambola.commons.collections.ListUtils;
import pt.karambola.geo.Units;
import pt.karambola.gpx.beans.Point;
import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.util.GpxUtils;

import static pl.nwg.dev.rambler.gpx.R.id.osmmap;

/**
 * Route Creator activity created by piotr on 02.05.17.
 */
public class RouteCreatorActivity extends Utils
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "Creator";

    private Map<Marker,GeoPoint> markerToCardinalWaypoint;

    private final int MAX_ZOOM_LEVEL = 19;
    private final int MIN_ZOOM_LEVEL = 4;

    /**
     * Routing profile to be used in the OSRM API request
     */
    private final String MODE_CAR = "driving";
    private final String MODE_BIKE = "cycling";
    private final String MODE_FOOT = "foot";

    Button locationButton;
    Button pencilButton;
    Button fitButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button modeButton;
    Button alternativesButton;
    Button saveButton;

    TextView routePrompt;

    private MapView mMapView;
    private IMapController mapController;

    private MapEventsReceiver mapEventsReceiver;

    private MyLocationNewOverlay mLocationOverlay;

    private RotationGestureOverlay mRotationGestureOverlay;

    private Route selectedOsrmRoute;

    private Map<Marker,Point> markerToPoi;

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

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));
        setContentView(R.layout.activity_route_creator);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (Data.sCardinalGeoPoints == null) {
            Data.sCardinalGeoPoints = new ArrayList<>();
        }

        if (Data.sRoutingProfile == null) {
            Data.sRoutingProfile = MODE_CAR;
        }

        Data.sSelectedAlternative = null;

        setUpMap();

        refreshMap();
    }

    private void setUpMap() {

        mMapView = (MapView) findViewById(osmmap);

        mMapView.setTilesScaledToDpi(true);

        mMapView.setTileSource(TileSourceFactory.MAPNIK);

        TilesOverlay tilesOverlay = mMapView.getOverlayManager().getTilesOverlay();
        tilesOverlay.setOvershootTileCache(tilesOverlay.getOvershootTileCache() * 2);

        mLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(this),mMapView);
        mLocationOverlay.enableMyLocation();

        mRotationGestureOverlay = new RotationGestureOverlay(mMapView);
        mRotationGestureOverlay.setEnabled(true);

        mMapView.setMaxZoomLevel(MAX_ZOOM_LEVEL);
        mMapView.setMinZoomLevel(MIN_ZOOM_LEVEL);

        mMapView.setMultiTouchControls(true);

        mapController = mMapView.getController();

        mapEventsReceiver = new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {

                return false;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {

                Data.sCardinalGeoPoints.add(new GeoPoint(p));
                clearRoutes();
                refreshMap();
                return false;
            }
        };

        restoreMapPosition();

        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {

                mMapDragged = true;

                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {

                mMapDragged = true;

                return false;
            }
        });

        setUpButtons();
        setButtonsState();
    }

    private void restoreMapPosition() {

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

    private void refreshMap() {

        mMapView.getOverlays().clear();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mMapView.getOverlays().add(0, mapEventsOverlay);

        mMapView.getOverlays().add(mLocationOverlay);

        mMapView.getOverlays().add(this.mRotationGestureOverlay);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlays().add(mScaleBarOverlay);

        mScaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);

        if (showPoi) {
            drawPoi();
        }

        Polyline routeOverlay = new Polyline();

        if (Data.osrmRoutes == null || Data.osrmRoutes.size() == 0) {

            routeOverlay.setColor(Color.parseColor("#006666"));
            routeOverlay.setPoints(Data.sCardinalGeoPoints);

            routePrompt.setText(String.format(getResources().getString(R.string.map_prompt_route), Data.sCardinalGeoPoints.size()));

        } else {

            List<GeoPoint> geoPoints = new ArrayList<>();

            if (Data.sSelectedAlternative == null) {
                Data.sSelectedAlternative = 0;
            }
            selectedOsrmRoute = Data.osrmRoutes.get(Data.sSelectedAlternative);
            for (int i = 0; i < selectedOsrmRoute.getRoutePoints().size(); i++) {
                RoutePoint routePoint = selectedOsrmRoute.getRoutePoints().get(i);
                geoPoints.add(new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude()));
            }
            routeOverlay.setColor(Color.parseColor("#0066ff"));
            routeOverlay.setPoints(geoPoints);
            routePrompt.setText(GpxUtils.getRouteNameAnnotated(selectedOsrmRoute, Units.METRIC));
        }

        mMapView.getOverlays().add(routeOverlay);

        markerToCardinalWaypoint = new HashMap<>();

        for (int i= 0; i < Data.sCardinalGeoPoints.size(); i++) {

            GeoPoint geoPoint = Data.sCardinalGeoPoints.get(i);

            Drawable icon = new BitmapDrawable(getResources(), makeMarkerBitmap(this, String.valueOf(i)));

            Marker marker = new Marker(mMapView);
            marker.setPosition(geoPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setDraggable(true);
            marker.setIcon(icon);

            markerToCardinalWaypoint.put(marker, geoPoint);
            mMapView.getOverlays().add(marker);

            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {

                    displayWaypointDialog(markerToCardinalWaypoint.get(marker));
                    return false;
                }
            });
            marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {

                }

                @Override
                public void onMarkerDragEnd(Marker marker) {
                    GeoPoint dragged = markerToCardinalWaypoint.get(marker);
                    dragged.setCoords(marker.getPosition().getLatitude(), marker.getPosition().getLongitude());

                    clearRoutes();
                    refreshMap();
                }

                @Override
                public void onMarkerDragStart(Marker marker) {

                }
            });

        }
        mMapView.invalidate();
        setButtonsState();
    }

    private void drawPoi() {

        BoundingBox mMapViewBoundingBox = mMapView.getBoundingBox();

        Data.sFilteredPoi = ListUtils.filter(Data.sPoiGpx.getPoints(), Data.sViewPoiFilter);

        /*
         * Let's assign a color to each existing POI type
         */
        List<String> wptTypes = GpxUtils.getDistinctPointTypes(Data.sFilteredPoi);

        Map<String,Integer>	wptTypeColourMap = new HashMap<>();
        int colourIdx = 0;
        for (String wptType: wptTypes) {
            wptTypeColourMap.put(wptType, typeColors[colourIdx++ % N_COLOURS]);
        }

        markerToPoi = new HashMap<>();

        for (Point poi : Data.sFilteredPoi) {

            GeoPoint markerPosition = new GeoPoint(poi.getLatitude(), poi.getLongitude());

            String displayName;
            if(poi.getName() != null && !poi.getName().isEmpty()) {
                displayName = poi.getName();
            } else {
                displayName = String.valueOf(Data.sFilteredPoi.indexOf(poi));
            }

            /*
             * Use the color from the map if the POI has a type defined.
             * If not - paint in grey.
             */
            int color;
            if (poi.getType() == null) {
                color = Color.parseColor("#999999");
            } else {
                color = wptTypeColourMap.get(poi.getType());
            }
            Drawable icon = new BitmapDrawable(getResources(), makeMarkerBitmap(this, displayName, color));

            Marker marker = new Marker(mMapView);
            marker.setPosition(markerPosition);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            marker.setDraggable(false);
            marker.setIcon(icon);

            markerToPoi.put(marker, poi);

            if (mMapViewBoundingBox.contains(markerPosition)) {
                mMapView.getOverlays().add(marker);
            }

            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {

                    addFromPoi(markerToPoi.get(marker));
                    return false;
                }
            });
        }
    }

    private void setUpButtons() {

        locationButton = (Button) findViewById(R.id.location_button);
        locationButton.setEnabled(false);
        locationButton.getBackground().setAlpha(0);
        locationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mapController.setZoom(18);
                mapController.setCenter(Data.sCurrentPosition);
                refreshMap();
                setButtonsState();
            }
        });

        pencilButton = (Button) findViewById(R.id.pencil_button);
        pencilButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askOsrm(Data.sCardinalGeoPoints);
            }
        });

        fitButton = (Button) findViewById(R.id.fit_button);
        fitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Data.sCardinalGeoPoints != null && Data.sCardinalGeoPoints.size() > 1) {
                    mMapView.zoomToBoundingBox(findBoundingBox(Data.sCardinalGeoPoints), false);
                }
                refreshMap();
                setButtonsState();
            }
        });
        zoomInButton = (Button) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() +1);
                refreshMap();
                setButtonsState();
            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() -1);
                refreshMap();
                setButtonsState();
            }
        });
        modeButton = (Button) findViewById(R.id.mode_button);
        modeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(Data.sRoutingProfile) {
                    case MODE_CAR:
                        Data.sRoutingProfile =  MODE_BIKE;
                        modeButton.setBackgroundResource(R.drawable.button_cycling);
                        break;
                    case MODE_BIKE:
                        Data.sRoutingProfile = MODE_FOOT;
                        modeButton.setBackgroundResource(R.drawable.button_walking);
                        break;
                    case MODE_FOOT:
                        Data.sRoutingProfile = MODE_CAR;
                        modeButton.setBackgroundResource(R.drawable.button_driving);
                        break;
                }
            }
        });
        alternativesButton = (Button) findViewById(R.id.alternatives_button);
        alternativesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Data.sSelectedAlternative < Data.sAlternativesNumber - 1) {
                    Data.sSelectedAlternative++;
                } else {
                    Data.sSelectedAlternative = 0;
                }
                refreshMap();
            }
        });
        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (selectedOsrmRoute != null) {
                    Data.sRoutesGpx.addRoute(selectedOsrmRoute);
                    clearRoutes();
                    Data.sCardinalGeoPoints = new ArrayList<>();

                    Intent i = new Intent(RouteCreatorActivity.this, RoutesBrowserActivity.class);
                    Data.sSelectedRouteIdx = Data.sRoutesGpx.getRoutes().indexOf(selectedOsrmRoute);
                    setResult(Data.NEW_ROUTE_ADDED, i);

                    finish();
                }
            }
        });

        routePrompt = (TextView) findViewById(R.id.route_prompt);

        final TextView copyright = (TextView) findViewById(R.id.copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setButtonsState() {

        if (Data.sCardinalGeoPoints != null && Data.sCardinalGeoPoints.size() > 1) {
            pencilButton.setEnabled(true);
            pencilButton.getBackground().setAlpha(255);
        } else {
            pencilButton.setEnabled(false);
            pencilButton.getBackground().setAlpha(100);
        }

        if (Data.sSelectedAlternative != null) {
            alternativesButton.setText((Data.sSelectedAlternative + 1) + "/" + Data.sAlternativesNumber);
            if (Data.sAlternativesNumber > 1) {
                alternativesButton.setEnabled(true);
                alternativesButton.getBackground().setAlpha(255);
            } else {
                alternativesButton.setEnabled(false);
                alternativesButton.getBackground().setAlpha(100);
            }
        } else {
            alternativesButton.setText("0/0");
            alternativesButton.setEnabled(false);
            alternativesButton.getBackground().setAlpha(100);
        }

        if (mMapView.getProjection().getZoomLevel() < MAX_ZOOM_LEVEL) {
            zoomInButton.setEnabled(true);
            zoomInButton.getBackground().setAlpha(255);
        } else {
            zoomInButton.setEnabled(false);
            zoomInButton.getBackground().setAlpha(100);
        }

        if (mMapView.getProjection().getZoomLevel() > MIN_ZOOM_LEVEL) {
            zoomOutButton.setEnabled(true);
            zoomOutButton.getBackground().setAlpha(255);
        } else {
            zoomOutButton.setEnabled(false);
            zoomOutButton.getBackground().setAlpha(100);
        }

        /*
         * When the Route Manager main activity (picker) is ready, this button will be adding selected route
         * to Data.sRoutesGpx, and close the Creator.
         */
        if (Data.osrmRoutes != null && Data.osrmRoutes.size() > 0 && Data.sSelectedAlternative != null) {
            saveButton.setEnabled(true);
            saveButton.getBackground().setAlpha(255);
        } else {
            saveButton.setEnabled(false);
            saveButton.getBackground().setAlpha(100);
        }

    }

    private void displayWaypointDialog(final GeoPoint geoPoint) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(getResources().getString(R.string.waypoint_delete))
                .setIcon(R.drawable.map_question)
                .setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.dialog_delete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Data.sCardinalGeoPoints.remove(geoPoint);

                        clearRoutes();
                        refreshMap();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog alert = builder.create();
        alert.show();

    }

    public void addFromPoi(final Point poi) {

        /*
         * The issue to workaround: a marker drawn over another one (in the same place) does not
         * cover it for some mysterious reason, so OnMarkerClickListener will be executed for both
         * of them (Why?!). Let's check if the just clicked POI coordinates already exist as
         * a cardinal way point. If so - let's skip this dialog.
         * NOTE: this will not work for overlapping markers of slightly different coordinates.
         */
        boolean pointExists = false;
        for (GeoPoint geoPoint : Data.sCardinalGeoPoints) {
            if (geoPoint.getLatitude() == poi.getLatitude() && geoPoint.getLongitude() == poi.getLongitude()) {
                pointExists = true;
                break;
            }
        }
        if (pointExists) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String titleText = getResources().getString(R.string.dialog_poi2wpt);
        String appendText = getResources().getString(R.string.dialog_append);
        String cancelText = getResources().getString(R.string.dialog_cancel);

        String messageText = "POI-> Way point";
        if (poi.getName() != null) {
            messageText = String.format(getString(R.string.dialog_insert_message), poi.getName());
        }

        builder.setCancelable(true)
                .setTitle(titleText)
                .setMessage(messageText)
                .setIcon(R.drawable.map_poi)
                .setPositiveButton(appendText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        GeoPoint geoPoint = new GeoPoint(poi.getLatitude(), poi.getLongitude());

                        Data.sCardinalGeoPoints.add(geoPoint);
                        refreshMap();

                    }
                })
                .setNegativeButton(cancelText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    public void clearRoutes() {
        Data.osrmRoutes = null;
        Data.sAlternativesNumber = 0;
        Data.sSelectedAlternative = null;
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

        loadSettings();
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

        saveSettings();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.menu_route_creator, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        menu.findItem(R.id.show_poi).setChecked(showPoi);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {

            case R.id.show_poi:

                showPoi = !showPoi;
                saveSettings();
                refreshMap();
                return true;


            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * The osmbonuspack API uses their own RoadManager class, as described here:
     * https://github.com/MKergall/osmbonuspack/wiki/Tutorial_1
     * However, the class uses the Mapquest API, which requires the user registration for the API key,
     * and enforces quite restrictive usage conditions.
     * We'll use the Open Street Routing Machine demo server instead.
     * Please read the API usage policy here:
     * https://github.com/Project-OSRM/osrm-backend/wiki/Api-usage-policy
     */
    private void askOsrm(final List<GeoPoint> waypoints) {

        String polyline = encode(waypoints);
        try {
            polyline = URLEncoder.encode(polyline, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            polyline = "";
        }
        final String uri = "http://router.project-osrm.org/route/v1/" + Data.sRoutingProfile + "/polyline(" + polyline + ")?alternatives=true&overview=full";

        Log.d(TAG, uri);

        AsyncTask<Void, Void, Boolean> getHttpRequest = new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected void onPreExecute() {

            }

            @Override
            protected Boolean doInBackground(Void... params) {

                responseString = null;

                HttpResponse response = null;
                try {
                    HttpClient client = new DefaultHttpClient();
                    HttpGet request = new HttpGet();
                    request.setURI(new URI(uri));
                    response = client.execute(request);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (ClientProtocolException e) {
                    Log.d(TAG, "ClientProtocolException: " + e);
                } catch (IOException e) {
                    Log.d(TAG, "IOException: " + e);
                }

                if (response != null) {
                    try {
                        InputStream inputStream = response.getEntity().getContent();
                        responseString = convertStreamToString(inputStream);
                    } catch (IOException e) {
                        responseString = null;
                        e.printStackTrace();
                    }
                }
                return true;
            }

            @Override
            protected void onPostExecute(Boolean result) {

                if (responseString != null && !responseString.isEmpty()) {

                    Log.d(TAG, "Response: " + responseString);

                    parseOsrmResponse(responseString); // and store results in List<Route> Data.osrmRoutes

                    refreshMap();

                } else {

                    Toast.makeText(getApplicationContext(), getString(R.string.no_osrm_reponse), Toast.LENGTH_SHORT).show();
                }
            }
        };
        getHttpRequest.execute();
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {

            case MotionEvent.ACTION_UP:

                if (mMapDragged) {

                    mMapDragged = false;

                    refreshMap();
                }
                break;
        }
        return true;

    }
}