FROM node:22-alpine AS web
WORKDIR /build/frontend
COPY frontend/package*.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

FROM maven:3.9.9-eclipse-temurin-17 AS java
WORKDIR /build/backend
COPY backend/pom.xml ./
RUN mvn -B -ntp dependency:go-offline
COPY backend/src ./src
COPY --from=web /build/frontend/dist /build/frontend/dist
RUN mvn -B -ntp verify

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S his && adduser -S -G his his
WORKDIR /app
COPY --from=java /build/backend/target/cloud-his-1.0.0.jar app.jar
RUN mkdir /app/data && chown -R his:his /app
USER his
ENV BIND_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
