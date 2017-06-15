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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

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

    Button poisButton;
    Button routesButton;
    Button newButton;
    Button openButton;
    Button saveButton;

    double currentLat, currentLon, currentAlt;

    ListView list;

    String[] web = new String[3];

    Integer[] imageId = {
            R.drawable.button_new,
            R.drawable.button_open,
            R.drawable.map_save
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

        mDrawerList = (ListView)findViewById(R.id.navList);
        mDrawerLayout = (DrawerLayout)findViewById(R.id.drawer_layout);
        mActivityTitle = getTitle().toString();

        addDrawerItems();
        setupDrawer();

        defaultRamblerFile = new File((this).getExternalFilesDir(null), "Rambler.gpx");

        defaultPoisFile = new File((this).getExternalFilesDir(null), "ramblerPois.gpx");
        defaultRoutesFile = new File((this).getExternalFilesDir(null), "ramblerRoutes.gpx");
        defaultTracksFile = new File((this).getExternalFilesDir(null), "ramblerTracks.gpx");

        boolean firstInstall = !defaultPoisFile.exists() || !defaultRoutesFile.exists() || !defaultTracksFile.exists();

        if (!defaultPoisFile.exists()) {
            try {
                defaultPoisFile.createNewFile();
                Data.mPoisGpx = new Gpx();
                GpxFileIo.parseOut(Data.mPoisGpx, defaultPoisFile);
                Data.mPoisGpx.resetIsChanged();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_rambler_pois), Toast.LENGTH_SHORT).show();
            }
        }
        if (!defaultRoutesFile.exists()) {
            try {
                defaultRoutesFile.createNewFile();
                Data.mRoutesGpx = new Gpx();
                GpxFileIo.parseOut(Data.mRoutesGpx, defaultRoutesFile);
                Data.mRoutesGpx.resetIsChanged();

            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), getString(R.string.failed_rambler_routes), Toast.LENGTH_SHORT).show();
            }
        }
        if (!defaultTracksFile.exists()) {
            try {
                defaultTracksFile.createNewFile();
                Data.mTracksGpx = new Gpx();
                GpxFileIo.parseOut(Data.mTracksGpx, defaultTracksFile);
                Data.mTracksGpx.resetIsChanged();

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

                Data.mPoisGpx.setPoints(inputGpx.getPoints());

                Data.mRoutesGpx.setRoutes(inputGpx.getRoutes());

                refreshLoadedDataInfo();
                TextView openFile = (TextView) findViewById(R.id.open_file);
                openFile.setText("sample.gpx");

                Toast.makeText(MainActivity.this, getString(R.string.sample_loaded), Toast.LENGTH_LONG).show();

            } catch (final IOException e) {
                e.printStackTrace();
            }

        }

        poisButton = (Button) findViewById(R.id.main_pois);
        poisButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                /*
                Intent i;
                i = new Intent(MainActivity.this, PoisActivity.class);
                i.putExtra("current_lat", String.valueOf(currentLat));
                i.putExtra("current_lon", String.valueOf(currentLon));
                i.putExtra("current_ele", String.valueOf(currentAlt));

                startActivityForResult(i, 90);
                */
            }

        });

        routesButton = (Button) findViewById(R.id.main_routes);
        routesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                /*
                Intent i;
                i = new Intent(MainActivity.this, RoutesBrowserActivity.class);
                i.putExtra("current_lat", String.valueOf(currentLat));
                i.putExtra("current_lon", String.valueOf(currentLon));
                i.putExtra("current_ele", String.valueOf(currentAlt));

                startActivityForResult(i, 90);
                */

                Intent i;
                i = new Intent(MainActivity.this, RoutesBrowserActivity.class);

                startActivity(i);
            }

        });

        newButton = (Button) findViewById(R.id.main_new);
        newButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                if (Data.mPoisGpx.isChanged() || Data.mRoutesGpx.isChanged() || Data.mTracksGpx.isChanged()) {

                    displayDataChangedDialog();

                } else {

                    Data.mPoisGpx = new Gpx();
                    Data.mRoutesGpx = new Gpx();
                    Data.mTracksGpx = new Gpx();

                    Data.mPoisGpx.resetIsChanged();
                    Data.mRoutesGpx.resetIsChanged();
                    Data.mTracksGpx.resetIsChanged();

                    Data.sSelectedRouteIdx = null;

                    refreshLoadedDataInfo();
                }

                Data.mPoisGpx = new Gpx();
                Data.mRoutesGpx = new Gpx();
                Data.mTracksGpx = new Gpx();

                refreshLoadedDataInfo();

            }

        });

        openButton = (Button) findViewById(R.id.main_open);
        openButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

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

        });

        saveButton = (Button) findViewById(R.id.main_save);
        saveButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {

                showSaveAsDialog();

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
                        if (Data.mPoisGpx.isChanged() || Data.mRoutesGpx.isChanged() || Data.mTracksGpx.isChanged()) {

                            displayDataChangedDialog();

                        } else {

                            Data.mPoisGpx = new Gpx();
                            Data.mRoutesGpx = new Gpx();
                            Data.mTracksGpx = new Gpx();

                            Data.mPoisGpx.resetIsChanged();
                            Data.mRoutesGpx.resetIsChanged();
                            Data.mTracksGpx.resetIsChanged();

                            refreshLoadedDataInfo();
                        }

                        Data.mPoisGpx = new Gpx();
                        Data.mRoutesGpx = new Gpx();
                        Data.mTracksGpx = new Gpx();

                        refreshLoadedDataInfo();

                        break;

                    case 1:
                        filePickerAction = ACTION_OPEN;

                        fileExploreIntent.putExtra(
                                FileBrowserActivity.startDirectoryParameter,
                                ramblerPath
                        );
                        startActivityForResult(
                                fileExploreIntent,
                                REQUEST_CODE_PICK_FILE
                        );
                        break;

                    case 2:
                        showSaveAsDialog();
                        break;

                    default:
                        break;
                }

            }
        });
    }

    private void setupDrawer() {
        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);


        mDrawerToggle = new ActionBarDrawerToggle(
                this,                  /* host Activity */
                mDrawerLayout,         /* DrawerLayout object */
                R.drawable.ic_drawer,  /* nav drawer image to replace 'Up' caret */
                R.string.drawer_open,  /* "open drawer" description for accessibility */
                R.string.drawer_close  /* "close drawer" description for accessibility */
        ) {
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mDrawerTitle);
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
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
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch(item.getItemId()) {

            case R.id.action_about:
                displayAboutDialog();
                return true;

            case R.id.action_exit:
                finish();
        }

        // Activate the navigation drawer toggle
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void displayAboutDialog() {

        String versionName = "-.-.-";
        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        final View layout = inflater.inflate(R.layout.about_dialog, null);

        final TextView gnu = (TextView) layout.findViewById(R.id.gnu);
        final TextView github = (TextView) layout.findViewById(R.id.github);

        String dialogTitle = getResources().getString(R.string.dialog_about) + " " + versionName;
        String okText = getResources().getString(R.string.dialog_ok);
        String creditsText = getResources().getString(R.string.credits_btn);
        String websiteText = getResources().getString(R.string.manual_btn);
        builder.setTitle(dialogTitle)
                .setIcon(R.drawable.ico_info)
                .setCancelable(false)
                .setView(layout)
                .setPositiveButton(okText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .setNeutralButton(creditsText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        //displayCreditsDialog();
                        Uri uri = Uri.parse("https://github.com/nwg-piotr/RamblerGPXEditor/blob/master/CREDITS.md");

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);

                    }
                })
                .setNegativeButton(websiteText, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                        Toast.makeText(getApplicationContext(), "Coming soon", Toast.LENGTH_SHORT).show();
                        /*
                        Uri uri = Uri.parse("http://dev.nwg.pl/rambler-user-guide");

                        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                        startActivity(intent);
                        */

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

        final Button karambola1Button = (Button) layout.findViewById(R.id.karambola_button);
        final Button karambola2Button = (Button) layout.findViewById(R.id.karambola_txt_button);

        karambola1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://sourceforge.net/projects/geokarambola");

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });
        karambola2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse("https://sourceforge.net/projects/geokarambola");

                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        String dialogTitle = getResources().getString(R.string.credits_btn);
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
        TextView loadedData = (TextView) findViewById(R.id.loaded_data);

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
                        String savingPoi = String.format(getString(R.string.poi_loaded), Data.mPoisGpx.getPoints().size());
                        String savingRoutes = String.format(getString(R.string.routes_loaded), Data.mRoutesGpx.getRoutes().size());
                        String savingTracks = String.format(getString(R.string.tracks_loaded), Data.mTracksGpx.getTracks().size());
                        Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                        Data.mGpx = new Gpx();

                        Data.mGpx.addPoints(Data.mPoisGpx.getPoints());
                        Data.mGpx.addRoutes(Data.mRoutesGpx.getRoutes());
                        Data.mGpx.addTracks(Data.mTracksGpx.getTracks());

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

        if (Data.mPoisGpx.getPoints().size() == 0 && Data.mRoutesGpx.getRoutes().size() == 0 && Data.mTracksGpx.getTracks().size() == 0) {
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

                                String savingPoi = String.format(getString(R.string.poi_loaded), Data.mPoisGpx.getPoints().size());
                                String savingRoutes = String.format(getString(R.string.routes_loaded), Data.mRoutesGpx.getRoutes().size());
                                String savingTracks = String.format(getString(R.string.tracks_loaded), Data.mTracksGpx.getTracks().size());
                                Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                                Data.mGpx = new Gpx();

                                Data.mGpx.addPoints(Data.mPoisGpx.getPoints());
                                Data.mGpx.addRoutes(Data.mRoutesGpx.getRoutes());
                                Data.mGpx.addTracks(Data.mTracksGpx.getTracks());

                                GpxFileIo.parseOut(Data.mGpx, new_file) ;
                            }
                        });

                AlertDialog alert = builder.create();

                alert.show();

            } else {

                // Just save
                String savingPoi = String.format(getString(R.string.poi_loaded), Data.mPoisGpx.getPoints().size());
                String savingRoutes = String.format(getString(R.string.routes_loaded), Data.mRoutesGpx.getRoutes().size());
                String savingTracks = String.format(getString(R.string.tracks_loaded), Data.mTracksGpx.getTracks().size());
                Toast.makeText(getApplicationContext(), getString(R.string.saving) + " " + savingPoi + ", " + savingRoutes + ", " + savingTracks, Toast.LENGTH_LONG).show();

                Data.mGpx = new Gpx();

                Data.mGpx.addPoints(Data.mPoisGpx.getPoints());
                Data.mGpx.addRoutes(Data.mRoutesGpx.getRoutes());
                Data.mGpx.addTracks(Data.mTracksGpx.getTracks());

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
        TextView loadedData = (TextView) findViewById(R.id.loaded_data);

        openFile.setText("");

        try {
            String loadedPoi = String.format(getString(R.string.poi_loaded), Data.mPoisGpx.getPoints().size()) + ", ";
            String loadedRoutes = String.format(getString(R.string.routes_loaded), Data.mRoutesGpx.getRoutes().size()) + ", ";
            String loadedTracks = String.format(getString(R.string.tracks_loaded), Data.mTracksGpx.getTracks().size());
            String setMe = loadedPoi + loadedRoutes + loadedTracks;
            loadedData.setText(setMe);
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

                        Data.mPoisGpx = new Gpx();
                        Data.mRoutesGpx = new Gpx();
                        Data.mTracksGpx = new Gpx();

                        Data.mPoisGpx.resetIsChanged();
                        Data.mRoutesGpx.resetIsChanged();
                        Data.mTracksGpx.resetIsChanged();

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

            if(Data.mPoisGpx == null) {

                try {
                    Data.mPoisGpx = GpxFileIo.parseIn(defaultPoisFile);

                    int purged_pois = GpxUtils.purgePointsSimilar(Data.mPoisGpx);

                    if (purged_pois != 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purged_pois + " " + getString(R.string.duplicated_poi), Toast.LENGTH_SHORT).show();
                    }
                    Data.mPoisGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerPois.gpx";
                    corruptedFile = defaultPoisFile;
                    corruptedGpx = Data.mPoisGpx;
                }
            }


            if(Data.mRoutesGpx == null) {

                try {
                    Data.mRoutesGpx = GpxFileIo.parseIn(defaultRoutesFile);

                    int purged_routes = GpxUtils.purgeRoutesOverlapping(Data.mRoutesGpx);

                    if (purged_routes != 0) {
                        Toast.makeText(getApplicationContext(), getString(R.string.removed) + " " + purged_routes + " " + getString(R.string.overlapping_routes), Toast.LENGTH_SHORT).show();
                    }
                    Data.mRoutesGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerRoutes.gpx";
                    corruptedFile = defaultRoutesFile;
                    corruptedGpx = Data.mRoutesGpx;
                }
            }


            if (Data.mTracksGpx == null) {

                try {
                    Data.mTracksGpx = GpxFileIo.parseIn(defaultTracksFile);

                    Data.mTracksGpx.resetIsChanged();

                } catch (Exception e) {

                    errorMessage = "ramblerTracks.gpx";
                    corruptedFile = defaultTracksFile;
                    corruptedGpx = Data.mTracksGpx;
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

            if(Data.mPoisGpx == null || Data.mRoutesGpx == null || Data.mTracksGpx == null) {

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
            if (gpx == Data.mPoisGpx) {

                Data.mPoisGpx = new Gpx();
                GpxFileIo.parseOut(Data.mPoisGpx, file);

            } else if (gpx == Data.mRoutesGpx) {

                Data.mRoutesGpx = new Gpx();
                GpxFileIo.parseOut(Data.mRoutesGpx, file);

            } else if (gpx == Data.mTracksGpx) {

                Data.mTracksGpx = new Gpx();
                GpxFileIo.parseOut(Data.mTracksGpx, file);
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

                if (Data.mPoisGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.mPoisGpx, defaultPoisFile);
                }

                if (Data.mRoutesGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.mRoutesGpx, defaultRoutesFile);
                }

                if (Data.mTracksGpx.isChanged()) {
                    GpxFileIo.parseOut(Data.mTracksGpx, defaultTracksFile);
                }

            } catch (Exception e) {

                Toast.makeText(getApplicationContext(), getString(R.string.error_saving_data) + " " + e, Toast.LENGTH_SHORT).show();

            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            finish();

        }

        @Override
        protected void onPreExecute() {

            if (Data.mPoisGpx.isChanged() || Data.mRoutesGpx.isChanged() || Data.mTracksGpx.isChanged()) {

                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setIcon(R.drawable.wait)
                        .setTitle(R.string.dialog_saving_data)
                        .setCancelable(false);

                alert = builder.create();
                alert.show();
            }
        }
    }

    public class openExternalGpxFile extends
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

                Data.mPoisGpx = new Gpx();
                Data.mPoisGpx.setPoints(gpxIn.getPoints());

                purger_pois = GpxUtils.purgePointsSimilar(Data.mPoisGpx);

                Data.mPoisGpx.resetIsChanged();

                Data.mRoutesGpx = new Gpx();
                Data.mRoutesGpx.setRoutes(gpxIn.getRoutes());

                purged_routes = GpxUtils.purgeRoutesOverlapping(Data.mRoutesGpx);

                Data.mRoutesGpx.resetIsChanged();

                Data.mTracksGpx = new Gpx();
                Data.mTracksGpx.setTracks(gpxIn.getTracks());

                Data.mTracksGpx.resetIsChanged();

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

                    GpxFileIo.parseOut(Data.mPoisGpx, defaultPoisFile);
                    Data.mPoisGpx.resetIsChanged();

                    GpxFileIo.parseOut(Data.mRoutesGpx, defaultRoutesFile);
                    Data.mRoutesGpx.resetIsChanged();

                    GpxFileIo.parseOut(Data.mTracksGpx, defaultTracksFile);
                    Data.mTracksGpx.resetIsChanged();

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