package com.yellowbkpk.geo.xapi.servlet;

import java.io.BufferedWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import com.yellowbkpk.geo.xapi.writer.XapiSink;

public enum Filetype {
    xml("text/xml; charset=utf-8", "com.yellowbkpk.geo.xapi.writer.XapiXmlWriter"),
    json("application/json", "com.yellowbkpk.geo.xapi.writer.XapiJsonWriter"),
    jsonp("application/json", "com.yellowbkpk.geo.xapi.writer.XapiJsonWriter");

    private final String contentTypeStr;
    private final String filetypeSinkClassName;
    private Constructor<XapiSink> constructor;

    private Filetype(String contentType, String sinkClass) {
        this.contentTypeStr = contentType;
        this.filetypeSinkClassName = sinkClass;
    }

    public String getContentTypeString() {
        return contentTypeStr;
    }

    public XapiSink getSink(BufferedWriter writer) {
        try {
            if (isSinkInstalled()) {
                XapiSink sink = constructor.newInstance(writer);
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
            Class<XapiSink> clazz = (Class<XapiSink>) Class.forName(filetypeSinkClassName);
            constructor = clazz.getConstructor(new Class[] { BufferedWriter.class });
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
