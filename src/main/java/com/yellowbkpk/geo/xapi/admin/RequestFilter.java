package com.yellowbkpk.geo.xapi.admin;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class RequestFilter {

    public static class AddressFilter {
        private int id;
        private String regex;
        private Date created;
        private String reason;

        public AddressFilter(String filter, String reason) {
            this.created = new Date();
            this.regex = filter;
            this.reason = reason;
        }

        public String getRegex() {
            return regex;
        }

        public Date getCreated() {
            return created;
        }

        public String getReason() {
            return reason;
        }

        public boolean matches(String address) {
            return Pattern.matches(regex, address);
        }

        public Integer getId() {
            return id;
        }
    }

    private static final List<AddressFilter> addressFilters = new ArrayList<AddressFilter>();
    private static int idNumber = 0;

    private RequestFilter() {

    }

    public synchronized static List<AddressFilter> getAllAddressFilters() {
        return addressFilters;
    }

    public synchronized static void addFilter(AddressFilter addressFilter) {
        addressFilters.add(addressFilter);
        addressFilter.id = idNumber++;
    }

    public synchronized static void deleteFilter(int id) {
        Iterator<AddressFilter> iterator = addressFilters.iterator();
        while (iterator.hasNext()) {
            AddressFilter filter = iterator.next();

            if (filter.id == id) {
                iterator.remove();
                return;
            }
        }
    }

    public static AddressFilter findFilterForHost(String remoteAddr) {
        List<AddressFilter> allAddressFilters = getAllAddressFilters();

        for (AddressFilter filter : allAddressFilters) {
            if (filter.matches(remoteAddr)) {
                return filter;
            }
        }

        return null;
    }

}
