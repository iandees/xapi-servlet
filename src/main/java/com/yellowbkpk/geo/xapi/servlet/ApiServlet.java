package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

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

public class ApiServlet extends HttpServlet {
	private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost", "xapi", "xapi", "xapi", true, false, null);
	private static final DatabasePreferences preferences = new DatabasePreferences(false, false);

	private static Logger log = Logger.getLogger("API");
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Parse URL
		String primitiveType;
		ArrayList<Long> ids = new ArrayList<Long>();
		try {
			StringBuffer urlBuffer = request.getRequestURL();
			if (request.getQueryString() != null) {
				urlBuffer.append("?").append(request.getQueryString());
			}
			String reqUrl = urlBuffer.toString();
			int lastSlash = reqUrl.lastIndexOf('/');
			int secondSlash = reqUrl.substring(0, lastSlash).lastIndexOf('/');
			String primitiveIdStr = reqUrl.substring(lastSlash + 1);
			primitiveType = reqUrl.substring(secondSlash + 1, lastSlash);
			primitiveIdStr = URLDecoder.decode(primitiveIdStr, "UTF-8");
			
			String[] primitiveIds = primitiveIdStr.split(",");
			ids.ensureCapacity(primitiveIds.length);
			for (String string : primitiveIds) {
				ids.add(Long.parseLong(string));
			}
			
			log.info("Query for " + primitiveType + " " + primitiveIdStr);
		} catch (NumberFormatException e) {
			response.sendError(500, "Could not parse query: " + e.getMessage());
			return;
		}
		
		// Query DB
		long start = System.currentTimeMillis();
		ReleasableIterator<EntityContainer> bboxData;
		PostgreSqlDatasetContext datasetReader = new PostgreSqlDatasetContext(loginCredentials, preferences);
		if("node".equals(primitiveType)) {
			bboxData = datasetReader.iterateNodes(ids);
		} else if("way".equals(primitiveType)) {
			bboxData = datasetReader.iterateWays(ids);
		} else if("relation".equals(primitiveType)) {
			bboxData = datasetReader.iterateRelations(ids);
		} else {
			response.sendError(500, "Unsupported operation.");
			return;
		}
		long middle = System.currentTimeMillis();
		log.info("Query complete: " + (middle - start) + "ms");
		
		// Build up a writer connected to the response output stream
		response.setContentType("application/json");
//		response.setHeader("Content-Disposition", "attachment; filename=\"xapi.osm\"");
		
		OutputStream outputStream = response.getOutputStream();
		String acceptEncodingHeader = request.getHeader("Accept-Encoding");
		if(acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip")) {
			outputStream = new GZIPOutputStream(outputStream);
			response.setHeader("Content-Encoding", "gzip");
		}
		
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream));
		
		// Serialize to the client
		Sink sink = new org.openstreetmap.osmosis.json.v0_6.JsonWriter(out);
		
		try {
			while (bboxData.hasNext()) {
				sink.process(bboxData.next());
			}
			
			sink.complete();
			
		} finally {
			bboxData.release();
			datasetReader.complete();
		}
		
		out.flush();
		out.close();
		long end = System.currentTimeMillis();
		log.info("Serialization complete: " + (end - middle) + "ms");
	}
}
