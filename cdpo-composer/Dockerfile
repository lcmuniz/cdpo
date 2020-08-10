# Use official Maven's alpine image as base
FROM maven:3.6-jdk-13

#Environment Variables
ENV REDIS_CEP_HOST  cep-cataloger-redis
ENV REDIS_CEP_PASS  2KXcBK8#gUq3wAgsf77LheGkjWv@HhZE
ENV REDIS_CEP_PORT  6379



RUN mkdir -p /cdpo-composer/
ADD . /cdpo-composer/
WORKDIR /cdpo-composer/


RUN mvn package

RUN ls target/


#Compilation
#CMD "java -jar target/cep-cataloger-1.0-SNAPSHOT.jar redis-cep-cataloger"
