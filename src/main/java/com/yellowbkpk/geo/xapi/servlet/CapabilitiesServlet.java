package com.yellowbkpk.geo.xapi.servlet;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class CapabilitiesServlet extends HttpServlet {

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		resp.setContentType("text/xml");
		PrintWriter writer = resp.getWriter();

		writer.append("<osm version=\"0.6\" generator=\"Java XAPI Server\">\n");
		writer.append("  <!-- Note that since this server only supports XAPI, only the area maximum is real. -->\n");
		writer.append("  <api>\n");
		writer.append("    <version minimum=\"0.6\" maximum=\"0.6\"/>\n");
		writer.append("    <area maximum=\"10.0\"/>\n");
		writer.append("    <tracepoints per_page=\"5000\"/>\n");
		writer.append("    <waynodes maximum=\"2000\"/>\n");
		writer.append("    <changesets maximum_elements=\"50000\"/>\n");
		writer.append("    <timeout seconds=\"300\"/>\n");
		writer.append("  </api>\n");
		writer.append("</osm>\n");
		writer.close();
	}

}
