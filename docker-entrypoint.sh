#!/bin/sh
set -eu

port="${PORT:-8080}"
case "$port" in
  ''|*[!0-9]*)
    echo "PORT must be a valid numeric TCP port." >&2
    exit 1
    ;;
esac

export CATALINA_OPTS="${CATALINA_OPTS:-} -Dtomcat.http.port=${port}"
exec catalina.sh run
