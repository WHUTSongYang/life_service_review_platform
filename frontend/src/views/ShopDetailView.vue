<template>
  <div class="shop-detail-page">
    <div class="page-header">
      <el-button link @click="goBack">返回店铺列表</el-button>
      <h2>店铺详情</h2>
    </div>

    <el-card v-if="shop" class="section-card" shadow="never">
      <template #header>
        <div class="card-title">店铺信息</div>
      </template>
      <div class="shop-info">
        <div><strong>{{ shop.name }}</strong></div>
        <div>{{ shop.type }} · {{ shop.address || "地址待完善" }}</div>
        <div>评分 {{ shop.avgScore || 0 }} · 点评 {{ shop.reviewCount || 0 }}</div>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-title">店铺商品（抢购）</div>
      </template>
      <div v-if="products.length === 0" class="empty-text">暂无上架商品</div>
      <div v-else class="product-grid">
        <article v-for="item in products" :key="item.id" class="product-card">
          <el-image v-if="item.image" :src="resolveImage(item.image)" fit="cover" class="product-image" />
          <div class="product-name">{{ item.name }}</div>
          <div class="product-price">￥{{ item.price }}</div>
          <div class="product-stock">库存：{{ item.stock }}</div>
          <el-button type="danger" size="small" :disabled="item.stock <= 0" @click="purchase(item.id)">
            {{ item.stock > 0 ? "立即抢购" : "已抢光" }}
          </el-button>
        </article>
      </div>
    </el-card>

    <el-card class="section-card" shadow="never">
      <template #header>
        <div class="card-title">全部点评</div>
      </template>
      <div v-if="reviews.length === 0" class="empty-text">暂无点评</div>
      <ul v-else class="review-list">
        <li v-for="item in reviews" :key="item.id" class="review-item">
          <div class="review-top">
            <el-tag size="small" type="warning">{{ item.score || 0 }}分</el-tag>
            <span class="review-meta">用户 {{ item.userId }} · {{ formatTime(item.createdAt) }}</span>
          </div>
          <p class="review-content">{{ item.content }}</p>
          <div v-if="parseImageList(item.images).length" class="review-images">
            <el-image
              v-for="img in parseImageList(item.images)"
              :key="img"
              :src="resolveImage(img)"
              class="review-image"
              fit="cover"
            />
          </div>
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import http, { baseURL, unwrap } from "../api/http";

const route = useRoute();
const router = useRouter();
const shopId = Number(route.params.id);

const shop = ref(null);
const reviews = ref([]);
const products = ref([]);

function goBack() {
  router.push("/shops/list");
}

async function loadShopDetail() {
  shop.value = await unwrap(http.get(`/api/shops/${shopId}`));
}

async function loadShopReviews() {
  reviews.value = await unwrap(http.get(`/api/shops/${shopId}/reviews`));
}

async function loadProducts() {
  products.value = await unwrap(http.get(`/api/shops/${shopId}/products`));
}

function alertPurchaseFailure(msg) {
  const m = String(msg || "");
  if (m.includes("仅限购买一次") || m.includes("已购买")) {
    return ElMessageBox.alert("您已购买该商品，无法继续", "抢购失败", {
      type: "warning",
      confirmButtonText: "知道了"
    });
  }
  if (m.includes("已抢光") || m.includes("库存")) {
    return ElMessageBox.alert("库存不足", "抢购失败", {
      type: "warning",
      confirmButtonText: "知道了"
    });
  }
  return Promise.resolve();
}

async function purchase(productId) {
  if (!localStorage.getItem("token")) {
    ElMessage.warning("请先登录后再抢购");
    router.push({ path: "/login", query: { redirect: route.fullPath } });
    return;
  }
  try {
    const res = await http.post(`/api/products/${productId}/purchase`);
    if (!res.data.success) {
      throw new Error(res.data.message || "请求失败");
    }
    if (res.status === 202) {
      const requestId = res.data.data?.requestId;
      if (!requestId) {
        throw new Error("受理失败");
      }
      const deadline = Date.now() + 30000;
      while (Date.now() < deadline) {
        await new Promise((r) => setTimeout(r, 400));
        const poll = await http.get(`/api/products/purchase-requests/${requestId}`);
        if (!poll.data.success) {
          throw new Error(poll.data.message || "查询失败");
        }
        const q = poll.data.data;
        if (q.status === "SUCCESS") {
          ElMessage.success("抢购成功，订单已创建");
          await loadProducts();
          return;
        }
        if (q.status === "FAILED") {
          await alertPurchaseFailure(q.message);
          const m = String(q.message || "");
          if (!m.includes("仅限购买一次") && !m.includes("已购买") && !m.includes("已抢光") && !m.includes("库存")) {
            ElMessage.error(m || "抢购失败，请稍后重试");
          }
          return;
        }
      }
      ElMessage.warning("处理时间较长，请稍后在「我的订单」中查看");
      return;
    }
    ElMessage.success("抢购成功，订单已创建");
    await loadProducts();
  } catch (err) {
    const msg = String(err?.message || "");
    await alertPurchaseFailure(msg);
    if (!msg.includes("仅限购买一次") && !msg.includes("已购买") && !msg.includes("已抢光") && !msg.includes("库存")) {
      ElMessage.error(msg || "抢购失败，请稍后重试");
    }
  }
}

function parseImageList(images) {
  if (!images) return [];
  return String(images)
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}

function resolveImage(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${baseURL}${path}`;
}

function formatTime(value) {
  if (!value) return "";
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(async () => {
  if (!Number.isFinite(shopId) || shopId <= 0) {
    ElMessage.error("店铺ID无效");
    router.push("/shops/list");
    return;
  }
  await Promise.all([loadShopDetail(), loadShopReviews(), loadProducts()]);
});
</script>

<style scoped>
.shop-detail-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
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

.section-card {
  margin-bottom: 12px;
}

.card-title {
  font-weight: 600;
}

.shop-info {
  display: grid;
  gap: 6px;
}

.empty-text {
  color: #909399;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.product-card {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 10px;
  display: grid;
  gap: 6px;
}

.product-image {
  width: 100%;
  height: 120px;
  border-radius: 8px;
}

.product-name {
  font-weight: 600;
}

.product-price {
  color: #f56c6c;
}

.product-stock {
  color: #909399;
  font-size: 12px;
}

.review-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.review-item {
  border-bottom: 1px solid #f0f0f0;
  padding: 10px 0;
}

.review-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.review-meta {
  color: #909399;
  font-size: 12px;
}

.review-content {
  margin: 0 0 8px;
}

.review-images {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.review-image {
  width: 88px;
  height: 88px;
  border-radius: 6px;
}

@media (max-width: 992px) {
  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
