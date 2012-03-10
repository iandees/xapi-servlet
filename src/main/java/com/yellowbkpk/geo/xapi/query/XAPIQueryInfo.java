package com.yellowbkpk.geo.xapi.query;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.postgis.Point;

import com.yellowbkpk.geo.xapi.db.Selector;
import com.yellowbkpk.geo.xapi.db.SelectorGroup;
import com.yellowbkpk.geo.xapi.servlet.Filetype;

public class XAPIQueryInfo {

    public enum RequestType {
        ALL("*"), NODE("node"), WAY("way"), RELATION("relation"), MAP("map");

        private static Map<String, RequestType> val = new HashMap<String, RequestType>();
        static {
            for (RequestType i : values()) {
                val.put(i.t, i);
            }
        }
        private String t;

        private RequestType(String t) {
            this.t = t;
        }

        public static RequestType fromValue(String v) {
            return val.get(v);
        }

        public String getT() {
            return t;
        }
    }

    private RequestType type;
    private List<Selector> selectors;
    private Filetype filetype;

    private XAPIQueryInfo(RequestType type, Filetype filetype, List<Selector> selectors) {
        this.type = type;
        this.filetype = filetype;
        this.selectors = selectors;
    }

    public static XAPIQueryInfo fromString(String str) throws XAPIParseException {
        ParseState state = new ParseState(str);
        List<Selector> selectors = new LinkedList<Selector>();

        RequestType type = parseRequestType(state);
        Filetype ftype = Filetype.xml;
        if (type == RequestType.MAP) {
            ftype = parseFiletype(state);
            state.expect("?");
            if ("bbox".equals(state.peek(4))) {
                selectors.add(parseBboxSelector(state));
            } else if ("poly".equals(state.peek(4))) {
                selectors.add(parsePolygonSelector(state));
            }

        } else {
            while (state.hasRemaining()) {
                // FIXME This peek check seems ugly.
                String nextChar = state.peek(1);
                if ("[".equals(nextChar)) {
                    List<Selector> sels = parseBracketedSelector(state, type);
                    selectors.addAll(sels);
                } else if (".".equals(nextChar)) {
                    ftype = parseFiletype(state);
                } else {
                    throw new XAPIParseException("Unknown text");
                }
            }
        }

        return new XAPIQueryInfo(type, ftype, selectors);
    }

    private static Filetype parseFiletype(ParseState state) throws XAPIParseException {
        if (state.canConsume(".")) {
            if (state.canConsume("xml")) {
                return Filetype.xml;
            } else if (state.canConsume("json")) {
                return Filetype.json;
            } else {
                throw new XAPIParseException("Unknown filetype specified.");
            }
        } else {
            return null;
        }
    }

    private static class ParseState {
        private StringBuffer buf = null;

        ParseState(String s) {
            buf = new StringBuffer(s);
        }

        boolean canConsume(String s) {
            if (s.length() > buf.length()) {
                return false;
            } else {
                String t = buf.substring(0, s.length());
                if (s.equals(t)) {
                    buf.delete(0, s.length());
                    return true;
                } else {
                    return false;
                }
            }
        }

        void expect(String s) throws XAPIParseException {
            if (s.length() > buf.length()) {
                throw new XAPIParseException("Expecting '" + s + "' but found end-of-string.");
            } else {
                String t = buf.substring(0, s.length());
                if (s.equals(t)) {
                    buf.delete(0, s.length());
                } else {
                    int length = Math.min(s.length() + 2, buf.length());
                    throw new XAPIParseException("Expecting '" + s + "', but found '" + peek(length) + "'.");
                }
            }
        }

        boolean hasRemaining() {
            return buf.length() > 0;
        }

        String peek(int n) throws XAPIParseException {
            if (n > buf.length()) {
                throw new XAPIParseException("Attempt to peek " + n + " characters, but there aren't that many left.");
            }
            return buf.substring(0, n);
        }

