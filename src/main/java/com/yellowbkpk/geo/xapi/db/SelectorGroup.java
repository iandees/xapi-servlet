package com.yellowbkpk.geo.xapi.db;

import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class SelectorGroup extends Selector {
    private List<Selector> selectors = new LinkedList<Selector>();

    public SelectorGroup(List<Selector> selectors) {
        super(null);
        this.selectors = selectors;
    }

    @Override
    public String getWhereString() {
        StringBuilder b = new StringBuilder();
        Iterator<Selector> iterator = selectors.iterator();

        while (iterator.hasNext()) {
            Selector selector = iterator.next();
            b.append("(");
            b.append(selector.getWhereString());
            b.append(")");

            if (iterator.hasNext()) {
                b.append(" OR ");
            }
        }

        return b.toString();
    }

    @Override
    public List<Object> getWhereParam() {
        List<Object> ret = new LinkedList<Object>();

        for (Selector selector : selectors) {
            ret.addAll(selector.getWhereParam());
        }

        return ret;
    }

    public List<Selector> getSelectors() {
        return Collections.unmodifiableList(selectors);
    }

}
