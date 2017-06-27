package pl.nwg.dev.rambler.gpx;

/**
 * Created by piotrm on 21.05.17.
 */

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import pt.karambola.geo.Units;
import pt.karambola.gpx.beans.Gpx;
import pt.karambola.gpx.io.GpxFileIo;
import pt.karambola.gpx.io.GpxStreamIo;
import pt.karambola.gpx.parser.GpxParser;
import pt.karambola.gpx.util.GpxUtils;

public class MainActivity extends Utils {

    Intent fileExploreIntent;
    String ramblerPath;
    private final int REQUEST_CODE_PICK_DIR = 1;
    private final int REQUEST_CODE_PICK_FILE = 2;

    String fileName = "myfile";

    SharedPreferences sharedPreferences;

    int filePickerAction = -1;
    private final int ACTION_OPEN = 1;
    private final int ACTION_SAVE_AS = 2;

    boolean saveInProgress;

    private ListView mDrawerList;
    private DrawerLayout mDrawerLayout;
    private ArrayAdapter<String> mAdapter;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mActivityTitle;

    private CharSequence mDrawerTitle;
    private CharSequence mTitle;

    private static final String TAG = "MainActivity";

    boolean mLocationAcquired = false;

    TableRow poisButton;
    TableRow routesButton;
    TableRow tracksButton;

    LinearLayout newButton;
    LinearLayout openButton;
    LinearLayout saveButton;
    LinearLayout syncButton;

    private boolean mOnSyncButton = false;

    ListView list;

    String[] web = new String[4];

    Integer[] imageId = {
            R.drawable.bar_new,
            R.drawable.bar_open,
            R.drawable.bar_save,
            R.drawable.bar_sync
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#ff00ffff")));
            actionBar.setStackedBackgroundDrawable(new ColorDrawable(Color.parseColor("#5500ffff")));
        }

        mTitle = mDrawerTitle = getTitle();

        sharedPreferences = getSharedPreferences("Rambler", MODE_PRIVATE);

        mLocationAcquired =  false;

        ramblerPath = (new File(Environment.getExternalStorageDirectory() + "/Rambler").toString());

        fileExploreIntent = new Intent(
                FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                null,
                this,
                FileBrowserActivity.class
        );

        web[0] = getResources().getString(R.string.new_gpx);
        web[1] = getResources().getString(R.string.open_gpx);
        web[2] = getResources().getString(R.string.save_gpx);
        web[3] = getResources().getString(R.string.sync_data);

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        /*
         * In case you wanted to use the app standalone, it would be enough to store the current
         * data set in a private folder, which is being deleted after uninstalling the app
         * or clearing data. However, I'd like the application to share data with other apps.
         * Disadvantage: the folder will have to be deleted manually when no longer necessary.
         *
         * defaultPoisFile = new File((this).getExternalFilesDir(null), "ramblerPois.gpx");
         * defaultRoutesFile = new File((this).getExternalFilesDir(null), "ramblerRoutes.gpx");
         * defaultTracksFile = new File((this).getExternalFilesDir(null), "ramblerTracks.gpx");
         */

        if (!sharedFolderExists()) {
            if (sharedFolderCreate()) {
                Toast.makeText(MainActivity.this, getString(R.string.shared_folder_created), Toast.LENGTH_SHORT).show();

            } else {
                Toast.makeText(MainActivity.this, getString(R.string.shared_folder_failure), Toast.LENGTH_SHORT).show();
            }
        }

        defaultPoisFile = new File(Environment.getExternalStorageDirectory() + "/RamblerSharedData/ramblerPois.gpx");
        defaultRoutesFile = new File(Environment.getExternalStorageDirectory() + "/RamblerSharedData/ramblerRoutes.gpx");
        defaultTracksFile = new File(Environment.getExternalStorageDirectory() + "/RamblerSharedData/ramblerTracks.gpx");

        boolean firstInstall = !defaultPoisFile.exists() || !defaultRoutesFile.exists() || !defaultTracksFile.exists();

