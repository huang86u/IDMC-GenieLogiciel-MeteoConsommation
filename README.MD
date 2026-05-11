# Projet Genie Logiciel

Ce depot contient une application Spring Boot pour etudier le lien entre les variations meteorologiques et la consommation electrique.

## Prerequis

- Java 21
- Maven Wrapper fourni avec le projet
- PostgreSQL si vous voulez utiliser une base persistante

Par defaut, l'application utilise une base H2 en memoire pour faciliter le lancement local et les tests. Pour PostgreSQL, renseignez les variables d'environnement suivantes:

```bash
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/meteo_consommation
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=mot_de_passe
```

## Lancement

```bash
git clone https://github.com/huang86u/IDMC-GenieLogiciel-MeteoConsommation.git
cd IDMC-GenieLogiciel-MeteoConsommation
./mvnw spring-boot:run
```

Sous Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

## Imports

- Electricite: `POST /api/donnees/importer-electricite` avec un fichier CSV dans le champ `file`
- Meteo: `POST /api/donnees/importer-meteo` avec un ou plusieurs fichiers CSV dans le champ `files`

Deux pages de test sont disponibles une fois l'application lancee:

- `http://localhost:8080/test-import.html`
- `http://localhost:8080/import-meteo.html`
