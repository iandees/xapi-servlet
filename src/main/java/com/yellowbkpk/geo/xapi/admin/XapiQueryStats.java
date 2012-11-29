package com.yellowbkpk.geo.xapi.admin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class XapiQueryStats {

    static final int MAX_STATS = 150;
    private static LinkedList<XapiQueryStats> allStats = new LinkedList<XapiQueryStats>();
    private static Map<String, XapiQueryStats> activeThreads = new HashMap<String, XapiQueryStats>();
    private static Map<String, Set<String>> activeQueries = new HashMap<String, Set<String>>();

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
	private List<Timepoint> timepoints = new ArrayList<Timepoint>();

    private XapiQueryStats(Thread requestThread) {
        this.startTime = System.currentTimeMillis();
        this.thread = requestThread;
        this.threadId = Long.toString(this.thread.getId(), 26);
        this.state = QueryState.NOT_STARTED;
        activeThreads.put(threadId, this);
    }

    public static synchronized XapiQueryStats beginTracking(Thread requestThread) {
        XapiQueryStats newStat = new XapiQueryStats(requestThread);
        allStats.addFirst(newStat);
        if (allStats.size() > MAX_STATS) {
            allStats.removeLast();
        }
        return newStat;
    }

    public static synchronized XapiQueryStats getByThreadId(String id) {
        return activeThreads.get(id);
    }

    public void receivedUrl(String reqUrl, String remoteHost) {
        this.request = reqUrl;
        this.remoteHost = remoteHost;
        this.state = QueryState.CONNECTED;
        synchronized (activeQueries) {
            Set<String> queries = activeQueries.get(remoteHost);
            if (queries == null) {
                queries = new HashSet<String>();
                activeQueries.put(remoteHost, queries);
            }
            queries.add(reqUrl);
        }
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
        synchronized (activeQueries) {
            Set<String> queries = activeQueries.get(remoteHost);
            if (queries != null) {
                queries.remove(this.request);
                if (queries.isEmpty()) {
                    activeQueries.remove(remoteHost);
                }
            }
        }
    }

    public void error(Exception e) {
        completionTime = System.currentTimeMillis();
        state = QueryState.ERROR;
        exception = e;
        thread = null;
        threadId = null;
        activeThreads.remove(threadId);
        synchronized (activeQueries) {
            Set<String> queries = activeQueries.get(remoteHost);
            if (queries != null) {
                queries.remove(this.request);
                if (queries.isEmpty()) {
                    activeQueries.remove(remoteHost);
                }
            }
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

    public long getDbQueryElapsedTime() {
        return (serializationStartTime - dbStartTime);
    }

    public long getSerializationElapsedTime() {
        return (completionTime - serializationStartTime);
    }

    public long getOverallElapsedTime() {
        return (completionTime - startTime);
    }

    public boolean isActive() {
        return thread != null;
    }

    public boolean hasException() {
        return exception != null;
    }

    public long getElementCount() {
        return elementCount;
    }

    public void killThread() {
        if (isActive()) {
            thread.interrupt();
            thread = null;
            threadId = null;
            activeThreads.remove(threadId);
            state = QueryState.KILLED;
            completionTime = System.currentTimeMillis();
            synchronized (activeQueries) {
                Set<String> queries = activeQueries.get(remoteHost);
                if (queries != null) {
                    queries.remove(this.request);
                    if (queries.isEmpty()) {
                        activeQueries.remove(remoteHost);
                    }
                }
            }
        }
    }

    public String getThreadId() {
        return threadId;
    }

    public static boolean isQueryAlreadyRunning(String query, String host) {
        synchronized (activeQueries) {
            Set<String> queries = activeQueries.get(host);

            boolean alreadyRunning = false;
            if (queries != null) {
                alreadyRunning = queries.contains(query);
            }

            return alreadyRunning;
        }
    }

    public Exception getException() {
        return exception;
    }

    class Timepoint {
    	public Timepoint(String timepointName, long timeMillis) {
    		this.name = timepointName;
    		this.time = timeMillis;
		}
		public String name;
    	public long time;
    }

	public void recordTimepoint(String timepointName) {
		this.timepoints.add(new Timepoint(timepointName, System.currentTimeMillis()));
	}

	public List<Timepoint> getTimepoints() {
		return this.timepoints;
	}

}
