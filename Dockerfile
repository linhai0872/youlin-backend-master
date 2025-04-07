# Docker 镜像构建
# @author <a href="https://github.com/linhai0872">林海
FROM maven:3.5-jdk-8-alpine as builder

# Copy local code to the container image.
WORKDIR /app
COPY pom.xml .
COPY src ./src

# Build a release artifact.
RUN mvn package -DskipTests

# Run the web service on container startup.
CMD ["java","-jar","/app/target/youlin-backend-0.0.1-SNAPSHOT.jar","--spring.profiles.active=prod"]

