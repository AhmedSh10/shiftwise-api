# --- build ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q package -DskipTests

# --- run ---
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 shiftwise
COPY --from=build /app/target/shiftwise-api-*.jar app.jar
USER shiftwise
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
