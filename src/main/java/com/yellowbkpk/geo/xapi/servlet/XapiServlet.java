package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.logging.Logger;
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
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.yellowbkpk.geo.xapi.admin.XapiQueryStats;
import com.yellowbkpk.geo.xapi.db.PostgreSqlDatasetContext;
import com.yellowbkpk.geo.xapi.db.Selector;
import com.yellowbkpk.geo.xapi.db.Selector.BoundingBox;
import com.yellowbkpk.geo.xapi.query.XAPIParseException;
import com.yellowbkpk.geo.xapi.query.XAPIQueryInfo;

public class XapiServlet extends HttpServlet {
	private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost", "xapi", "xapi", "xapi", true, false, null);
	private static final DatabasePreferences preferences = new DatabasePreferences(false, false);
	private static final double MAX_BBOX_AREA = 10.0;

	private static Logger log = Logger.getLogger("XAPI");
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		XapiQueryStats tracker = XapiQueryStats.beginTracking(Thread.currentThread());
		try {
			// Parse URL
			XAPIQueryInfo info = null;
			try {
				StringBuffer urlBuffer = request.getRequestURL();
				if (request.getQueryString() != null) {
					urlBuffer.append("?").append(request.getQueryString());
				}
				String reqUrl = urlBuffer.toString();
				String query = reqUrl.substring(reqUrl.lastIndexOf('/') + 1);
				query = URLDecoder.decode(query, "UTF-8");
				tracker.receivedUrl(query, request.getRemoteHost());
				log.info("Query " + query);
				info = XAPIQueryInfo.fromString(query);
			} catch (XAPIParseException e) {
				tracker.error(e);
				response.sendError(500, "Could not parse query: " + e.getMessage());
				return;
			}
			
			if(info.getBboxSelectors().size() + info.getTagSelectors().size() < 1) {
				tracker.error();
				response.sendError(500, "Must have at least one selector.");
				return;
			}
			
			double totalArea = 0;
			for (BoundingBox bbox : info.getBboxSelectors()) {
				totalArea += bbox.area();
			}
			if(totalArea > MAX_BBOX_AREA) {
				tracker.error();
				response.sendError(500, "Maximum bounding box area is " + MAX_BBOX_AREA + " square degrees.");
				return;
			}
			
			// Query DB
			tracker.startDbQuery();
			long start = System.currentTimeMillis();
	        PostgreSqlDatasetContext datasetReader = new PostgreSqlDatasetContext(loginCredentials, preferences);
	        ReleasableIterator<EntityContainer> bboxData = makeRequestIterator(datasetReader, info);
	        if (bboxData == null) {
				response.sendError(500, "Unsupported operation.");
				return;
			}
			tracker.startSerialization();
			long middle = System.currentTimeMillis();
			log.info("Query complete: " + (middle - start) + "ms");
			
			// Build up a writer connected to the response output stream
			response.setContentType("text/xml; charset=utf-8");
			response.setHeader("Content-Disposition", "attachment; filename=\"xapi.osm\"");
			
			OutputStream outputStream = response.getOutputStream();
			String acceptEncodingHeader = request.getHeader("Accept-Encoding");
			if(acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip")) {
				outputStream = new GZIPOutputStream(outputStream);
				response.setHeader("Content-Encoding", "gzip");
			}
			
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream));
			
			// Serialize to the client
			Sink sink = new org.openstreetmap.osmosis.xml.v0_6.XmlWriter(out);

			long elements = 0;
			try {
				while (bboxData.hasNext()) {
					elements++;
					sink.process(bboxData.next());
				}
				
				sink.complete();
				
			} finally {
				bboxData.release();
				datasetReader.complete();
				tracker.elementsSerialized(elements);
			}
			
			out.flush();
			out.close();
			long end = System.currentTimeMillis();
			log.info("Serialization complete: " + (end - middle) + "ms");
			tracker.complete();
		} catch(OsmosisRuntimeException e) {
			tracker.error(e);
			throw e;
		} catch(IOException e) {
			tracker.error(e);
			throw e;
		}
	}

    /**
     * Creates an Osmosis releasable iterator over all the elements which are selected by the query.
     *
     * @param datasetReader The database context to use when executing queries.
     * @param info Object encapsulating the query information.
     * @return An iterator over all the entities which match the query, or null if the query could not be executed.
     */
    public static ReleasableIterator<EntityContainer> makeRequestIterator(PostgreSqlDatasetContext datasetReader, XAPIQueryInfo info) {
        ReleasableIterator<EntityContainer> bboxData = null;

        if(XAPIQueryInfo.RequestType.NODE.equals(info.getKind())) {
            bboxData = datasetReader.iterateSelectedNodes(info.getBboxSelectors(), info.getTagSelectors());
        } else if(XAPIQueryInfo.RequestType.WAY.equals(info.getKind())) {
            bboxData = datasetReader.iterateSelectedWays(info.getBboxSelectors(), info.getTagSelectors());
        } else if(XAPIQueryInfo.RequestType.RELATION.equals(info.getKind())) {
            bboxData = datasetReader.iterateSelectedRelations(info.getBboxSelectors(), info.getTagSelectors());
        } else if(XAPIQueryInfo.RequestType.ALL.equals(info.getKind())) {
            bboxData = datasetReader.iterateSelectedPrimitives(info.getBboxSelectors(), info.getTagSelectors());
        } else if(XAPIQueryInfo.RequestType.MAP.equals(info.getKind())) {
            Selector.BoundingBox boundingBox = info.getBboxSelectors().get(0);
            bboxData = datasetReader.iterateBoundingBox(boundingBox.getLeft(),
                                                        boundingBox.getRight(),
                                                        boundingBox.getTop(),
                                                        boundingBox.getBottom(), true);
        }

        return bboxData;
    }
}
