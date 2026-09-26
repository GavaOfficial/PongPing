#!/usr/bin/env bash
# Builds the web version of PongPing (plain HTML5/JavaScript, no plugins).
#
#   1. translates the Java sources to JavaScript  -> web/js/pongping.js
#   2. assembles the site with the game resources -> dist-web/
#
# Serve dist-web/ with any static web server, e.g.:  python3 -m http.server -d dist-web 8000
set -euo pipefail
cd "$(dirname "$0")/.."

OUT=dist-web
TRANSPILER=tools/java2js/target/java2js-1.0.jar

if command -v java >/dev/null 2>&1 && command -v mvn >/dev/null 2>&1; then
    if [ ! -f "$TRANSPILER" ] || [ -n "$(find tools/java2js/src tools/java2js/pom.xml -newer "$TRANSPILER" 2>/dev/null)" ]; then
        echo "[1/3] Building the Java -> JavaScript translator..."
        (cd tools/java2js && mvn -q -B package)
    fi

    echo "[2/3] Translating Java sources to JavaScript..."
    # The web build uses src-web/Main.java as entry point instead of the desktop src/Main.java.
    java -jar "$TRANSPILER" web/js/pongping.js src src-web --exclude src/Main.java
else
    # Without Java/Maven the committed web/js/pongping.js is used as is.
    echo "[1-2/3] Java or Maven not found: using the existing web/js/pongping.js"
fi

echo "[3/3] Assembling $OUT/..."
rm -rf "$OUT"
mkdir -p "$OUT"
cp -r web/. "$OUT/"
cp icon.png "$OUT/"
cp -r lingue temi "$OUT/"
if [ -d music ]; then cp -r music "$OUT/"; fi

# Fonts loaded by the game (the paths used in the Java code).
FONTS=$(grep -rhoE '"font/[^"]+\.ttf"' src | tr -d '"' | sort -u)
for f in $FONTS; do
    mkdir -p "$OUT/$(dirname "$f")"
    cp "$f" "$OUT/$f"
done

# Directory listings used by ContextLoader in web mode.
for dir in "$OUT/temi/GameBack" "$OUT"/temi/Padle/*; do
    [ -d "$dir" ] && (cd "$dir" && ls | grep -E '\.(png|jpg|jpeg|gif|txt)$' > index.list || true)
done

# Manifest of the resources to preload (what the desktop JAR contains).
(
    cd "$OUT"
    {
        echo '{"files": ['
        { echo icon.png; echo "$FONTS"; find lingue temi -type f; if [ -d music ]; then find music -type f; fi; } \
            | grep -v '^$' | sort -u | sed 's/.*/    "&"/' | sed '$!s/$/,/'
        echo ']}'
    } > assets.json
)
echo "Done: $OUT/ ($(du -sh "$OUT" | cut -f1))"
