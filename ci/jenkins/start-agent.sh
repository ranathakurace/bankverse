#!/usr/bin/env bash
set -euo pipefail
script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
if docker info >/dev/null 2>&1; then docker_cmd=(docker); else docker_cmd=(sudo docker); fi
config_dir="${XDG_CONFIG_HOME:-$HOME/.config}/bankverse-ci"
mkdir -p "$config_dir"
chmod 700 "$config_dir"
export BANKVERSE_AGENT_SECRET_FILE="$config_dir/agent-secret"
if [[ ! -s "$BANKVERSE_AGENT_SECRET_FILE" ]]; then
    read -r -s -p 'Paste the bankverse-ui-agent secret from its Jenkins node page: ' agent_secret
    printf '\n'
    if [[ -z "$agent_secret" ]]; then echo 'No secret supplied; nothing started.'; exit 1; fi
    (umask 077; printf '%s' "$agent_secret" > "$BANKVERSE_AGENT_SECRET_FILE")
    unset agent_secret
fi
chmod 600 "$BANKVERSE_AGENT_SECRET_FILE"
if ! "${docker_cmd[@]}" network inspect bankverse-ci >/dev/null 2>&1; then
    "${docker_cmd[@]}" network create bankverse-ci
fi
if ! "${docker_cmd[@]}" network inspect bankverse-ci --format '{{range .Containers}}{{.Name}}{{println}}{{end}}' | grep -Fxq jenkins; then
    "${docker_cmd[@]}" network connect bankverse-ci jenkins
fi
"${docker_cmd[@]}" compose -f "$script_dir/compose.agent.yaml" build
"${docker_cmd[@]}" compose -f "$script_dir/compose.agent.yaml" up -d
"${docker_cmd[@]}" compose -f "$script_dir/compose.agent.yaml" ps
printf '\nConfirm bankverse-ui-agent is ONLINE in Jenkins before using Jenkinsfile.agent.\n'
