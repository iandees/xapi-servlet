package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public enum Filetype {
    xml("text/xml; charset=utf-8", "org.openstreetmap.osmosis.xml.v0_6.XmlWriter");

    private final String contentTypeStr;
    private final String filetypeSinkClassName;
    private Constructor<Sink> constructor;

    private Filetype(String contentType, String sinkClass) {
        this.contentTypeStr = contentType;
        this.filetypeSinkClassName = sinkClass;
    }

    public String getContentTypeString() {
        return contentTypeStr;
    }

    public Sink getSink(BufferedWriter writer) {
        try {
            if (isSinkInstalled()) {
                Sink sink = constructor.newInstance(writer);
                return sink;
            } else {
                return null;
            }
        } catch (SecurityException e) {
            throw new RuntimeException("Could not instantiate serialization sink.", e);
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not instantiate serialization sink.", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not instantiate serialization sink.", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Could not instantiate serialization sink.", e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException("Could not instantiate serialization sink.", e);
        }
    }

    public boolean isSinkInstalled() {
        try {
            Class<Sink> clazz = (Class<Sink>) Class.forName(filetypeSinkClassName);
            constructor = clazz.getConstructor(new Class[] { BufferedWriter.class });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
