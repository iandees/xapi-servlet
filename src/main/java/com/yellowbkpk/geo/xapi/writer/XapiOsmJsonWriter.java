package com.yellowbkpk.geo.xapi.writer;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openstreetmap.osmosis.core.OsmosisConstants;
import org.openstreetmap.osmosis.json.v0_6.impl.OsmWriter;
import org.openstreetmap.osmosis.json.v0_6.impl.XmlConstants;

public class XapiOsmJsonWriter extends OsmWriter {

    private Map<String, String> extras = new HashMap<String, String>();

    public XapiOsmJsonWriter(String elementName, int indentLevel) {
        super(indentLevel, true);
    }

    public void begin() {
        startObject(true);
        
        addAttribute("version", XmlConstants.OSM_VERSION, true);
        addAttribute("generator", "Osmosis " + OsmosisConstants.VERSION, false);
        
        for (Entry<String, String> entry : extras.entrySet()) {
            addAttribute(entry.getKey(), entry.getValue(), false);
        }
    }

    public void setExtra(String key, String value) {
        extras.put(key, value);
    }

}