        if (!defaultPoisFile.exists()) {
            try {
                defaultPoisFile.createNewFile();
                Data.sPoiGpx = new Gpx();
                GpxFileIo.parseOut(Data.sPoiGpx, defaultPoisFile);
                Data.sPoiGpx.resetIsChanged();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_rambler_pois), Toast.LENGTH_SHORT).show();
            }
        }
        if (!defaultRoutesFile.exists()) {
            try {
                defaultRoutesFile.createNewFile();
                Data.sRoutesGpx = new Gpx();
                GpxFileIo.parseOut(Data.sRoutesGpx, defaultRoutesFile);
                Data.sRoutesGpx.resetIsChanged();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_rambler_routes), Toast.LENGTH_SHORT).show();
            }
        }
        if (!defaultTracksFile.exists()) {
            try {
                defaultTracksFile.createNewFile();
                Data.sTracksGpx = new Gpx();
                GpxFileIo.parseOut(Data.sTracksGpx, defaultTracksFile);
                Data.sTracksGpx.resetIsChanged();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_rambler_tracks), Toast.LENGTH_SHORT).show();
            }
        }

        if (!firstInstall) {

            new loadDefaultDataFiles().execute();

        } else {

            AssetManager assetManager = getAssets();
            try {
                InputStream inputStream = assetManager.open("sample.gpx");

                Gpx inputGpx = GpxStreamIo.parseIn(new GpxParser(), inputStream);

                Data.sPoiGpx.setPoints(inputGpx.getPoints());

                Data.sRoutesGpx.setRoutes(inputGpx.getRoutes());

                refreshLoadedDataInfo();
                TextView openFile = (TextView) findViewById(R.id.open_file);
                openFile.setText("sample.gpx");

                Toast.makeText(MainActivity.this, getString(R.string.sample_loaded), Toast.LENGTH_LONG).show();

            } catch (final IOException e) {
                e.printStackTrace();
            }
        }

        poisButton = (TableRow) findViewById(R.id.main_poi_btn);
        poisButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                /*
                 * Let's work on a copy of POI data, (to be saved or not on exit).
                 */
                Data.sCopiedPoiGpx = Utils.copyPoiGpx(Data.sPoiGpx);
                Data.sCopiedPoiGpx.resetIsChanged();

                Intent i;
                i = new Intent(MainActivity.this, PoiActivity.class);

                startActivity(i);
            }

        });

        routesButton = (TableRow) findViewById(R.id.main_routes_btn);
        routesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i;
                i = new Intent(MainActivity.this, RoutesBrowserActivity.class);

                startActivity(i);
            }

        });

        tracksButton = (TableRow) findViewById(R.id.main_tracks_btn);
        tracksButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                Intent i;
                i = new Intent(MainActivity.this, TracksBrowserActivity.class);

                startActivity(i);
            }

        });

        newButton = (LinearLayout) findViewById(R.id.bar_new);
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fileNew();
            }
        });

        openButton = (LinearLayout) findViewById(R.id.bar_open);
        openButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                fileOpen();
            }
        });

        saveButton = (LinearLayout) findViewById(R.id.bar_save);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                showSaveAsDialog();
            }
        });

        syncButton = (LinearLayout) findViewById(R.id.bar_sync);
        syncButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                syncData();
            }
        });

        if (!ramblerFolderExists()) {
            if (ramblerFolderCreate()) {
                Toast.makeText(MainActivity.this, getString(R.string.rambler_folder_created), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(MainActivity.this, getString(R.string.rambler_folder_failure), Toast.LENGTH_SHORT).show();
            }
        }

        String versionName = "-.-.-";
        PackageInfo packageInfo = null;

        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
            Log.d(TAG, "versionName = " + versionName);

        } catch (PackageManager.NameNotFoundException e) {
            Log.d(TAG, "Couldn't obtain versionName");
            e.printStackTrace();
        }

        String savedAppVer = "";
        /* todo This could be easily replaced with just saving to shared preferences
        try {
            savedAppVer = Data.ramblerProperties.getProperty("app.ver");
        } catch(Exception e) {
            Log.d(TAG, "Error restoring savedAppVer" + e);
        }
        */


        Log.d(TAG, "savedAppVer: " + savedAppVer);

        /* Let's turn it off temporarily

        if (packageInfo != null) {

            Log.d(TAG, "versionName: " + versionName);
            Log.d(TAG, "savedAppVer: " + savedAppVer);

            if (!versionName.equals(savedAppVer) || savedAppVer.equals("")) {
                displayWhatsNewDialog();
            }

        }
        */

    }

    public static boolean ramblerFolderExists() {

        File folder = new File(Environment.getExternalStorageDirectory() + "/Rambler");
        return folder.exists();
    }

    public static boolean ramblerFolderCreate() {

        File folder = new File(Environment.getExternalStorageDirectory() + "/Rambler");
        return folder.mkdirs();
    }

    public static boolean sharedFolderExists() {

        File folder = new File(Environment.getExternalStorageDirectory() + "/RamblerSharedData");
        return folder.exists();
    }

    public static boolean sharedFolderCreate() {

        File folder = new File(Environment.getExternalStorageDirectory() + "/RamblerSharedData");
        return folder.mkdirs();
    }

    private void addDrawerItems() {

        CustomList adapter = new
                CustomList(MainActivity.this, web, imageId);
        list=(ListView)findViewById(R.id.navList);
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                switch (position) {
                    case 0:
                        fileNew();
                        break;

                    case 1:
                        fileOpen();
                        break;

                    case 2:
                        showSaveAsDialog();
                        break;

                    case 3:
                        syncData();
                        break;

                    default:
                        break;
                }

            }
        });
    }

    private void fileNew() {
        if (Data.sPoiGpx.isChanged() || Data.sRoutesGpx.isChanged() || Data.sTracksGpx.isChanged()) {

            displayDataChangedDialog();

        } else {

            Data.sPoiGpx = new Gpx();
            Data.sRoutesGpx = new Gpx();
            Data.sTracksGpx = new Gpx();

            Data.sPoiGpx.resetIsChanged();
            Data.sRoutesGpx.resetIsChanged();
            Data.sTracksGpx.resetIsChanged();

            refreshLoadedDataInfo();
        }

        Data.sPoiGpx = new Gpx();
        Data.sRoutesGpx = new Gpx();
        Data.sTracksGpx = new Gpx();

        refreshLoadedDataInfo();
    }
    private void fileOpen() {
        filePickerAction = ACTION_OPEN;

        fileExploreIntent.putExtra(
                FileBrowserActivity.startDirectoryParameter,
                ramblerPath
        );
        startActivityForResult(
                fileExploreIntent,
                REQUEST_CODE_PICK_FILE
        );
    }
    private void syncData() {

        mOnSyncButton = true;

        if (!saveInProgress) {

            new saveDefaultDataFiles().execute();

        } else {

            Toast.makeText(getApplicationContext(), getString(R.string.saving_wait), Toast.LENGTH_SHORT).show();
        }
    }

    private void setupDrawer() {
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);


        mDrawerToggle = new ActionBarDrawerToggle(
                this,
                mDrawerLayout,
                R.drawable.ic_drawer,
                R.string.drawer_open,
                R.string.drawer_close
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu();
            }
        };
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        mDrawerToggle.setDrawerIndicatorEnabled(true);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        // Sync the toggle state after onRestoreInstanceState has occurred.
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        /* Here we can enable/disable menu items as shown below:
         *
         * menu.findItem(R.id.action_lv).setEnabled(Utils.mPeerId != null);
         */
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {

            case R.id.action_about:
                displayAboutDialog();
                return true;

            case R.id.action_credits:
                displayCreditsDialog();
                return true;

            case R.id.action_settings:
                displaySettingsDialog();
                return true;

            case R.id.action_exit:
                finish();
        }

        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.about_dialog, null);

        PackageInfo packageInfo =  null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        if (packageInfo != null) {
            TextView version = (TextView) layout.findViewById(R.id.version_name);
            version.setText(String.format(getResources().getString(R.string.version_name), packageInfo.versionName));
        }

        final TextView gnu = (TextView) layout.findViewById(R.id.gnu);
        final TextView github = (TextView) layout.findViewById(R.id.github);

        String dialogTitle = getResources().getString(R.string.dialog_about);
        String okText = getResources().getString(R.string.dialog_ok);
        String creditsText = getResources().getString(R.string.credits_btn);
        String websiteText = getResources().getString(R.string.manual_btn);
        builder.setTitle(dialogTitle)
                .setIcon(R.drawable.icon)
                .setCancelable(false)
                .setView(layout)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton(creditsText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        displayCreditsDialog();
                    }
                })
                .setNegativeButton(websiteText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Toast.makeText(getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
                    }
                });

        AlertDialog alert = builder.create();

        gnu.setMovementMethod(LinkMovementMethod.getInstance());
        github.setMovementMethod(LinkMovementMethod.getInstance());

        alert.show();
    }

    private void displayCreditsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.credits_dialog, null);

        final List<TextView> clickableFields = new ArrayList<>();
        clickableFields.add((TextView) layout.findViewById(R.id.aosp_name));
        clickableFields.add((TextView) layout.findViewById(R.id.aosp_license));
        clickableFields.add((TextView) layout.findViewById(R.id.karambola_name));
        clickableFields.add((TextView) layout.findViewById(R.id.karambola_license));
        clickableFields.add((TextView) layout.findViewById(R.id.osmdroid_name));
        clickableFields.add((TextView) layout.findViewById(R.id.osmdroid_license));
        clickableFields.add((TextView) layout.findViewById(R.id.osmbp_name));
        clickableFields.add((TextView) layout.findViewById(R.id.osmbp_license));
        clickableFields.add((TextView) layout.findViewById(R.id.osrm_name));
        clickableFields.add((TextView) layout.findViewById(R.id.osrm_license));
        clickableFields.add((TextView) layout.findViewById(R.id.osrm_license_demo_server));
        clickableFields.add((TextView) layout.findViewById(R.id.filebrowser_name));
        clickableFields.add((TextView) layout.findViewById(R.id.filebrowser_license));

        String dialogTitle = getResources().getString(R.string.credits);
        String okText = getResources().getString(R.string.dialog_ok);
        builder.setTitle(dialogTitle)
                .setIcon(R.drawable.ico_info)
                .setCancelable(true)
                .setView(layout)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();

        for (TextView textView : clickableFields) {
            textView.setMovementMethod(LinkMovementMethod.getInstance());
        }

        alert.show();
    }

    private void displaySettingsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.settings_dialog, null);

        final Spinner spinner = (Spinner) layout.findViewById(R.id.units_spinner);
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.units_array));
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(dataAdapter);
        spinner.setSelection(Data.sUnitsInUse.getCode());
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {

                switch(pos) {
                    case 0:
                        Data.sUnitsInUse = Units.METRIC;
                        break;
                    case 1:
                        Data.sUnitsInUse = Units.IMPERIAL;
                        break;
                    case 2:
                        Data.sUnitsInUse = Units.NAUTICAL;
                        break;
                    default:
                        break;
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        final CheckBox rotationCheckBox = (CheckBox) layout.findViewById(R.id.rotationCheckBox);
        rotationCheckBox.setChecked(Data.sAllowRotation);
        rotationCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Data.sAllowRotation = !Data.sAllowRotation;
                rotationCheckBox.setChecked(Data.sAllowRotation);
            }
        });

        builder.setTitle(getResources().getString(R.string.settings))
                .setIcon(R.drawable.ico_settings)
                .setCancelable(true)
                .setView(layout)
                .setPositiveButton(getResources().getString(R.string.dialog_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        saveSettings();
                    }
                })
                .setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    private void displayWhatsNewDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.whatsnew_dialog, null);

        final CheckBox dontShowCheckBox = (CheckBox) layout.findViewById(R.id.dontShowCheckBox);

        String dialogTitle = getResources().getString(R.string.whats_new_title);
        String messageText = getResources().getString(R.string.whats_new_message);
        String okText = getResources().getString(R.string.dialog_ok);
        builder.setTitle(dialogTitle)
                .setMessage(messageText)
                .setIcon(R.drawable.ico_info)
                .setCancelable(true)
                .setView(layout)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        if (dontShowCheckBox.isChecked()) {

                            PackageInfo packageInfo = null;
                            try {
                                packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
                                String versionName = packageInfo.versionName;
                                /* todo - as said above: replace with shared preferences
                                Data.ramblerProperties.setProperty("app.ver", String.valueOf(versionName));
                                saveProperties(Data.ramblerProperties, String.valueOf(Data.propertiesFile), "Rambler saved properties");
                                */

                            } catch (PackageManager.NameNotFoundException e) {
                                e.printStackTrace();
                            }

                        }
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

        TextView textView = (TextView) alert.findViewById(android.R.id.message);
        textView.setTextSize(14);

        dontShowCheckBox.setChecked(false);

    }

    @Override
    protected void onResume() {
        super.onResume();

        refreshLoadedDataInfo();
        loadSettings();
    }

    @Override
    public void onStart() {
        super.onStart();

        Intent intent = getIntent();

        if (intent != null) {

            final Uri data = intent.getData();

            if (data != null) {

                final String filePath = data.getEncodedPath ();

                if (filePath != null && !filePath.isEmpty()) {

                    intent.setData(null);
                    externalGpxFile = filePath;
                    new openExternalGpxFile().execute();
                }
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        //Handle the back button
        if(keyCode == KeyEvent.KEYCODE_BACK) {

            if (!saveInProgress) {
                new saveDefaultDataFiles().execute();

                return true;

            } else {

                Toast.makeText(this, getString(R.string.saving_wait), Toast.LENGTH_SHORT).show();
                return false;
            }

        } else {
            return super.onKeyDown(keyCode, event);
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        TextView openFile = (TextView) findViewById(R.id.open_file);

        if (requestCode == REQUEST_CODE_PICK_FILE) {
            if (resultCode == RESULT_OK) {

                String fileFullPath = data.getStringExtra(
                        pl.nwg.dev.rambler.gpx.FileBrowserActivity.returnFileParameter);

                switch(filePickerAction) {

                    case ACTION_OPEN:

                        externalGpxFile = fileFullPath;

                        new openExternalGpxFile().execute();

                        break;

                    case ACTION_SAVE_AS:
                        // Save all to the picked file
                        String savingPoi = String.format(getString(R.string.poi_loaded), Data.sPoiGpx.getPoints().size());
                        String savingRoutes = String.format(getString(R.string.routes_loaded), Data.sRoutesGpx.getRoutes().size());
                        String savingTracks = String.format(getString(R.string.tracks_loaded), Data.sTracksGpx.getTracks().size());
                        Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                        Data.mGpx = new Gpx();

                        Data.mGpx.addPoints(Data.sPoiGpx.getPoints());
                        Data.mGpx.addRoutes(Data.sRoutesGpx.getRoutes());
                        Data.mGpx.addTracks(Data.sTracksGpx.getTracks());

                        GpxFileIo.parseOut(Data.mGpx, fileFullPath);


                        Data.lastOpenFile = fileFullPath;

                        try {
                            String[] splitFullPath = fileFullPath.split("/");
                            String filaname = splitFullPath[splitFullPath.length -1];
                            openFile.setText(filaname);
                        } catch(Exception e) {
                            openFile.setText(String.valueOf(e));
                        }
                        break;
                }


            } else {
                Toast.makeText(
                        this,
                        getString(R.string.no_file_selected),
                        Toast.LENGTH_LONG).show();
            }
        }
        refreshLoadedDataInfo();
    }

    private void showSaveAsDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View saveAsLayout = inflater.inflate(R.layout.save_gpx_dialog_layout, null);

        final EditText filename = (EditText) saveAsLayout.findViewById(R.id.save_new_filename);

        File rambler_folder = new File(Environment.getExternalStorageDirectory() + "/Rambler");
        final String path = rambler_folder.toString();

        final Intent fileExploreIntent = new Intent(
                FileBrowserActivity.INTENT_ACTION_SELECT_FILE,
                null,
                this,
                FileBrowserActivity.class
        );

        filename.setText(fileName);

        String dialogTitle = getResources().getString(R.string.dialog_savegpx_saveasnew);
        String saveText = getResources().getString(R.string.dialog_save_changes_save);
        String saveAsText = getResources().getString(R.string.file_pick);
        String cancelText = getResources().getString(R.string.dialog_cancel);

        builder.setTitle(dialogTitle)
                .setView(saveAsLayout)
                .setIcon(R.drawable.map_save)
                .setCancelable(true)
                .setNeutralButton(cancelText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                })
                .setNegativeButton(saveAsText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {


                        filePickerAction = ACTION_SAVE_AS;

                        fileExploreIntent.putExtra(
                                FileBrowserActivity.startDirectoryParameter,
                                path
                        );
                        startActivityForResult(
                                fileExploreIntent,
                                REQUEST_CODE_PICK_FILE
                        );
                    }
                })
                .setPositiveButton(saveText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        fileName = filename.getText().toString().trim();
                        saveGpxDestructive(fileName);

                    }
                });

        AlertDialog alert = builder.create();
        alert.show();

        final Button saveButton = alert.getButton(AlertDialog.BUTTON_POSITIVE);

        final TextWatcher validate_name = new TextWatcher(){

            @Override
            public void afterTextChanged(Editable arg0) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {

                saveButton.setEnabled(!arg0.toString().equals(""));
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {

                saveButton.setEnabled(!s.toString().equals(""));

            }};
        filename.addTextChangedListener(validate_name);
    }

    private void saveGpxDestructive(String filename) {

        if (Data.sPoiGpx.getPoints().size() == 0 && Data.sRoutesGpx.getRoutes().size() == 0 && Data.sTracksGpx.getTracks().size() == 0) {
            Toast.makeText(this, getString(R.string.nothing_to_save), Toast.LENGTH_LONG).show();
            return;
        }

        boolean path_ok;
        File rambler_folder = new File(Environment.getExternalStorageDirectory() + "/Rambler");

        path_ok = rambler_folder.exists() || rambler_folder.mkdirs();

        if (path_ok) {

            final String new_file = rambler_folder.toString() + "/" + filename + ".gpx";

            if (new File(new_file).exists()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                String dialogTitle = getResources().getString(R.string.dialog_overwrite_title);
                String dialogMessage = getResources().getString(R.string.dialog_overwrite_message);
                String saveText = getResources().getString(R.string.dialog_save_changes_save);
                String cancelText = getResources().getString(R.string.dialog_cancel);

                builder.setTitle(dialogTitle)
                        .setIcon(R.drawable.map_warning)
                        .setMessage(dialogMessage)
                        .setCancelable(true)
                        .setNegativeButton(cancelText, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                            }
                        })
                        .setPositiveButton(saveText, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                String savingPoi = String.format(getString(R.string.poi_loaded), Data.sPoiGpx.getPoints().size());
                                String savingRoutes = String.format(getString(R.string.routes_loaded), Data.sRoutesGpx.getRoutes().size());
                                String savingTracks = String.format(getString(R.string.tracks_loaded), Data.sTracksGpx.getTracks().size());
                                Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                                Data.mGpx = new Gpx();

                                Data.mGpx.addPoints(Data.sPoiGpx.getPoints());
                                Data.mGpx.addRoutes(Data.sRoutesGpx.getRoutes());
                                Data.mGpx.addTracks(Data.sTracksGpx.getTracks());

                                GpxFileIo.parseOut(Data.mGpx, new_file) ;
                            }
                        });

                AlertDialog alert = builder.create();

                alert.show();

            } else {

                // Just save
                String savingPoi = String.format(getString(R.string.poi_loaded), Data.sPoiGpx.getPoints().size());
                String savingRoutes = String.format(getString(R.string.routes_loaded), Data.sRoutesGpx.getRoutes().size());
                String savingTracks = String.format(getString(R.string.tracks_loaded), Data.sTracksGpx.getTracks().size());
                Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                Data.mGpx = new Gpx();

                Data.mGpx.addPoints(Data.sPoiGpx.getPoints());
                Data.mGpx.addRoutes(Data.sRoutesGpx.getRoutes());
                Data.mGpx.addTracks(Data.sTracksGpx.getTracks());

                GpxFileIo.parseOut(Data.mGpx, new_file) ;

            }

            TextView openFile = (TextView) findViewById(R.id.open_file);
            Data.lastOpenFile = new_file;

            try {
                String[] splitFullPath = new_file.split("/");
                String filaname = splitFullPath[splitFullPath.length -1];
                openFile.setText(filaname);
            } catch(Exception e) {
                openFile.setText(String.valueOf(e));
            }

        } else {

            Toast.makeText(getApplicationContext(), getString(R.string.failed_writing_gpx), Toast.LENGTH_LONG).show();
        }
    }

    public void refreshLoadedDataInfo() {

        TextView openFile = (TextView) findViewById(R.id.open_file);
        openFile.setText("");

        if (Data.sPoiGpx == null || Data.sRoutesGpx == null || Data.sTracksGpx == null) {
            return;
        }

        try {
            TextView poiStatus = (TextView) findViewById(R.id.poi_manager_text);
            poiStatus.setText(String.format(getString(R.string.main_poi_loaded), Data.sPoiGpx.getPoints().size()));

            TextView routesStatus = (TextView) findViewById(R.id.route_manager_text);
            routesStatus.setText(String.format(getString(R.string.main_routes_loaded), Data.sRoutesGpx.getRoutes().size()));

            TextView tracksStatus = (TextView) findViewById(R.id.track_manager_text);
            tracksStatus.setText(String.format(getString(R.string.main_tracks_loaded), Data.sTracksGpx.getTracks().size()));
        } catch(Exception e) {
            Toast.makeText(getApplicationContext(), getString(R.string.read_error) + e, Toast.LENGTH_SHORT).show();
        }
    }

    private void displayDataChangedDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String dialogTitle = getResources().getString(R.string.dialog_data_changed_title);
        String messageText = getResources().getString(R.string.dialog_data_changed_message);
        String saveText = getResources().getString(R.string.dialog_save);
        String dontSaveText = getResources().getString(R.string.dialog_dont_save);
        String cancelText = getResources().getString(R.string.dialog_cancel);

        builder.setMessage(messageText)
                .setTitle(dialogTitle)
                .setIcon(R.drawable.map_question)
                .setCancelable(false)
                .setPositiveButton(saveText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        showSaveAsDialog();
                    }
                })
                .setNegativeButton(dontSaveText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Data.sPoiGpx = new Gpx();
                        Data.sRoutesGpx = new Gpx();
                        Data.sTracksGpx = new Gpx();

                        Data.sPoiGpx.resetIsChanged();
                        Data.sRoutesGpx.resetIsChanged();
                        Data.sTracksGpx.resetIsChanged();

                        refreshLoadedDataInfo();
                    }
                })
                .setNeutralButton(cancelText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();

    }

    public class loadDefaultDataFiles extends
            AsyncTask<Void, Boolean, Void> {

        AlertDialog alert;

        String errorMessage;
        File corruptedFile;
        Gpx corruptedGpx;

        @Override
        protected Void doInBackground(Void... params) {

            if(Data.sPoiGpx == null) {

                try {
                    Data.sPoiGpx = GpxFileIo.parseIn(defaultPoisFile);

                    int purged_pois = GpxUtils.purgePointsSimilar(Data.sPoiGpx);

                    if (purged_pois != 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purged_pois + " " + getString(R.string.duplicated_poi), Toast.LENGTH_SHORT).show();
                    }
                    Data.sPoiGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerPois.gpx";
                    corruptedFile = defaultPoisFile;
                    corruptedGpx = Data.sPoiGpx;
                }
            }


            if(Data.sRoutesGpx == null) {

                try {
                    Data.sRoutesGpx = GpxFileIo.parseIn(defaultRoutesFile);

                    int purged_routes = GpxUtils.purgeRoutesOverlapping(Data.sRoutesGpx);

                    if (purged_routes != 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purged_routes + " " + getString(R.string.overlapping_routes), Toast.LENGTH_SHORT).show();
                    }
                    Data.sRoutesGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerRoutes.gpx";
                    corruptedFile = defaultRoutesFile;
                    corruptedGpx = Data.sRoutesGpx;
                }
            }


            if (Data.sTracksGpx == null) {

                try {
                    Data.sTracksGpx = GpxFileIo.parseIn(defaultTracksFile);

                    Data.sTracksGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerTracks.gpx";
                    corruptedFile = defaultTracksFile;
                    corruptedGpx = Data.sTracksGpx;
                }
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {

            if (alert != null) {

                alert.dismiss();
            }

            if (errorMessage != null) {

                handleCorruptedFileError(errorMessage, corruptedFile, corruptedGpx);

            }
            refreshLoadedDataInfo();

        }

        @Override
        protected void onPreExecute() {

            if(Data.sPoiGpx == null || Data.sRoutesGpx == null || Data.sTracksGpx == null) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setIcon(R.drawable.wait)
                        .setTitle(R.string.dialog_loading_data)
                        .setCancelable(false);

                alert = builder.create();
                alert.show();
            }
        }
    }

    private void handleCorruptedFileError(String message, File file, Gpx gpx) {

        try {
            file.createNewFile();
            if (gpx == Data.sPoiGpx) {

                Data.sPoiGpx = new Gpx();
                GpxFileIo.parseOut(Data.sPoiGpx, file);

            } else if (gpx == Data.sRoutesGpx) {

                Data.sRoutesGpx = new Gpx();
                GpxFileIo.parseOut(Data.sRoutesGpx, file);

            } else if (gpx == Data.sTracksGpx) {

                Data.sTracksGpx = new Gpx();
                GpxFileIo.parseOut(Data.sTracksGpx, file);
            }


        } catch (Exception e) {
            Log.d(TAG, "Failed creating " + file.toString());
        }
        Toast.makeText(getApplicationContext(), message + " " + getString(R.string.default_file_corrupted), Toast.LENGTH_LONG).show();

    }

    private class saveDefaultDataFiles extends
            AsyncTask<Void, Boolean, Void> {

        AlertDialog alert;

        @Override
        protected Void doInBackground(Void... params) {

            try {

                if (Data.sPoiGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.sPoiGpx, defaultPoisFile);
                }

                if (Data.sRoutesGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.sRoutesGpx, defaultRoutesFile);
                }

                if (Data.sTracksGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.sTracksGpx, defaultTracksFile);
                }

                Data.sPoiGpx.resetIsChanged();
                Data.sRoutesGpx.resetIsChanged();
                Data.sTracksGpx.resetIsChanged();


            } catch (Exception e) {

                Toast.makeText(getApplicationContext(), getString(R.string.error_saving_data) + " " + e, Toast.LENGTH_SHORT).show();

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            if (!mOnSyncButton) {
                finish();
            } else {
                mOnSyncButton = false;
                if(alert != null) {
                    alert.dismiss();
                }
            }

        }

        @Override
        protected void onPreExecute() {

            if (Data.sPoiGpx.isChanged() || Data.sRoutesGpx.isChanged() || Data.sTracksGpx.isChanged()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setIcon(R.drawable.wait)
                        .setTitle(R.string.dialog_saving_data)
                        .setCancelable(false);

                alert = builder.create();
                alert.show();
            }
        }
    }

    private class openExternalGpxFile extends
            AsyncTask<Void, Boolean, Void> {

        AlertDialog alert;

        int purger_pois, purged_routes;

        @Override
        protected Void doInBackground(Void... params) {

            Gpx gpxIn = new Gpx();

            try {
                gpxIn = GpxFileIo.parseIn(externalGpxFile);

            } catch (Exception e) {

                Toast.makeText(getApplicationContext(), getString(R.string.error_opening_file) + " " + e, Toast.LENGTH_SHORT).show();

            }

            if (gpxIn != null) {

                Data.sPoiGpx = new Gpx();
                Data.sPoiGpx.setPoints(gpxIn.getPoints());

                purger_pois = GpxUtils.purgePointsSimilar(Data.sPoiGpx);

                Data.sPoiGpx.resetIsChanged();

                Data.sRoutesGpx = new Gpx();
                Data.sRoutesGpx.setRoutes(gpxIn.getRoutes());

                purged_routes = GpxUtils.purgeRoutesOverlapping(Data.sRoutesGpx);

                Data.sRoutesGpx.resetIsChanged();

                Data.sTracksGpx = new Gpx();
                Data.sTracksGpx.setTracks(gpxIn.getTracks());

                Data.sTracksGpx.resetIsChanged();

            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            alert.dismiss();

            TextView openFile = (TextView) findViewById(R.id.open_file);
            refreshLoadedDataInfo();

            try {
                String[] splitFullPath = externalGpxFile.split("/");
                String filaname = splitFullPath[splitFullPath.length -1];
                openFile.setText(filaname);
            } catch(Exception e) {
                openFile.setText(String.valueOf(e));
            }

            if (purger_pois != 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purger_pois + " " + getString(R.string.duplicated_poi), Toast.LENGTH_SHORT).show();
            }

            if (purged_routes != 0) {
                Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purged_routes + " " + getString(R.string.overlapping_routes), Toast.LENGTH_SHORT).show();
            }

            saveJustOpenedData();
        }

        @Override
        protected void onPreExecute() {

            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

            builder.setIcon(R.drawable.wait)
                    .setTitle(R.string.dialog_loading_data)
                    .setCancelable(false);

            alert = builder.create();
            alert.show();

        }
    }

    private void saveJustOpenedData() {

        saveInProgress = true;

        final Handler mSaveHandler = new Handler();

        Runnable r = new Runnable()
        {
            @Override
            public void run()
            {

                try {

                    GpxFileIo.parseOut(Data.sPoiGpx, defaultPoisFile);
                    Data.sPoiGpx.resetIsChanged();

                    GpxFileIo.parseOut(Data.sRoutesGpx, defaultRoutesFile);
                    Data.sRoutesGpx.resetIsChanged();

                    GpxFileIo.parseOut(Data.sTracksGpx, defaultTracksFile);
                    Data.sTracksGpx.resetIsChanged();

                } catch (Exception e) {

                    if (Log.isLoggable(TAG, Log.DEBUG)) {
                        Log.d(TAG, "Error saving to default files:" + e);
                    }

                }

                mSaveHandler.post(new Runnable()  //If you want to update the UI, queue the code on the UI thread
                {
                    public void run() {

                        Toast.makeText(getApplicationContext(), getString(R.string.default_files_saved), Toast.LENGTH_SHORT).show();
                        saveInProgress = false;

                    }
                });
            }
        };

        Thread t = new Thread(r);
        t.start();
    }

}