@echo off
echo NETTOYAGE DOCKER

:: ------------------------------------------
:: Arrêter et supprimer les conteneurs
:: ------------------------------------------
echo Arrêter tous les conteneurs
FOR /F "tokens=*" %%i IN ('docker ps -aq') DO docker stop %%i

echo Supprimer tous les conteneurs
FOR /F "tokens=*" %%i IN ('docker ps -aq') DO docker rm %%i

:: Vérification manuelle (optionnel, pour reproduire la logique "if empty")
docker ps -aq | findstr "." >nul
if %errorlevel% neq 0 (
    echo Aucun conteneur à arrêter ou supprimer.
)

:: ------------------------------------------
:: Images
:: ------------------------------------------
echo Supprimer toutes les images
:: On utilise -f pour forcer la suppression sans confirmation
FOR /F "tokens=*" %%i IN ('docker images -q') DO docker rmi -f %%i

:: ------------------------------------------
:: Réseaux
:: ------------------------------------------
echo Supprimer tous les réseaux non utilisés
docker network prune -f

:: ------------------------------------------
:: Nettoyage global
:: ------------------------------------------
echo Nettoyage global (prune)
docker system prune -a --volumes -f

docker ps --all

:: ------------------------------------------
:: Volumes
:: ------------------------------------------
echo Supprimer tous les volumes
FOR /F "tokens=*" %%i IN ('docker volume ls -q') DO docker volume rm %%i

echo Supprimer les volumes non utilisés (prune)
docker volume prune -f

docker volume ls
docker network ls

:: ------------------------------------------
:: Dossier local
:: ------------------------------------------
echo Nettoyage auditaccess
:: Sous Windows, pas besoin de sudo, mais il faut les droits administrateur 
:: si le dossier est protégé. /s = récursif, /q = silencieux (pas de confirmation)
if exist auditaccess-poc rmdir /s /q auditaccess-poc

echo Nettoyage termine.
pause
