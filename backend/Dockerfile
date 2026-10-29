# Etapa de compilación
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Copiar archivos de Maven para aprovechar la caché de capas
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Puerto configurado en application-dev.yaml
EXPOSE ${BACKEND_PORT}

ENTRYPOINT ["java", "-jar", "app.jar"]
