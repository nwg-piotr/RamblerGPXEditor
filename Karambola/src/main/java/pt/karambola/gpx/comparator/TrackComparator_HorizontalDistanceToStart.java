/*
 * TrackComparator_HorizontalDistanceToStart.java
 *
 * Copyright (c) 2016 Karambola. All rights reserved.
 *
 * This file is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 3.0
 * of the License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this file; if not, write to the
 * Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */


package pt.karambola.gpx.comparator;

import java.util.Comparator;

import pt.karambola.gpx.beans.Point;
import pt.karambola.gpx.beans.Track;
import pt.karambola.gpx.beans.TrackPoint;
import pt.karambola.gpx.util.GpxUtils;

public class
TrackComparator_HorizontalDistanceToStart
	implements Comparator<Track>
{
	private final 	Point 	ref = new Point() ;

	public
	TrackComparator_HorizontalDistanceToStart( final double refLat, final double refLon )
	{
		ref.setLatitude( refLat ) ;
		ref.setLongitude( refLon ) ;
	}


	@Override
	public int
	compare( final Track t1, final Track t2 )
	{
		if (t1 == t2)	return  0 ;
		if (t1 == null)	return  1 ;
		if (t2 == null) return -1 ;

		final TrackPoint start1 = t1.getTrackSegments().get(0).getTrackPoints( ).get( 0 ) ;
		final double 	 d1 	= GpxUtils.horizontalDistance( ref, start1 ) ;

		final TrackPoint start2 = t2.getTrackSegments().get(0).getTrackPoints( ).get( 0 ) ;
		final double 	 d2 	= GpxUtils.horizontalDistance( ref, start2 ) ;

		return Double.compare( d1, d2 ) ;
	}
}
