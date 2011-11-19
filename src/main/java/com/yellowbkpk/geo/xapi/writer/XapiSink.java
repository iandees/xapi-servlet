package com.yellowbkpk.geo.xapi.writer;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.task.v0_6.Sink;

public interface XapiSink extends Sink {

    void setExtra(String key, String value);

    void process(EntityContainer next);

    void complete();

}
