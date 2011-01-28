package com.yellowbkpk.geo.xapi.query;

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
            // TODO: check the tag selector parameters too

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing pubs.", e);
        }
    }

    @Test
    public void testFromStringNodePub() {
        try {
            XAPIQueryInfo info = XAPIQueryInfo.fromString("node[amenity=pub]");
            Assert.assertEquals(info.getBboxSelectors().size(), 0);
            Assert.assertEquals(info.getKind(), RequestType.NODE);
            Assert.assertEquals(info.getTagSelectors().size(), 1);
            // TODO: check the tag selector parameters too

        } catch (XAPIParseException e) {
            Assert.fail("Shouldn't fail parsing pubs.", e);
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
            Assert.fail("Shouldn't fail parsing pubs.", e);
        }
    }
}
