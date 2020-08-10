# Use official Maven's alpine image as base
FROM maven:3.6-jdk-13

#Environment Variables
ENV CPU_OVERLOAD 0.15
ENV CPU_UNDERLOAD 0.85
ENV MEMORY_OVERLOAD 0.85
ENV MEMORY_UNDERLOAD 0.15

#ENV REDIS_CEP_HOST  redis-cep-worker
ENV REDIS_CEP_HOST  cep-cataloger-redis
ENV REDIS_CEP_PASS  2KXcBK8#gUq3wAgsf77LheGkjWv@HhZE
ENV REDIS_CEP_PORT  6379

ENV NATS_HOST nats-cep
ENV NATS_PORT 4222

ENV RABBITMQ_HOST rabbitmq-cep
#ENV RABBITMQ_PASSWORD g%7wavG!W^Z&5cYXu@yeR6nnho97A7Sd
#ENV RABBITMQ_USERNAME cep-handler

ENV EXCHANGE EXCHANGE

ENV MODE mono

ENV BROKER Rabbitmq

ENV WORKER_ID 1


# Sleep time for each cicle of registering new event types
ENV TIMEOUT_A 100000

# Sleep time for each cicle of checking underload of resources
ENV TIMEOUT_U 100000

# Sleep time for each cicle of checking underload of resources
ENV TIMEOUT_O 100000




#Compilation
RUN mkdir -p /cep-worker/
ADD . /cep-worker/
WORKDIR /cep-worker/
RUN mvn -Dmaven.test.skip=true package
RUN ls target/


#CMD  java -jar target/CEPWorker-1.0.jar
