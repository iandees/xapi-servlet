package com.yellowbkpk.geo.xapi.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class XapiServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Parse URL
		String requestedUrl = request.getRequestURL().toString();
		String query = requestedUrl.substring(requestedUrl.lastIndexOf('/'));
		
		// Query DB
		
		// Serialize output
		response.getWriter().println("Hello World");
	}
}
