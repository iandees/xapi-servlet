package com.yellowbkpk.geo.xapi.servlet;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.openstreetmap.osmosis.core.container.v0_6.EntityContainer;
import org.openstreetmap.osmosis.core.container.v0_6.NodeContainer;
import org.openstreetmap.osmosis.core.container.v0_6.RelationContainer;
import org.openstreetmap.osmosis.core.container.v0_6.WayContainer;
import org.openstreetmap.osmosis.core.database.DatabaseLoginCredentials;
import org.openstreetmap.osmosis.core.database.DatabasePreferences;
import org.openstreetmap.osmosis.core.domain.v0_6.EntityType;
import org.openstreetmap.osmosis.core.domain.v0_6.Node;
import org.openstreetmap.osmosis.core.domain.v0_6.OsmUser;
import org.openstreetmap.osmosis.core.domain.v0_6.Relation;
import org.openstreetmap.osmosis.core.domain.v0_6.RelationMember;
import org.openstreetmap.osmosis.core.domain.v0_6.Tag;
import org.openstreetmap.osmosis.core.domain.v0_6.Way;
import org.openstreetmap.osmosis.core.domain.v0_6.WayNode;
import org.openstreetmap.osmosis.core.lifecycle.ReleasableIterator;
import org.openstreetmap.osmosis.pgsnapshot.common.DatabaseContext;
import org.openstreetmap.osmosis.pgsnapshot.common.NodeLocationStoreType;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlCopyWriter;
import org.openstreetmap.osmosis.pgsnapshot.v0_6.PostgreSqlTruncator;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.yellowbkpk.geo.xapi.db.PostgreSqlDatasetContext;
import com.yellowbkpk.geo.xapi.query.XAPIParseException;
import com.yellowbkpk.geo.xapi.query.XAPIQueryInfo;

/**
 * Tests of the XAPI servlet functionality.
 * 
 * This pulls the database into the test loop, but there doesn't seem to be any
 * way of mocking it out in any clever way without re-implementing most of
 * postgres' parser frontend.
 */
public class XapiServletTest {
    private static final DatabaseLoginCredentials loginCredentials = new DatabaseLoginCredentials("localhost",
            "xapi_test", "xapi", "xapi", true, false, null);
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
        list.add(node(4, 1, 0.0, 0.0));
        list.add(node(5, 1, 2.0, 2.0, "shop", "pub"));
        list.add(node(6, 1, 2.0, 2.0, "shop", "supermarket"));

        long way1_nodes[] = { 1, 2, 3 };
        long way2_nodes[] = {};
        list.add(way(1, 1, way1_nodes, "highway", "residential"));
        list.add(way(2, 1, way2_nodes, "highway", "residential"));

        RelationMember rel1_members[] = { new RelationMember(1, EntityType.Node, "foo"),
                new RelationMember(1, EntityType.Way, "bar") };
        list.add(relation(1, 1, rel1_members, "type", "route"));

