# Projet Genie Logiciel - Meteo / Consommation

Application Spring Boot permettant de charger, nettoyer, croiser, analyser et visualiser les donnees meteo et de consommation electrique pour etudier le Grand Est sur l'annee 2014.

## Fonctionnalites

- chargement automatique des donnees cote backend depuis un zip de dataset
- filtrage automatique sur l'annee 2014
- filtrage meteo sur les departements du Grand Est
- conversion des horodatages meteo UTC vers Europe/Paris
- tableau de bord web en HTML / CSS / JavaScript
- indicateurs de synthese, tendances quotidiennes, analyse par departement
- correlation temperature / consommation
- comparaison saisonniere
- estimation simple de consommation par regression lineaire

## Stack technique

- Java 21
- Spring Boot 3.4.3
- Spring Web
- Spring Data JPA
- H2 pour le profil local par defaut
- PostgreSQL disponible via un profil dedie
- Maven Wrapper

## Lancement rapide en local

Le profil `local` est actif par defaut et utilise une base H2 fichier stockee dans `data/`.

```bash
.\mvnw.cmd spring-boot:run
```

Si la base est vide, le backend peut initialiser automatiquement les donnees a partir du zip configure dans `application-local.properties`.
La variable d'environnement supportee est :

- `DATASET_ZIP_PATH`

Ensuite ouvrir :

- application web : `http://localhost:8080`
- console H2 : `http://localhost:8080/h2-console`

Configuration H2 par defaut :

- JDBC URL : `jdbc:h2:file:./data/meteo-consommation`
- user : `sa`
- password : vide

## Installation et lancement sur un autre PC

Pour executer le projet sur une autre machine, il faut simplement :

1. recuperer le projet
2. installer Java 21
3. recuperer le fichier `DatasetGenieLogiciel.zip`
4. indiquer au backend ou se trouve ce zip
5. lancer l'application avec le Maven Wrapper fourni

Le point le plus important est le dataset. Le backend charge automatiquement les donnees au premier demarrage si la base locale est vide, mais il faut que le chemin du zip soit accessible via la variable d'environnement `DATASET_ZIP_PATH`.

### Windows

Dans PowerShell, a la racine du projet :

```powershell
$env:DATASET_ZIP_PATH="C:\chemin\vers\DatasetGenieLogiciel.zip"
.\mvnw.cmd spring-boot:run
```

### macOS / Linux

Dans un terminal, a la racine du projet :

```bash
export DATASET_ZIP_PATH="/chemin/vers/DatasetGenieLogiciel.zip"
./mvnw spring-boot:run
```

Ensuite ouvrir :

- application web : `http://localhost:8080`
- console H2 : `http://localhost:8080/h2-console`

Notes utiles :

- Maven n'a pas besoin d'etre installe globalement, le projet utilise le wrapper `mvnw`
- le premier demarrage peut etre plus long, car le backend initialise la base locale avec le dataset
- les demarrages suivants sont plus rapides, car la base H2 locale est deja remplie
- si `DATASET_ZIP_PATH` n'est pas renseigne, l'application utilisera la valeur par defaut definie dans `src/main/resources/application-local.properties`, qui doit donc etre adaptee si elle ne correspond pas a la machine

## Utilisation avec PostgreSQL

Le profil `postgres` est disponible dans `src/main/resources/application-postgres.properties`.

Variables supportees :

- `POSTGRES_URL`
- `POSTGRES_USERNAME`
- `POSTGRES_PASSWORD`

Exemple :

```bash
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/meteo_consommation"
$env:POSTGRES_USERNAME="postgres"
$env:POSTGRES_PASSWORD="postgres"
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=postgres
```

## Parcours d'utilisation

1. Lancer l'application.
2. Laisser le backend charger les donnees si la base est vide.
3. Ouvrir `http://localhost:8080`.
4. Utiliser les filtres de date et de departements.
5. Explorer les pages :
   - synthese generale
   - vue Grand Est
   - profils par departement
   - relation meteo / consommation
   - analyse saisonniere
   - scenario d'estimation
   - transparence des donnees

## Endpoints utiles

### Imports backend

- `POST /api/donnees/importer-electricite`
- `POST /api/donnees/importer-meteo`

Ces endpoints existent encore, mais l'interface utilisateur n'en depend plus.

### Dashboard

- `GET /api/dashboard/departments`
- `GET /api/dashboard/overview`
- `POST /api/dashboard/estimate`

## Tests

Compiler :

```bash
.\mvnw.cmd compile
```

Lancer les tests :

```bash
.\mvnw.cmd test
```

## Structure principale

- `src/main/java/.../controller`
- `src/main/java/.../service`
- `src/main/java/.../repository`
- `src/main/java/.../dto`
- `src/main/java/.../support`
- `src/main/resources/static`

## Notes de conception

- la consommation Grand Est est reconstruite a partir des anciennes regions `Alsace`, `Champagne-Ardenne` et `Lorraine` lorsque ces series sont presentes
- les departements pris en compte sont `08, 10, 51, 52, 54, 55, 57, 67, 68, 88`
- l'estimation repose sur une regression lineaire simple basee sur les donnees filtrees dans le dashboard
