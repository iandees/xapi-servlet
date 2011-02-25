package com.yellowbkpk.geo.xapi.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class StatsServlet extends HttpServlet {

	private static final DateFormat timeFormat = DateFormat.getDateTimeInstance();

	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		long now = System.currentTimeMillis();
		
		response.setContentType("text/html");
		PrintWriter writer = response.getWriter();
		
		writer.append("<h1>All Requests</h1>\n");
		writer.append("<table border='1'>");
		writer.append("<tr>");
		writer.append("<th>Timestamp</th>");
		writer.append("<th>Remote Addr</th>");
		writer.append("<th>State</th>");
		writer.append("<th>Request</th>");
		writer.append("<th>Elements</th>");
		writer.append("<th>Runtime</th>");
		//writer.append("<th>Action</th>");
		writer.append("</tr>\n");
		List<XapiQueryStats> allTrackers = XapiQueryStats.getAllTrackers();
		for (XapiQueryStats stat : allTrackers) {
			writer.append("<tr>");
			writer.append("<td>").append(timeFormat.format(stat.getStartTime())).append("</td>");
			writer.append("<td>").append(stat.getRemoteAddress()).append("</td>");
			writer.append("<td>").append(stat.getState().toString()).append("</td>");
			writer.append("<td>").append(stat.getRequest()).append("</td>");
			if(stat.isActive()) {
				writer.append("<td>-</td>");
				writer.append("<td>").append(prettyTime(stat.getStartTime(), now)).append("</td>");
				//writer.append("<td><a href='kill?id=").append(stat.getThreadId()).append("'>Kill</a>");
			} else {
				writer.append("<td>").append(Long.toString(stat.getElementCount())).append("</td>");
				writer.append("<td>").append(prettyTime(stat.getStartTime(), stat.getEndTime())).append("</td>");
			}
			writer.append("</tr>\n");
		}
		writer.append("</table>\n");
	}

	private String prettyTime(long startTime, long endTime) {
		long deltaMs = endTime - startTime;
		StringBuilder b = new StringBuilder();
		long x = deltaMs;
		long millis = x % 1000;
		x /= 1000;
		long seconds = x % 60;
		x /= 60;
		long minutes = x % 60;
		x /= 60;
		long hours = x % 24;
		x /= 24;
		long days = x;
		
		if(days > 0) {
			b.append(days);
			b.append(" day");
			b.append(days == 1 ? "" : "s");
		}
		if(hours > 0) {
			if(days > 0) { b.append(" "); }
			b.append(hours);
			b.append(" hr");
			b.append(hours == 1 ? "" : "s");
		}
		if(minutes > 0) {
			if(hours > 0) { b.append(" "); }
			b.append(minutes);
			b.append(" min");
			b.append(minutes == 1 ? "" : "s");
		}
		if(minutes > 0) { b.append(" "); }
		b.append(seconds);
		b.append(".");
		b.append(millis);
		b.append(" sec");
		b.append(seconds == 1 ? "" : "s");
		return b.toString();
	}
}
