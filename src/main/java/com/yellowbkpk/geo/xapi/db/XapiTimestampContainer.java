package com.yellowbkpk.geo.xapi.db;

import java.util.Date;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.store.StoreClassRegister;
import org.openstreetmap.osmosis.core.store.StoreWriter;

public class XapiTimestampContainer extends EntityContainer {

    private Date timestamp;

    public XapiTimestampContainer(long timestamp) {
        this.timestamp = new Date(timestamp * 1000L);
    }

    @Override
    public void store(StoreWriter sw, StoreClassRegister scr) {
        sw.writeLong(timestamp.getTime());
    }

    @Override
    public Entity getEntity() {
        return null;
    }

    @Override
    public EntityContainer getWriteableInstance() {
        return null;
    }

    @Override
    public void process(EntityProcessor processor) {

    }

}
