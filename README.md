Rambler GPX Editor
===================================

This project is an attempt to create a GPX editor, based on existing commercial application code, 
but this time using opensource APIs and open data. 

**Activities:**

- **Routes Manager** *almost ready* | Allows to browse routes and launch other route-related activities;
- **Route Editor** *ready* | Allows to edit route properties, move/add/remove waypoints, draw a route manually;
- **Route Creator** *ready* | Automatically creates a route based on cardinal waypoints; 
uses [Open Source Routing Machine](http://project-osrm.org) and their Demo Server (at least temporarily); 
uses GeoKarambola GpxUtils.simplifyRoute to reduce waypoints number;
- **Route Optimizer** *ready* | Reduces waypoints number to desired amount on the fly; uses [GeoKarambola library](https://sourceforge.net/projects/geokarambola);
- **POI** (Points Of Interest) **Manager** *pending*;
- *Enter an idea here.*