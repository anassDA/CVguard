# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# Etape 1 — construction
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Le cache BuildKit conserve le depot local Maven d'une construction a l'autre.
RUN --mount=type=cache,target=/root/.m2 mvn -B -ntp clean package -DskipTests

# ---------------------------------------------------------------------------
# Etape 2 — execution
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy

# Tesseract et ses donnees de langue francaise et anglaise (regle R6).
# Les paquets de developpement fournissent les liens symboliques non versionnes
# (libtesseract.so, liblept.so) que JNA recherche au chargement.
RUN apt-get update \
 && apt-get install --no-install-recommends -y \
        tesseract-ocr \
        tesseract-ocr-fra \
        tesseract-ocr-eng \
        libtesseract-dev \
        libleptonica-dev \
 && rm -rf /var/lib/apt/lists/*

ENV TESSDATA_PREFIX=/usr/share/tesseract-ocr/4.00/tessdata

# ENF-04 — moindre privilege : le service ne s'execute pas sous l'identite root.
RUN useradd --system --create-home --shell /usr/sbin/nologin cvguard
USER cvguard

WORKDIR /app
COPY --from=build /build/target/cvguard-*.jar /app/cvguard.jar

EXPOSE 8080

# ENF-05 — les documents sont traites en memoire ; aucun volume de donnees n'est monte.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/cvguard.jar"]
