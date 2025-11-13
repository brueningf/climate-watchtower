import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  // Dev server options: proxy API requests to backend to avoid CORS during development
  server: {
    // Vite default port is 5173; explicit here for clarity
    port: 5173,
    proxy: {
      // Proxy any request starting with /api to the Spring Boot backend running on localhost:8080
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false,
        // Remove the /api prefix when forwarding to the backend (optional)
        rewrite: (path) => path.replace(/^\/api/, ''),
      },
    },
  },
  plugins: [react(), tailwindcss()],
})
