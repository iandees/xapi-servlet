package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.util.ArrayList;
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

public class ApiServlet extends HttpServlet {
	private static final DatabasePreferences preferences = new DatabasePreferences(false, false);

    private String host = getServletContext().getInitParameter("xapi.db.host");
    private String database = getServletContext().getInitParameter("xapi.db.database");
    private String user = getServletContext().getInitParameter("xapi.db.username");
    private String password = getServletContext().getInitParameter("xapi.db.password");
    private DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials(host, database, user, password, true, false, null);
	
	private static Logger log = Logger.getLogger("API");
	
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		XapiQueryStats tracker = XapiQueryStats.beginTracking(Thread.currentThread());
		try {
			// Parse URL
			String primitiveType;
			ArrayList<Long> ids = new ArrayList<Long>();
			Filetype filetype = Filetype.xml;
			try {
				StringBuffer urlBuffer = request.getRequestURL();
				if (request.getQueryString() != null) {
					urlBuffer.append("?").append(request.getQueryString());
				}
				String reqUrl = urlBuffer.toString();
				tracker.receivedUrl(reqUrl, request.getRemoteHost());
				int lastSlash = reqUrl.lastIndexOf('/');
				int secondSlash = reqUrl.substring(0, lastSlash).lastIndexOf('/');
				String primitiveIdStr = reqUrl.substring(lastSlash + 1);
				int lastDot = primitiveIdStr.lastIndexOf(".");
				if(lastDot > 0) {
					String filetypeStr = primitiveIdStr.substring(lastDot + 1);
					filetype = Filetype.valueOf(filetypeStr);
					primitiveIdStr = primitiveIdStr.substring(0, lastDot);
				}
				primitiveType = reqUrl.substring(secondSlash + 1, lastSlash);
				primitiveIdStr = URLDecoder.decode(primitiveIdStr, "UTF-8");
				
				String[] primitiveIds = primitiveIdStr.split(",");
				ids.ensureCapacity(primitiveIds.length);
				for (String string : primitiveIds) {
					ids.add(Long.parseLong(string));
				}
				
				log.info("Query for " + primitiveType + " " + primitiveIdStr);
			} catch (NumberFormatException e) {
				tracker.error(e);
				response.sendError(500, "Could not parse query: " + e.getMessage());
				return;
			}
			
			if(!filetype.isSinkInstalled()) {
				response.sendError(500, "I don't know how to serialize that.");
				return;
			}
			
			// Query DB
			ReleasableIterator<EntityContainer> bboxData = null;
			PostgreSqlDatasetContext datasetReader = null;
			long middle;
			long elements = 0;
			try {
    			tracker.startDbQuery();
    			long start = System.currentTimeMillis();
    			datasetReader = new PostgreSqlDatasetContext(loginCredentials, preferences);
    			
    			if("node".equals(primitiveType)) {
    				bboxData = datasetReader.iterateNodes(ids);
    			} else if("way".equals(primitiveType)) {
    				bboxData = datasetReader.iterateWays(ids);
    			} else if("relation".equals(primitiveType)) {
    				bboxData = datasetReader.iterateRelations(ids);
    			} else {
    				tracker.error();
    				response.sendError(500, "Unsupported operation.");
    				return;
    			}
    			tracker.startSerialization();
    			middle = System.currentTimeMillis();
    			log.info(primitiveType + " " + ids + " complete: " + (middle - start) + "ms");
    			
    			// Build up a writer connected to the response output stream
    			response.setContentType(filetype.getContentTypeString());
    			
    			OutputStream outputStream = response.getOutputStream();
    			String acceptEncodingHeader = request.getHeader("Accept-Encoding");
    			if(acceptEncodingHeader != null && acceptEncodingHeader.contains("gzip")) {
    				outputStream = new GZIPOutputStream(outputStream);
    				response.setHeader("Content-Encoding", "gzip");
    			}
    			
    			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(outputStream));
    			
    			// Serialize to the client
    			Sink sink = filetype.getSink(out);
    			
				while (bboxData.hasNext()) {
					elements++;
					sink.process(bboxData.next());
				}
				
				sink.complete();
				
				out.flush();
				out.close();
			} finally {
				bboxData.release();
				datasetReader.complete();
				tracker.elementsSerialized(elements);
			}
			
			long end = System.currentTimeMillis();
			log.info("Serialization complete: " + (end - middle) + "ms");
			tracker.complete();
		} catch(OsmosisRuntimeException e) {
			tracker.error(e);
			throw e;
		} catch(IOException e) {
			tracker.error(e);
			throw e;
		} catch(RuntimeException e) {
			tracker.error(e);
			throw e;
		}
	}
}
