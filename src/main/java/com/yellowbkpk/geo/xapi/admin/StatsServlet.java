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

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long now = System.currentTimeMillis();

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();

        writer.println("<html><head>");
        writer.println("<script type=\"text/javascript\">");
        writer.println("<!--");
        writer.println("  function toggle(id) {");
        writer.println("    var e = document.getElementById(id);");
        writer.println("    if(e.style.display == 'block')");
        writer.println("      e.style.display = 'none';");
        writer.println("    else");
        writer.println("      e.style.display = 'block';");
        writer.println("  }");
        writer.println("//-->");
        writer.println("</script>");
        writer.println("</head><body>");

        writer.append("<h1>Last ").append(Integer.toString(XapiQueryStats.MAX_STATS)).println(" Requests</h1>\n");
        writer.println("<table border='1'>");
        writer.println("<tr>");
        writer.println("<th>Timestamp</th>");
        writer.println("<th>Remote Addr</th>");
        writer.println("<th>State</th>");
        writer.println("<th>Request</th>");
        writer.println("<th>Elements</th>");
        writer.println("<th>Runtime</th>");
        // writer.println("<th>Action</th>");
        writer.println("</tr>\n");

        int exNum = 0;
        int timeNum = 0;
        boolean even = false;
        List<XapiQueryStats> allTrackers = XapiQueryStats.getAllTrackers();
        for (XapiQueryStats stat : allTrackers) {
            writer.append("<tr class=\"").append(even ? "even_row" : "odd_row").println("\">");
            writer.append("<td>").append(timeFormat.format(stat.getStartTime())).println("</td>");
            writer.append("<td>").append(stat.getRemoteAddress()).println("</td>");
            if (stat.hasException()) {
                writer.append("<td><a href=\"#\" onClick=\"toggle('ex-").append(Integer.toString(exNum)).println("');return false;\">");
                writer.println(stat.getState().toString());
                writer.println("</a>");
                writer.append("<div id='ex-").append(Integer.toString(exNum)).println("' style='display:none;'><pre>");
                stat.getException().printStackTrace(writer);
                writer.println("</pre></div>");
                writer.println("</td>");
                exNum++;
            } else {
                writer.append("<td>").append(stat.getState().toString()).println("</td>");
            }
            writer.append("<td><tt>").append(stat.getRequest()).println("</tt></td>");
            if (stat.isActive()) {
                writer.println("<td>-</td>");
                writer.append("<td><a href=\"#\" onClick=\"toggle('time-").append(Integer.toString(timeNum)).append("');return false;\">").append(prettyTimeOngoing(stat.getStartTime())).println("</a>");
                writer.append("<div id='time-").append(Integer.toString(timeNum)).println("' style='display:none;'><table border='1'>");
                writer.println("<tr><th>Timepoint</th><th>Millis</th></tr>");
                XapiQueryStats.Timepoint prev = null;
                for (XapiQueryStats.Timepoint tp : stat.getTimepoints()) {
                	if (prev != null) {
                		long elapsed = tp.time - prev.time;
                		writer.append("<tr><td>").append(tp.name).append("</td><td>").append(prettyTime(elapsed)).println("</td></tr>");
                	}
                	prev = tp;
				}
                writer.println("</table></div>");
                writer.println("</td>");
            } else {
                writer.append("<td>").append(Long.toString(stat.getElementCount())).println("</td>");
                writer.append("<td><a href=\"#\" onClick=\"toggle('time-").append(Integer.toString(timeNum)).append("');return false;\">").append(prettyTime(stat.getOverallElapsedTime())).println("</a>");
                writer.append("<div id='time-").append(Integer.toString(timeNum)).println("' style='display:none;'><table border='1'>");
                writer.println("<tr><th>Timepoint</th><th>Millis</th></tr>");
                XapiQueryStats.Timepoint prev = null;
                for (XapiQueryStats.Timepoint tp : stat.getTimepoints()) {
                	if (prev != null) {
                		long elapsed = tp.time - prev.time;
                		writer.append("<tr><td>").append(tp.name).append("</td><td>").append(prettyTime(elapsed)).println("</td></tr>");
                	}
                	prev = tp;
				}
                writer.println("</table></div>");
                writer.println("</td>");
            }
            timeNum++;
            writer.println("</tr>\n");
            even = !even;
        }
        writer.println("</table>\n");
        writer.println("</body></html>");
    }

    private String prettyTime(long elapsedMillis) {
        StringBuilder b = new StringBuilder();
        long x = elapsedMillis;
        long millis = x % 1000;
        x /= 1000;
        long seconds = x % 60;
        x /= 60;
        long minutes = x % 60;
        x /= 60;
        long hours = x % 24;
        x /= 24;
        long days = x;

        if (days > 0) {
            b.append(days);
            b.append(" day");
            b.append(days == 1 ? "" : "s");
        }
        if (hours > 0) {
            if (days > 0) {
                b.append(" ");
            }
            b.append(hours);
            b.append(" hr");
            b.append(hours == 1 ? "" : "s");
        }
        if (minutes > 0) {
            if (hours > 0) {
                b.append(" ");
            }
            b.append(minutes);
            b.append(" min");
            b.append(minutes == 1 ? "" : "s");
        }
        if (minutes > 0) {
            b.append(" ");
        }
        b.append(seconds);
        b.append(".");
        b.append(millis);
        b.append(" sec");
        b.append(seconds == 1 ? "" : "s");
        return b.toString();
    }

    private String prettyTimeOngoing(long startTime) {
        long deltaMs = System.currentTimeMillis() - startTime;
        return prettyTime(deltaMs);
    }

    private String prettyTime(long startTime, long endTime) {
        long deltaMs = endTime - startTime;
        return prettyTime(deltaMs);
    }
}
