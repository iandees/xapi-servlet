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
        Polygon result;

        result = new Polygon(new LinearRing[] { new LinearRing(points) });
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
