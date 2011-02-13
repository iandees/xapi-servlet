package com.yellowbkpk.geo.xapi.admin;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.postgresql.util.MD5Digest;

public class XapiQueryStats {

	private static final int MAX_STATS = 100;
	private static LinkedList<XapiQueryStats> allStats = new LinkedList<XapiQueryStats>();
	private static Map<String, XapiQueryStats> activeThreads = new HashMap<String, XapiQueryStats>();
	
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
	private String threadId;

	private XapiQueryStats(Thread requestThread) {
		this.startTime = System.currentTimeMillis();
		this.thread = requestThread;
		this.threadId = Long.toString(this.thread.getId(), 26);
		XapiQueryStats.activeThreads.put(threadId, this);
		this.state = QueryState.NOT_STARTED;
	}

	public static synchronized XapiQueryStats beginTracking(Thread requestThread) {
		XapiQueryStats newStat = new XapiQueryStats(requestThread);
		allStats.addFirst(newStat);
		return newStat;
	}
	
	public static synchronized XapiQueryStats getByThreadId(String id) {
		return activeThreads.get(id);
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
		threadId = null;
		activeThreads.remove(threadId);
		if(allStats.size() > MAX_STATS) {
			allStats.removeLast();
			allStats.addFirst(this);
		}
	}
	
	public void error(Exception e) {
		completionTime = System.currentTimeMillis();
		state = QueryState.ERROR;
		exception = e;
		thread = null;
		threadId = null;
		activeThreads.remove(threadId);
		if(allStats.size() > MAX_STATS) {
			allStats.removeLast();
			allStats.addFirst(this);
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
		return thread != null;
	}

	public long getElementCount() {
		return elementCount;
	}
	
	public void killThread() {
		if(isActive()) {
			thread.interrupt();
			thread = null;
			threadId = null;
			activeThreads.remove(threadId);
			state = QueryState.KILLED;
			completionTime = System.currentTimeMillis();
			if(allStats.size() > MAX_STATS) {
				allStats.removeLast();
				allStats.addFirst(this);
			}
		}
	}
	
	public String getThreadId() {
		return threadId;
	}

}
