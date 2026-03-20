<template>
  <div class="shop-list-page">
    <div class="page-header">
      <el-button link @click="goHome">返回首页</el-button>
      <div class="brand-badge">评</div>
      <h2>店铺列表</h2>
    </div>

    <div class="brand-banner">
      <div class="banner-title">生活服务点评平台</div>
      <div class="banner-subtitle">发现真实口碑，找到更适合你的本地服务</div>
    </div>

    <el-card shadow="never" class="filter-card">
      <div class="filter-row">
        <el-select v-model="filters.type" placeholder="全部分类" clearable style="width: 180px" @change="searchShops">
          <el-option label="全部分类" value="" />
          <el-option v-for="item in shopTypes" :key="item" :label="item" :value="item" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="搜索店铺名或地址" clearable @keyup.enter="searchShops">
          <template #append>
            <el-button @click="searchShops">查询</el-button>
          </template>
        </el-input>
        <el-button @click="resetFilters">重置</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="list-card">
      <div v-if="shops.length === 0" class="empty-wrap">
        <el-empty description="暂无店铺数据" />
      </div>
      <div v-else class="shop-list">
        <article v-for="shop in shops" :key="shop.id" class="shop-item" @click="goShopDetail(shop.id)">
          <img :src="shopImage(shop)" alt="shop" class="shop-cover" />
          <div class="shop-main">
            <div class="title-row">
              <h3 class="shop-name">{{ shop.name }}</h3>
              <el-tag size="small" type="info">{{ shop.type || "其他" }}</el-tag>
              <el-tag :type="shop.promotion ? 'danger' : 'info'" size="small">{{ shop.promotion ? "有促销" : "无促销" }}</el-tag>
            </div>
            <p class="shop-meta">{{ shop.address || "地址待完善" }}</p>
            <p class="shop-meta">评分 {{ shop.avgScore || 0 }} · 点评 {{ shop.reviewCount || 0 }}</p>
            <div class="actions">
              <el-button link type="primary" @click.stop="goShopDetail(shop.id)">查看该店铺全部点评</el-button>
            </div>
          </div>
        </article>
      </div>
      <div class="pager">
        <el-pagination
          layout="prev, pager, next, total"
          :current-page="pager.page + 1"
          :page-size="pager.size"
          :total="pager.total"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import http, { baseURL, unwrap } from "../api/http";

const route = useRoute();
const router = useRouter();

const shopTypes = ref([]);
const shops = ref([]);
const filters = reactive({
  type: "",
  keyword: ""
});
const pager = reactive({
  page: 0,
  size: 10,
  total: 0
});

function goHome() {
  router.push("/shops");
}

function goShopDetail(shopId) {
  if (!shopId) return;
  router.push(`/shops/${shopId}`);
}

function shopImage(shop) {
  const src = shop?.image || "";
  if (!src) return "https://via.placeholder.com/180x120?text=Shop";
  if (src.startsWith("http://") || src.startsWith("https://")) return src;
  return `${baseURL}${src}`;
}

async function loadShopTypes() {
  shopTypes.value = await unwrap(http.get("/api/shops/types"));
}

async function loadShops() {
  const res = await http.get("/api/shops", {
    params: {
      page: pager.page,
      size: pager.size,
      type: filters.type || undefined,
      keyword: filters.keyword || undefined
    }
  });
  if (!res.data?.success) {
    throw new Error(res.data?.message || "查询店铺失败");
  }
  shops.value = Array.isArray(res.data.data) ? res.data.data : [];
  const headerTotal = Number(res.headers["x-total-count"]);
  pager.total = Number.isFinite(headerTotal) && headerTotal >= 0 ? headerTotal : shops.value.length;
}

async function searchShops() {
  pager.page = 0;
  syncQueryToRoute();
  await loadShops();
}

async function handlePageChange(pageNumber) {
  pager.page = Math.max(0, pageNumber - 1);
  syncQueryToRoute();
  await loadShops();
}

function resetFilters() {
  filters.type = "";
  filters.keyword = "";
  searchShops();
}

function syncQueryFromRoute() {
  filters.type = typeof route.query.type === "string" ? route.query.type : "";
  filters.keyword = typeof route.query.keyword === "string" ? route.query.keyword : "";
  const pageNum = Number(route.query.page);
  pager.page = Number.isFinite(pageNum) && pageNum > 0 ? pageNum - 1 : 0;
}

function syncQueryToRoute() {
  const query = {};
  if (filters.type) query.type = filters.type;
  if (filters.keyword) query.keyword = filters.keyword;
  if (pager.page > 0) query.page = String(pager.page + 1);
  router.replace({ path: "/shops/list", query });
}

watch(
  () => route.fullPath,
  async () => {
    syncQueryFromRoute();
    await loadShops();
  }
);

onMounted(async () => {
  try {
    await loadShopTypes();
    syncQueryFromRoute();
    await loadShops();
  } catch (e) {
    ElMessage.error(e.message || "加载店铺列表失败");
  }
});
</script>

<style scoped>
.shop-list-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
  background: linear-gradient(180deg, #fff7f0 0%, #f7f8fc 160px);
  min-height: 100vh;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.page-header h2 {
  margin: 0;
}

.brand-badge {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #ff8a2a, #ff5a00);
  font-weight: 700;
}

.brand-banner {
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  background: linear-gradient(120deg, #fff4e7, #ffe2cc);
  border: 1px solid #ffd3ae;
}

.banner-title {
  font-size: 18px;
  color: #de5c00;
  font-weight: 700;
}

.banner-subtitle {
  margin-top: 4px;
  color: #8d5b35;
  font-size: 13px;
}

.filter-card {
  margin-bottom: 12px;
  border: 1px solid #ffe1c3;
}

.list-card {
  border: 1px solid #ffe6d2;
}

.empty-wrap {
  padding: 18px 0;
}

.shop-list {
  display: grid;
  gap: 10px;
}

.shop-item {
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 14px;
  padding: 12px;
  border: 1px solid #f4e2d2;
  border-radius: 10px;
  background: #fff;
  cursor: pointer;
}

.shop-item:hover {
  border-color: #ffc79f;
}

.shop-cover {
  width: 200px;
  height: 126px;
  border-radius: 8px;
  object-fit: cover;
  background: #f2f2f2;
}

.shop-main {
  min-width: 0;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.shop-name {
  margin: 0;
  font-size: 20px;
}

.shop-meta {
  margin: 10px 0 0;
  color: #6f7885;
}

.actions {
  margin-top: 10px;
}

.filter-row {
  display: grid;
  grid-template-columns: 180px 1fr auto;
  gap: 10px;
}

.pager {
  margin-top: 14px;
  display: flex;
  justify-content: flex-end;
}

@media (max-width: 860px) {
  .shop-item {
    grid-template-columns: 1fr;
  }
  .shop-cover {
    width: 100%;
    height: 180px;
  }
}
</style>
