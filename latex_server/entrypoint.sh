#!/bin/bash

cp -rf /work/cgi-bin /var/www/.
cp -rf /work/html /var/www/.
cp -rf /work/httpd.conf /etc/httpd/conf/.

chmod a+x /var/www/cgi-bin/*
chmod a+w -R /var/www/html


rm -rf /run/httpd/* /tmp/httpd*

PORT={SET_PORT}
#register with discovery service (mysql or zookeeper maybe)
THIS_HOST=$(curl http://169.254.169.254/latest/meta-data/local-ipv4)
rm -rf /EFS/run/services/latex2pdf/$THIS_HOST:$PORT
chmod o+w -R /EFS/run/services/latex2pdf
chmod o+w /EFS/data/latex2pdf
su -s /bin/sh apache -c "touch /EFS/run/services/latex2pdf/$THIS_HOST:$PORT"

exec /usr/sbin/apachectl -DFOREGROUND
