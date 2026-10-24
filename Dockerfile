FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn --batch-mode --no-transfer-progress clean package -DskipTests

FROM tomcat:10.1-jdk17-temurin
ENV APP_ENV=production
ENV WEBSITES_PORT=8080
RUN rm -rf /usr/local/tomcat/webapps/*
RUN sed -i 's/port="8080"/port="${tomcat.http.port:8080}"/' /usr/local/tomcat/conf/server.xml
COPY --from=build /app/target/smart-library.war /usr/local/tomcat/webapps/ROOT.war
COPY docker-entrypoint.sh /usr/local/bin/docker-entrypoint.sh
EXPOSE 8080
ENTRYPOINT ["/bin/sh", "/usr/local/bin/docker-entrypoint.sh"]
