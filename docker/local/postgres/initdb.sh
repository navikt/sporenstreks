#!/bin/bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER sporenstreks WITH PASSWORD 'sporenstreks';
    CREATE DATABASE sporenstreks;
    CREATE SCHEMA sporenstreks;
    GRANT ALL PRIVILEGES ON DATABASE sporenstreks TO sporenstreks;
EOSQL

psql -v ON_ERROR_STOP=1 --username "sporenstreks" --dbname "sporenstreks" <<-EOSQL
    CREATE TABLE refusjonskrav (
                           data jsonb NOT NULL
    );
    CREATE INDEX virksomhetsnummer ON refusjonskrav ((data ->> 'virksomhetsnummer'));
    CREATE INDEX identitetsnummer ON refusjonskrav ((data  ->> 'identitetsnummer'));
EOSQL
