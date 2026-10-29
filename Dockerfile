FROM eclipse-temurin:21-jdk AS construccion
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 aplicacion
COPY --from=construccion /app/target/gestion-tickets-ia-*.jar app.jar
USER aplicacion
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
