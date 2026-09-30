#!/usr/bin/env sh

set -eu

if [ "$#" -ne 2 ]; then
  echo "Usage: restore.sh <postgres-url> <backup-file>" >&2
  exit 2
fi

if [ "${CONFIRM_DATABASE_RESTORE:-}" != "yes" ]; then
  echo "Set CONFIRM_DATABASE_RESTORE=yes to confirm the destructive restore." >&2
  exit 2
fi

database_url=$1
backup_file=$2

if [ ! -r "$backup_file" ]; then
  echo "Backup file is not readable: $backup_file" >&2
  exit 2
fi

pg_restore \
  --dbname="$database_url" \
  --clean \
  --if-exists \
  --no-owner \
  --no-privileges \
  --exit-on-error \
  --single-transaction \
  "$backup_file"
