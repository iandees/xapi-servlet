package com.yellowbkpk.geo.xapi.db;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.openstreetmap.osmosis.core.domain.v0_6.Entity;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.store.StoreClassRegister;
import org.openstreetmap.osmosis.core.store.StoreReader;

public class LastUpdateTimestamp extends Entity implements Comparable<LastUpdateTimestamp> {

    private static final List<Tag> EMPTY_TAG_LIST = new ArrayList<Tag>(0);
    private Date time;
    
    public LastUpdateTimestamp(StoreReader sr, StoreClassRegister scr) {
        super(sr, scr);

        this.time = new Date(sr.readLong() * 1000L);
    }
    
    public LastUpdateTimestamp(Date lastUpdate) {
        super(0, 0, new Date(), OsmUser.NONE, 0, EMPTY_TAG_LIST); // minimal underlying entity
        this.time = lastUpdate;
    }

    @Override
    public int compareTo(LastUpdateTimestamp o) {
        return o.time.compareTo(time);
    }

    @Override
    public EntityType getType() {
        return null;
    }

    @Override
    public Entity getWriteableInstance() {
        return this;
    }

}
