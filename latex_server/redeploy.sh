#!/bin/bash

set -e

set -o allexport
source SETTINGS
set +o allexport

############################################################
mkdir -p target
rm -rf target/work
cp -rf work target/.

sed -e "s/{SET_PORT}/$PORT/g" work/cgi-bin/latex2pdf.sh > target/work/cgi-bin/latex2pdf.sh
sed -e "s/{SET_PORT}/$PORT/g" entrypoint.sh > target/entrypoint.sh

############################################################

source $DISTRIB_HOME/bin/task_definition/publish_new_revision.sh

source $DISTRIB_HOME/bin/aws/update_service.sh
