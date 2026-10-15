#!/usr/bin/env bash
# Bump versionName, commit, tag, push.
# Pakai: ./bump-version.sh [patch|minor|major]  (default: patch)
set -euo pipefail

PART="${1:-patch}"
FILE="app/build.gradle.kts"

CURRENT=$(grep -oE '\?: "[0-9]+\.[0-9]+\.[0-9]+"' "$FILE" | head -n1 | tr -d '?: " ')
if [ -z "$CURRENT" ]; then
  echo "versionName tidak ditemukan di $FILE" >&2
  exit 1
fi

IFS='.' read -r MAJOR MINOR PATCH <<< "$CURRENT"
case "$PART" in
  major) MAJOR=$((MAJOR + 1)); MINOR=0; PATCH=0 ;;
  minor) MINOR=$((MINOR + 1)); PATCH=0 ;;
  patch) PATCH=$((PATCH + 1)) ;;
  *) echo "Pakai: $0 [patch|minor|major]" >&2; exit 1 ;;
esac
NEXT="${MAJOR}.${MINOR}.${PATCH}"

sed -i -E "s/(\?: \")[0-9]+\.[0-9]+\.[0-9]+(\")/\1${NEXT}\2/" "$FILE"

git add "$FILE"
git commit -m "chore(release): bump to ${NEXT}"
git tag -a "v${NEXT}" -m "Es Teh Evren v${NEXT}"
git push origin HEAD
git push origin "v${NEXT}"

echo "OK: v${CURRENT} -> v${NEXT} (tag pushed, CI release berjalan)"
