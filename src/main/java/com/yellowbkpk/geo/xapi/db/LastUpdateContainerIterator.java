package com.yellowbkpk.geo.xapi.db;

import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;

public class LastUpdateContainerIterator implements ReleasableIterator<LastUpdateContainer> {

    private ReleasableIterator<LastUpdateTimestamp> source;

    public LastUpdateContainerIterator(ReleasableIterator<LastUpdateTimestamp> source) {
        this.source = source;
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasNext() {
        return source.hasNext();
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public LastUpdateContainer next() {
        return new LastUpdateContainer(source.next());
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void remove() {
        source.remove();
    }
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void release() {
        source.release();
    }

}
