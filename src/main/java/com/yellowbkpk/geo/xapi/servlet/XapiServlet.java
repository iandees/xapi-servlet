package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

import com.yellowbkpk.geo.xapi.db.PostgreSqlDatasetContext;
import com.yellowbkpk.geo.xapi.db.Selector;
import com.yellowbkpk.geo.xapi.query.XAPIParseException;
import com.yellowbkpk.geo.xapi.query.XAPIQueryInfo;

public class XapiServlet extends HttpServlet {
	private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost", "xapi", "xapi", "xapi", true, false, null);
	private static final DatabasePreferences preferences = new DatabasePreferences(false, false);

	private static Logger log = Logger.getLogger("XAPI");
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
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
			log.info("Query " + query);
			info = XAPIQueryInfo.fromString(query);
		} catch (XAPIParseException e) {
			response.sendError(500, "Could not parse query: " + e.getMessage());
			return;
		}
		
		// Query DB
		long start = System.currentTimeMillis();
		ReleasableIterator<EntityContainer> bboxData;
		PostgreSqlDatasetContext datasetReader = new PostgreSqlDatasetContext(loginCredentials, preferences);
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
		} else {
			response.sendError(500, "Unsupported operation.");
			return;
		}
		long middle = System.currentTimeMillis();
		log.info("Query complete: " + (middle - start) + "ms");
		
		// Build up a writer connected to the response output stream
		response.setContentType("text/xml; charset=utf-8");
		response.setHeader("Content-Disposition", "attachment; filename=\"xapi.osm\"");
		log.info("Just about to start serializing.");
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(response.getOutputStream()));
		
		// Serialize to the client
		Sink sink = new org.openstreetmap.osmosis.xml.v0_6.XmlWriter(out);
		
		try {
			while (bboxData.hasNext()) {
				sink.process(bboxData.next());
			}
			
			sink.complete();
			
		} finally {
			bboxData.release();
		}
		
		out.flush();
		out.close();
		long end = System.currentTimeMillis();
		log.info("Serialization complete: " + (end - middle) + "ms");
	}
}
