package com.yellowbkpk.geo.xapi.servlet;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.runtime.RecognitionException;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.impl.Selector;

import com.yellowbkpk.geo.xapi.XAPIQueryInfo;

public class XapiServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Parse URL
		XAPIQueryInfo info = null;
		try {
			String requestedUrl = request.getRequestURL().toString();
			String query = requestedUrl.substring(requestedUrl.lastIndexOf('/') + 1);
			query = URLDecoder.decode(query, "UTF-8");
			info = XAPIQueryInfo.fromString(query);
		} catch (RecognitionException e) {
			response.sendError(500, "Could not parse query: " + e.getMessage());
		}
		
		// Query DB
		
		// Serialize output
		response.getWriter().println(info.getKind() + " request:");
		for (Selector select : info.getSelectors()) {
			response.getWriter().println("  " + select.getWhereString() + ", " + Arrays.toString(select.getWhereParam()));
		}
	}
}
