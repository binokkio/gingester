FROM openjdk:11

RUN apt-get update && apt-get install -y maven
RUN git clone --depth 1 --branch v0.7.25 https://github.com/binokkio/gingester.git

WORKDIR gingester
RUN mvn package

WORKDIR /
RUN ln -s gingester/executable/target/gingester-*.jar gingester.jar

ENTRYPOINT ["java", "-jar", "gingester.jar"]
