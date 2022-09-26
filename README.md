# Loadtest

Setup `LOADTEST_*` env vars, create a `.env` file at the root of the repo with:

```conf
LOADTEST_BACKEND_URL=https://loadtest.api.group30.socra-sigl.fr
LOADTEST_FRONTEND_URL=https://loadtest.group30.socra-sigl.fr
LOADTEST_CONSTANT_CONCURRENT_USERS=200
LOADTEST_DURATION=5
```

To run:

```sh
docker compose run loadtest
```
