/*Run these commands as POSTGRES*/
DO
$body$
BEGIN
  IF NOT EXISTS (
      SELECT *
      FROM   pg_catalog.pg_user
      WHERE  usename = 'ogame_user') THEN

    CREATE ROLE OGAME_USER LOGIN PASSWORD 'ogame';
  END IF;
END
$body$;

CREATE SCHEMA IF NOT EXISTS OGAME;