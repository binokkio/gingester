FROM maven:3-eclipse-temurin-17 AS build
RUN git clone https://github.com/binokkio/gingester.git
RUN mvn -f gingester/pom.xml clean package

FROM eclipse-temurin:17-jre-centos7
COPY --from=build /gingester/executable/target/gingester-*.jar /
RUN ln -s gingester-*.jar gingester.jar
ENTRYPOINT ["java", "-jar", "gingester.jar"]
