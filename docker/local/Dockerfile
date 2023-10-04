FROM eclipse-temurin:17-jre-centos7
COPY executable/target/gingester-*.jar /
RUN ln -s gingester-*.jar gingester.jar
ENTRYPOINT ["java", "-jar", "gingester.jar"]
