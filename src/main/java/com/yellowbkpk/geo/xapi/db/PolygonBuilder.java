// This software is released into the Public Domain.  See copying.txt for details.
package com.yellowbkpk.geo.xapi.db;

import org.postgis.LinearRing;
import org.postgis.Point;
import org.postgis.Polygon;

/**
 * Builds PostGIS Polygon objects based on a series of points.
 * 
 * @author Brett Henderson
 */
public class PolygonBuilder {
    /**
     * Creates a PostGIS Polygon object corresponding to the provided Point
     * list.
     * 
     * @param points
     *            The points to build a polygon from.
     * @return The Polygon object.
     */
    public static Polygon createPolygon(Point[] points) {
        if(points.length < 3) {
            throw new IllegalArgumentException("Not enough points to build a polygon for specified argument.");
        }
        
        Point first = points[0];
        Point last = points[points.length - 1];
        if(!first.equals(last)) {
            Point[] p = new Point[points.length + 1];
            System.arraycopy(points, 0, p, 0, points.length);
            p[points.length] = p[0];
            points = p;
        }
        
        LinearRing linearRing = new LinearRing(points);
        Polygon result = new Polygon(new LinearRing[] { linearRing });
        result.srid = 4326;

        return result;
    }

    public static Polygon buildBoundingPolygon(double left, double right, double top, double bottom) {
        Point[] bboxPoints = new Point[5];
        bboxPoints[0] = new Point(left, bottom);
        bboxPoints[1] = new Point(left, top);
        bboxPoints[2] = new Point(right, top);
        bboxPoints[3] = new Point(right, bottom);
        bboxPoints[4] = new Point(left, bottom);
        return PolygonBuilder.createPolygon(bboxPoints);
    }
}
