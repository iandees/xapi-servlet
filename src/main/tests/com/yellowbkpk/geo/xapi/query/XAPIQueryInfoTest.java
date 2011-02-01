package com.yellowbkpk.geo.xapi.query;

import com.yellowbkpk.geo.xapi.db.Selector;
import org.testng.Assert;
import org.testng.annotations.Test;
import com.yellowbkpk.geo.xapi.query.XAPIQueryInfo.RequestType;

public class XAPIQueryInfoTest {
    @Test
    public void testFromStringAnyPub() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("*[amenity=pub]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.ALL);
            Assert.assertEquals(info.getTagSelectors().size(), 1);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getWhereParam().size(), 2);
            Assert.assertEquals(sel.getWhereParam().get(0), "amenity");
            Assert.assertEquals(sel.getWhereParam().get(1), "pub");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing pubs.", e);
        }
    }

    @Test
    public void testFromStringNodeRestaurant() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("node[amenity=restaurant]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.NODE);
            Assert.assertEquals(info.getTagSelectors().size(), 1);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Tag.class);
            Assert.assertEquals(sel.getWhereParam().size(), 2);
            Assert.assertEquals(sel.getWhereParam().get(0), "amenity");
            Assert.assertEquals(sel.getWhereParam().get(1), "restaurant");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing restaurants.", e);
        }
    }

    @Test
    public void testFromStringWaysInArea() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("way[bbox=-180,-90,1.8e+2,90.0]");
            Assert.assertEquals(info.getBboxSelectors().size(), 1);
            Assert.assertEquals(info.getKind(), RequestType.WAY);
            Assert.assertEquals(info.getTagSelectors().size(), 0);

            Assert.assertEquals(info.getBboxSelectors().get(0).getLeft(), -180, 1.0e-6);
            Assert.assertEquals(info.getBboxSelectors().get(0).getRight(), 180, 1.0e-6);
            Assert.assertEquals(info.getBboxSelectors().get(0).getTop(), 90, 1.0e-6);
            Assert.assertEquals(info.getBboxSelectors().get(0).getBottom(), -90, 1.0e-6);

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing bboxes.", e);
        }
    }

    @Test
    public void testFromStringByUserID() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("relation[@uid=1]");
            // tag selectors is a misnomer - it's all selectors except the bbox one
            Assert.assertEquals(info.getTagSelectors().size(), 1);
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.RELATION);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Uid.class);
            Assert.assertEquals(sel.getWhereParam().size(), 1);
            Assert.assertEquals(sel.getWhereParam().get(0), 1);

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing user ID.", e);
        }
    }

    @Test
    public void testFromStringByUserName() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("way[@user=TestUser]");
            // tag selectors is a misnomer - it's all selectors except the bbox one
            Assert.assertEquals(info.getTagSelectors().size(), 1);
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.WAY);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.User.class);
            Assert.assertEquals(sel.getWhereParam().size(), 1);
            Assert.assertEquals(sel.getWhereParam().get(0), "TestUser");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing user name.", e);
        }
    }

    @Test
    public void testFromStringByChangesetID() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("*[@changeset=1]");
            // tag selectors is a misnomer - it's all selectors except the bbox one
            Assert.assertEquals(info.getTagSelectors().size(), 1);
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.ALL);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Changeset.class);
            Assert.assertEquals(sel.getWhereParam().size(), 1);
            Assert.assertEquals(sel.getWhereParam().get(0), 1);

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing changeset ID.", e);
        }
    }

    @Test
    public void testFromStringWildcard() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("*[amenity=*]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.ALL);
            Assert.assertEquals(info.getTagSelectors().size(), 1);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Tag.Wildcard.class);
            Assert.assertEquals(sel.getWhereParam().size(), 1);
            Assert.assertEquals(sel.getWhereParam().get(0), "amenity");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing amenities with wildcard.", e);
        }
    }

    @Test
    public void testFromStringWildcardMultipleKeys() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("*[amenity|shop=*]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.ALL);
            Assert.assertEquals(info.getTagSelectors().size(), 2);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Tag.Wildcard.class);
            Assert.assertEquals(sel.getWhereParam().size(), 1);
            Assert.assertEquals(sel.getWhereParam().get(0), "amenity");

            Selector sel2 = info.getTagSelectors().get(1);
            Assert.assertEquals(sel2.getClass(), Selector.Tag.Wildcard.class);
            Assert.assertEquals(sel2.getWhereParam().size(), 1);
            Assert.assertEquals(sel2.getWhereParam().get(0), "shop");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing amenities/shops with wildcard.", e);
        }
    }


    @Test
    public void testFromStringMultipleValues() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("*[amenity=pub|restaurant]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.ALL);
            Assert.assertEquals(info.getTagSelectors().size(), 2);

            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Tag.class);
            Assert.assertEquals(sel.getWhereParam().size(), 2);
            Assert.assertEquals(sel.getWhereParam().get(0), "amenity");
            Assert.assertEquals(sel.getWhereParam().get(1), "pub");

            Selector sel2 = info.getTagSelectors().get(1);
            Assert.assertEquals(sel2.getClass(), Selector.Tag.class);
            Assert.assertEquals(sel2.getWhereParam().size(), 2);
            Assert.assertEquals(sel2.getWhereParam().get(0), "amenity");
            Assert.assertEquals(sel2.getWhereParam().get(1), "restaurant");

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing amenities with many values.", e);
        }
    }

    @Test
    public void testFromStringInvalid() {
        assertDoesNotParse("*[amenity");
        assertDoesNotParse("*[amenity]");
        assertDoesNotParse("*[amenity=]");
        assertDoesNotParse("*[amenity=");
        assertDoesNotParse("*[=pub]");
        assertDoesNotParse("*[amenity=pub|]");
        assertDoesNotParse("*[amenity=*|]");
        assertDoesNotParse("*[*=pub]");
        assertDoesNotParse("nodes[amenity=pub]");
        assertDoesNotParse("node[]");
        assertDoesNotParse("node[@uid=non_numeric]");
        assertDoesNotParse("node[@changeset=non_numeric]");
    }

    @Test
    public void testFromStringValid() {
        // colons and unicode text should parse OK
        assertDoesParse("node[name:ja=ウィキペディアにようこそ]");
        // underscores and semicolons are commonly-used in OSM tags
        assertDoesParse("*[nonsense_variable_names=foo;bar;baz;bat]");
        /* here's what the xapi docs have to say about escaping:
         *
         * Values within predicates can be escaped by prefixing a backslash character.
         * The following characters can be escaped in this way: | [ ] * / = ( ) \ and space
         */
        assertDoesParseTag("*[foo\\|bar=something]", "foo|bar", "something");
        assertDoesParseTag("*[foo\\[bar\\]=something]", "foo[bar]", "something");
        assertDoesParseTag("*[foo\\*bar=something]", "foo*bar", "something");
        // note: is this going to work with the servlet's url parsing?
        assertDoesParseTag("*[foo\\/bar=something]", "foo/bar", "something");
        assertDoesParseTag("*[foo\\=bar=something]", "foo=bar", "something");
        assertDoesParseTag("*[foo\\(bar\\)=something]", "foo(bar)", "something");
        assertDoesParseTag("*[foo\\\\bar=something]", "foo\\bar", "something");
        // note: although this doesn't, strictly speaking, need to be escaped, it seems to be
        // part of the previous XAPI implementation's spec.
        assertDoesParseTag("*[foo\\ bar=something]", "foo bar", "something");
        // check that @s at the beginning can be escaped without being interpreted as an attribute selector
        assertDoesParseTag("*[\\@foobar=something]", "@foobar", "something");
    }

    private void assertDoesNotParse(String query) {
        boolean gotException = false;
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString(query);
        } catch (XAPIParseException e) {
            gotException = true;
        }
        Assert.assertTrue(gotException, "Should have got an exception trying to parse invalid query string: " + query);
    }

    private void assertDoesParse(String query) {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString(query);
        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing valid query: " + query + ".", e);
        }
    }

    private void assertDoesParseTag(String query, String key, String value) {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString(query);
            Assert.assertEquals(info.getTagSelectors().size(), 1);
            Selector sel = info.getTagSelectors().get(0);
            Assert.assertEquals(sel.getClass(), Selector.Tag.class);
            Assert.assertEquals(sel.getWhereParam().size(), 2);
            Assert.assertEquals(sel.getWhereParam().get(0), key);
            Assert.assertEquals(sel.getWhereParam().get(1), value);

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing valid query: " + query + ".", e);
        }
    }
}
