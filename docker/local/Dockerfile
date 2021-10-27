FROM openjdk:11

RUN mkdir gingester
COPY executable gingester/executable

WORKDIR /
RUN ln -s gingester/executable/target/gingester-*.jar gingester.jar

ENTRYPOINT ["java", "-jar", "gingester.jar"]
