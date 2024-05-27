FROM maven:3.8.4-openjdk-17-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean package -DskipTests

FROM openjdk:17-slim
COPY --from=build /home/app/target/*.jar /usr/local/lib/app.jar
COPY wait-for-it.sh /usr/local/bin/wait-for-it.sh
RUN chmod +x /usr/local/bin/wait-for-it.sh
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "/usr/local/bin/wait-for-it.sh db:5432 -- java -jar /usr/local/lib/app.jar"]
