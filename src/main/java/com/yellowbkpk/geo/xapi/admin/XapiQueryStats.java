package com.yellowbkpk.geo.xapi.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class XapiQueryStats {

	private static final int MAX_STATS = 100;
	private static List<XapiQueryStats> allStats = new ArrayList<XapiQueryStats>(100);
	
	private QueryState state;
	
	private long startTime;
	private long dbStartTime;
	private long serializationStartTime;
	private long completionTime;
	
	private Thread thread;
	private String request;
	private Exception exception;
	private long elementCount;
	private String remoteHost;

	private XapiQueryStats(Thread requestThread) {
		this.startTime = System.currentTimeMillis();
		this.thread = requestThread;
		this.state = QueryState.NOT_STARTED;
	}

	public static synchronized XapiQueryStats beginTracking(Thread requestThread) {
		XapiQueryStats newStat = new XapiQueryStats(requestThread);
		allStats.add(newStat);
		return newStat;
	}

	public void receivedUrl(String reqUrl, String remoteHost) {
		this.request = reqUrl;
		this.remoteHost = remoteHost;
		this.state = QueryState.CONNECTED;
	}

	public void startDbQuery() {
		dbStartTime = System.currentTimeMillis();
		state = QueryState.DATABASE_QUERY;
	}

	public void startSerialization() {
		serializationStartTime = System.currentTimeMillis();
		state = QueryState.SERIALIZATION;
	}

	public void complete() {
		completionTime = System.currentTimeMillis();
		state = QueryState.DONE;
		thread = null;
		if(allStats.size() > MAX_STATS) {
			allStats.remove(allStats.size() - 1);
			allStats.add(this);
		}
	}
	
	public void error(Exception e) {
		completionTime = System.currentTimeMillis();
		state = QueryState.ERROR;
		exception = e;
		thread = null;
		if(allStats.size() > MAX_STATS) {
			allStats.remove(allStats.size() - 1);
			allStats.add(this);
		}
	}

	public void error() {
		error(null);
	}

	public void elementsSerialized(long elements) {
		elementCount = elements;
	}

	public static synchronized List<XapiQueryStats> getAllTrackers() {
		return Collections.unmodifiableList(allStats);
	}

	public String getRemoteAddress() {
		return this.remoteHost;
	}

	public QueryState getState() {
		return this.state;
	}

	public String getRequest() {
		return this.request;
	}

	public long getStartTime() {
		return startTime;
	}

	public long getEndTime() {
		return completionTime;
	}

	public boolean isActive() {
		return !QueryState.ERROR.equals(state)
				&& !QueryState.DONE.equals(state)
				&& !QueryState.NOT_STARTED.equals(state);
	}

	public long getElementCount() {
		return elementCount;
	}

}
