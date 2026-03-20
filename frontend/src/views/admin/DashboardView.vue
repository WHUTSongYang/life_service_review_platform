<template>
  <div class="dashboard-page">
    <el-card shadow="never" class="section">
      <template #header>
        <span>数据筛选</span>
      </template>
      <div class="filters">
        <el-select v-model="filters.shopId" clearable placeholder="全部可管理店铺" style="width: 260px">
          <el-option label="全部可管理店铺" value="" />
          <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
        <el-select v-model="filters.days" style="width: 140px">
          <el-option :value="7" label="近7天" />
          <el-option :value="15" label="近15天" />
          <el-option :value="30" label="近30天" />
        </el-select>
        <el-button type="primary" @click="loadAll">查询</el-button>
      </div>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header>
        <span>日营业额（PAID / 按支付完成日）</span>
      </template>
      <el-empty v-if="!dailyRevenue.length" description="暂无营业额数据" />
      <div v-else class="bar-list">
        <div v-for="item in dailyRevenue" :key="item.date" class="bar-row">
          <div class="bar-date">{{ item.date }}</div>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: revenuePercent(item.revenue) + '%' }"></div>
          </div>
          <div class="bar-value">￥{{ formatMoney(item.revenue) }}</div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section">
      <template #header>
        <span>畅销商品 Top10（按支付订单数）</span>
      </template>
      <el-empty v-if="!bestSellers.length" description="暂无畅销商品数据" />
      <el-table v-else :data="bestSellers" style="width: 100%">
        <el-table-column type="index" label="#" width="60" />
        <el-table-column prop="productName" label="商品" min-width="220" />
        <el-table-column prop="orderCount" label="支付订单数" width="140" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import http, { unwrap } from "../../api/http";

const shops = ref([]);
const dailyRevenue = ref([]);
const bestSellers = ref([]);
const filters = reactive({
  shopId: "",
  days: 7
});

const maxRevenue = computed(() => {
  if (!dailyRevenue.value.length) return 0;
  return Math.max(...dailyRevenue.value.map((item) => Number(item.revenue || 0)));
});

function revenuePercent(value) {
  const max = maxRevenue.value;
  if (max <= 0) return 0;
  return Math.max(2, Math.round((Number(value || 0) / max) * 100));
}

function formatMoney(value) {
  return Number(value || 0).toFixed(2);
}

async function loadShops() {
  shops.value = await unwrap(http.get("/api/shops/manage"));
}

async function loadDailyRevenue() {
  dailyRevenue.value = await unwrap(
    http.get("/api/admin/dashboard/daily-revenue", {
      params: {
        days: filters.days,
        shopId: filters.shopId || undefined
      }
    })
  );
}

async function loadBestSellers() {
  bestSellers.value = await unwrap(
    http.get("/api/admin/dashboard/best-sellers", {
      params: {
        days: filters.days,
        shopId: filters.shopId || undefined,
        limit: 10
      }
    })
  );
}

async function loadAll() {
  await Promise.all([loadDailyRevenue(), loadBestSellers()]);
}

onMounted(async () => {
  await loadShops();
  await loadAll();
});
</script>

<style scoped>
.dashboard-page {
  padding: 4px;
}

.section {
  margin-bottom: 12px;
}

.filters {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.bar-list {
  display: grid;
  gap: 10px;
}

.bar-row {
  display: grid;
  grid-template-columns: 120px 1fr 120px;
  align-items: center;
  gap: 10px;
}

.bar-date {
  color: #606266;
  font-size: 13px;
}

.bar-track {
  height: 12px;
  border-radius: 6px;
  background: #f0f2f5;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  border-radius: 6px;
  background: linear-gradient(90deg, #4f8cff, #72d5ff);
}

.bar-value {
  text-align: right;
  font-variant-numeric: tabular-nums;
  color: #303133;
}
</style>
