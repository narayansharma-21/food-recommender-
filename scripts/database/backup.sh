#!/usr/bin/env sh

set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: backup.sh <postgres-url> <backup-file>" >&2
  exit 2
fi

database_url=$1
backup_file=$2

umask 077
mkdir -p "$(dirname "$backup_file")"

pg_dump \
  --dbname="$database_url" \
  --format=custom \
  --no-owner \
  --no-privileges \
  --file="$backup_file"

pg_restore --list "$backup_file" >/dev/null
