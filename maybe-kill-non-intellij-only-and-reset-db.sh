#!/bin/bash

kill $(pgrep java | tail +3) && rm -rf /tmp/{0,1}
