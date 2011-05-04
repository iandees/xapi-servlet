package com.yellowbkpk.geo.xapi.db;

import java.util.LinkedList;
import java.util.List;

import org.postgis.PGgeometry;
import org.postgis.Point;

public abstract class Selector {

    protected String where = "";
    protected List<Object> param = new LinkedList<Object>();

    public Selector(String string, Object... params) {
        this.where = string;
        for (Object object : params) {
            this.param.add(object);
        }
    }

    public String getWhereString() {
        return this.where;
    }

    public List<Object> getWhereParam() {
        return this.param;
    }

    public static class Tag extends Selector {
        public Tag(String key, String value) {
            super(" tags @> hstore(?, ?)", key, value);
        }

        public static class Wildcard extends Selector {
            public Wildcard(String key) {
                super(" exist(tags, ?)", key);
            }
        }
    }

    public static class Polygon extends Selector {
        public Polygon(Point[] points) {
            super(" ST_Intersects(geom, ?)", new PGgeometry(PolygonBuilder.createPolygon(points)));
        }
    }

    public static class BoundingBox extends Selector {
        private double left;
        private double right;
        private double bottom;
        private double top;

        public BoundingBox(double left, double right, double top, double bottom) {
            super(" geom && ?", new PGgeometry(PolygonBuilder.buildBoundingPolygon(left, right, top, bottom)));
            this.left = left;
            this.right = right;
            this.bottom = bottom;
            this.top = top;
        }

        public double getLeft() {
            return left;
        }

        public double getRight() {
            return right;
        }

        public double getTop() {
            return top;
        }

        public double getBottom() {
            return bottom;
        }

        public double area() {
            return (top - bottom) * (right - left);
        }
    }

    public static class Changeset extends Selector {
        public Changeset(int changeset) {
            super(" changeset_id = ?", changeset);
        }
    }

    public static class Uid extends Selector {
        public Uid(int uid) {
            super(" user_id = ?", uid);
        }
    }

    public static class User extends Selector {
        public User(String name) {
            super("user_name = ?", name);
        }
    }

    public static class ChildPredicate extends Selector {
        protected ChildPredicate(String string, Object... params) {
            super(string, params);
        }

        // selects those elements which have tags, or no tags if negateQuery is
        // true.
        public static class Tag extends ChildPredicate {
            public Tag(boolean negateQuery) {
                super(" array_length(akeys(tags),1) is" + (negateQuery ? "" : " not") + " null");
            }
        }

        // selects those ways which have nodes, or no nodes if negateQuery is
        // true.
        public static class WayNode extends ChildPredicate {
            public WayNode(boolean negateQuery) {
                // alternative - not sure which is the better until it can be
                // tested at scale:
                // super((negateQuery ? " not" : "") +
                // " exists(select way_id from way_nodes where way_id=id)");
                super(" array_length(nodes,1) is" + (negateQuery ? "" : " not") + " null");
            }
        }

        public static class RelationMember extends ChildPredicate {
            private RelationMember(boolean negateQuery, String memberType) {
                super((negateQuery ? " not" : "")
                        + " exists(select relation_id from relation_members where relation_id = id and member_type='"
                        + memberType + "')");
            }

            public static RelationMember node(boolean negateQuery) {
                return new RelationMember(negateQuery, "N");
            }

            public static RelationMember way(boolean negateQuery) {
                return new RelationMember(negateQuery, "W");
            }

            public static RelationMember relation(boolean negateQuery) {
                return new RelationMember(negateQuery, "R");
            }
        }

        // selects those nodes which are (or are not) used as part of a way
        public static class NodeUsed extends ChildPredicate {
            public NodeUsed(boolean negateQuery) {
                super((negateQuery ? " not" : "") + " exists(select node_id from way_nodes where node_id=id)");
            }
        }
    }
}
