FROM ubuntu
MAINTAINER neo
ENV DEBIAN_FRONTEND noninteractive
RUN apt-get -y update
RUN apt-get -y install software-properties-common
RUN dpkg -s ansible || (apt-add-repository -y ppa:ansible/ansible && apt-get -y -q update && apt-get -y install ansible)