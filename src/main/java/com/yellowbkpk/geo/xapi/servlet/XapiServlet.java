package com.yellowbkpk.geo.xapi.servlet;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.antlr.runtime.ANTLRStringStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;

import com.yellowbkpk.geo.xapi.antlr.XAPILexer;
import com.yellowbkpk.geo.xapi.antlr.XAPIParser;

public class XapiServlet extends HttpServlet {
	public void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// Parse URL
		try {
			String requestedUrl = request.getRequestURL().toString();
			String query = requestedUrl.substring(requestedUrl.lastIndexOf('/') + 1);
			CharStream stream = new ANTLRStringStream(query);
			XAPILexer lexer = new XAPILexer(stream);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			XAPIParser parser = new XAPIParser(tokens);
			parser.xapi().getTree();
		} catch (RecognitionException e) {
			response.sendError(500, "Could not parse query: " + e.getMessage());
		}
		
		// Query DB
		
		// Serialize output
		response.getWriter().println("Hello World");
	}
}
