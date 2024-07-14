ARG IGNITION_VERSION
FROM inductiveautomation/ignition:${IGNITION_VERSION}
 
COPY modules/*.modl /usr/local/bin/ignition/user-lib/modules/
