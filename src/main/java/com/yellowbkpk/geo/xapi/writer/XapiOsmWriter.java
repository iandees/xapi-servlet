package com.yellowbkpk.geo.xapi.writer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.osmosis.core.OsmosisConstants;
import org.openstreetmap.osmosis.xml.v0_6.impl.OsmWriter;
import org.openstreetmap.osmosis.xml.v0_6.impl.XmlConstants;

public class XapiOsmWriter extends OsmWriter {

    private Map<String, String> extras = new HashMap<String, String>();

    public XapiOsmWriter(String elementName, int indentLevel) {
        super(elementName, indentLevel, true);
    }

    public void begin() {
        beginOpenElement();
        
        addAttribute("version", XmlConstants.OSM_VERSION);
        addAttribute("generator", "Osmosis " + OsmosisConstants.VERSION);
        
        for (Entry<String, String> entry : extras.entrySet()) {
            addAttribute(entry.getKey(), entry.getValue());
        }
        
        endOpenElement(false);
    }

    public void setExtra(String key, String value) {
        extras.put(key, value);
    }

}