        void skip(int n) throws XAPIParseException {
            if (n > buf.length()) {
                throw new XAPIParseException("Attempt to skip " + n + " characters, but there aren't that many left.");
            }
            buf.delete(0, n);
        }

        ParseState copy() {
            return new ParseState(buf.toString());
        }
    }

    private static RequestType parseRequestType(ParseState state) throws XAPIParseException {
        for (RequestType type : RequestType.values()) {
            if (state.canConsume(type.getT())) {
                return type;
            }
        }
        throw new XAPIParseException("Query string should start with node, way, relation or *.");
    }

    private static List<Selector> parseBracketedSelector(ParseState state, RequestType type) throws XAPIParseException {
        state.expect("[");
        List<Selector> sels = parseSelector(state, type);
        state.expect("]");
        return sels;
    }

    private static List<Selector> parseSelector(ParseState state, RequestType type) throws XAPIParseException {
        List<Selector> selectors = new LinkedList<Selector>();

        if (state.canConsume("@")) {
            selectors.add(parseAttributeSelector(state));
        } else {
            List<String> maybeKeys = parseKeys(state);

            if (state.canConsume("=")) {
                if (maybeKeys.size() == 1 && maybeKeys.get(0).equals("bbox")) {
                    selectors.add(parseBboxRHS(state));
                } else if (maybeKeys.size() == 1 && maybeKeys.get(0).equals("poly")) {
                    selectors.add(parsePolygonRHS(state));
                } else if (maybeKeys.size() > 0) {
                    List<Selector> tagSelectors = parseTagSelectors(maybeKeys, state);
                    SelectorGroup tagGroup = new SelectorGroup(tagSelectors);
                    selectors.add(tagGroup);
                } else {
                    throw new XAPIParseException("Cannot parse tag query with unescaped special characters.");
                }
            } else {
                // looks like a child element predicate
                if (maybeKeys.size() == 1) {
                    selectors.add(parseChildPredicate(maybeKeys.get(0), type));
                } else {
                    throw new XAPIParseException(
                            "Cannot parse - expression does not look like child predicate selector.");
                }
            }
        }

        return selectors;
    }

    private static Selector parseChildPredicate(String key, RequestType type) throws XAPIParseException {
        ParseState state = new ParseState(key);
        boolean negateTest = false;
        Selector selector = null;

        if (state.canConsume("not(")) {
            negateTest = true;
        }

        if (state.canConsume("tag")) {
            selector = new Selector.ChildPredicate.Tag(negateTest);

        } else {
            if (type == RequestType.NODE) {
                if (state.canConsume("way")) {
                    selector = new Selector.ChildPredicate.NodeUsed(negateTest);

                } else {
                    throw new XAPIParseException("Unexpected child predicate on node. Expected: way or tag.");
                }

            } else if (type == RequestType.WAY) {
                if (state.canConsume("nd")) {
                    selector = new Selector.ChildPredicate.WayNode(negateTest);

                } else {
                    throw new XAPIParseException("Unexpected child predicate on way. Expected: nd or tag.");
                }

            } else if (type == RequestType.RELATION) {
                if (state.canConsume("node")) {
                    selector = Selector.ChildPredicate.RelationMember.node(negateTest);

                } else if (state.canConsume("way")) {
                    selector = Selector.ChildPredicate.RelationMember.way(negateTest);

                } else if (state.canConsume("relation")) {
                    selector = Selector.ChildPredicate.RelationMember.relation(negateTest);

                } else {
                    throw new XAPIParseException(
                            "Unexpected child predicate on relation. Expected: node, way, relation or tag.");
                }

            } else {
                throw new XAPIParseException(
                        "Child predicate on request types other than node, way and relation are not supported.");
            }
        }

        if (negateTest) {
            state.expect(")");
        }

        if (selector == null) {
            throw new XAPIParseException("Child predicate expression not recognised.");
        }

        return selector;
    }

