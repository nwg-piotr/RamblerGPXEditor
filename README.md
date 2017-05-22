Rambler GPX Editor
===================================

This project is an attempt to create a GPX editor, based on existing commercial application code, 
but this time using opensource APIs and open data. 

At the moment the project has just one useful activity, which is a Route Creator, drawing a route between Start, Mid-points 
and Finish on the basis of Open Street Routing Machine data returned by the OSRM Demo Server.

As soon as this sandbox starts working well enough, I'll start re-implementing the rest of existing activities,
 and adding them to the project:

- Routes Manager;
- Route Editor;
- Route Simplifier;
- POI (Points Of Interest) Manager;
- *Enter an idea here*.

## Recent changes 

*Possibly it should be moved to another file*

###(23rd May 2017)

All the code imported into a fresh Android Studio project, and shared from scratch.

###(22nd May 2017)

MainActivity (together with the FileBrowser) added, needs further adaptation. At the moment the POI Manager button 
does nothing (this isn't going to change soon), and the Route Managar button leads temporarily to the Route Creator activity.
