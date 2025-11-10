# ===== STAGE 1: build with Maven =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY . .
RUN mvn -B -DskipTests package
# Copy built WAR and rename to ROOT.war so app is served at '/'
RUN set -eux; WAR_FILE=$(ls target/*.war | head -n1); cp "$WAR_FILE" /src/ROOT.war

# ===== STAGE 2: runtime Tomcat 10 =====
FROM tomcat:10.1-jdk17-temurin
RUN rm -rf /usr/local/tomcat/webapps/*
ENV TZ=Europe/Rome \
    CATALINA_OPTS="-Xms256m -Xmx512m" \
    JAVA_OPTS=""
COPY --from=build /src/ROOT.war /usr/local/tomcat/webapps/ROOT.war
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s CMD wget -qO- http://localhost:8080/ || exit 1
