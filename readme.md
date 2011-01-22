Java XAPI
=========

The Java XAPI is a partial implementation of the XAPI read-only OSM server in Java.
It is implemented as a servlet and uses pieces of Osmosis to perform the database
queries.

See the OSM Wiki for more information about XAPI: http://wiki.openstreetmap.org/wiki/Xapi

Installation
------------

1. Set up an Osmosis pgsnapshot 0.6 schema in a PostGIS database:

    `createdb xapi`
   
    `createlang plpgsql xapi`
   
    `createuser xapi`
    
    `echo "alter role xapi password 'xapi';" | psql -d xapi`
    
    `psql -d xapi -f /usr/share/postgresql/8.4/contrib/postgis.sql`
    
    `psql -d xapi -f /usr/share/postgresql/8.4/contrib/spatial_ref_sys.sql`
    
    `psql -d xapi -f /usr/share/postgresql/8.4/contrib/hstore-new.sql`
   
    `psql -d xapi -f ~/osmosis/package/script/pgsnapshot_schema_0.6.sql`
   
    `psql -d xapi -f ~/osmosis/package/script/pgsnapshot_schema_0.6_linestring.sql`
   
    `echo "CREATE INDEX idx_nodes_tags ON nodes USING GIN(tags);" | psql -d xapi`
    
    `echo "CREATE INDEX idx_ways_tags ON ways USING GIN(tags);" | psql -d xapi`

2. Import a planet file (or other piece of OSM data)
   
    `bzcat planet-latest.osm.bz2 | bin/osmosis --read-xml file="/dev/stdin" --write-pgsql user="xapi" database="xapi"`

3. Grab the latest XAPI war and deploy it with a servlet container like Tomcat or Jetty.
