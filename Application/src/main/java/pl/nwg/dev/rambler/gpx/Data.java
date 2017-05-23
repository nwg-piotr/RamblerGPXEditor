package pl.nwg.dev.rambler.gpx;

import org.osmdroid.util.GeoPoint;

import java.util.List;

import pt.karambola.gpx.beans.Gpx;
import pt.karambola.gpx.beans.Route;

/**
 * Created by piotr on 16.05.17.
 * Common data stored here
 */

public final class Data {

    public static List<GeoPoint> sCardinalGeoPoints;
    public static GeoPoint sCurrentPosition;
    public static List<Route> osrmRoutes;

    public static String sRoutingProfile;

    public static int sAlternativesNumber;
    public static Integer sSelectedAlternative;

    public static Gpx mGpx = null;

    public static Gpx mPoisGpx = null;
    public static Gpx mRoutesGpx = null;
    public static Gpx mTracksGpx = null;

    public static String lastOpenFile = null;

    public static Integer sLastZoom;
    public static GeoPoint sLastCenter;

}
