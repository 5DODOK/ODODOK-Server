# ---- Build stage ----
FROM gradle:8.8-jdk21 AS builder
WORKDIR /app
COPY . .
# 테스트 빼고 fat jar 생성 (spring-boot plugin 기준)
RUN ./gradlew bootJar -x test

# ---- Run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# gradle 이미지의 기본 프로젝트 경로는 /home/gradle/project 또는 /app일 수 있어 헷갈릴 수 있어
# 위에서 WORKDIR을 /app으로 고정했으니 /app/build/libs에서 가져옵니다.
COPY --from=builder /app/build/libs/*.jar app.jar

# Render는 동적으로 PORT 환경변수를 줍니다. 반드시 그 포트로 바인딩해야 해요.
ENV PORT=8080
EXPOSE 8080
CMD ["sh", "-c", "java -Dserver.port=${PORT} -jar app.jar"]