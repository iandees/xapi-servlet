package com.yellowbkpk.geo.xapi.admin;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ThreadKillServlet extends HttpServlet {
    private static Logger log = Logger.getLogger("Admin");

    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String id = request.getParameter("id");
        log.info("Attempt to kill thread id " + id);
        XapiQueryStats stats = XapiQueryStats.getByThreadId(id);
        stats.killThread();
        response.sendRedirect("stats");
    }
}
