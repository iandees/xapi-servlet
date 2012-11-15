package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openstreetmap.osmosis.core.OsmosisRuntimeException;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.time.DateFormatter;
import org.openstreetmap.osmosis.core.time.DateParser;
import org.openstreetmap.osmosis.core.util.PropertiesPersister;
import org.springframework.dao.EmptyResultDataAccessException;

import com.yellowbkpk.geo.xapi.admin.XapiQueryStats;
import com.yellowbkpk.geo.xapi.db.PostgreSqlDatasetContext;
import com.yellowbkpk.geo.xapi.db.Selector;
import com.yellowbkpk.geo.xapi.writer.XapiSink;

public class TiledApiServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private static final DatabasePreferences preferences = new DatabasePreferences(false, false);

    private static final String LOCAL_STATE_FILE = "state.txt";

    private static Logger log = Logger.getLogger("API");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("starting TiledApiServlet doGet");

        String host = getServletContext().getInitParameter("xapi.db.host");
        String database = getServletContext().getInitParameter("xapi.db.database");
        String user = getServletContext().getInitParameter("xapi.db.username");
        String password = getServletContext().getInitParameter("xapi.db.password");
        DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials(host, database, user, password, true,
                false, null);

        String workingDirectory = getServletContext().getInitParameter("xapi.workingDirectory");

        String corsHeaderValue = getServletContext().getInitParameter("xapi.corsHeader");

        XapiQueryStats tracker = XapiQueryStats.beginTracking(Thread.currentThread());
        try {
            // Parse URL
            Filetype filetype = Filetype.xml;
            int zoom, x, y;
            try {
                StringBuffer urlBuffer = request.getRequestURL();
                String queryString = request.getQueryString();
                if (request.getQueryString() != null) {
                    urlBuffer.append("?").append(queryString);
                }
                String reqUrl = urlBuffer.toString();
                tracker.receivedUrl(reqUrl, request.getRemoteHost());
                
                reqUrl = URLDecoder.decode(reqUrl, "UTF-8");

                log.info("reqUrl: " + reqUrl);
                
                Pattern pattern = Pattern.compile("\\/api\\/0\\.6\\/tiled\\/(\\d{0,2})\\/(\\d*)\\/(\\d*)");
                Matcher matcher = pattern.matcher(reqUrl);
                if (!matcher.find()) {
                	tracker.error();
                	response.sendError(500, "Invalid request.");
                	return;
                }

                zoom = Integer.parseInt(matcher.group(1));
            	x = Integer.parseInt(matcher.group(2));
            	y = Integer.parseInt(matcher.group(3));
            } catch (NumberFormatException e) {
                tracker.error(e);
                response.sendError(500, "Could not parse query: " + e.getMessage());
                return;
            }
            
            if (!filetype.isSinkInstalled()) {
                response.sendError(500, "I don't know how to serialize that.");
                return;
            }
            
            if (zoom < 12) {
            	response.sendError(400, "Zoom level is too low.");
            	return;
            }
            
            // Build bounding box from tile
            double left = tile2lon(x, zoom);
            double right = tile2lon(x+1, zoom);
            double top = tile2lat(y, zoom);
            double bottom = tile2lat(y+1, zoom);

            // Query DB
            ReleasableIterator<EntityContainer> bboxData = null;
            PostgreSqlDatasetContext datasetReader = null;
            long middle;
            long elements = 0;
            try {
                tracker.startDbQuery();
                long start = System.currentTimeMillis();
                datasetReader = new PostgreSqlDatasetContext(loginCredentials, preferences);

                bboxData = datasetReader.iterateBoundingBox(left, right, top, bottom, true);
                
                tracker.startSerialization();
                middle = System.currentTimeMillis();
                log.info("Tile " + zoom + "/" + x + "/" + y + " complete: " + (middle - start) + "ms");

                // Build up a writer connected to the response output stream
                response.setContentType(filetype.getContentTypeString());

                OutputStream outputStream = response.getOutputStream();
                String acceptEncodingHeader = request.getHeader("Accept-Encoding");
                if (acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip")) {
                    outputStream = new GZIPOutputStream(outputStream);
                    response.setHeader("Content-Encoding", "gzip");
                }

                if (corsHeaderValue != null) {
                    response.setHeader("Access-Control-Allow-Origin", corsHeaderValue);
                }

                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream));

                // Serialize to the client
                XapiSink sink = filetype.getSink(out);

                try {
                    Date planetDate = getDatabaseLastModifiedDate(workingDirectory);
                    sink.setExtra("xapi:planetDate", new DateFormatter().format(planetDate));
                    sink.setExtra("xmlns:xapi", "http://jxapi.openstreetmap.org/");
                } catch (Exception e) {
                    log.log(Level.WARNING, "Could not read state.txt so skipped setting planet date.");
                }

                while (bboxData.hasNext()) {
                    elements++;
                    sink.process(bboxData.next());
                }

                sink.complete();

                out.flush();
                out.close();
                tracker.elementsSerialized(elements);
            } catch (Exception e) {
                tracker.error(e);
                log.log(Level.WARNING, "Error serializing: ", e);
                return;
            } finally {
                if (bboxData != null) {
                    bboxData.release();
                }
                if (datasetReader != null) {
                    datasetReader.complete();
                    datasetReader.release();
                }
            }

            long end = System.currentTimeMillis();
            log.info("Tile " + zoom + "/" + x + "/" + y + " serialization complete: " + (end - middle) + "ms");
            tracker.complete();
        } catch (OsmosisRuntimeException e) {
            tracker.error(e);
            throw e;
        } catch (IOException e) {
            tracker.error(e);
            throw e;
        } catch (RuntimeException e) {
            tracker.error(e);
            throw e;
        }
    }
    
    private double tile2lat(int y, int zoom) {
    	double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, zoom);
        return Math.toDegrees(Math.atan(Math.sinh(n)));
	}

	private double tile2lon(int x, int zoom) {
		return x / Math.pow(2.0, zoom) * 360.0 - 180.0;
	}

	@Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doOptions(req, resp);

        String corsHeaderValue = getServletContext().getInitParameter("xapi.corsHeader");

        if (corsHeaderValue != null) {
            resp.setHeader("Access-Control-Allow-Origin", corsHeaderValue);
        }
    }

    private Date getDatabaseLastModifiedDate(String workingDirectory) {
        PropertiesPersister localStatePersistor = new PropertiesPersister(new File(workingDirectory, LOCAL_STATE_FILE));
        Properties properties = localStatePersistor.load();
        return new DateParser().parse(properties.getProperty("timestamp"));
    }
}
