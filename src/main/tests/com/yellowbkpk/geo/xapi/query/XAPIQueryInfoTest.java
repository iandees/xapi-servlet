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
}
