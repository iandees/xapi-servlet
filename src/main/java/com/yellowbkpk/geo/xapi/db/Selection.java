package com.yellowbkpk.geo.xapi.db;

import java.util.LinkedList;

public class Selection {
    private StringBuilder string = new StringBuilder("TRUE");
    private LinkedList<Object> params = new LinkedList<Object>();

    public void addSelector(String combine, Selector s) {
    	string.append(" ");
    	string.append(combine);
    	string.append(" ");
        string.append(s.getWhereString());
        params.add(s.getWhereParam());
    }

    public String getWhereString() {
        return string.toString();
    }

    public Object[] getWhereParams() {
        return params.toArray();
    }

}
