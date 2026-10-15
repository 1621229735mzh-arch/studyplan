#!/usr/bin/env bash
# 初次签发在 Nginx 启动前占用 80；续期使用在线 webroot，不停止网站。
set -euo pipefail
cd "$(dirname "${BASH_SOURCE[0]}")/.."
dc=(docker compose -f compose.bundle.yaml)
case "${1:-}" in
  issue)
    domain="$(sed -n 's/^DOMAIN=//p' .env)"
    [[ "$domain" =~ ^[a-z0-9]([a-z0-9.-]*[a-z0-9])?$ && "$domain" == *.* ]] || exit 1
    read -r -p 'Certificate contact email: ' email
    [[ "$email" == *@*.* ]] || exit 1
    echo "Read the current Let's Encrypt subscriber agreement: https://letsencrypt.org/repository/"
    read -r -p 'Accept agreement and request a certificate for this domain? Type yes: ' accepted
    [[ "$accepted" == yes ]] || exit 1
    "${dc[@]}" run --rm --no-deps -p 80:80 certbot certonly --standalone \
      --non-interactive --agree-tos --email "$email" --cert-name study -d "$domain"
    ;;
  renew|check-renewal)
    extra=()
    [[ "$1" != check-renewal ]] || extra=(--dry-run)
    "${dc[@]}" run --rm --no-deps certbot renew --webroot -w /var/www/acme "${extra[@]}"
    "${dc[@]}" exec -T frontend nginx -t
    "${dc[@]}" exec -T frontend nginx -s reload
    ;;
  *) echo 'Usage: bash scripts/bundle-cert.sh issue|renew|check-renewal'; exit 2 ;;
esac
