package com.yellowbkpk.geo.xapi.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatsServlet extends HttpServlet {

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		
		writer.append("<h1>Active</h1>\n");
		writer.append("<table>");
		writer.append("<tr>");
		writer.append("<th>Remote Addr</th>");
		writer.append("<th>State</th>");
		writer.append("<th>Request</th>");
		writer.append("</tr>\n");
		List<XapiQueryStats> activeTrackers = XapiQueryStats.getActiveTrackers();
		for (XapiQueryStats stat : activeTrackers) {
			writer.append("<tr>");
			writer.append("<td>").append(stat.getRemoteAddress()).append("</td>");
			writer.append("<td>").append(stat.getState().toString()).append("</td>");
			writer.append("<td>").append(stat.getRequest()).append("</td>");
			writer.append("</tr>\n");
		}
		writer.append("</table>\n");
		
		writer.append("<h1>All Requests</h1>\n");
		writer.append("<table>");
		writer.append("<tr>");
		writer.append("<th>Remote Addr</th>");
		writer.append("<th>State</th>");
		writer.append("<th>Request</th>");
		writer.append("</tr>\n");
		List<XapiQueryStats> allTrackers = XapiQueryStats.getAllTrackers();
		for (XapiQueryStats stat : allTrackers) {
			writer.append("<tr>");
			writer.append("<td>").append(stat.getRemoteAddress()).append("</td>");
			writer.append("<td>").append(stat.getState().toString()).append("</td>");
			writer.append("<td>").append(stat.getRequest()).append("</td>");
			writer.append("</tr>\n");
		}
		writer.append("</table>\n");
	}
}
