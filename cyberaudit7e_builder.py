import os
import hashlib

# Le fichier de commande généré
OUTPUT_CMD = "rebuild_project.cmd"

# Racine du projet Java = dossier courant
PROJECT_ROOT = "."

# Dossiers à exclure
EXCLUDED_DIRS = {".git", ".mvn", ".vscode", "target"}

# Fichiers à exclure
EXCLUDED_FILES = {".gitignore", ".gitattributes"}


def is_text_file(path, blocksize=512):
    """Détecte si un fichier est texte (pas d'octet nul)."""
    try:
        with open(path, "rb") as f:
            chunk = f.read(blocksize)
            return b"\x00" not in chunk
    except:
        return False


def escape_line(line):
    """
    Escape minimal pour echo( :
    - on laisse passer tout le Markdown
    - on évite seulement le cas où la ligne commence par '('
      (car echo( interprète directement après le '(')
    """
    if line.startswith("("):
        return "^" + line
    return line


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
    script_name = os.path.basename(__file__)
    original_hashes = {}

    with open(OUTPUT_CMD, "w", encoding="utf-8-sig", newline="\r\n") as cmd:
        # Relance automatique dans cmd.exe si lancé depuis PowerShell
        writeln(cmd, "@echo off")
        writeln(cmd, ":: Force l'execution dans cmd.exe si lance depuis PowerShell")
        writeln(cmd, "if not \"%ComSpec%\"==\"%SystemRoot%\\System32\\cmd.exe\" (")
        writeln(cmd, "    \"%SystemRoot%\\System32\\cmd.exe\" /c \"%~f0\" %*")
        writeln(cmd, "    exit /b")
        writeln(cmd, ")")
        writeln(cmd, "")

        # Bannière cybernétique
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
        writeln(cmd, "echo Reconstruction du projet Java...")
        writeln(cmd, "")

        # 1. Création des dossiers
        for root, dirs, files in os.walk(PROJECT_ROOT):
            # Normaliser root relatif
            rel_root = os.path.relpath(root, PROJECT_ROOT)
            if rel_root == ".":
                rel_root = ""

            # Exclure les dossiers
            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for d in dirs:
                dir_path = os.path.join(rel_root, d) if rel_root else d
                writeln(cmd, f"mkdir \"{dir_path}\"")

        # 2. Écriture des fichiers texte + SHA-256
        for root, dirs, files in os.walk(PROJECT_ROOT):
            rel_root = os.path.relpath(root, PROJECT_ROOT)
            if rel_root == ".":
                rel_root = ""

            dirs[:] = [d for d in dirs if d not in EXCLUDED_DIRS]

            for file in files:
                # Exclure certains fichiers
                if file in EXCLUDED_FILES:
                    continue
                if file == script_name:
                    continue
                if file == OUTPUT_CMD:
                    continue

                file_path = os.path.join(root, file)
                rel_path = os.path.relpath(file_path, PROJECT_ROOT)

                if not is_text_file(file_path):
                    print(f"[IGNORÉ] Binaire : {rel_path}")
                    continue

                # Hash attendu
                expected_hash = sha256_of_file(file_path)
                original_hashes[rel_path] = expected_hash

                writeln(cmd, "")
                writeln(cmd, f":: Fichier : {rel_path}")
                writeln(cmd, f"echo Creation de {rel_path}")

                first = True
                with open(file_path, "r", encoding="utf-8", errors="ignore") as f:
                    for line in f:
                        line = line.rstrip("\n\r")
                        safe = escape_line(line)

                        if first:
                            writeln(cmd, f"echo({safe} > \"{rel_path}\"")
                            first = False
                        else:
                            writeln(cmd, f"echo({safe} >> \"{rel_path}\"")

                if first:
                    # Fichier vide
                    writeln(cmd, f"type nul > \"{rel_path}\"")

                # Vérification SHA-256 immédiate
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

        for rel_path, expected_hash in original_hashes.items():
            label = rel_path.replace("/", "_").replace("\\", "_")

            writeln(cmd, f"echo Verification : {rel_path}")
            writeln(cmd, f"if not exist \"{rel_path}\" (echo MANQUANT : {rel_path} & goto next_{label})")

            writeln(cmd, f"certutil -hashfile \"{rel_path}\" SHA256 > \"{rel_path}.sha\"")
            writeln(cmd, f"findstr /C:\"{expected_hash.upper()}\" \"{rel_path}.sha\" >nul")
            writeln(cmd, f"if %errorlevel%==0 (echo IDENTIQUE : {rel_path}) else (echo DIFFERENT : {rel_path})")
            writeln(cmd, f"del \"{rel_path}.sha\"")

            writeln(cmd, f":next_{label}")
            # writeln(cmd, "")

        writeln(cmd, "")
        writeln(cmd, "echo Diff termine.")
        writeln(cmd, "pause")

    print(f"Fichier {OUTPUT_CMD} genere avec succes.")


if __name__ == "__main__":
    generate_cmd()
