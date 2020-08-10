FROM openjdk:8

RUN apt-get update && apt-get install -y maven
COPY . /project
RUN  cd /project && mvn -DskipTests package

#run the spring boot application
ENTRYPOINT ["java", "-jar","/project/target/cdpo-0.0.1-SNAPSHOT.jar"]