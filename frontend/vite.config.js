import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue2';
export default defineConfig({plugins:[vue()],server:{port:5173,proxy:{'/api':'http://127.0.0.1:8080'}},build:{outDir:'dist'}});
