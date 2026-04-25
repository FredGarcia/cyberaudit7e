# Script Python qui parcourt l’intégralité du projet Java, lit chaque fichier source et ressource, 
# puis génère un unique fichier de commande Windows (.cmd). 
# Ce fichier .cmd doit contenir, sous forme concaténée et encodée, tout le contenu du projet, 
# et être capable de recréer fidèlement l’arborescence et tous les fichiers du projet Java original. 
# En résumé: un seul fichier .cmd autonome qui permet de redéployer le projet source depuis zéro.

# Script Python qui parcourt l’intégralité du projet Java, lit chaque fichier source et ressource, 
# puis génère un unique fichier de commande Windows (.cmd).

import os
import hashlib
from pathlib import Path

# Configuration du projet
PROJECT_ROOT = "."
OUTPUT_CMD = "cyberaudit-rebuild.cmd"
EXCLUDED_DIRS = {".git", ".vscode", "target"}
EXCLUDED_FILES = {".gitignore", ".gitattributes", "mvnw", "mvnw.cmd", "cyberaudit7e-builder.py", OUTPUT_CMD}

# ============== Fonctions utilitaires ==============


def sha256_of_bytes(data):
    return hashlib.sha256(data).hexdigest()


def escape_line_for_cmd(line):
    """
    Échappement pour le batch.
    IMPORTANT : On ne touche PAS aux guillemets (") pour préserver le code Java.
    On échappe seulement les caractères qui perturbent la structure de la commande CMD.
    """
    # 1. Le caret (^) est le caractère d'échappement, il doit être doublé
    line = line.replace("^", "^^")
    
    # 2. Les opérateurs de contrôle et redirection
    line = line.replace("&", "^&")
    line = line.replace("|", "^|")
    line = line.replace("<", "^<")
    line = line.replace(">", "^>")
    
    # 3. Le pourcentage
    line = line.replace("%", "%%")
    
    # NOTE IMPORTANTE :
    # On n'échappe PAS les guillemets ("). 
    # On n'échappe PAS les parenthèses () car on utilise 'echo('.
    
    return line


def writeln(cmd, text):
    cmd.write(text + "\r\n")

# ============== Génération du fichier .cmd ==============


def generate_cmd():
    script_name = Path(__file__).name
    original_hashes = {}

    with open(OUTPUT_CMD, "w", encoding="utf-8-sig", newline="\r\n") as cmd:
        writeln(cmd, "@echo off")
        writeln(cmd, "setlocal disabledelayedexpansion")
        writeln(cmd, "echo ================================================")
        writeln(cmd, "echo   RECONSTRUCTION DU PROJET JAVA")
        writeln(cmd, "echo ================================================")
        writeln(cmd, "echo.")

        # Création des dossiers
        for root, dirs, files in os.walk(PROJECT_ROOT):
            rel_root = Path(root).relative_to(PROJECT_ROOT)
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]
            for d in dirs:
                dir_path = os.path.join(rel_root, d) if rel_root != Path('.') else d
                writeln(cmd, f'if not exist "{dir_path}" mkdir "{dir_path}"')

        # Écriture des fichiers
        for root, dirs, files in os.walk(PROJECT_ROOT):
            rel_root = Path(root).relative_to(PROJECT_ROOT)
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for file in files:
                if file in EXCLUDED_FILES or file == script_name:
                    continue

                file_path = Path(root) / file
                rel_path = file_path.relative_to(PROJECT_ROOT)

                # Lecture du contenu
                try:
                    with open(file_path, 'rb') as f:
                        data = f.read()
                    content = data.decode('utf-8')
                except UnicodeDecodeError:
                    print(f"[IGNORÉ] binaire: {rel_path}")
                    continue
                
                # Calcul du hash
                expected_hash = sha256_of_bytes(data)
                original_hashes[str(rel_path)] = expected_hash
                cmd.write(f'echo Décompression de {rel_path}\r\n')

                lines = content.splitlines()
                if not lines:
                    cmd.write(f'type nul > "{rel_path}"\r\n')
                else:
                    first = True
                    for line in lines:
                        safe_line = escape_line_for_cmd(line)
                        
                        # ASTUCE CRITIQUE :
                        # On place la redirection (>> fichier) AVANT la commande echo.
                        # Cela évite que les guillemets présents dans 'safe_line' 
                        # n'interfèrent avec l'analyse syntaxique de la redirection.
                        if first:
                            writeln(cmd, f'> "{rel_path}" echo( {safe_line}')
                            first = False
                        else:
                            writeln(cmd, f'>> "{rel_path}" echo( {safe_line}')

                # Vérification SHA-256
                writeln(cmd, f'certutil -hashfile "{rel_path}" SHA256 | findstr /I /C:"{expected_hash.upper()}" >nul')
                writeln(cmd, f'if %errorlevel%==0 (echo    [OK] {rel_path}) else (echo    [ERREUR] {rel_path})')

        # Mode DIFF
        writeln(cmd, "echo.")
        writeln(cmd, "===== MODE DIFF =====")
        for rel_path, expected_hash in original_hashes.items():
            label = rel_path.replace("/", "_").replace("\\", "_")
            writeln(cmd, f'if not exist "{rel_path}" (echo MANQUANT : {rel_path} & goto next_{label})')
            writeln(cmd, f'certutil -hashfile "{rel_path}" SHA256 | findstr /I /C:"{expected_hash.upper()}" >nul')
            writeln(cmd, f'if %errorlevel%==0 (echo IDENTIQUE : {rel_path}) else (echo DIFFERENT : {rel_path})')
            writeln(cmd, f':next_{label}')

        writeln(cmd, "echo.")
        writeln(cmd, "echo Reconstruction terminée.")
        writeln(cmd, "pause")
        
    print(f"✅ {OUTPUT_CMD} généré")


if __name__ == "__main__":
    generate_cmd()
    # ============== POST-TRAITEMENT : Nettoyage des lignes vides ==============
print(f"Nettoyage de {OUTPUT_CMD} en cours...")

with open(OUTPUT_CMD, "r", encoding="utf-8-sig") as f:
    lines = f.readlines()

# Supprimer uniquement les lignes réellement vides
clean_lines = [line for line in lines if line.rstrip("\r\n") != ""]

with open(OUTPUT_CMD, "w", encoding="utf-8-sig", newline="\r\n") as f:
    f.writelines(clean_lines)

print(f"✅ {OUTPUT_CMD} généré et compacté avec succès.")
if __name__ == "__main__":
    generate_cmd()
