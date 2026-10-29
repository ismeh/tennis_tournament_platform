#!/bin/bash
export PATH="/home/ism/.nvm/versions/node/v22.23.1/bin:$PATH"
cd /home/ism/tennis_tournament_platform/frontend
node node_modules/@angular/cli/bin/ng.js build 2>&1
