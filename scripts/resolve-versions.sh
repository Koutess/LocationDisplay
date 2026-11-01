#!/usr/bin/env bash
# Prints the Gradle -P flags (Yarn, Loader, Fabric API) for a given Minecraft version,
# looked up live from Fabric's metadata. Needs: curl, jq.
#   gradle build $(scripts/resolve-versions.sh 1.21.4)
set -euo pipefail
MC="${1:?usage: resolve-versions.sh <minecraft version, e.g. 1.21.4>}"

YARN=$(curl -fsSL "https://meta.fabricmc.net/v2/versions/yarn/${MC}" | jq -r 'sort_by(.build) | last | .version')
LOADER=$(curl -fsSL "https://meta.fabricmc.net/v2/versions/loader" | jq -r '[.[] | select(.stable)][0].version')
API=$(curl -fsSL "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/maven-metadata.xml" \
      | grep -oE '<version>[^<]+</version>' | sed -E 's#</?version>##g' \
      | grep -F -- "+${MC}" | grep -E -- "\+${MC//./\\.}\$" | sort -V | tail -1)

for v in "$YARN" "$LOADER" "$API"; do
  if [ -z "$v" ] || [ "$v" = "null" ]; then
    echo "Could not resolve versions for Minecraft ${MC}" >&2
    exit 1
  fi
done
echo "-Pminecraft_version=${MC} -Pyarn_mappings=${YARN} -Ploader_version=${LOADER} -Pfabric_version=${API}"
