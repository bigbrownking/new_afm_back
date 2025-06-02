FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /a

RUN mkdir -p /app/pdfs

COPY --from=build /app/target/*.jar app.jar

COPY --from=build /app/*.pdf /app/pdfs/

EXPOSE 8080

# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]