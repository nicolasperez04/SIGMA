FROM eclipse-temurin:21-jdk

EXPOSE 8080

WORKDIR /app

COPY ./pom.xml /app
COPY ./.mvn /app/.mvn
COPY ./mvnw /app

RUN ./mvnw dependency:go-offline

COPY ./src /app/src

RUN ./mvnw clean install -DskipTests    #nos saltamos los test unitarios (pero esto al momento de pasar a producción tiene que estar)


ENTRYPOINT ["java", "-jar","/app/target/SIGMA-0.0.1-SNAPSHOT.jar"]