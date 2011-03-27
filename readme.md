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
    
    `createuser xapi` You *do* want the user to be a superuser.
    
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

Development
-----------

I used Maven for the dependency and build management. Make sure you have `mvn` installed before
doing any of this.

1. Clone the github xapi-servlet repo

2. Check out the Osmosis SVN code and Compile the JARs by executing `ant publish` from the root
osmosis directory.

3. Manually install the Osmosis dependencies into your local Maven repository by executing `mvn install:install-file -DgroupId=org.openstreetmap.osmosis -DartifactId=pgsnapshot -Dversion=0.36-SNAPSHOT -Dpackaging=jar -Dfile=build/binary/osmosis-pgsnapshot.jar` for the Osmosis `core`, `pgsnapshot`, `hstore-jdbc`
and `xml` modules.

4. Finally, run `mvn compile war:war` from the xapi-servlet directory to generate a
deployable servlet war.

 - Note that this will fail if you don't have the JSON or PBF Osmosis JARs installed (You're not missing anything: I don't specify how to do that in this readme). To fix that, comment out the two `dependency` blocks in the `pom.xml` that mention `pbf` or `json`.

Thanks
------

This app was built on the shoulders of other people's open source code:

- bretth's Osmosis: for the database schema and querying code
- emacsen's xapi-ui: for the HTML debugging page he created to help build XAPI query URLs
- zere (and MapQuest's) help with everything; especially the parser!
