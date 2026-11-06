import {defineConfig} from '@playwright/test';
export default defineConfig({testDir:'./e2e',fullyParallel:false,workers:1,timeout:90000,retries:0,use:{actionTimeout:10000,baseURL:process.env.E2E_BASE_URL||'http://127.0.0.1:8086',channel:process.env.E2E_BROWSER||'msedge',headless:true,viewport:{width:1440,height:1000},screenshot:'only-on-failure',trace:'retain-on-failure'},reporter:'list'});
