package pl.nwg.dev.rambler.gpx;

import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import pt.karambola.geo.Units;
import pt.karambola.gpx.beans.Gpx;
import pt.karambola.gpx.beans.Route;
import pt.karambola.gpx.comparator.RouteComparator;
import pt.karambola.gpx.predicate.RouteFilter;

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

    /**
     * Index of currently selected route on the Data.sFilteredRoutes list
     * (Routes Browser - filtered view); null if nothing selected
     */
    public static Integer sSelectedRouteIdx = null;

    /**
     * We'll edit a copy of selected route in case user given up
     */
    public static Route sCopiedRoute;
    public static List<GeoPoint> routeNodes;

    /**
     * view filtering
     */
    public static List<String> sSelectedRouteTypes;
    public static Double sDstStartMinValue = null;
    public static Double sDstStartMaxValue = null;
    public static Double sLengthMinValue = null;
    public static Double sLengthMaxValue = null;
    public static List<Route> sFilteredRoutes = new ArrayList<>();
    public static RouteFilter sViewRouteFilter = new RouteFilter();

    /**
     * Comparator used in the Select Route dialog popup
     */
    public static Comparator<Route> rteComparator = RouteComparator.NAME;
    public static Route pickedRoute;
    public static Units sUnitsInUse = Units.METRIC; // this will have to be user-configurable

    public static final int POINTS_DISPLAY_LIMIT = 20;

}
