package pl.nwg.dev.rambler.gpx;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.beans.RoutePoint;
import pt.karambola.gpx.util.GpxUtils;

import static android.graphics.Bitmap.Config.ARGB_8888;

/**
 * Created by piotr on 30.01.16.
 */
public class Utils extends Activity {

    private static final String TAG = "Utils";

    protected GoogleApiClient mGoogleApiClient;

    protected String responseString;

    protected String sdRootTxt = "";

    protected File defaultRamblerFile;

    protected File defaultPoisFile;
    protected File defaultRoutesFile;
    protected File defaultTracksFile;

    protected String externalGpxFile;

    /**
     * Calculate bounding box for given List of GeoPoints
     */
    protected BoundingBox findBoundingBox(List<GeoPoint> geoPoints) {

        double minLat = Double.MAX_VALUE;
        double maxLat = Double.MIN_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = Double.MIN_VALUE;

        for (GeoPoint geoPoint : geoPoints) {

            final double latitude = geoPoint.getLatitude();
            final double longitude = geoPoint.getLongitude();

            minLat = Math.min(minLat, latitude);
            minLon = Math.min(minLon, longitude);
            maxLat = Math.max(maxLat, latitude);
            maxLon = Math.max(maxLon, longitude);

        }
        return new BoundingBox(maxLat, maxLon, minLat, minLon);
    }

    protected Bitmap makeMarkerBitmap(Context context, String hoverText) {

        Resources resources = context.getResources();
        float scale = resources.getDisplayMetrics().density;

        Bitmap bitmap = BitmapFactory.decodeResource(resources, R.drawable.wp);

        bitmap = bitmap.copy(ARGB_8888, true);

        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(Color.DKGRAY);
        paint.setTextSize(14 * scale);
        paint.setShadowLayer(1f, 0f, 1f, Color.WHITE);

        Rect bounds = new Rect();
        paint.getTextBounds(hoverText, 0, hoverText.length(), bounds);

        int x = (bitmap.getWidth() - bounds.width()) / 2;
        if (x < 0) x = 0;
        int y = bounds.height();
        canvas.drawText(hoverText, x, y, paint);

        return  bitmap;
    }

    protected static String convertStreamToString(InputStream inputStream) throws IOException {
        if (inputStream != null) {
            Writer writer = new StringWriter();

            char[] buffer = new char[1024];
            try {
                Reader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"),1024);
                int n;
                while ((n = reader.read(buffer)) != -1) {
                    writer.write(buffer, 0, n);
                }
            } finally {
                inputStream.close();
            }
            return writer.toString();
        } else {
            return "";
        }
    }

    /* The Polyline encode/decode code below was adapted from Polyline utils
     * Copyright 2014 Google Inc. All rights reserved.
     *
     * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
     * file except in compliance with the License. You may obtain a copy of the License at
     *
     *     http://www.apache.org/licenses/LICENSE-2.0
     *
     * Unless required by applicable law or agreed to in writing, software distributed under
     * the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
     * ANY KIND, either express or implied. See the License for the specific language governing
     * permissions and limitations under the License.
     */

    /**
     * Decodes an encoded path string into a sequence of LatLngs.
     */
    public static List<GeoPoint> decode(final String encodedPath) {

        int len = encodedPath.length();

        final List<GeoPoint> path = new ArrayList<>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            path.add(new GeoPoint(lat * 1e-5, lng * 1e-5));
        }

