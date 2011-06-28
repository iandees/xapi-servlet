package com.yellowbkpk.geo.xapi.servlet;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openstreetmap.osmosis.core.time.DateFormatter;
import org.openstreetmap.osmosis.core.time.DateParser;
import org.openstreetmap.osmosis.core.util.PropertiesPersister;

public class CapabilitiesServlet extends HttpServlet {
    private static final String LOCAL_STATE_FILE = "state.txt";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        float maxBboxArea = Float.parseFloat(getServletContext().getInitParameter("xapi.max_bbox_area"));

        String workingDirectory = getServletContext().getInitParameter("xapi.workingDirectory");
        Date planetDate = getDatabaseLastModifiedDate(workingDirectory);
        
        resp.setContentType("text/xml");
        PrintWriter writer = resp.getWriter();

        writer.append("<osm version=\"0.6\" generator=\"Java XAPI Server\" ");
        writer.append("xmlns:xapi=\"http://jxapi.openstreetmap.org/\" ");
        writer.append("xapi:planetDate=\"").append(new DateFormatter().format(planetDate)).append("\">\n");
        writer.append("  <!-- Note that since this server only supports XAPI, only the area maximum is real. -->\n");
        writer.append("  <api>\n");
        writer.append("    <version minimum=\"0.6\" maximum=\"0.6\"/>\n");
        writer.append("    <area maximum=\"").append(Float.toString(maxBboxArea)).append("\"/>\n");
        writer.append("    <tracepoints per_page=\"5000\"/>\n");
        writer.append("    <waynodes maximum=\"2000\"/>\n");
        writer.append("    <changesets maximum_elements=\"50000\"/>\n");
        writer.append("    <timeout seconds=\"300\"/>\n");
        writer.append("  </api>\n");
        writer.append("</osm>\n");
        writer.close();
    }

    private Date getDatabaseLastModifiedDate(String workingDirectory) {
        PropertiesPersister localStatePersistor = new PropertiesPersister(new File(workingDirectory, LOCAL_STATE_FILE));
        Properties properties = localStatePersistor.load();
        return new DateParser().parse(properties.getProperty("timestamp"));
    }

}
