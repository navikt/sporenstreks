Sporenstreks docker compose oppsett
================

Det finnes per nå 2 docker compose oppsett;
- docker-compose.yml: avhengighetene til backend-serveren ment for utvikling av serveren
- complete-backend.yml: avhengighetene til backend + backendserveren, ment for utvikling av frontend

For å starte hele backend må du kunne hente docker images from NAV sitt GitHub docker repo, det gjør du ved å
logge inn i docker med et Github personal access token:

- Gå til https://github.com/settings/tokens
- Opprett et token, og kopier det til et sikkert sted (du kan bruke et du har om du har)
- Gi tokenet tilgangen "read:packages"
- "Enable SSO" på tokenet ditt (det er en knapp i listen over tokens)
- Åpne kommandolinjen og skriv inn "docker login ghcr.io -u <ditt github brukernavn>"
- Når du blir spurt om passord, skriv inn tokenet du fikk i steg 2

Presto, nå skal du kunne starte backenden ved å stå i denne mappen og skrive
docker-compose -f complete-backend.yml up

Backendserveren er da tilgjengelig på port 8080