    private static Selector.Polygon parseBboxSelector(ParseState state) throws XAPIParseException {
        state.expect("bbox=");
        return parseBboxRHS(state);
    }

    private static Selector.Polygon parseBboxRHS(ParseState state) throws XAPIParseException {
        Double left = parseDouble(state);
        state.expect(",");
        Double bottom = parseDouble(state);
        state.expect(",");
        Double right = parseDouble(state);
        state.expect(",");
        Double top = parseDouble(state);

        if (left > right) {
            throw new XAPIParseException("Left is greater than right.");
        }

        if (bottom > top) {
            throw new XAPIParseException("Bottom is greater than top.");
        }

        if (bottom < -90 || bottom > 90) {
            throw new XAPIParseException("Bottom is out of range.");
        }

        if (top < -90 || top > 90) {
            throw new XAPIParseException("Top is out of range.");
        }

        if (left < -180 || left > 180) {
            throw new XAPIParseException("Left is out of range.");
        }

        if (right < -180 || right > 180) {
            throw new XAPIParseException("Right is out of range.");
        }

        return new Selector.Polygon(left, right, top, bottom);
    }

    private static void fillDigits(ParseState state, StringBuffer buf) throws XAPIParseException {
        while (state.hasRemaining() && Character.isDigit(state.peek(1).codePointAt(0))) {
            buf.append(state.peek(1));
            state.skip(1);
        }
    }

    private static boolean consumeAppend(String s, ParseState state, StringBuffer buf) {
        if (state.canConsume(s)) {
            buf.append(s);
            return true;
        } else {
            return false;
        }
    }

    private static Double parseDouble(ParseState state) throws XAPIParseException {
        StringBuffer buf = new StringBuffer();
        consumeAppend("-", state, buf);
        fillDigits(state, buf);
        if (consumeAppend(".", state, buf)) {
            fillDigits(state, buf);
        }
        if (state.canConsume("e") || state.canConsume("E")) {
            buf.append("e");
            consumeAppend("+", state, buf);
            consumeAppend("-", state, buf);
            fillDigits(state, buf);
        }
        try {
            return Double.parseDouble(buf.toString());
        } catch (NumberFormatException ex) {
            throw new XAPIParseException(ex);
        }
    }

    private static Selector parseAttributeSelector(ParseState state) throws XAPIParseException {
        if (state.canConsume("user")) {
            state.expect("=");
            return new Selector.User(parseUnescaped(state));

        } else if (state.canConsume("uid")) {
            state.expect("=");
            return new Selector.Uid(parseInt(state));

        } else if (state.canConsume("changeset")) {
            state.expect("=");
            return new Selector.Changeset(parseInt(state));

        } else {
            throw new XAPIParseException("Attribute selector not recognised.");
        }
    }

    private static List<String> parseKeys(ParseState state) throws XAPIParseException {
        List<String> keys = new LinkedList<String>();

        // parse first key, which might have some special characters in it, just
        // not
        // the pipe character for key separation, or the equals character.
        keys.add(parseUnescaped(state));

        if (state.canConsume("|")) {
            do {
                keys.add(parseUnescaped(state));
            } while (state.canConsume("|"));
        }

        return keys;
    }

    enum SpecialChar {
        LEFT_SQ_BRACKET("["), RIGHT_SQ_BRACKET("]"), LEFT_PAREN("["), RIGHT_PAREN("]"), ASTERISK("*"), KV_SEPARATOR("|"), KEY_SEPARATOR(
                "=");

        private String s;

        SpecialChar(String str) {
            s = str;
        }

        String getS() {
            return s;
        }
    }

    private static boolean hasSpecialCharacters(String s) throws XAPIParseException {
        for (SpecialChar sp : SpecialChar.values()) {
            if (s.indexOf(sp.getS()) != -1) {
                return true;
            }
        }
        return false;
    }