        return list;
    }

    /**** tag selection tests ****/

    // simple tag selection
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

    // wildcard tag selection
    @Test
    public void testTagNodeWildcardSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[amenity=*]", expected);
    }

    // wildcard tag selection on any type
    @Test
    public void testTagAnyWildcardSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("*[amenity=*]", expected);
    }

    // select by tag using multiple keys
    @Test
    public void testTagWithMultipleKeys() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 5));
        execQuery("node[amenity|shop=pub]", expected);
    }

    // select by tag using multiple values
    @Test
    public void testTagWithMultipleValues() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[amenity=pub|restaurant]", expected);
        execQuery("node[amenity=pub][amenity=restaurant]", expected);
    }

    // select by tag using multiple keys and wildcard
    @Test
    public void testTagWithMultipleKeysAndWildcard() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        expected.add(new EntityRef(EntityType.Node, 5));
        expected.add(new EntityRef(EntityType.Node, 6));
        execQuery("node[amenity|shop=*]", expected);
    }

    /**** bbox selection tests ****/

    @Test
    public void testBboxNodeSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 3));
        expected.add(new EntityRef(EntityType.Node, 4));
        execQuery("node[bbox=-0.01,-0.01,0.01,0.01]", expected);
    }

    @Test
    public void testBboxAnySelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 3));
        expected.add(new EntityRef(EntityType.Node, 4));
        // note that the way also brings in node#2, even though it's outside the
        // bbox
        expected.add(new EntityRef(EntityType.Way, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        // and it brings in the relation via node#1.
        expected.add(new EntityRef(EntityType.Relation, 1));
        execQuery("*[bbox=-0.01,-0.01,0.01,0.01]", expected);
    }

    /**** child predicate tests ****/

    // node has no tags
    @Test
    public void testNodeChildPredicateNotTags() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 4));
        execQuery("node[not(tag)]", expected);
    }

    // node has tags
    @Test
    public void testNodeChildPredicateTags() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        expected.add(new EntityRef(EntityType.Node, 5));
        expected.add(new EntityRef(EntityType.Node, 6));
        execQuery("node[tag]", expected);
    }

    // anything which has no tags
    @Test
    public void testAnyChildPredicateNotTags() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 4));
        execQuery("*[not(tag)]", expected);
    }

    // has way nodes
    @Test
    public void testWayHasNodes() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Way, 1));
        // way result will also return the nodes belonging to the way...
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("way[nd]", expected);
    }

    // has no way nodes
    @Test
    public void testWayHasNoNodes() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Way, 2));
        execQuery("way[not(nd)]", expected);
    }

    // node is used in a way
    @Test
    public void testNodeIsUsedInWay() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        expected.add(new EntityRef(EntityType.Node, 2));
        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[way]", expected);
    }

    // node is not used in a way
    @Test
    public void testNodeIsNotUsedInWay() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 4));
        expected.add(new EntityRef(EntityType.Node, 5));
        expected.add(new EntityRef(EntityType.Node, 6));
        execQuery("node[not(way)]", expected);
    }

    // relation has node member
    @Test
    public void testRelationHasNodeMember() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Relation, 1));
        execQuery("relation[node]", expected);
    }

    // relation has no node members

    // relation has way member

    // relation has no way members

    // relation has relation member

    // relation has no relation members

    /**** multiple selector tests ****/

    // test tag & bbox
    @Test
    public void testTagAndBboxNodeSelection() {
        HashSet<EntityRef> expected = new HashSet<EntityRef>();
        expected.add(new EntityRef(EntityType.Node, 1));
        execQuery("node[amenity=pub][bbox=-0.01,-0.01,0.01,0.01]", expected);

        expected.add(new EntityRef(EntityType.Node, 3));
        execQuery("node[bbox=-0.01,-0.01,0.01,0.01][amenity=*]", expected);
    }

    // child predicate and bbox

    // multiple tag selectors (not supported by original XAPI?)

    // relation has only relation members

    /**
     * Simple tuple-class of entity type and ID to allow the expected results of
     * the tests to be judged.
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
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

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
     * @param query
     *            The XAPI URL query to make.
     * @param expectedResults
     *            Set of entities which are expected to be returned.
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
                // don't bother to account for bound elements at the moment...
                if (ent.getEntity().getType() != EntityType.Bound) {
                    if (!workingSet.contains(ref)) {
                        Assert.fail("Unexpected element: " + ref.toString() + ".");
                    }
                }
                workingSet.remove(ref);
            }

            if (!workingSet.isEmpty()) {
                StringBuilder str = new StringBuilder();
                str.append("Returned set doesn't contain all expected elements. Missing: [");
                for (EntityRef ref : workingSet) {
                    str.append(ref.toString());
                    str.append(", ");
                }
                str.append("].");
                Assert.fail(str.toString());
            }

        } finally {
            iterator.release();
            iterator = null;

            context.complete();
            context.release();
            context = null;
        }
    }

    // utility function to build elements
    private EntityContainer node(long id, int version, double lon, double lat, String... tags) {
        Date timestamp = new Date();
        LinkedList<Tag> constructedTags = new LinkedList<Tag>();
        for (int i = 0; i < tags.length; i += 2) {
            constructedTags.add(new Tag(tags[i], tags[i + 1]));
        }
        Node n = new Node(id, version, timestamp, OsmUser.NONE, 1, constructedTags, lon, lat);
        return new NodeContainer(n);
    }

    private WayContainer way(long id, int version, long[] way_nodes, String... tags) {
        Date timestamp = new Date();
        LinkedList<Tag> constructedTags = new LinkedList<Tag>();
        for (int i = 0; i < tags.length; i += 2) {
            constructedTags.add(new Tag(tags[i], tags[i + 1]));
        }
        LinkedList<WayNode> wn = new LinkedList<WayNode>();
        for (long nID : way_nodes) {
            wn.add(new WayNode(nID));
        }
        Way w = new Way(id, version, timestamp, OsmUser.NONE, 1, constructedTags, wn);
        return new WayContainer(w);
    }

    private RelationContainer relation(long id, int version, RelationMember mems[], String... tags) {
        Date timestamp = new Date();
        LinkedList<Tag> constructedTags = new LinkedList<Tag>();
        for (int i = 0; i < tags.length; i += 2) {
            constructedTags.add(new Tag(tags[i], tags[i + 1]));
        }
        LinkedList<RelationMember> rm = new LinkedList<RelationMember>();
        for (RelationMember m : mems) {
            rm.add(m);
        }
        Relation r = new Relation(id, version, timestamp, OsmUser.NONE, 1, constructedTags, rm);
        return new RelationContainer(r);
    }

    // internal function to drop all data to the database (ensure that the tests
    // are run on the same
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
        PostgreSqlCopyWriter writer = new PostgreSqlCopyWriter(loginCredentials, preferences,
                NodeLocationStoreType.InMemory);
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
