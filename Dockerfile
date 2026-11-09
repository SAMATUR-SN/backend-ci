# ==========================================
# ÉTAPE 1 : Build de l'application (Maven + Java 25)
# ==========================================
FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /app

# Wrapper Maven et pom.xml d'abord : la couche des dépendances reste en cache
# tant que pom.xml ne change pas, au lieu d'être retéléchargée à chaque modification du code
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x ./mvnw && ./mvnw -B -q dependency:go-offline

# Sources ensuite. Les tests tournent dans Jenkins avant la construction de l'image.
COPY src src
RUN ./mvnw -B -q package -DskipTests

# ==========================================
# ÉTAPE 2 : Image finale légère (JRE 25)
# ==========================================
FROM eclipse-temurin:25-jre-jammy

# Création d'un utilisateur non-root dédié pour des raisons de sécurité
RUN groupadd -g 1000 samatur && \
    useradd -u 1000 -g samatur -m -s /bin/bash samatur

WORKDIR /app

# Récupération du fichier .jar généré depuis l'étape de build, au nom de l'utilisateur non-root
COPY --from=builder --chown=samatur:samatur /app/target/*.jar app.jar

# Passage à l'utilisateur non-root
USER samatur

# Exposition du port de l'application
EXPOSE 8080

# Mémoire de la JVM bornée : deux instances tournent en même temps pendant la mise à jour sans coupure.
# JAVA_TOOL_OPTIONS est lu par la JVM elle-même et reste surchargeable au déploiement.
ENV JAVA_TOOL_OPTIONS="-Xmx512m"

# Santé du conteneur, utilisée par Docker, Traefik et le pipeline CD.
# L'image n'a ni curl ni wget : bash ouvre la connexion lui-même (/dev/tcp), sans paquet de plus à scanner.
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=3 \
  CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/8080 && printf "GET /actuator/health HTTP/1.0\r\nHost: localhost\r\n\r\n" >&3 && grep -q "\"status\":\"UP\"" <&3'

# Forme exec (tableau) : l'application reçoit directement les signaux d'arrêt (SIGTERM)
ENTRYPOINT ["java", "-jar", "app.jar"]
