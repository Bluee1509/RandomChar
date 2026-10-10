# ============================================
# Giai đoạn 1: Build ứng dụng bằng Maven
# ============================================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

# Bước 1: Copy pom.xml và tải dependencies trước
# (layer này sẽ được Docker cache, không phải tải lại mỗi lần build)
COPY pom.xml .
RUN mvn dependency:go-offline -B -DskipTests

# Bước 2: Copy source code
COPY src ./src

# Bước 3: Build offline (dùng dependencies đã tải ở bước 1)
RUN mvn clean package -o -DskipTests

# ============================================
# Giai đoạn 2: Chạy ứng dụng bằng JDK nhẹ
# ============================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy file JAR từ giai đoạn build
COPY --from=build /app/target/*.jar app.jar

# Render sẽ inject biến PORT, mặc định 8080
ENV PORT=8080
EXPOSE 8080

# Chạy ứng dụng
ENTRYPOINT ["java", "-jar", "app.jar"]