    private static List<Selector> parseTagSelectors(List<String> keys, ParseState state) throws XAPIParseException {
        List<Selector> selectors = new LinkedList<Selector>();

        if (state.canConsume("*")) {
            // RHS is wildcard
            for (String k : keys) {
                selectors.add(new Selector.Tag.Wildcard(k));
            }
        } else {
            // RHS is list of values
            List<String> values = parseKeys(state);
            for (String k : keys) {
                for (String v : values) {
                    selectors.add(new Selector.Tag(k, v));
                }
            }
        }

        return selectors;
    }

    private static Integer parseInt(ParseState state) throws XAPIParseException {
        StringBuffer buf = new StringBuffer();
        consumeAppend("-", state, buf);
        fillDigits(state, buf);
        try {
            return Integer.parseInt(buf.toString());
        } catch (NumberFormatException ex) {
            throw new XAPIParseException(ex);
        }
    }

    /**
     * Parse a string from the parser state, allowing special characters except
     * for the multiple key separator and key/value separator ('|' and '=').
     * 
     * @param state
     *            Parser state.
     * @return String read from parser.
     * @throws XAPIParseException
     */
    private static String parseEscaped(ParseState state) throws XAPIParseException {
        StringBuffer buf = new StringBuffer();
        do {
            String next = state.peek(1);
            if (next.equals("=") || next.equals("|")) {
                break;
            }
            buf.append(next);
            state.skip(1);
        } while (true);

        if (buf.length() < 1) {
            throw new XAPIParseException("Unable to find a string at '" + state.peek(1) + "'.");
        }

        return buf.toString();
    }

    private static String parseUnescaped(ParseState state) throws XAPIParseException {
        StringBuffer buf = new StringBuffer();
        do {
            String next = state.peek(1);
            for (SpecialChar ch : SpecialChar.values()) {
                if (next.equals(ch.getS())) {
                    if (buf.length() < 1) {
                        throw new XAPIParseException("Unable to find a string at '" + state.peek(1) + "'.");
                    }

                    return buf.toString();
                }
            }
            if (next.equals("\\")) {
                state.skip(1);
                next = state.peek(1);
            }
            buf.append(next);
            state.skip(1);
        } while (true);
    }

    /**
     * Same as parseUnescaped() but instead of erroring at the end of the buffer
     * it returns the string that had been parsed up to that point.
     */
    private static String parseToEndOfSection(ParseState state) throws XAPIParseException {
        StringBuffer buf = new StringBuffer();
        do {
            if(!state.hasRemaining()) {
                return buf.toString();
            }
            String next = state.peek(1);
            if (next.equals("]")) {
                if (buf.length() < 1) {
                    throw new XAPIParseException("Unable to find a string at '" + state.peek(1) + "'.");
                }

                return buf.toString();
            }
            buf.append(next);
            state.skip(1);
        } while (true);
    }
    
    private static Selector.Polygon parsePolygonSelector(ParseState state) throws XAPIParseException {
        state.expect("poly=");
        return parsePolygonRHS(state);
    }

    private static Selector.Polygon parsePolygonRHS(ParseState state) throws XAPIParseException {
        int i = 0;
        String chars = parseToEndOfSection(state);
        List<Point> points = new LinkedList<Point>();
        float lat = 0;
        float lon = 0;

        while (i < chars.length()) {
            int b;
            int shift = 0;
            int result = 0;
            do {
                b = chars.charAt(i++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            float dlat = (((result & 1) > 0) ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = chars.charAt(i++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            float dlng = (((result & 1) > 0) ? ~(result >> 1) : (result >> 1));
            lon += dlng;

            points.add(new Point(lon * 1e-5, lat * 1e-5));
        }

        return new Selector.Polygon(points.toArray(new Point[points.size()]));
    }

    public RequestType getKind() {
        return type;
    }

    public List<Selector> getSelectors() {
        return selectors;
    }

    public Filetype getFiletype() {
        return filetype;
    }

}
