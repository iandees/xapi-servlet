package com.yellowbkpk.geo.xapi.servlet;

import com.yellowbkpk.geo.xapi.db.PostgreSqlDatasetContext;
import com.yellowbkpk.geo.xapi.query.XAPIParseException;
import com.yellowbkpk.geo.xapi.query.XAPIQueryInfo;
import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlCopyWriter;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlTruncator;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

/**
 * Tests of the XAPI servlet functionality.
 *
 * This pulls the database into the test loop, but there doesn't seem to be any way of mocking it out
 * in any clever way without re-implementing most of postgres' parser frontend.
 */
public class XapiServletTest {
    private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost", "xapi_test", "xapi", "xapi", true, false, null);
    private static final DatabasePreferences preferences = new DatabasePreferences(false, false);

    @BeforeMethod(alwaysRun = true)
    public void populateDB() {
        PostgreSqlCopyWriter writer = new PostgreSqlCopyWriter(loginCredentials, preferences, NodeLocationStoreType.InMemory);
        Date timestamp = new Date();
        LinkedList<Tag> tags = new LinkedList<Tag>();
        tags.add(new Tag("amenity", "pub"));
        writer.process(new NodeContainer(new Node(1, 1, timestamp, OsmUser.NONE, 1, tags, 0.0, 0.0)));
        writer.complete();
    }

    @AfterMethod(alwaysRun = true)
    public void truncateDB() {
        PostgreSqlTruncator truncator = new PostgreSqlTruncator(loginCredentials, preferences);
        truncator.run();
    }

    @Test
    public void testTagNodeSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        execQuery("node[amenity=pub]", expected);
    }

    /**
     * Simple tuple-class of entity type and ID to allow the expected results of the tests to
     * be judged.
     */
    private static class EntityRef {
        private EntityType type;
        private long id;

        public EntityRef(EntityType t, long i) {
            type = t;
            id = i;
        }

        EntityType getType() {
            return type;
        }

        long getID() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            EntityRef entityRef = (EntityRef) o;

            return (id == entityRef.id) && (type == entityRef.type);
        }

        @Override
        public int hashCode() {
            int result = type != null ? type.hashCode() : 0;
            result = 31 * result + (int) (id ^ (id >>> 32));
            return result;
        }

        @Override
        public String toString() {
            return "Element(" + type.toString() + ", " + id + ")";
        }
    }

    public void execQuery(String query, Set<EntityRef> expectedResults) {
        XAPIQueryInfo info = null;
        try {
            info = XAPIQueryInfo.fromString(query);
        } catch (XAPIParseException ex) {
            Assert.fail("Parsing query shouldn't fail.", ex);
        }

        PostgreSqlDatasetContext context = new PostgreSqlDatasetContext(loginCredentials, preferences);
        ReleasableIterator<EntityContainer> iterator = XapiServlet.makeRequestIterator(context, info);

        HashSet<EntityRef> workingSet = new HashSet<EntityRef>(expectedResults);
        while (iterator.hasNext()) {
            EntityContainer ent = iterator.next();
            EntityRef ref = new EntityRef(ent.getEntity().getType(), ent.getEntity().getId());
            Assert.assertTrue(workingSet.contains(ref), "Unexpected element.");
            workingSet.remove(ref);
        }

        Assert.assertTrue(workingSet.isEmpty(), "Returned set doesn't contain all expected elements.");
    }
}
