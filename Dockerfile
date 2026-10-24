FROM maven:3.9.11-eclipse-temurin-21 AS build

WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B package \
    && jar_count="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | wc -l)" \
    && test "$jar_count" -eq 1 \
    && jar_path="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' -print -quit)" \
    && cp "$jar_path" /application.jar

FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app
COPY --from=build /application.jar /app/application.jar

ENV PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/application.jar"]
