#!/usr/bin/env bash
# 在解压后的部署包中执行。只创建新 .env，不覆盖密码、不启动服务。
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
if [[ -e .env ]]; then
    echo '.env already exists; nothing changed.'
    exit 1
fi
read -r -p 'Website domain (e.g. onlystudy.loc.cc): ' domain
[[ ${#domain} -le 253 && "$domain" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ && "$domain" == *.* ]] || { echo 'Invalid domain'; exit 1; }
read -r -p 'Existing application login username (NOT MySQL username): ' account
[[ "$account" =~ ^[A-Za-z0-9_-]{1,64}$ ]] || { echo 'Invalid username'; exit 1; }
read -r -s -p 'Fallback application password (used ONLY if account is absent): ' account_password
printf '\n'
[[ "$account_password" =~ ^[A-Za-z0-9_!@%+=.,:-]{16,64}$ ]] || { echo 'Use 16-64 characters: letters, digits or _!@%+=.,:-'; exit 1; }
command -v openssl >/dev/null
umask 077
set -o noclobber
{
    printf 'DOMAIN=%s\n' "$domain"
    printf 'MYSQL_ROOT_PASSWORD=%s\n' "$(openssl rand -hex 24)"
    printf 'DB_PASSWORD=%s\n' "$(openssl rand -hex 24)"
    printf 'PRESET_ACCOUNT_USERNAME=%s\n' "$account"
    printf 'PRESET_ACCOUNT_PASSWORD=%s\n' "$account_password"
} > .env
unset account_password
echo '.env created with mode 600. Keep it private. Existing account passwords are preserved on import.'
