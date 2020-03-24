#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER sporenstreks WITH PASSWORD 'sporenstreks';
    CREATE DATABASE sporenstreks;
    CREATE SCHEMA sporenstreks;
    GRANT ALL PRIVILEGES ON DATABASE sporenstreks TO sporenstreks;
EOSQL

psql -v ON_ERROR_STOP=1 --username "sporenstreks" --dbname "sporenstreks" <<-EOSQL
    CREATE FUNCTION get_pk(data jsonb)
        RETURNS  jsonb AS
        '
        select json_build_array(data -> ''arbeidsforhold'' -> ''arbeidsgiver'' ->> ''arbeidsgiverId'',data -> ''arbeidsforhold'' -> ''arbeidstaker'' ->> ''identitetsnummer'',data -> ''periode'' ->> ''fom'',data -> ''periode'' ->> ''tom'',data ->> ''ytelse'')::jsonb;
        '
        LANGUAGE sql
        IMMUTABLE;
EOSQL
