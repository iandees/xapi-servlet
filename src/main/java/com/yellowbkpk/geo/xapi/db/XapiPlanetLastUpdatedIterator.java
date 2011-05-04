package com.yellowbkpk.geo.xapi.db;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

public class XapiPlanetLastUpdatedIterator implements ReleasableIterator<EntityContainer> {

    private long timestamp;
    private boolean hasWrittenTimestamp;

    public XapiPlanetLastUpdatedIterator(SimpleJdbcTemplate jdbcTemplate) {
        timestamp = jdbcTemplate.queryForLong("SELECT last_update FROM xapi_status");
    }

    @Override
    public boolean hasNext() {
        return !hasWrittenTimestamp;
    }

    @Override
    public EntityContainer next() {
        return new XapiTimestampContainer(timestamp);
    }

    @Override
    public void remove() {

    }

    @Override
    public void release() {
    }

}
