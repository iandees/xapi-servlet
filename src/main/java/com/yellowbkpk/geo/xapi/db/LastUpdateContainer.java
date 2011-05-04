package com.yellowbkpk.geo.xapi.db;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.EntityProcessor;
import org.openstreetmap.osmosis.core.store.StoreClassRegister;
import org.openstreetmap.osmosis.core.store.StoreReader;
import org.openstreetmap.osmosis.core.store.StoreWriter;

public class LastUpdateContainer extends EntityContainer {

    private LastUpdateTimestamp lastUpdate;


    /**
     * Creates a new instance.
     * 
     * @param bound
     *            The bound to wrap.
     */
    public LastUpdateContainer(LastUpdateTimestamp bound) {
        this.lastUpdate = bound;
    }


    /**
     * Creates a new instance.
     * 
     * @param sr
     *            The store to read state from.
     * @param scr
     *            Maintains the mapping between classes and their identifiers
     *            within the store.
     */
    public LastUpdateContainer(StoreReader sr, StoreClassRegister scr) {
        lastUpdate = new LastUpdateTimestamp(sr, scr);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void store(StoreWriter sw, StoreClassRegister scr) {
        lastUpdate.store(sw, scr);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void process(EntityProcessor processor) {
        processor.process(this);
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LastUpdateTimestamp getEntity() {
        return lastUpdate;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public LastUpdateContainer getWriteableInstance() {
        if (lastUpdate.isReadOnly()) {
            return new LastUpdateContainer(lastUpdate.getWriteableInstance());
        } else {
            return this;
        }
    }
}
