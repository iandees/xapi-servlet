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
import org.openstreetmap.osmosis.pgsnapshot.common.DatabaseContext;
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlCopyWriter;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlTruncator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.*;

/**
 * Tests of the XAPI servlet functionality.
 *
 * This pulls the database into the test loop, but there doesn't seem to be any way of mocking it out
 * in any clever way without re-implementing most of postgres' parser frontend.
 */
public class XapiServletTest {
    private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost", "xapi_test", "xapi", "xapi", true, false, null);
    private static final DatabasePreferences preferences = new DatabasePreferences(false, false);
    private DatabaseContext dbCtx = null;

    /**
     * @return The data to be used to fill the database for the tests.
     */
    private Collection<EntityContainer> dataSample() {
        LinkedList<EntityContainer> list = new LinkedList<EntityContainer>();

        list.add(node(1, 1, 0.0, 0.0, "amenity", "pub"));
        list.add(node(2, 1, 1.0, 1.0, "amenity", "pub"));
        list.add(node(3, 1, 0.0, 0.0, "amenity", "restaurant"));

        return list;
    }

    @Test
    public void testTagNodeSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        execQuery("node[amenity=pub]", expected);

        expected.clear();
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[amenity=restaurant]", expected);
    }

    @Test
    public void testTagNodeWildcardSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[amenity=*]", expected);
    }

    @Test
    public void testTagAndBboxNodeSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        execQuery("node[amenity=pub][bbox=-0.01,-0.01,0.01,0.01]", expected);

        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[bbox=-0.01,-0.01,0.01,0.01][amenity=*]", expected);
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

    /**
     * Executes a query and compares it to the expected result.
     *
     * @param query The XAPI URL query to make.
     * @param expectedResults Set of entities which are expected to be returned.
     */
    private void execQuery(String query, Set<EntityRef> expectedResults) {
        XAPIQueryInfo info = null;
        try {
            info = XAPIQueryInfo.fromString(query);
        } catch (XAPIParseException ex) {
            Assert.fail("Parsing query shouldn't fail.", ex);
        }

        PostgreSqlDatasetContext context = new PostgreSqlDatasetContext(loginCredentials, preferences);
        ReleasableIterator<EntityContainer> iterator = XapiServlet.makeRequestIterator(context, info);

        try {
            HashSet<EntityRef> workingSet = new HashSet<EntityRef>(expectedResults);
            while (iterator.hasNext()) {
                EntityContainer ent = iterator.next();
                EntityRef ref = new EntityRef(ent.getEntity().getType(), ent.getEntity().getId());
                Assert.assertTrue(workingSet.contains(ref), "Unexpected element.");
                workingSet.remove(ref);
            }

            Assert.assertTrue(workingSet.isEmpty(), "Returned set doesn't contain all expected elements.");

        } finally {
            iterator.release();
            iterator = null;

            context.complete();
            context.release();
            context = null;
        }
    }

    // utility function to build a node
    private EntityContainer node(long id, int version, double lon, double lat, String... tags) {
        Date timestamp = new Date();
        LinkedList<Tag> constructedTags = new LinkedList<Tag>();
        for (int i = 0; i < tags.length; i += 2) {
            constructedTags.add(new Tag(tags[i], tags[i+1]));
        }
        Node n = new Node(id, version, timestamp, OsmUser.NONE, 1, constructedTags, lon, lat);
        return new NodeContainer(n);
    }

    // internal function to drop all data to the database (ensure that the tests are run on the same
    // data each time.)
    private void truncate() {
        dbCtx = new DatabaseContext(loginCredentials);

        // make sure that the table is empty first
        PostgreSqlTruncator truncator = new PostgreSqlTruncator(loginCredentials, preferences);
        truncator.run();

        // the truncator will release the database context anyway...
        dbCtx = null;
    }

    // internal method to setup the data being used in the tests.
    private void setupSample() {
        dbCtx = new DatabaseContext(loginCredentials);

        // then copy in the sample data
        PostgreSqlCopyWriter writer = new PostgreSqlCopyWriter(loginCredentials, preferences, NodeLocationStoreType.InMemory);
        Collection<EntityContainer> ents = dataSample();
        for (EntityContainer ec : ents) {
            writer.process(ec);
        }
        writer.complete();
        writer.release();

        dbCtx.release();
        dbCtx = null;
    }

    @BeforeClass(alwaysRun = true)
    public void populateDB() {
        truncate();

        setupSample();

        dbCtx = new DatabaseContext(loginCredentials);
    }

    @AfterClass(alwaysRun = true)
    public void truncateDB() {
        dbCtx.release();
        dbCtx = null;

        truncate();
    }

}
