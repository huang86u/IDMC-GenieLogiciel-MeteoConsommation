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
- PostgreSQL disponible via un profil dédier
- Jeu de données téléchargeable ci-dessous : <a href="https://drive.google.com/file/d/1Ikv5DKR1QinmYtLXZMMJTuVj29TkTeUz/view?usp=sharing" download>DatasetGenieLogiciel.zip</a>
- *Note : Maven n'a pas besoin d'être installé globalement, le projet intègre directement le wrapper Maven (mvnw / mvnw.cmd).*

## Lancement du projet
Télecharger le projet et le dataset, puis suivre les instructions ci-dessous pour lancer l'application.
```bash
git clone https://github.com/huang86u/IDMC-GenieLogiciel-MeteoConsommation.git
```
### Option A: Profil Local (H2 + Import ZIP)
1. Placer le fichier `DatasetGenieLogiciel.zip` dans un emplacement accessible.
Windows (PowerShell) :
````bash
$env:DATASET_ZIP_PATH="C:\chemin\vers\DatasetGenieLogiciel.zip"
.\mvnw.cmd spring-boot:run
````
macOS / Linux (Terminal) :
```bash
export DATASET_ZIP_PATH="/chemin/vers/DatasetGenieLogiciel.zip"
./mvnw spring-boot:run
````
OPTION B: Profil PostgreSQL
1. Configurer une base PostgreSQL et créer une base de données
2. Exécuter ça en remplace les valeurs de ``POSTGRES_URL``, ``POSTGRES_USERNAME`` et ``POSTGRES_PASSWORD`` par les valeurs de votre base PostgreSQL :
```bash
$env:SPRING_PROFILES_ACTIVE="postgres"
$env:POSTGRES_URL="jdbc:postgresql://localhost:5432/nom_db"
$env:POSTGRES_USERNAME="votre_user"
$env:POSTGRES_PASSWORD="votre_password"
.\\mvnw.cmd spring-boot:run
```
3. Pour ajouter les données à la base PostgreSQL, ouvrez ses 2 fichiers : ```src/main/java/com/example/genielogicielmeteoconsommation/testimport.html``` et ```src/main/java/com/example/genielogicielmeteoconsommation/import-meteo.html```

- ```testimport.html``` : Mettez le fichier eco2mix-regional
- ```import-meteo.html``` : Mettez les 10 fichiers départementaux de données météo du Grand Est.

Celà va prendre un peu de temps selon la puissance de votre machine.
## Accès à l'application
- Application web : `http://localhost:8080`
- Console H2 (profil local) : `http://localhost:8080/h2-console

## Tests et développement
Pour lancer la suite de tests unitaires :
```bash
.\mvnw.cmd test
```
Compile et package l'application :
```bash
.\mvnw.cmd clean package
```

Notes utiles :

- le premier demarrage peut etre plus long, car le backend initialise la base locale avec le dataset
- les demarrages suivants sont plus rapides, car la base H2 locale est deja remplie
- si `DATASET_ZIP_PATH` n'est pas renseigne, l'application utilisera la valeur par defaut definie dans `src/main/resources/application-local.properties`, qui doit donc etre adaptee si elle ne correspond pas a la machine

### Imports backend

- `POST /api/donnees/importer-electricite`
- `POST /api/donnees/importer-meteo`

Ces endpoints existent encore, mais l'interface utilisateur n'en depend plus.

### Dashboard

- `GET /api/dashboard/departments`
- `GET /api/dashboard/overview`

## Structure principale
- `POST /api/dashboard/estimate`

## Notes de conception

- la consommation Grand Est est reconstruite a partir des anciennes regions `Alsace`, `Champagne-Ardenne` et `Lorraine` lorsque ces series sont presentes
- les departements pris en compte sont `08, 10, 51, 52, 54, 55, 57, 67, 68, 88`
- l'estimation repose sur une regression lineaire simple basee sur les donnees filtrees dans le dashboard
