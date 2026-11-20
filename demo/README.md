# Fabflix LAN change demos

每次任务结束后，在 `demo/iterations/<task>/index.html` 建一个**只展示本次改动**的静态页；`demo/index.html` 指向最新一版。不要把完整 Fabflix、数据库、Ollama 或任何密钥放进这个目录。

## Serve

```bash
./scripts/demo/serve.sh
# open http://<LAN-IP>:4173/iterations/<task>/
```

- Binds `0.0.0.0:4173`; default port can be changed with `PORT=8080 ./scripts/demo/serve.sh`.
- On macOS, allow Python to accept incoming connections if the firewall prompts.
- The demo is LAN-only and has no auth; do not put secrets or private data in it.

## Add an iteration

1. Create `demo/iterations/<task>/index.html` (self-contained HTML/CSS/JS is fine).
2. Point `demo/index.html` at it (meta refresh or link).
3. Start/keep the server running; verify with `curl -I http://<LAN-IP>:4173/iterations/<task>/`.
4. Give the URL in the final response. Keep older iteration pages for history.
