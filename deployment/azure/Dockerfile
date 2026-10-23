FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress -DskipTests package

FROM tomcat:10.1-jdk17-temurin
ENV APP_ENV=production
ENV WEBSITES_PORT=8080
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /workspace/target/smart-library.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
CMD ["catalina.sh", "run"]
