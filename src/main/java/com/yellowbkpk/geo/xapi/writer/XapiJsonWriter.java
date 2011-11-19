package com.yellowbkpk.geo.xapi.writer;

import java.io.BufferedWriter;
import java.io.File;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.json.common.BaseJsonWriter;
import org.openstreetmap.osmosis.json.common.CompressionMethod;

public class XapiJsonWriter extends BaseJsonWriter implements XapiSink {
    
    private XapiOsmJsonWriter osmWriter;
    
    
    /**
     * Creates a new instance.
     * 
     * @param writer
     *            The writer to send all data to.
     */
    public XapiJsonWriter(BufferedWriter writer) {
        super(writer);
        
        osmWriter = new XapiOsmJsonWriter("osm", 0);
    }
    
    
    /**
     * Creates a new instance.
     * 
     * @param file
     *            The file to write.
     * @param compressionMethod
     *            Specifies the compression method to employ.
     */
    public XapiJsonWriter(File file, CompressionMethod compressionMethod) {
        super(file, compressionMethod);
        
        osmWriter = new XapiOsmJsonWriter("osm", 0);
    }
    
    
    /**
     * {@inheritDoc}
     */
    public void process(EntityContainer entityContainer) {
        initialize();
        
        osmWriter.process(entityContainer);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void beginElementWriter() {
        osmWriter.begin();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void endElementWriter() {
        osmWriter.end();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    protected void setWriterOnElementWriter(BufferedWriter writer) {
        osmWriter.setWriter(writer);
    }
    
    
    public void setExtra(String key, String value) {
        osmWriter.setExtra(key, value);
    }
}
