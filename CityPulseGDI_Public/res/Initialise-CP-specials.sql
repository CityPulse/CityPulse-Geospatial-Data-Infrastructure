  ALTER TABLE ways ADD COLUMN citycostmulti double precision,
  ADD COLUMN pollutionmulti double precision,
  ADD COLUMN timemulti double precision,
  ADD COLUMN distancemulti double precision
  UPDATE ways SET cityCostMulti = 1.0;
  UPDATE ways SET pollutionmulti = 1.0;
  UPDATE ways SET timemulti = 1.0;
  UPDATE ways SET distancemulti = 1.0;
  
  ----
  -- DROP SEQUENCE cp_route_requests_request_id_seq;
CREATE SEQUENCE cp_route_requests_request_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

-- DROP SEQUENCE cp_routes_request_id_seq;
CREATE SEQUENCE cp_routes_request_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

-- DROP SEQUENCE cp_routes_route_id_seq;
CREATE SEQUENCE cp_routes_route_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;
ALTER TABLE cp_routes_route_id_seq
  OWNER TO wp4;

-- DROP SEQUENCE cp_distance_requests_request_id_seq;
CREATE SEQUENCE cp_distance_requests_request_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

-- DROP SEQUENCE cp_distances_request_id_seq;
CREATE SEQUENCE cp_distances_request_id_seq
  INCREMENT 1
  MINVALUE 1
  MAXVALUE 9223372036854775807
  START 1
  CACHE 1;

----


-- Table: cp_distance_requests

-- DROP TABLE cp_distance_requests;

CREATE TABLE cp_distance_requests
(
  repetition_id integer,
  request_id integer NOT NULL DEFAULT nextval('cp_distance_requests_request_id_seq'::regclass),
  from_id bigint,
  amenity character varying,
  count integer,
  from_geom geometry(Point,4326),
  CONSTRAINT cp_distance_requests_request_id_key UNIQUE (request_id)
)
WITH (
  OIDS=FALSE
);


-- Index: distreq_index

-- DROP INDEX distreq_index;

CREATE INDEX distreq_index
  ON cp_distance_requests
  USING gist
  (from_geom);



-- DROP TABLE cp_distances;

CREATE TABLE cp_distances
(
  request_id integer NOT NULL DEFAULT nextval('cp_distances_request_id_seq'::regclass),
  osm_id bigint,
  direct_distance numeric,
  route_distance numeric,
  better_than_before boolean,
  edge_count integer,
  euclidean_geom geometry(Geometry,4326),
  route_geom geometry(Geometry,4326),
  edge_length_array double precision[],
  duration numeric,
  CONSTRAINT cp_distances_request_id_fkey FOREIGN KEY (request_id)
      REFERENCES cp_distance_requests (request_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);


-- Index: dis_route_index

-- DROP INDEX dis_route_index;

CREATE INDEX dis_route_index
  ON cp_distances
  USING gist
  (route_geom);

-- Index: dist_index

-- DROP INDEX dist_index;

CREATE INDEX dist_index
  ON cp_distances
  USING gist
  (euclidean_geom);



-- Table: cp_distance_metric_experiment

-- DROP TABLE cp_distance_metric_experiment;

CREATE TABLE cp_distance_metric_experiment
(
  "row.names" text,
  from_sensor integer,
  to_sensor integer,
  from_node integer,
  to_node integer,
  osm_id double precision,
  foot text,
  highway text,
  name text,
  st_length double precision
)
WITH (
  OIDS=FALSE
);


-----


-- Table: cp_event_stream

-- DROP TABLE cp_event_stream;

CREATE TABLE cp_event_stream
(
  event_uuid uuid,
  event_topic character varying,
  geom geometry(Geometry,4326)
)
WITH (
  OIDS=FALSE
);


-- Index: event_stream_index

-- DROP INDEX event_stream_index;

CREATE INDEX event_stream_index
  ON cp_event_stream
  USING gist
  (geom);


  -- Table: cp_events

-- DROP TABLE cp_events;

CREATE TABLE cp_events
(
  event_uuid uuid,
  event_topic character varying,
  event_time timestamp without time zone,
  close_time timestamp without time zone,
  geom geometry(Geometry,4326)
)
WITH (
  OIDS=FALSE
);

-- Index: event_index

-- DROP INDEX event_index;

CREATE INDEX event_index
  ON cp_events
  USING gist
  (geom);

----


-- Table: cp_route_requests

-- DROP TABLE cp_route_requests;

CREATE TABLE cp_route_requests
(
  request_id integer NOT NULL DEFAULT nextval('cp_route_requests_request_id_seq'::regclass),
  from_id bigint,
  to_id bigint,
  cost_metric character varying,
  from_geom geometry(Point,4326),
  to_geom geometry(Point,4326),
  CONSTRAINT cp_route_requests_request_id_key UNIQUE (request_id)
)
WITH (
  OIDS=FALSE
);



-- Table: cp_routes

-- DROP TABLE cp_routes;

CREATE TABLE cp_routes
(
  request_id integer NOT NULL DEFAULT nextval('cp_routes_request_id_seq'::regclass),
  route_id integer NOT NULL DEFAULT nextval('cp_routes_route_id_seq'::regclass),
  cost numeric,
  geom geometry(Geometry,4326),
  edge_count integer,
  CONSTRAINT cp_routes_request_id_fkey FOREIGN KEY (request_id)
      REFERENCES cp_route_requests (request_id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);


-- Index: routes_index

-- DROP INDEX routes_index;

CREATE INDEX routes_index
  ON cp_routes
  USING gist
  (geom);



-----


-- DROP TABLE cp_sensors;

CREATE TABLE cp_sensors
(
  sensor_uuid uuid NOT NULL,
  sensor_annotation_id character varying,
  sercvice_category character varying,
  traffic integer,
  geom geometry(Geometry,4326),
  CONSTRAINT uuid_key PRIMARY KEY (sensor_uuid)
)
WITH (
  OIDS=FALSE
);





---


CREATE SCHEMA  observations;


-- DROP TABLE observations.cp_observations;

CREATE TABLE observations.cp_observations
(
  sampling_time timestamp without time zone,
  sensor_uuid uuid,
  observation_uuid uuid NOT NULL,
  data json,
  quality json,
  CONSTRAINT cp_observations_pkey PRIMARY KEY (observation_uuid),
  CONSTRAINT cp_observations_sensor_uuid_fkey FOREIGN KEY (sensor_uuid)
      REFERENCES cp_sensors (sensor_uuid) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);


-- Index: observations.timeindex

-- DROP INDEX observations.timeindex;

CREATE INDEX timeindex
  ON observations.cp_observations
  USING btree
  (sampling_time);

-- Index: observations.uuidindex

-- DROP INDEX observations.uuidindex;

CREATE INDEX uuidindex
  ON observations.cp_observations
  USING btree
  (sensor_uuid);

-- Table: observations.p_s_observation_uuid

-- DROP TABLE observations.p_s_observation_uuid;

CREATE TABLE observations.p_s_observation_uuid
(
  main uuid,
  secondary uuid
)
WITH (
  OIDS=FALSE
);




----












  