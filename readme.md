Java XAPI
=========

The Java XAPI is a partial implementation of the XAPI read-only OSM server in Java.
It is implemented as a servlet and uses pieces of Osmosis to perform the database
queries.

See the OSM Wiki for more information about XAPI: http://wiki.openstreetmap.org/wiki/Xapi

Installation
------------

1. Set up an Osmosis pgsnapshot 0.6 schema in a PostGIS database:

    `createdb xapi
    createlang plpgsql xapi
    createuser xapi
    echo "alter role xapi password 'xapi';" | psql -d xapi
    psql -d xapi -f /usr/share/postgresql/8.4/contrib/postgis.sql
    psql -d xapi -f /usr/share/postgresql/8.4/contrib/spatial_ref_sys.sql
    psql -d xapi -f /usr/share/postgresql/8.4/contrib/hstore-new.sql
    psql -d xapi -f ~/osmosis/package/script/pgsnapshot_schema_0.6.sql
    psql -d xapi -f ~/osmosis/package/script/pgsnapshot_schema_0.6_linestring.sql
    echo "CREATE INDEX idx_nodes_tags ON nodes USING GIN(tags);" | psql -d xapi
    echo "CREATE INDEX idx_ways_tags ON ways USING GIN(tags);" | psql -d xapi`

2. Import a planet file (or other piece of OSM data)
   
    `bzcat planet-latest.osm.bz2 | bin/osmosis --read-xml file="/dev/stdin" --write-pgsql user="xapi" database="xapi"`

3. Grab the latest XAPI war and deploy it with a servlet container like Tomcat or Jetty.

Development
-----------

I used Maven for the dependency and build management. Make sure you have `mvn` installed before
doing any of this.

1. Clone the github xapi-servlet and xapi-antlr repos

2. Check out the Osmosis SVN code and Compile the JARs by executing `ant publish` from the root
osmosis directory.

3. Manually install the Osmosis dependencies into your local Maven repository by executing `mvn
install:install-file -DgroupId=org.openstreetmap.osmosis -DartifactId=pgsnapshot -Dversion=0.36-SNAPSHOT
-Dpackaging=jar -Dfile=build/binary/osmosis-pgsnapshot.jar` for the Osmosis `core`, `pgsnapshot`,
and `xml` modules.

4. Install the `xapi-antlr` module by running `mvn install` from the xapi-antlr directory.

5. Finally, run `mvn compile war:war` from the xapi-servlet directory to generate a
deployable servlet war.

Thanks
------

This app was built on the shoulders of other people's open source code:

- bretth's Osmosis: for the database schema and querying code
- emacsen's xapi-ui: for the HTML debugging page he created to help build XAPI query URLs
