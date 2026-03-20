<template>
  <div class="admin-layout">
    <header class="admin-header">
      <div class="brand">
        <span class="brand-badge">评</span>
        <span class="brand-text">生活服务点评平台 · 管理端</span>
      </div>
      <div class="actions">
        <el-button link @click="goUserHome">返回用户端</el-button>
      </div>
    </header>

    <main class="admin-main">
      <aside class="admin-menu">
        <el-menu :default-active="activeMenu" router>
          <el-menu-item index="/admin/dashboard">数据可视化</el-menu-item>
          <el-menu-item index="/admin/shops/audit">店铺审核</el-menu-item>
          <el-menu-item index="/admin/shops/manage">店铺管理</el-menu-item>
          <el-menu-item index="/admin/products">商品管理</el-menu-item>
        </el-menu>
      </aside>
      <section class="admin-content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();

const activeMenu = computed(() => {
  if (route.path.startsWith("/admin/dashboard")) return "/admin/dashboard";
  if (route.path.startsWith("/admin/products")) return "/admin/products";
  if (route.path.startsWith("/admin/shops/manage")) return "/admin/shops/manage";
  return "/admin/shops/audit";
});

function goUserHome() {
  router.push("/shops");
}
</script>

<style scoped>
.admin-layout {
  min-height: 100vh;
  background: #f5f7fb;
}

.admin-header {
  height: 56px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand-badge {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  color: #fff;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #ff8a2a, #ff5a00);
}

.brand-text {
  font-weight: 600;
}

.admin-main {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 12px;
  padding: 12px;
}

.admin-menu {
  background: #fff;
  border-radius: 10px;
  padding: 10px 0;
}

.admin-content {
  min-width: 0;
}
</style>
