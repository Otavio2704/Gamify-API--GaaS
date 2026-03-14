# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS builder

WORKDIR /app

# Copia apenas o pom.xml primeiro para cachear as dependências
COPY pom.xml .
RUN mvn dependency:go-offline -B 2>/dev/null || true

# Copia o código fonte e compila
COPY src ./src
RUN mvn clean package -DskipTests -B

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine AS runtime

# Metadados
LABEL maintainer="GamifyAPI"
LABEL description="GamifyAPI — Gamificação como Serviço"

WORKDIR /app

# Usuário não-root para segurança
RUN addgroup -S gamify && adduser -S gamify -G gamify
USER gamify

# Copia o JAR da fase de build
COPY --chown=gamify:gamify --from=builder /app/target/*.jar app.jar

# Porta padrão da aplicação
EXPOSE 8080

# Variáveis de ambiente padrão (sobrescritas via docker-compose ou env)
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseContainerSupport"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
