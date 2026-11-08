# ==========================================
# ÉTAPE 1 : Build de l'application (Maven + Java 25)
# ==========================================
FROM eclipse-temurin:25-jdk-jammy AS builder

WORKDIR /app

# Copie des fichiers de configuration Maven et des sources
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# Autoriser l'exécution du wrapper Maven et compiler le projet en ignorant les tests pour aller plus vite
RUN chmod +x ./mvnw && ./mvnw clean package -DskipTests

# ==========================================
# ÉTAPE 2 : Image finale légère (JRE 25)
# ==========================================
FROM eclipse-temurin:25-jre-jammy

# Création d'un utilisateur non-root dédié pour des raisons de sécurité
RUN groupadd -g 1000 samatur && \
    useradd -u 1000 -g samatur -m -s /bin/bash samatur

WORKDIR /app

# Récupération du fichier .jar généré depuis l'étape de build
COPY --from=builder /app/target/*.jar app.jar

# Attribution des droits à l'utilisateur non-root
RUN chown -R samatur:samatur /app

# Passage à l'utilisateur non-root
USER samatur

# Exposition du port de l'application
EXPOSE 8080

# Utilisation des variables d'environnement pour la configuration (BDD, etc.)
# L'utilisation de la forme exec (tableau) permet à l'application de recevoir proprement les signaux d'arrêt (SIGTERM)
ENTRYPOINT ["java", "-jar", "app.jar"]