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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

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
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.karambola.commons.collections.ListUtils;
import pt.karambola.geo.Units;
import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.predicate.RouteFilter;
import pt.karambola.gpx.util.GpxUtils;

import static pl.nwg.dev.rambler.gpx.R.id.osmmap;

/**
 * Route Picker activity created by piotr on 02.05.17.
 */
public class RoutesBrowserActivity extends Utils
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "Picker";

    private final int MAX_ZOOM_LEVEL = 19;
    private final int MIN_ZOOM_LEVEL = 4;

    private Button locationButton;
    private Button fitButton;
    private Button nextButton;
    private Button previousButton;
    private Button searchButton;

    private Button editRouteButton;

    TextView routePrompt;

    TextView routesSummary;

    private MapView mMapView;
    private IMapController mapController;

    private MapEventsReceiver mapEventsReceiver;

    private List<GeoPoint> mAllGeopoints;

    private int mFilteredRoutesNumber = 0;
    private int mAllRoutesNumber = 0;

    /**
     * View filtering
     */
    boolean enable_type;
    boolean enable_dst;
    boolean enable_age;

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
        setContentView(R.layout.activity_routes_browser);

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

                Data.sSelectedRouteIdx = null;
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

        mAllRoutesNumber = Data.mRoutesGpx.getRoutes().size();

        Data.sFilteredRoutes = ListUtils.filter(Data.mRoutesGpx.getRoutes(), Data.sViewRouteFilter);

        mFilteredRoutesNumber = Data.sFilteredRoutes.size();

        for(int i = 0; i < mFilteredRoutesNumber; i++) {

            final Route route = Data.sFilteredRoutes.get(i);

            List<RoutePoint> routePoints = route.getRoutePoints();

            int halfWayPoint = routePoints.size() / 2;

            List<GeoPoint> geoPoints = new ArrayList<>();

            for(int j = 0; j < routePoints.size(); j++) {

                RoutePoint routePoint = routePoints.get(j);
                GeoPoint geoPoint = new GeoPoint(routePoint.getLatitude(), routePoint.getLongitude());
                geoPoints.add(geoPoint);

                if (Data.sSelectedRouteIdx == null) {
                    mAllGeopoints.add(geoPoint);

                    if (j == halfWayPoint) {

                        Drawable icon = new BitmapDrawable(getResources(), makeRouteNameBitmap(this, route.getName()));

                        Marker marker = new Marker(mMapView);
                        marker.setPosition(geoPoint);
                        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                        marker.setDraggable(false);
                        marker.setIcon(icon);
                        marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                            @Override
                            public boolean onMarkerClick(Marker marker, MapView mapView) {
                                return false;
                            }
                        });

                        mMapView.getOverlays().add(marker);
                    }

                } else {

                    if (i == Data.sSelectedRouteIdx) {
                        mAllGeopoints.add(geoPoint);

                        if (j == halfWayPoint) {

                            Drawable icon = new BitmapDrawable(getResources(), makeRouteNameBitmap(this, route.getName()));

                            Marker marker = new Marker(mMapView);
                            marker.setPosition(geoPoint);
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            marker.setDraggable(false);
                            marker.setIcon(icon);
                            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                                @Override
                                public boolean onMarkerClick(Marker marker, MapView mapView) {
                                    return false;
                                }
                            });

                            mMapView.getOverlays().add(marker);
                        }
                    }
                }

            }

            final Polyline routeOverlay = new Polyline();
            routeOverlay.setPoints(geoPoints);

            if (Data.sSelectedRouteIdx != null) {

                if (i == Data.sSelectedRouteIdx) {

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

        if (Data.sSelectedRouteIdx != null) {
            routePrompt.setText(GpxUtils.getRouteNameAnnotated(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx), Units.METRIC));
        } else {
            routePrompt.setText(getResources().getString(R.string.route_edit_prompt));
        }
        routesSummary.setText(String.format(getResources().getString(R.string.x_of_y_routes), mFilteredRoutesNumber, mAllRoutesNumber));

        if(zoom_to_fit && mAllGeopoints.size() > 0) {
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

                if(mAllGeopoints != null && mAllGeopoints.size() > 0) {
                    mMapView.zoomToBoundingBox(findBoundingBox(mAllGeopoints), false);
                } else {
                    Toast.makeText(getApplicationContext(), getString(R.string.no_routes_in_view), Toast.LENGTH_SHORT).show();
                }
            }
        });
        fitButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                displayFilterDialog();
                return false;
            }
        });
        nextButton = (Button) findViewById(R.id.picker_next_button);
        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Data.sSelectedRouteIdx == null) {
                    Data.sSelectedRouteIdx = 0;
                } else {
                    if (Data.sSelectedRouteIdx < mFilteredRoutesNumber -1) {
                        Data.sSelectedRouteIdx++;
                    } else {
                        Data.sSelectedRouteIdx = 0;
                    }
                }
                refreshMap();
            }
        });
        previousButton = (Button) findViewById(R.id.picker_previous_button);
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (Data.sSelectedRouteIdx == null) {
                    Data.sSelectedRouteIdx = 0;
                } else {
                    if (Data.sSelectedRouteIdx > 0) {
                        Data.sSelectedRouteIdx--;
                    } else {
                        Data.sSelectedRouteIdx = mFilteredRoutesNumber -1;
                    }
                }
                refreshMap();
            }
        });

        searchButton = (Button) findViewById(R.id.picker_search_button);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySelectRouteDialog();
            }
        });

        routesSummary = (TextView) findViewById(R.id.routes_summary);

        routePrompt = (TextView) findViewById(R.id.picker_route_prompt);

        final TextView copyright = (TextView) findViewById(R.id.copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setButtonsState() {

        if (mFilteredRoutesNumber > 1) {
            nextButton.setEnabled(true);
            nextButton.getBackground().setAlpha(255);
        } else {
            nextButton.setEnabled(false);
            nextButton.getBackground().setAlpha(100);
        }

        if (mFilteredRoutesNumber > 1) {
            previousButton.setEnabled(true);
            previousButton.getBackground().setAlpha(255);
        } else {
            previousButton.setEnabled(false);
            previousButton.getBackground().setAlpha(100);
        }

        /*
         * Open a dialog to select a route by name
         */
        if (Data.sFilteredRoutes != null && Data.sFilteredRoutes.size() > 0) {
            searchButton.setEnabled(true);
            searchButton.getBackground().setAlpha(255);
        } else {
            searchButton.setEnabled(false);
            searchButton.getBackground().setAlpha(100);
        }

        if (Data.sViewRouteFilter.isEnabled()) {
            fitButton.setBackgroundResource(R.drawable.map_filter_on);
        } else {
            fitButton.setBackgroundResource(R.drawable.map_fit);
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
        getMenuInflater().inflate(R.menu.menu_routes_browser, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        /*
         * We'll enable/disable menu options here
         */
        menu.findItem(R.id.routes_delete_selected).setEnabled(Data.sSelectedRouteIdx != null);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Intent i;

        switch (item.getItemId()) {

            case R.id.routes_new_autorute:
                i = new Intent(RoutesBrowserActivity.this, RouteCreatorActivity.class);
                startActivity(i);
                return true;

            case R.id.routes_delete_selected:

                final Route route = Data.sFilteredRoutes.get(Data.sSelectedRouteIdx);
                String deleteMessage;
                if (route.getName() != null) {
                    String deleteMessageFormat = getResources().getString(R.string.dialog_delete_route_message);
                    deleteMessage = String.format(deleteMessageFormat, route.getName());
                } else {
                    deleteMessage = getResources().getString(R.string.about_to_delete_route);
                }
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.map_warning)
                        .setTitle(R.string.dialog_delete_route)
                        .setMessage(deleteMessage)
                        .setPositiveButton(R.string.dialog_delete, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                Data.mRoutesGpx.removeRoute(route);
                                Data.sSelectedRouteIdx = null;
                                refreshMap();

                            }

                        })
                        .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                            }

                        })
                        .show();

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void displayFilterDialog() {

        final List<String> rteTypes = GpxUtils.getDistinctRouteTypes(Data.mRoutesGpx.getRoutes());

        // just used to build the multichoice selector
        final String[] all_types = rteTypes.toArray(new String[rteTypes.size()]);

        final boolean[] selections = new boolean[rteTypes.size()];

        for (int i = 0; i < rteTypes.size(); i++) {
            if (Data.sViewRouteFilter.getAcceptedTypes().contains(rteTypes.get(i))) {
                selections[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.routes_filter_dialog, null);

        final EditText dstStartMin = (EditText) layout.findViewById(R.id.route_distance_min);
        if (Data.sViewRouteFilter.getDistanceMin() != null) {
            double dst_min = Data.sViewRouteFilter.getDistanceMin() / 1000;
            dstStartMin.setText(String.valueOf(dst_min));
        } else {
            dstStartMin.setText("");
        }

        final EditText dstStartMax = (EditText) layout.findViewById(R.id.route_distance_max);
        if (Data.sViewRouteFilter.getDistanceMax() != null) {
            double dst_max = Data.sViewRouteFilter.getDistanceMax() / 1000;
            dstStartMax.setText(String.valueOf(dst_max));
        } else {
            dstStartMax.setText("");
        }

        final EditText lengthMin = (EditText) layout.findViewById(R.id.route_length_min);
        if (Data.sViewRouteFilter.getLengthMin() != null) {
            Double length_min = Data.sViewRouteFilter.getLengthMin() / 1000;
            lengthMin.setText(String.valueOf(length_min));
        } else {
            lengthMin.setText("");
        }

        final EditText lengthMax = (EditText) layout.findViewById(R.id.route_length_max);
        if (Data.sViewRouteFilter.getLengthMax() != null) {
            Double length_max = Data.sViewRouteFilter.getLengthMax() / 1000;
            lengthMax.setText(String.valueOf(length_max));
        } else {
            lengthMax.setText("");
        }

        final CheckBox dstCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_distance_on);

        if (Data.sCurrentPosition == null) {
            dstCheckBox.setText(getResources().getString(R.string.location_unavailable));
            dstCheckBox.setChecked(false);
            dstCheckBox.setEnabled(false);
            enable_dst = false;
        } else {

            dstCheckBox.setChecked(Data.sViewRouteFilter.isDistanceFilterEnabled());
            dstCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    enable_dst = isChecked;
                    dstStartMin.setEnabled(isChecked);
                    dstStartMax.setEnabled(isChecked);
                }
            });
        }

        final CheckBox lengthCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_length_on);
        lengthCheckBox.setChecked(Data.sViewRouteFilter.isLengthFilterEnabled());
        lengthCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enable_age = isChecked;
                lengthMin.setEnabled(isChecked);
                lengthMax.setEnabled(isChecked);
            }
        });

        String dialogTitle = getResources().getString(R.string.dialog_routes_filter_title);
        String okText = getResources().getString(R.string.dialog_filter_set);
        String clearText = getResources().getString(R.string.dialog_filter_clear);
        String cancelText = getResources().getString(R.string.dialog_cancel);
        builder.setTitle(dialogTitle)
                .setIcon(R.drawable.map_filter)
                .setCancelable(true)
                .setNegativeButton(cancelText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNeutralButton(clearText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        // clear current filters
                        Data.sViewRouteFilter = new RouteFilter();
                        Data.sSelectedRouteIdx = null;
                        refreshMap();

                    }
                })
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Data.sSelectedRouteTypes = new ArrayList<>();

                        for (int i = 0; i < selections.length; i++) {
                            if (selections[i]) {
                                Data.sSelectedRouteTypes.add(rteTypes.get(i));
                            }
                        }

                        if (!dstStartMin.getText().toString().isEmpty()) {
                            Data.sDstStartMinValue = Double.valueOf(dstStartMin.getText().toString()) * 1000;
                        } else {
                            Data.sDstStartMinValue = null;
                        }

                        if (!dstStartMax.getText().toString().isEmpty()) {
                            Data.sDstStartMaxValue = Double.valueOf(dstStartMax.getText().toString()) * 1000;
                        } else {
                            Data.sDstStartMaxValue = null;
                        }

                        if (!lengthMin.getText().toString().isEmpty()) {
                            Data.sLengthMinValue = Double.valueOf(lengthMin.getText().toString()) * 1000;
                        } else {
                            Data.sLengthMinValue = null;
                        }

                        if (!lengthMax.getText().toString().isEmpty()) {
                            Data.sLengthMaxValue = Double.valueOf(lengthMax.getText().toString()) * 1000;
                        } else {
                            Data.sLengthMaxValue = null;
                        }

                        final CheckBox typeCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_types_on);
                        if (typeCheckBox.isChecked()) {
                            Data.sViewRouteFilter.enableTypeFilter(Data.sSelectedRouteTypes);
                        } else {
                            Data.sViewRouteFilter.disableTypeFilter();
                        }

                        final CheckBox dstCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_distance_on);
                        if (dstCheckBox.isChecked()) {
                            Data.sViewRouteFilter.enableDistanceFilter(Data.sCurrentPosition.getLatitude(), Data.sCurrentPosition.getLongitude(),
                                    Data.sCurrentPosition.getAltitude(), Data.sDstStartMinValue, Data.sDstStartMaxValue);

                        } else {
                            Data.sViewRouteFilter.disableDistanceFilter();
                        }

                        final CheckBox lengthCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_length_on);
                        if (lengthCheckBox.isChecked()) {
                            Data.sViewRouteFilter.enableLengthFilter(Data.sLengthMinValue, Data.sLengthMaxValue);
                        } else {
                            Data.sViewRouteFilter.disableLengthFilter();
                        }

                        Data.sSelectedRouteIdx = null;

                        refreshMap(true);

                    }
                });

        builder.setMultiChoiceItems(all_types, selections, new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1, boolean arg2) {

                selections[arg1] = arg2;

                final CheckBox typeCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_types_on);

                int selected_types_counter = 0;
                for (int i = 0; i < selections.length; i++) {
                    if (selections[i]) {
                        selected_types_counter++;
                    }
                }

                String display = getString(R.string.dialog_filter_type) + " " + String.format(getString(R.string.dialog_types_of_types), selected_types_counter, all_types.length);
                typeCheckBox.setText(display);

            }
        });
        builder.setView(layout);

        final AlertDialog alert = builder.create();

        alert.show();

        dstStartMax.setEnabled(Data.sViewRouteFilter.isDistanceFilterEnabled());
        dstStartMin.setEnabled(Data.sViewRouteFilter.isDistanceFilterEnabled());

        lengthMin.setEnabled(Data.sViewRouteFilter.isLengthFilterEnabled());
        lengthMax.setEnabled(Data.sViewRouteFilter.isLengthFilterEnabled());

        alert.getListView().setEnabled(Data.sViewRouteFilter.isTypeFilterEnabled());

        final CheckBox typeCheckBox = (CheckBox) layout.findViewById(R.id.route_filter_types_on);
        typeCheckBox.setChecked(Data.sViewRouteFilter.isTypeFilterEnabled());

        int selected_types_counter = 0;
        for (int i = 0; i < selections.length; i++) {
            if (selections[i]) {
                selected_types_counter++;
            }
        }

        String display = getString(R.string.dialog_filter_type) + " " + String.format(getString(R.string.dialog_types_of_types), selected_types_counter, all_types.length);
        typeCheckBox.setText(display);

        typeCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enable_type = isChecked;
                if (isChecked) {
                    alert.getListView().setEnabled(true);
                    alert.getListView().setAlpha(1f);
                } else {
                    alert.getListView().setEnabled(false);
                    alert.getListView().setAlpha(0.5f);
                }
            }
        });
    }

    private void displaySelectRouteDialog() {

        if (Data.mRoutesGpx.getRoutes().size() == 0 || Data.sFilteredRoutes.size() == 0) {

            Toast.makeText(getApplicationContext(), getResources().getString(R.string.no_routes_memory), Toast.LENGTH_LONG).show();
            return;
        }

        Route[] sortedRoutesArray = new Route[Data.sFilteredRoutes.size()];
        sortedRoutesArray = Data.sFilteredRoutes.toArray(sortedRoutesArray);

        Arrays.sort(sortedRoutesArray, Data.rteComparator);

        List<String> gpxRteDisplayNames = new ArrayList<>();

        for (Route route : sortedRoutesArray) {

            gpxRteDisplayNames.add(GpxUtils.getRouteNameAnnotated(route, Data.sUnitsInUse));

        }

        final List<Route> sortedRoutes = new ArrayList<>(Arrays.asList(sortedRoutesArray));

        List<String> allNames = new ArrayList<>();
        allNames.addAll(gpxRteDisplayNames);

        String[] menu_entries = new String[allNames.size()];
        menu_entries = allNames.toArray(menu_entries);

        final int idxOfSelectedRoute;

        if (Data.sSelectedRouteIdx != null) {
            /*
             * This is a little bit insane:
             * find the route in the sorted (by Data.rteComparator) array
             * by its index on the filtered view list, ughhh...
             */
            idxOfSelectedRoute = sortedRoutes.indexOf(Data.sFilteredRoutes.get(Data.sSelectedRouteIdx));

        } else {

            idxOfSelectedRoute = -1;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.CustomDialog);

        builder.setCancelable(true)
                .setPositiveButton(getResources().getString(R.string.quit_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        refreshMap();

                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_edit), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        /* todo this will need entirely new code
                        Data.editRouteHereCenter = mMap.getCameraPosition().target;
                        Data.editRouteZoom = mMap.getCameraPosition().zoom;

                        displayEditDialog(Data.pickedRoute);
                        */

                    }
                });

        builder.setSingleChoiceItems(menu_entries, idxOfSelectedRoute, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                Route pickedRoute = sortedRoutes.get(which);

                Data.sSelectedRouteIdx = Data.sFilteredRoutes.indexOf(pickedRoute);

                editRouteButton.setEnabled(Data.sSelectedRouteIdx != null);

                refreshMap(true);

            }
        });

        AlertDialog alert = builder.create();

        alert.show();

        editRouteButton = alert.getButton(AlertDialog.BUTTON_NEUTRAL);
        editRouteButton.setEnabled(Data.pickedRoute != null);

        int width = (int)(getResources().getDisplayMetrics().widthPixels*0.90);
        int height = (int)(getResources().getDisplayMetrics().heightPixels*0.90);

        alert.getWindow().setLayout(width, height);

    }
}