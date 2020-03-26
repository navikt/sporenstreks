export ALTINN_API_GW_API_KEY=$(cat /var/run/secrets/nais.io/apigw/altinn/x-nav-apiKey)
export SERVICE_USER_USERNAME=$(cat /var/run/secrets/nais.io/service_user/username)
export SERVICE_USER_PASSWORD=$(cat /var/run/secrets/nais.io/service_user/password)
