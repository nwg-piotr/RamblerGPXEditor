package pl.nwg.dev.rambler.gpx;

import android.Manifest;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.text.InputFilter;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
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
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pt.karambola.commons.collections.ListUtils;
import pt.karambola.gpx.beans.Point;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.predicate.PointFilter;
import pt.karambola.gpx.util.GpxUtils;

import static pl.nwg.dev.rambler.gpx.R.id.osmmap;

/**
 * Route Creator activity created by piotr on 02.05.17.
 */
public class PoiActivity extends Utils
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private final String TAG = "Creator";

    private Map<Marker,Point> markerToPoi;

    private final int MAX_ZOOM_LEVEL = 19;
    private final int MIN_ZOOM_LEVEL = 4;

    Button locationButton;

    Button fitButton;
    Button zoomInButton;
    Button zoomOutButton;
    Button saveButton;

    TextView poiPrompt;

    private MapView mMapView;
    private IMapController mapController;

    private MapEventsReceiver mapEventsReceiver;

    private MyLocationNewOverlay mLocationOverlay;

    boolean enable_type;
    boolean enable_dst;
    boolean enable_age;

    Double dstMinValue = null;
    Double dstMaxValue = null;
    Integer ageMinValue = null;
    Integer ageMaxValue = null;

    List<String> selectedPOITypes;

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
        setContentView(R.layout.activity_poi);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        if (Data.sCardinalGeoPoints == null) {
            Data.sCardinalGeoPoints = new ArrayList<>();
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

                /*
                RoutePoint routePoint = new RoutePoint();
                routePoint.setLatitude(p.getLatitude());
                routePoint.setLongitude(p.getLongitude());
                Data.sCopiedRoute.addRoutePoint(routePoint);

                refreshMap();
                */
                return false;
            }
        };

        restoreMapPosition();

        mMapView.setMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                //refreshMap();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
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

        Data.sFilteredPoi = ListUtils.filter(Data.sCopiedPoiGpx.getPoints(), Data.sViewPoiFilter);

        /*
         * Let's assign a color to each existing POI type
         */
        List<String> wptTypes = GpxUtils.getDistinctPointTypes(Data.sFilteredPoi);

        Map<String,Integer>	wptTypeColourMap = new HashMap<>();
        int colourIdx = 0;
        for (String wptType: wptTypes) {
            wptTypeColourMap.put(wptType, typeColors[colourIdx++ % N_COLOURS]);
        }

        mMapView.getOverlays().clear();

        MapEventsOverlay mapEventsOverlay = new MapEventsOverlay(mapEventsReceiver);
        mMapView.getOverlays().add(0, mapEventsOverlay);

        mMapView.getOverlays().add(mLocationOverlay);

        ScaleBarOverlay mScaleBarOverlay = new ScaleBarOverlay(mMapView);
        mMapView.getOverlays().add(mScaleBarOverlay);

        mScaleBarOverlay.setScaleBarOffset(
                (int) (getResources().getDisplayMetrics().widthPixels / 2 - getResources()
                        .getDisplayMetrics().xdpi / 2), 10);

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
            marker.setDraggable(true);
            marker.setIcon(icon);

            markerToPoi.put(marker, poi);

            mMapView.getOverlays().add(marker);

            marker.setOnMarkerDragListener(new Marker.OnMarkerDragListener() {
                @Override
                public void onMarkerDrag(Marker marker) {}

                @Override
                public void onMarkerDragEnd(Marker marker) {

                    Point draggedPoi = markerToPoi.get(marker);
                    draggedPoi.setLatitude(marker.getPosition().getLatitude());
                    draggedPoi.setLongitude(marker.getPosition().getLongitude());

                    refreshMap();
                }

                @Override
                public void onMarkerDragStart(Marker marker) {}
            });

            marker.setOnMarkerClickListener(new Marker.OnMarkerClickListener() {
                @Override
                public boolean onMarkerClick(Marker marker, MapView mapView) {

                    displayEditDialog(markerToPoi.get(marker));
                    return false;
                }
            });
        }
        poiPrompt.setText(String.format(getResources().getString(R.string.x_of_y_poi_press),Data.sFilteredPoi.size(), Data.sCopiedPoiGpx.getPoints().size()));

        mMapView.invalidate();
        setButtonsState();
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
                setButtonsState();
            }
        });

        fitButton = (Button) findViewById(R.id.fit_button);
        fitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Data.sFilteredPoi != null && Data.sFilteredPoi.size() > 0) {
                    mMapView.zoomToBoundingBox(findBoundingBox(pointsToGeoPoints(Data.sFilteredPoi)), true);
                }
                setButtonsState();
            }
        });
        fitButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                displayFilterDialog();
                return false;
            }
        });
        zoomInButton = (Button) findViewById(R.id.zoom_in_button);
        zoomInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() +1);
                setButtonsState();
            }
        });
        zoomOutButton = (Button) findViewById(R.id.zoom_out_button);
        zoomOutButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mapController.setZoom(mMapView.getProjection().getZoomLevel() -1);
                setButtonsState();
            }
        });
        saveButton = (Button) findViewById(R.id.save_button);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!Data.sCopiedPoiGpx.getPoints().isEmpty()) {

                    Data.sPoiGpx = copyPoiGpx(Data.sCopiedPoiGpx);
                }
                Data.sCopiedPoiGpx.resetIsChanged();
                refreshMap();
            }
        });

        poiPrompt = (TextView) findViewById(R.id.route_prompt);

        final TextView copyright = (TextView) findViewById(R.id.copyright);
        copyright.setMovementMethod(LinkMovementMethod.getInstance());
    }

    private void setButtonsState() {

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

        if (Data.sCopiedPoiGpx.isChanged()) {
            saveButton.setEnabled(true);
            saveButton.getBackground().setAlpha(255);
        } else {
            saveButton.setEnabled(false);
            saveButton.getBackground().setAlpha(100);
        }

        if (Data.sViewPoiFilter.isEnabled()) {
            fitButton.setBackgroundResource(R.drawable.map_filter_on);
        } else {
            fitButton.setBackgroundResource(R.drawable.map_fit);
        }
    }

    /**
     * POI edit dialog
     */
    private void displayEditDialog(final Point currentPOI) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View poiEditLayout = inflater.inflate(R.layout.poi_edit_dialog, null);

        final EditText editName = (EditText) poiEditLayout.findViewById(R.id.poi_name_edit);
        editName.setFilters(new InputFilter[] {
                new InputFilter.LengthFilter(19)
        });
        TextView displayTime = (TextView) poiEditLayout.findViewById(R.id.poi_time);
        if (currentPOI.getTime() != null) {
            displayTime.setText(String.valueOf(currentPOI.getTime()));
        }

        final EditText editEle = (EditText) poiEditLayout.findViewById(R.id.poi_edit_altitue_edit);
        final EditText editType = (EditText) poiEditLayout.findViewById(R.id.poi_type_edit);
        final EditText editDesc = (EditText) poiEditLayout.findViewById(R.id.poi_edit_description_edit);

        final Spinner spinner = (Spinner) poiEditLayout.findViewById(R.id.type_spinner);
        final List<String> wptTypes = GpxUtils.getDistinctPointTypes(Data.sCopiedPoiGpx.getPoints());
        wptTypes.add(0, getResources().getString(R.string.type));

        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, wptTypes);

        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinner.setAdapter(dataAdapter);
        setUpSpinnerListener(spinner, editType);


        if (currentPOI.getElevation() != null) {
            editEle.setText(String.valueOf(currentPOI.getElevation()));
        }
        editName.setText(currentPOI.getName());
        if (currentPOI.getType() != null) {
            editType.setText(currentPOI.getType()) ;
        }
        if (currentPOI.getElevation() != null)  {
            editEle.setText(String.valueOf(currentPOI.getElevation()));
        }
        if (currentPOI.getDescription() != null) {
            editDesc.setText(currentPOI.getDescription());
        }

        String dialogTitle = shortenDouble(currentPOI.getLatitude()) + "," + shortenDouble(currentPOI.getLongitude());
        String okText = getResources().getString(R.string.dialog_ok);
        String deleteText = getResources().getString(R.string.dialog_delete);
        String cancelText = getResources().getString(R.string.dialog_cancel);
        builder.setTitle(dialogTitle)
                .setIcon(R.drawable.map_poi)
                .setCancelable(true)
                .setView(poiEditLayout)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (editName.getText().toString().equals("") || editName.getText().toString().isEmpty()) {
                            editName.setText(getResources().getString(R.string.unnamed));
                        }

                        String name = editName.getText().toString();
                        if (name.length() > 20) name = name.substring(0, 21);
                        currentPOI.setName(name);

                        if (!editEle.getText().toString().isEmpty()) {
                            currentPOI.setElevation(Double.valueOf(editEle.getText().toString()));
                        } else {
                            currentPOI.setElevation(null);
                        }

                        if (!editDesc.getText().toString().isEmpty()) {
                            currentPOI.setDescription(editDesc.getText().toString().trim());
                        } else {
                            currentPOI.setDescription(null);
                        }

                        if (!editType.getText().toString().isEmpty()) {
                            currentPOI.setType(editType.getText().toString().trim());
                        } else {
                            currentPOI.setType(null);
                        }

                        currentPOI.setTime(new Date());

                        if (!Data.sViewPoiFilter.evaluate(currentPOI)) {
                            Toast.makeText(getApplicationContext(), getResources().getString(R.string.warning_poi_filtered_out), Toast.LENGTH_LONG).show();
                        }

                        refreshMap();
                    }
                })
                .setNeutralButton(deleteText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Data.sCopiedPoiGpx.removePoint(currentPOI);
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

    private void setUpSpinnerListener(Spinner spinner, final EditText edit_text) {

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {

                if (pos != 0) {
                    String item = adapterView.getItemAtPosition(pos).toString();
                    edit_text.setText(item);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

    }

    private void displayFilterDialog() {

        final List<String> wptTypes = GpxUtils.getDistinctPointTypes(Data.sCopiedPoiGpx.getPoints());

        final String[] all_types = wptTypes.toArray(new String[wptTypes.size()]);

        final boolean[] selections = new boolean[wptTypes.size()];

        for (int i = 0; i < wptTypes.size(); i++) {
            if (Data.sViewPoiFilter.getAcceptedTypes().contains(wptTypes.get(i))) {
                selections[i] = true;
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View poisFilterLayout = inflater.inflate(R.layout.pois_filter_dialog, null);

        final EditText dstMin = (EditText) poisFilterLayout.findViewById(R.id.pois_distance_min);
        if (Data.sViewPoiFilter.getDistanceMin() != null) {
            double dst_min = Data.sViewPoiFilter.getDistanceMin() / 1000;
            dstMin.setText(String.valueOf(dst_min));
        } else {
            dstMin.setText("");
        }

        final EditText dstMax = (EditText) poisFilterLayout.findViewById(R.id.pois_distance_max);
        if (Data.sViewPoiFilter.getDistanceMax() != null) {
            double dst_max = Data.sViewPoiFilter.getDistanceMax() / 1000;
            dstMax.setText(String.valueOf(dst_max));
        } else {
            dstMax.setText("");
        }

        final EditText ageMin = (EditText) poisFilterLayout.findViewById(R.id.pois_age_min);
        if (Data.sViewPoiFilter.getAgeMin() != null) {
            int age_min = Data.sViewPoiFilter.getAgeMin();
            ageMin.setText(String.valueOf(age_min));
        } else {
            ageMin.setText("");
        }

        final EditText ageMax = (EditText) poisFilterLayout.findViewById(R.id.pois_age_max);
        if (Data.sViewPoiFilter.getAgeMax() != null) {
            int age_max = Data.sViewPoiFilter.getAgeMax();
            ageMax.setText(String.valueOf(age_max));
        } else {
            ageMax.setText("");
        }

        final CheckBox dstCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_distance_on);

        if (Data.sCurrentPosition == null) {
            dstCheckBox.setText(getResources().getString(R.string.location_unavailable));
            dstCheckBox.setChecked(false);
            dstCheckBox.setEnabled(false);
            enable_dst = false;
        } else {

            dstCheckBox.setChecked(Data.sViewPoiFilter.isDistanceFilterEnabled());
            dstCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    enable_dst = isChecked;
                    dstMin.setEnabled(isChecked);
                    dstMax.setEnabled(isChecked);
                }
            });
        }

        final CheckBox ageCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_age_on);
        ageCheckBox.setChecked(Data.sViewPoiFilter.isAgeFilterEnabled());
        ageCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                enable_age = isChecked;
                ageMin.setEnabled(isChecked);
                ageMax.setEnabled(isChecked);
            }
        });

        String dialogTitle = getResources().getString(R.string.dialog_pois_filter_title);
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

                        Data.sViewPoiFilter = new PointFilter();

                        refreshMap();

                    }
                })
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        selectedPOITypes = new ArrayList<>();

                        for (int i = 0; i < selections.length; i++) {
                            if (selections[i]) {
                                selectedPOITypes.add(wptTypes.get(i));
                            }
                        }

                        if (!dstMin.getText().toString().isEmpty()) {
                            dstMinValue = Double.valueOf(dstMin.getText().toString()) * 1000;
                        } else {
                            dstMinValue = null;
                        }

                        if (!dstMax.getText().toString().isEmpty()) {
                            dstMaxValue = Double.valueOf(dstMax.getText().toString()) * 1000;
                        } else {
                            dstMaxValue = null;
                        }

                        if (!ageMin.getText().toString().isEmpty()) {
                            ageMinValue = Integer.valueOf(ageMin.getText().toString());
                        } else {
                            ageMinValue = null;
                        }

                        if (!ageMax.getText().toString().isEmpty()) {
                            ageMaxValue = Integer.valueOf(ageMax.getText().toString());
                        } else {
                            ageMaxValue = null;
                        }

                        List<Point> mPOIset = Data.sCopiedPoiGpx.getPoints();

                        final CheckBox typeCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_types_on);
                        if (typeCheckBox.isChecked()) {
                            Data.sViewPoiFilter.enableTypeFilter(selectedPOITypes);
                        } else {
                            Data.sViewPoiFilter.disableTypeFilter();
                        }

                        final CheckBox dstCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_distance_on);
                        if (dstCheckBox.isChecked()) {
                            Data.sViewPoiFilter.enableDistanceFilter(Data.sCurrentPosition.getLatitude(),
                                    Data.sCurrentPosition.getLongitude(), Data.sCurrentPosition.getAltitude(), dstMinValue, dstMaxValue);
                        } else {
                            Data.sViewPoiFilter.disableDistanceFilter();
                        }

                        final CheckBox ageCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_age_on);
                        if (ageCheckBox.isChecked()) {
                            Data.sViewPoiFilter.enableAgeFilter(ageMinValue, ageMaxValue);
                        } else {
                            Data.sViewPoiFilter.disableAgeFilter();
                        }

                        Data.sFilteredPoi = ListUtils.filter(mPOIset, Data.sViewPoiFilter);

                        refreshMap();

                    }
                });

        builder.setMultiChoiceItems(all_types, selections, new DialogInterface.OnMultiChoiceClickListener() {

            @Override
            public void onClick(DialogInterface arg0, int arg1, boolean arg2) {

                selections[arg1] = arg2;

                final CheckBox typeCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_types_on);

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
        builder.setView(poisFilterLayout);

        final AlertDialog alert = builder.create();

        alert.show();

        dstMax.setEnabled(Data.sViewPoiFilter.isDistanceFilterEnabled());
        dstMin.setEnabled(Data.sViewPoiFilter.isDistanceFilterEnabled());

        ageMin.setEnabled(Data.sViewPoiFilter.isAgeFilterEnabled());
        ageMax.setEnabled(Data.sViewPoiFilter.isAgeFilterEnabled());

        alert.getListView().setEnabled(Data.sViewPoiFilter.isTypeFilterEnabled());

        final CheckBox typeCheckBox = (CheckBox) poisFilterLayout.findViewById(R.id.pois_filter_types_on);
        typeCheckBox.setChecked(Data.sViewPoiFilter.isTypeFilterEnabled());

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
                    alert.getListView().setAlpha(1);
                } else {
                    alert.getListView().setEnabled(false);
                    alert.getListView().setAlpha(0.2f);
                }
            }
        });
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        /*
         * Handle the back button
         */
        if(keyCode == KeyEvent.KEYCODE_BACK ) {

            /*
             * If data changed
             */
            if (Data.sCopiedPoiGpx.isChanged()) {
                new AlertDialog.Builder(this)
                        .setIcon(R.drawable.map_warning)
                        .setTitle(R.string.dialog_save_changes_title)
                        .setMessage(R.string.dialog_poi_changed_message)

                        .setPositiveButton(R.string.dialog_apply, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                if (!Data.sCopiedPoiGpx.getPoints().isEmpty()) {

                                    Data.sPoiGpx = copyPoiGpx(Data.sCopiedPoiGpx);
                                }
                                finish();
                            }
                        })
                        .setNegativeButton(R.string.dialog_discard, new DialogInterface.OnClickListener() {

                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                finish();
                            }

                        })
                        .show();
                return true;

            } else {

                finish();
                return true;
            }

        } else {
            return super.onKeyDown(keyCode, event);
        }
    }
}