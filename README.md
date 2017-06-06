Rambler GPX Editor
===================================

This project is an attempt to create a GPX editor, based on existing commercial application code, 
but this time using opensource APIs and open data. 

**Activities:**

- **Routes Manager** *partially ready* | Allows to browse routes and launch other route-related activities;
- **Route Editor** *planned* | Allows to edit route properties, move/add/remove waypoints, draw a route manually;
- **Route Creator** *ready* | Automatically creates a route based on cardinal waypoints; 
uses [Open Source Routing Machine](http://project-osrm.org) and their Demo Server (at least temporarily); 
uses GeoKarambola GpxUtils.simplifyRoute to reduce waypoints number;
- **Route Simplifier** *planned* Reduces waypoints number to desired amount on the fly; uses GeoKarambola library;
- **POI** (Points Of Interest) Manager *planned*;
- *Enter an idea here.*