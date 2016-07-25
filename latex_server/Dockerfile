# Creates renatbek/latex_server:latest
FROM centos:7
MAINTAINER Renat Bekbolatov <renatbek@gmail.com>

RUN yum -y update

RUN yum install -y \
    texlive \
    texlive-wrapfig \
    httpd

RUN yum clean all

ADD target/work /work

ADD target/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

EXPOSE 80
ENTRYPOINT ["/entrypoint.sh"]
