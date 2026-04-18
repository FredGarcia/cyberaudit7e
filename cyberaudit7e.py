import os
import hashlib

OUTPUT_CMD = "rebuild_project.cmd"
PROJECT_ROOT = "."  # dossier contenant ton projet Java
EXCLUDED_DIRS = {".git", ".mvn", ".vscode", "target"}

def is_text_file(path, blocksize=512):
    """Détecte si un fichier est texte (pas d'octet nul)."""
    try:
        with open(path, "rb") as f:
            chunk = f.read(blocksize)
            return b"\x00" not in chunk
    except:
        return False


def escape_line(line):
    """Échappe les caractères spéciaux Windows pour echo."""
    return (
        line.replace("^", "^^")
            .replace(">", "^>")
            .replace("<", "^<")
            .replace("|", "^|")
            .replace("&", "^&")
            .replace("%", "%%")
    )


def sha256_of_file(path):
    """Calcule le SHA-256 d'un fichier texte."""
    h = hashlib.sha256()
    with open(path, "rb") as f:
        h.update(f.read())
    return h.hexdigest()


def writeln(cmd, text):
    """Écrit une ligne avec fin de ligne Windows."""
    cmd.write(text + "\r\n")


def generate_cmd():
    with open(OUTPUT_CMD, "w", encoding="utf-8-sig", newline="\r\n") as cmd:

        writeln(cmd, "@echo off")
        writeln(cmd, "echo ================================================")
        writeln(cmd, "echo   SNAPSHOT CYBERNETIQUE DU PROJET JAVA")
        writeln(cmd, "echo ================================================")
        writeln(cmd, "echo.")
        writeln(cmd, "echo  - reconstruit le projet a l'identique")
        writeln(cmd, "echo  - verifie cryptographiquement chaque fichier")
        writeln(cmd, "echo  - compare avec l'original")
        writeln(cmd, "echo  - ne depend d'aucun outil externe")
        writeln(cmd, "echo  - est lisible par un humain")
        writeln(cmd, "echo  - est portable sur n'importe quel Windows")
        writeln(cmd, "echo  - C'est un artefact cybernetique")
        writeln(cmd, "echo.")
        writeln(cmd, "echo ================================================")
        writeln(cmd, "echo.")
        writeln(cmd, "")

        # Dictionnaire des hashes originaux
        original_hashes = {}

        # 1. Création des dossiers
        for root, dirs, files in os.walk(PROJECT_ROOT):
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for d in dirs:
                rel = os.path.relpath(os.path.join(root, d), PROJECT_ROOT)
                writeln(cmd, f"mkdir \"{rel}\"")

        # 2. Écriture directe des fichiers texte + SHA-256
        for root, dirs, files in os.walk(PROJECT_ROOT):
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for file in files:
                file_path = os.path.join(root, file)
                rel_path = os.path.relpath(file_path, PROJECT_ROOT)

                # Ignorer le script Python lui-même
                if file == os.path.basename(__file__):
                    continue

                if file in {".gitignore", ".gitattributes"}:
                    print(f"[IGNORÉ] Fichier exclu : {rel_path} {file}")
                    continue

                if not is_text_file(file_path):
                    print(f"[IGNORÉ] Binaire : {rel_path}")
                    continue

                # Hash attendu
                expected_hash = sha256_of_file(file_path)
                original_hashes[rel_path] = expected_hash

                writeln(cmd, "")
                writeln(cmd, f":: Fichier : {rel_path}")
                writeln(cmd, f"echo Création de {rel_path}")

                first = True
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line = line.rstrip("\n\r")
                        safe = escape_line(line)

                        if first:
                            writeln(cmd, f"echo {safe} > \"{rel_path}\"")
                            first = False
                        else:
                            writeln(cmd, f"echo {safe} >> \"{rel_path}\"")

                if first:
                    writeln(cmd, f"type nul > \"{rel_path}\"")

                # Vérification SHA-256
                writeln(cmd, f"certutil -hashfile \"{rel_path}\" SHA256 > \"{rel_path}.sha\"")
                writeln(cmd, f"findstr /C:\"{expected_hash.upper()}\" \"{rel_path}.sha\" >nul")
                writeln(cmd, f"if %errorlevel%==0 (echo OK : {rel_path}) else (echo ERREUR : {rel_path})")
                writeln(cmd, f"del \"{rel_path}.sha\"")

        # 3. Mode DIFF : comparaison avec l’original
        writeln(cmd, "")
        writeln(cmd, "echo.")
        writeln(cmd, "echo ===== MODE DIFF =====")
        writeln(cmd, "echo Comparaison avec le projet original...")
        writeln(cmd, "")

        # Génération du bloc DIFF
        for rel_path, expected_hash in original_hashes.items():
            writeln(cmd, f"echo Vérification : {rel_path}")
            label = rel_path.replace("/", "_").replace("\\", "_")
            writeln(cmd, f"if not exist \"{rel_path}\" (echo MANQUANT : {rel_path} & goto next_{label})")
            writeln(cmd, f"certutil -hashfile \"{rel_path}\" SHA256 > \"{rel_path}.sha\"")
            writeln(cmd, f"findstr /C:\"{expected_hash.upper()}\" \"{rel_path}.sha\" >nul")
            writeln(cmd, f"if %errorlevel%==0 (echo IDENTIQUE : {rel_path}) else (echo DIFFERENT : {rel_path})")
            writeln(cmd, f"del \"{rel_path}.sha\"")
            writeln(cmd, f":next_{label}")
            writeln(cmd, "")

        writeln(cmd, "")
        writeln(cmd, "echo Diff terminé.")
        writeln(cmd, "pause")

    print(f"Fichier {OUTPUT_CMD} généré avec succès.")

if __name__ == "__main__":
    generate_cmd()
