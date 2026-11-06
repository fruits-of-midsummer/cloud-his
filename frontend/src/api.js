import axios from 'axios';
import { Message } from 'element-ui';
export const api=axios.create({baseURL:'/api',timeout:20000,withCredentials:true});
let csrf='';
export function setCsrf(token){csrf=token||'';}
api.interceptors.request.use(config=>{if(config.method!=='get')config.headers['X-CSRF-Token']=csrf;return config;});
api.interceptors.response.use(r=>r.data,error=>{const message=error.response?.data?.message||'连接失败，请检查服务是否运行';Message.error(message);if(error.response?.status===401 && !error.config?.url?.includes('/auth/login'))window.dispatchEvent(new Event('session-expired'));return Promise.reject(error);});
export const money=cents=>new Intl.NumberFormat('zh-CN',{style:'currency',currency:'CNY'}).format(Number(cents||0)/100);
export const date=value=>value?String(value).replace('T',' ').slice(0,19):'—';
export const today=()=>new Intl.DateTimeFormat('en-CA',{timeZone:'Asia/Shanghai',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
export const key=()=>crypto.randomUUID();
export const cents=value=>Math.round(Number(value||0)*100);
export const labels={ADMIN:'系统管理员',CASHIER:'挂号收费',DOCTOR:'门诊医生',CHECK:'检查',LAB:'检验',TREAT:'处置',PHARMACY:'药房药师',DRUG:'药品',REG:'挂号',REGISTERED:'未看诊',IN_PROGRESS:'看诊中',DIAGNOSED:'已确诊',COMPLETED:'已完成',CANCELLED:'已撤销',UNPAID:'未收费',PAID:'已收费',REFUNDED:'已退费',PENDING:'待执行',DISPENSED:'已发药',RETURNED:'已退药',STOCK_IN:'入库',DISPENSE:'发药',RETURN:'退药'};
export function label(v){return labels[v]||v||'—';}
export async function reason(vm,title){try{return (await vm.$prompt('请输入操作原因',title,{inputValidator:v=>!!v?.trim()&&v.length<=500||'请填写500字以内原因',confirmButtonText:'确认',cancelButtonText:'取消'})).value.trim();}catch{return null;}}
export function swallow(promise){promise.catch(()=>{});}
