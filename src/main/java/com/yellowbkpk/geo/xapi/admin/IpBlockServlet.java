package com.yellowbkpk.geo.xapi.admin;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DateFormat;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.yellowbkpk.geo.xapi.admin.RequestFilter.AddressFilter;

public class IpBlockServlet extends HttpServlet {

    private static final DateFormat timeFormat = DateFormat.getDateTimeInstance();

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        long now = System.currentTimeMillis();

        response.setContentType("text/html");
        PrintWriter writer = response.getWriter();
        
        writer.println("<html><body>");

        String[] filters = request.getParameterValues("filter");
        String[] reasons = request.getParameterValues("reason");
        if (filters != null && filters.length > 0 && reasons != null && reasons.length > 0) {
            String filter = filters[0];
            String reason = reasons[0];
            if (filter.length() < 1 || reason.length() < 1) {
                writer.println("<h4>Didn't Add Filter</h4>");
                writer.println("<p>Missing some required field.</p>");
            } else {
                RequestFilter.addFilter(new RequestFilter.AddressFilter(filter, reason));

                writer.println("<h4>Added Filter</h4>");
                writer.append("<p>Filter regex is <tt>").append(filter).append("</tt> with a reason of \"")
                        .append(reason).println("\".</p>");
            }
        }

        String[] deletes = request.getParameterValues("delete");
        if (deletes != null && deletes.length > 0) {
            int id = Integer.parseInt(deletes[0]);
            RequestFilter.deleteFilter(id);

            writer.println("<h4>Deleted Filter</h4>");
            writer.append("<p>Filter was removed.</p>");
        }
        
        writer.append("<h1>Blocked Addresses</h1>\n");
        writer.println("<table border='1'>");
        writer.println("<tr>");
        writer.println("<th>Created</th>");
        writer.println("<th>Filter</th>");
        writer.println("<th>Reason</th>");
        writer.println("<th>Action</th>");
        writer.println("</tr>\n");
        
        int exNum = 0;
        boolean even = false;
        List<AddressFilter> allFilters = RequestFilter.getAllAddressFilters();
        for (AddressFilter f : allFilters) {
            writer.append("<tr class=\"").append(even ? "even_row" : "odd_row").println("\">");
            writer.append("<td>").append(timeFormat.format(f.getCreated())).println("</td>");
            writer.append("<td><tt>").append(f.getRegex()).println("</tt></td>");
            writer.append("<td>").append(f.getReason()).println("</td>");
            writer.append("<td><a href=\"?delete=").append(f.getId().toString()).println("\">Del</a></td>");
            writer.println("</tr>\n");
            even = !even;
        }
        writer.println("</table>\n");
        
        writer.println("<form method=\"get\">");
        writer.println("<label for=\"filter\">Filter</label><input type=\"text\" name=\"filter\" id=\"filter\" size=\"16\"><br/>");
        writer.println("<label for=\"reason\">Reason</label><input type=\"text\" name=\"reason\" id=\"reason\" size=\"32\"><br/>");
        writer.println("<input type=\"submit\">");
        writer.println("</form>\n");
        
        writer.println("</body></html>");
    }

}
