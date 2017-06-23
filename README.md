Rambler GPX Editor
===================================

This project is an attempt to create a GPX editor, based on an existing commercial application code, 
but this time using opensource APIs and open data. 

**Activities:**

- **Route Manager** Allows to browse routes and launch other route-related activities;
- **Route Editor** Allows to edit route properties, move/add/remove waypoints, draw a route manually;
- **Route Creator** Automatically creates a route based on cardinal waypoints; 
uses [Open Source Routing Machine](http://project-osrm.org) and their Demo Server (at least 
temporarily); 
uses GeoKarambola GpxUtils.simplifyRoute to reduce waypoints number;
- **Route Optimizer** Reduces waypoints number to desired amount on the fly; uses 
[GeoKarambola library](https://sourceforge.net/projects/geokarambola);
- **POI Manager** Allows to browse and manage your POI (Points Of Interest) data;
- **Track Manager** *in progress* | The activity to  browse, delete, import, export tracks. Also 
conversion to routes could be done here.
- *Enter an idea here.*

See [CHANGELOG](https://github.com/nwg-piotr/RamblerGPXEditor/blob/master/CHANGELOG.md) to learn more.