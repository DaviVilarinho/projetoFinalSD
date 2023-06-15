#!/bin/bash

./runOnlyReplicas.sh &
./runAdminServer.sh &
./runOrderServer.sh