        return path;
    }

    /**
     * Decodes an encoded path string into a sequence of RoutePoints.
     */
    public static List<RoutePoint> decodeToRoutePoints(final String encodedPath) {

        int len = encodedPath.length();

        final List<RoutePoint> path = new ArrayList<>(len / 2);
        int index = 0;
        int lat = 0;
        int lng = 0;

        while (index < len) {
            int result = 1;
            int shift = 0;
            int b;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lat += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            result = 1;
            shift = 0;
            do {
                b = encodedPath.charAt(index++) - 63 - 1;
                result += b << shift;
                shift += 5;
            } while (b >= 0x1f);
            lng += (result & 1) != 0 ? ~(result >> 1) : (result >> 1);

            RoutePoint routePoint = new RoutePoint();
            routePoint.setLatitude(lat * 1e-5);
            routePoint.setLongitude(lng * 1e-5);
            path.add(routePoint);
        }

        return path;
    }

    /**
     * Encodes a sequence of GeoPoints into an encoded path string.
     */
    public static String encode(final List<GeoPoint> path) {
        long lastLat = 0;
        long lastLng = 0;

        final StringBuffer result = new StringBuffer();

        for (final GeoPoint point : path) {
            long lat = Math.round(point.getLatitude() * 1e5);
            long lng = Math.round(point.getLongitude() * 1e5);

            long dLat = lat - lastLat;
            long dLng = lng - lastLng;

            encode(dLat, result);
            encode(dLng, result);

            lastLat = lat;
            lastLng = lng;
        }
        return result.toString();
    }

    private static void encode(long v, StringBuffer result) {
        v = v < 0 ? ~(v << 1) : v << 1;
        while (v >= 0x20) {
            result.append(Character.toChars((int) ((0x20 | (v & 0x1f)) + 63)));
            v >>= 5;
        }
        result.append(Character.toChars((int) (v + 63)));
    }

    /**
     * Encodes an array of GeoPoints into an encoded path string.
     */
    public static String encode(GeoPoint[] path) {
        return encode(Arrays.asList(path));
    }

    /**
     * Parses received OSRM JSON string and stores result in List<Route> Data.osrmRoutes
     */
    protected void parseOsrmResponse(final String json_string){

        Log.d(TAG, "Parsing OSRM response");

        Data.osrmRoutes = new ArrayList<>();

        try {
            JSONObject receivedJson = new JSONObject(json_string);

            if (receivedJson.getString("code").toUpperCase().equals("OK")) {

                JSONArray osrmRoutes = receivedJson.getJSONArray("routes");

                Data.sAlternativesNumber = osrmRoutes.length();
                Log.d(TAG, "Found routes# = " + Data.sAlternativesNumber);

                for (int i = 0; i < osrmRoutes.length(); i++) {

                    Route route = new Route();
                    route.setType("OSRM");

                    JSONObject osrmRoute = osrmRoutes.getJSONObject(i);
                    String encodedPolyline = osrmRoute.getString("geometry");
                    //Log.d(TAG, "EncPolyline: " + encodedPolyline);
                    route.setRoutePoints(decodeToRoutePoints(encodedPolyline));

                    Log.d(TAG, "Route: " + route);

                    float distance = Float.valueOf(osrmRoute.getString("distance"));

                    GpxUtils.simplifyRoute(route, (int)distance / 100, 6d);

                    Log.d(TAG, "Simplified: " + route);

                    /*
                     * Let's use the last waypoint name (if received) as a temporary route name
                     * In case we received nothing, use a generic, Linux timestamp based name
                     */
                    String tmpName = "OSMR_" + String.valueOf(System.currentTimeMillis()).substring(7);

                    JSONArray osrmWaypoints = receivedJson.getJSONArray("waypoints");
                    JSONObject lastWaypoint = osrmWaypoints.getJSONObject(osrmWaypoints.length() -1);
                    if (!lastWaypoint.getString("name").isEmpty()) {
                        tmpName = lastWaypoint.getString("name");
                    }
                    route.setName(tmpName);
                    Log.d(TAG, "Named: " + tmpName);

                    Data.osrmRoutes.add(route);
                }

            } else {

                Toast.makeText(getApplicationContext(), receivedJson.getString("code") + ": " + receivedJson.getString("message"), Toast.LENGTH_LONG).show();
            }

        } catch(JSONException e){
            e.printStackTrace();
        }
    }

}