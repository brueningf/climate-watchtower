# Frontend (Vite + React)

This folder contains a minimal Vite + React application.

Quick start

```bash
# from repo root
cd frontend
npm install
npm run dev     # starts Vite dev server on default port (usually 5173)
```

Development proxy

The Vite config proxies requests starting with `/api` to `http://localhost:8080` so the frontend can call the Spring Boot API during development.

Build for production

```bash
cd frontend
npm run build
```

If you want the Spring Boot app to serve the built assets, copy `frontend/dist` into `src/main/resources/static` as part of your Gradle build process or add a Gradle task to run the frontend build and include outputs.

