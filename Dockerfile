
FROM ubuntu:18.04

WORKDIR /failslow

COPY . .

RUN \
	# Install the dependencies
	apt-get update && \
    apt-get install -y python-pip && \
	apt-get install -y  openjdk-8-jdk maven vim git net-tools inetutils-ping && \
    apt-get -y install libcppunit-dev && \
    apt-get -y install python-setuptools python2.7-dev && \
    apt-get -y install openssl libssl-dev && \
	apt-get -y install ssh && \
    apt-get -y install libsasl2-modules-gssapi-mit libsasl2-modules libsasl2-dev && \
	# Set JAVA_HOME
	echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64" >> ~/.bashrc


