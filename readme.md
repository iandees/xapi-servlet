Java XAPI
=========

The Java XAPI is a partial implementation of the XAPI read-only OSM server in Java.
It is implemented as a servlet and uses pieces of Osmosis to perform the database
queries.

See the OSM Wiki for more information about XAPI: http://wiki.openstreetmap.org/wiki/Xapi

Installation
------------

These setup steps assume you're working on a Ubuntu 12.x installation.

0. Make sure you have the required packages installed:

    `sudo apt-get install postgresql-9.1 postgresql-9.1-postgis postgresql-contrib-9.1 openjdk-7-jre`

1. Set up an Osmosis pgsnapshot 0.6 schema in a PostGIS database:

    `sudo su - postgres` *(These commands are meant to be run as user `postgres`)*
    
    `createuser -s xapi`
    
    `psql -d xapi -c "ALTER ROLE xapi PASSWORD 'xapi';"`
    
    `createdb xapi`
    
    `psql -d xapi -f /usr/share/postgresql/9.1/contrib/postgis-1.5/postgis.sql`
    
    `psql -d xapi -f /usr/share/postgresql/9.1/contrib/postgis-1.5/spatial_ref_sys.sql`
    
    `psql -d xapi -c "CREATE EXTENSION hstore;"`
        
    `psql -d xapi -f ~/osmosis/script/pgsnapshot_schema_0.6.sql`
    
    `psql -d xapi -f ~/osmosis/script/pgsnapshot_schema_0.6_linestring.sql`

    `exit` *(Brings us back to original user.)*

2. Tune your postgresql settings for a faster import. For a good discussion on tuning your database settings, see [this post](http://www.paulnorman.ca/blog/2011/11/loading-a-pgsnapshot-schema-with-a-planet-take-2/) by Paul Norman.

3. Import a planet file (or other piece of OSM data)
    
    `bin/osmosis --read-bin file=planet-latest.osm.pbf --log-progress --write-pgsql user="xapi" database="xapi" password="xapi"`

4. Add indexes for tags.

    `psql -d xapi -c "CREATE INDEX idx_nodes_tags ON nodes USING GIN(tags);" | psql -d xapi`
    
    `psql -d xapi -c "CREATE INDEX idx_ways_tags ON ways USING GIN(tags);" | psql -d xapi`

    `psql -d xapi -c "CREATE INDEX idx_relations_tags ON relations USING GIN(tags);" | psql -d xapi`
  
5. Grab the latest XAPI war and deploy it with a servlet container like Tomcat or Jetty.

Keep Your Database Up to Date
-----------------------------

0. Create a working directory for the Osmosis replication metadata.

    `mkdir ~/.osmosis`

1. Initialize the working directory for Osmosis's use.

    `~/osmosis/bin/osmosis --read-replication-interval-init workingDirectory=~/.osmosis`

2. Find the minutely state file corresponding to a time very near but **before** your planet
   file's timestamp. I do this by viewing the minutely diff directory and looking for a
   timestamp near the one on my planet file. Copy the URL for that diff file and download it
   to Osmosis's newly-created working directory.

    e.g. `wget -O ~/.osmosis/state.txt http://planet.example.com/minutely-replicate/000/000/000.state.txt`

3. Run Osmosis to catch the database up.

    ```
    ~osmosis/bin/osmosis --read-replication-interval workingDirectory=~/.osmosis \
                         --write-pgsql-change database="xapi" user="xapi" password="xapi"
    ```

Development
-----------

I used Maven for the dependency and build management. Make sure you have `mvn` installed before
doing any of this.

1. Clone the github xapi-servlet repo

2. Check out the Osmosis SVN code and Compile the JARs by executing `ant publish` from the root
osmosis directory.

3. Manually install the Osmosis dependencies into your local Maven repository by executing `mvn install:install-file -DgroupId=org.openstreetmap.osmosis -DartifactId=pgsnapshot -Dversion=0.39 -Dpackaging=jar -Dfile=build/binary/osmosis-pgsnapshot.jar` from the Osmosis `core`, `pgsnapshot`, `hstore-jdbc`
and `xml` directories.

4. Finally, run `mvn compile war:war` from the xapi-servlet directory to generate a
deployable servlet war.

 - Note that this will fail if you don't have the JSON or PBF Osmosis JARs installed (You're not missing anything: I don't specify how to do that in this readme). To fix that, comment out the two `dependency` blocks in the `pom.xml` that mention `pbf` or `json`. At the moment the PBF and JSON output is removed to make it easier to add the `planetDate` timestamp to the output.

Known Issues
------------

See the [issues list](https://github.com/iandees/xapi-servlet/issues) for all issues, but these limit functionality:

1. [Bug 7](https://github.com/iandees/xapi-servlet/issues/7) - Relation queries ignore any bounding box predicates.

2. [Bug 12](https://github.com/iandees/xapi-servlet/issues/12) - Relations aren't included in the "all elements" query.

3. PBF and JSON support was removed in f0f003 to make it easier to add the `planetDate` functionality.

Thanks
------

This app was built on the shoulders of other people's open source code:

- bretth's Osmosis: for the database schema and querying code
- emacsen's xapi-ui: for the HTML debugging page he created to help build XAPI query URLs
- zere (and MapQuest's) help with everything; especially the parser!
