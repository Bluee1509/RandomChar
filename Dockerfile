# Giai đoạn 1: Build file .jar bằng Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# Tải dependency trước và thử lại tối đa 3 lần nếu gặp lỗi mạng
RUN for i in 1 2 3; do mvn -B -ntp dependency:go-offline \
      -Dmaven.wagon.http.retryHandler.count=5 && break || sleep 10; done

COPY src ./src
RUN mvn -B -ntp clean package -DskipTests \
      -Dmaven.wagon.http.retryHandler.count=5

# Giai đoạn 2: Chạy ứng dụng bằng JDK siêu nhẹ
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]