FROM openjdk:17-alpine
RUN apk update
RUN apk upgrade
RUN apk add maven
WORKDIR app
ENV PORT 8080
EXPOSE 8080
COPY . /app
RUN mvn package -DskipTests
ENTRYPOINT exec java $JAVA_OPTS -jar target/*.jar