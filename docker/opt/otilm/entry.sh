#!/bin/sh

otilmHome="/opt/otilm"
. "${otilmHome}/static-functions"

log "INFO" "Launching the Scheduler"
exec java $JAVA_OPTS -jar ./app.jar

#exec "$@"
