CHANGELOG
===============
**13th June, 2017**

- Route Creator sets activity result NEW_ROUTE_ADDED to force the Routes Browser open the just created route properties dialog.
- 'Clear all routes' activated in the Browser menu.
- [Build 0.0.3.0](https://github.com/nwg-piotr/RamblerGPXEditor/raw/master/Application/RamblerGPXeditor-release-0.0.3.0.apk) uploaded.

**12th June, 2017**

- A null pointer exception bug fixed in the Routes Browser. 
- Routes Browser: importing routes and tracks from external gpx files added.
- Build 0.0.2.9 uploaded (not really tested, seems to work pretty well at first sight).

**10th June, 2017**

- 'Draw manually' entry added to the Routes Browser menu. Creates a new, empty route and launches the Route Editor.
- Build 0.0.2.8 uploaded.

**9th June, 2017**

- Route Point properties dialog added (touch the point to open the dialog popup).
- Last route point marker added (checkered flag - touch to select the route).
- Pencil button in the Routes Browser has now two actions attached: touch to edit route points, long press to edit route properties.
- Build 0.0.2.7 uploaded.

**8th June, 2017**

- In the Routes Browser selecting 'Edit' launches the route properties dialog first. You'll find the 'Edit points' button inside.
Also the on-screen 'Edit' button added (pencil).
- Route Editor: the number of route points (markers) to draw at a time limited to 20 nearest 
to the center of the map. Utils.getNearestRoutePoints(IGeoPoint mapCenter, Route route) used instead 
of original GpxUtils.getPointNamesSortedByDistance).
- Google Play Services version updated (11.0.0).
- Fresh build uploaded (0.0.2.5).

**7th June, 2017**

- **RouteEditorActivity** skeleton code added, some basic features already work. 

**6th June, 2017**

- Route labels in the browser touchable: click to select the route.

**30th May, 2017**

- User-Agent and copyright info updated according to [Important notes on using osmdroid in your app](https://github.com/osmdroid/osmdroid/wiki/Important-notes-on-using-osmdroid-in-your-app) Wiki.
- Routes Browser: the Search button (magnifier) launches the dialog to select a route by name. The dialog layout needs to be improved.
Also sorting order must be user-configurable (long press the button to change?).
- Build 0.0.2.4 uploaded.

**29th May, 2017**

- Route deletion activated in the Routes Browser menu. Removes the selected route from Data.mRoutesGpx, and gets back to the refreshed browser display.
- Build 0.0.2.2 uploaded.

**28th May, 2017**

- Route labels added to the Routes Browser (in the half way route point). To be decided later if to make the labels clickable.
- Build 0.0.2.1 uploaded.


**27th May, 2017**

- Refactoring: former Route Picker from now on is the Routes Browser.
- Bugs in the Routes Browser fixed.
- (simple) Support for the routing server error added.
- New build: 0.0.2.0.apk uploaded.

**26th May, 2017**

Route picker:
- **view filtering added.** Long press the Zoom-to-fit button to enter values. *Known bug in the distance filter and the route length filter: 
entering values which produce no results hangs the app. To be fixed tomorrow.*

**25th May, 2017**

Route picker: 
- **buttons to select a route activated (Prev / Next).** Touch the map anywhere to clear selection. *Given up on selecting routes by touching polylines: it's damned difficult to use.*
- The route prompt line displays annotated name of currently selected route.

New apk 0.0.1.9 uploaded.

**24th May, 2017**
- Map display improved, thanks to [osmdroid sample code](https://github.com/osmdroid/osmdroid/tree/master/OpenStreetMapViewer).
- Route Manager button now leads to a very basic browser activity, which will finally become the Route Picker (main activity of the Route Manager).
- Route Creator attached to the Route Manager menu. Save button active, adds the just created route to Data.mRoutesGpx.

**23rd May, 2017**

- All the code imported into a fresh Android Studio project, to finally get rid of the old app rudiments. Project shared from scratch.

**22nd May, 2017**

- MainActivity (together with the FileBrowser) added, needs further adaptation. At the moment the POI Manager button does nothing (this isn't going to change soon), and the Route Manager button leads temporarily to the Route Creator activity.