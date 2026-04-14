import axios from "axios";

/** 开发默认直连后端；生产构建前设置 VITE_API_BASE（见 frontend/.env.production.example） */
const envBase = import.meta.env.VITE_API_BASE;
export const baseURL =
  envBase === undefined || envBase === null ? "http://localhost:8080" : envBase;

const http = axios.create({
  baseURL,
  timeout: 8000
});

http.interceptors.request.use((config) => {
  const token = localStorage.getItem("token");
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function unwrap(promise) {
  return promise.then((res) => {
    if (!res.data.success) {
      throw new Error(res.data.message || "请求失败");
    }
    return res.data.data;
  });
}

export function authHeaders() {
  const token = localStorage.getItem("token");
  return token ? { Authorization: `Bearer ${token}` } : {};
}

export default http;
