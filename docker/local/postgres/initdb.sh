#!/usr/bin/env bash
set -e

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    CREATE USER sporenstreks WITH PASSWORD 'sporenstreks';
    CREATE DATABASE sporenstreks;
    CREATE SCHEMA sporenstreks;
    GRANT ALL PRIVILEGES ON DATABASE sporenstreks TO sporenstreks;
EOSQL

psql -v ON_ERROR_STOP=1 --username "sporenstreks" --dbname "sporenstreks" <<-EOSQL
    CREATE TABLE refusjonskrav (
                           data jsonb NOT NULL,
                           referansenummer SERIAL UNIQUE
    );

    ALTER SEQUENCE refusjonskrav_referansenummer_seq RESTART WITH 452171;

    CREATE INDEX status ON refusjonskrav ((data  ->> 'status'));
    CREATE INDEX id ON refusjonskrav ((data ->> 'id'));
    CREATE INDEX influx on refusjonskrav ((data ->> 'indeksertInflux'));
    CREATE INDEX virksomhetsnummer ON refusjonskrav ((data ->> 'virksomhetsnummer'));
    CREATE INDEX identitetsnummer ON refusjonskrav ((data  ->> 'identitetsnummer'));
EOSQL

psql -v ON_ERROR_STOP=1 --username "sporenstreks" --dbname "sporenstreks" <<-EOSQL
    CREATE TABLE kvittering (
                           data jsonb NOT NULL
    );

    CREATE INDEX idx_kvittering_status ON kvittering ((data  ->> 'status'));
    CREATE INDEX idx_kvittering_id ON kvittering ((data ->> 'id'));
    CREATE INDEX idx_kvittering_virksomhetsnummer ON kvittering ((data ->> 'virksomhetsnummer'));
EOSQL

psql -v ON_ERROR_STOP=1 --username "sporenstreks" --dbname "sporenstreks" <<-EOSQL
    create table bakgrunnsjobb (
    jobb_id uuid unique not null,
    type VARCHAR(100) not null,
    behandlet timestamp,
    opprettet timestamp not null,

    status VARCHAR(50) not null,
    kjoeretid timestamp not null,

    forsoek int not null default 0,
    maks_forsoek int not null,
    data jsonb
)
EOSQL
