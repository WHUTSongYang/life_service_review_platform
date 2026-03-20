<template>
  <div class="profile-page">
    <div class="page-header">
      <el-button link @click="goHome">返回首页</el-button>
      <h2>个人中心</h2>
    </div>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span>个人信息</span>
      </template>
      <el-form :model="profileForm" label-width="90px" class="profile-form">
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="profileForm.phone" placeholder="可留空" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" placeholder="可留空" />
        </el-form-item>
      </el-form>
      <el-button type="primary" @click="saveProfile">保存信息</el-button>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span>签到</span>
      </template>
      <div class="sign-card">
        <div class="sign-stats">
          <div class="stat-item">
            <div class="stat-value">{{ signInStatus.monthSignedDays }}</div>
            <div class="stat-label">本月已签到</div>
          </div>
          <div class="stat-item">
            <div class="stat-value">{{ signInStatus.continuousSignedDays }}</div>
            <div class="stat-label">连续签到</div>
          </div>
        </div>
        <div class="sign-action">
          <el-tag :type="signInStatus.signedToday ? 'success' : 'warning'">
            {{ signInStatus.signedToday ? "今日已签到" : "今日未签到" }}
          </el-tag>
          <el-button type="primary" :disabled="signInStatus.signedToday" @click="signInToday">
            {{ signInStatus.signedToday ? "已签到" : "立即签到" }}
          </el-button>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span>我的订单</span>
      </template>
      <el-empty v-if="!myOrders.length" description="暂无订单" />
      <el-table v-else :data="myOrders" style="width: 100%">
        <el-table-column prop="shopName" label="店铺" min-width="140" />
        <el-table-column prop="productName" label="商品" min-width="140" />
        <el-table-column prop="amount" label="金额" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-space>
              <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="payOrder(row.id)">支付</el-button>
              <el-button v-if="row.status === 'PENDING'" size="small" @click="cancelOrder(row.id)">取消</el-button>
            </el-space>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-card shadow="never" class="section-card">
      <template #header>
        <span>我的点评</span>
      </template>
      <el-empty v-if="!myReviews.length" description="你还没有发布点评" />
      <div v-else class="review-list">
        <article v-for="item in myReviews" :key="item.id" class="review-item">
          <div class="review-main">
            <h4>{{ item.shopName }}</h4>
            <p class="review-meta">{{ item.shopType || "其他" }} · {{ item.shopAddress || "地址待完善" }}</p>
            <p class="review-content">{{ item.content || "暂无内容" }}</p>
            <div class="review-images" v-if="reviewCover(item)">
              <el-image :src="reviewCover(item)" fit="cover" />
            </div>
          </div>
          <div class="review-side">
            <el-tag type="warning">{{ item.score || 0 }}分</el-tag>
            <el-button link type="primary" @click="openDetail(item.id)">查看详情</el-button>
            <el-button link type="danger" @click="removeReview(item.id)">删除</el-button>
          </div>
        </article>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { useRouter } from "vue-router";
import http, { baseURL, unwrap } from "../api/http";

const router = useRouter();
const profileForm = reactive({
  nickname: "",
  phone: "",
  email: ""
});
const myReviews = ref([]);
const myOrders = ref([]);
const signInStatus = reactive({
  signedToday: false,
  monthSignedDays: 0,
  continuousSignedDays: 0,
  date: ""
});

function goHome() {
  router.push("/shops");
}

function parseImageList(images) {
  if (!images) return [];
  return images
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}

function resolveImage(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${baseURL}${path}`;
}

function reviewCover(item) {
  const first = parseImageList(item.images)[0];
  return first ? resolveImage(first) : "";
}

async function loadProfile() {
  const data = await unwrap(http.get("/api/users/me"));
  profileForm.nickname = data.nickname || "";
  profileForm.phone = data.phone || "";
  profileForm.email = data.email || "";
}

async function saveProfile() {
  const data = await unwrap(
    http.put("/api/users/me", {
      nickname: profileForm.nickname,
      phone: profileForm.phone,
      email: profileForm.email
    })
  );
  localStorage.setItem("nickname", data.nickname || "");
  window.dispatchEvent(new Event("auth-change"));
  ElMessage.success("个人信息已更新");
}

async function loadMyReviews() {
  myReviews.value = await unwrap(http.get("/api/users/me/reviews"));
}

async function loadMyOrders() {
  myOrders.value = await unwrap(http.get("/api/orders/mine"));
}

async function loadSignInStatus() {
  const data = await unwrap(http.get("/api/users/me/sign-in/status"));
  signInStatus.signedToday = !!data.signedToday;
  signInStatus.monthSignedDays = Number(data.monthSignedDays || 0);
  signInStatus.continuousSignedDays = Number(data.continuousSignedDays || 0);
  signInStatus.date = data.date || "";
}

function openDetail(reviewId) {
  router.push(`/reviews/${reviewId}`);
}

async function removeReview(reviewId) {
  try {
    await ElMessageBox.confirm("删除后不可恢复，确认删除这条点评吗？", "删除点评", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
  } catch (_e) {
    return;
  }
  await unwrap(http.delete(`/api/reviews/${reviewId}`));
  ElMessage.success("点评已删除");
  await loadMyReviews();
}

async function payOrder(orderId) {
  await unwrap(http.post(`/api/orders/${orderId}/pay`));
  ElMessage.success("支付成功");
  await loadMyOrders();
}

async function cancelOrder(orderId) {
  await unwrap(http.post(`/api/orders/${orderId}/cancel`));
  ElMessage.success("订单已取消");
  await loadMyOrders();
}

async function signInToday() {
  const data = await unwrap(http.post("/api/users/me/sign-in"));
  signInStatus.signedToday = !!data.signedToday;
  signInStatus.monthSignedDays = Number(data.monthSignedDays || 0);
  signInStatus.continuousSignedDays = Number(data.continuousSignedDays || 0);
  signInStatus.date = data.date || "";
  ElMessage.success("签到成功");
}

function statusText(status) {
  if (status === "PAID") return "已支付";
  if (status === "CANCELLED") return "已取消";
  return "待支付";
}

function statusTagType(status) {
  if (status === "PAID") return "success";
  if (status === "CANCELLED") return "info";
  return "warning";
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadMyReviews(), loadMyOrders(), loadSignInStatus()]);
});
</script>

<style scoped>
.profile-page {
  max-width: 1100px;
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

.profile-form {
  max-width: 560px;
}

.sign-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.sign-stats {
  display: flex;
  gap: 22px;
}

.stat-item {
  min-width: 120px;
}

.stat-value {
  font-size: 28px;
  line-height: 1;
  color: #ff6a00;
  font-weight: 700;
}

.stat-label {
  margin-top: 6px;
  color: #8d95a3;
  font-size: 13px;
}

.sign-action {
  display: flex;
  align-items: center;
  gap: 10px;
}

.review-list {
  display: grid;
  gap: 10px;
}

.review-item {
  border: 1px solid #f0f0f0;
  border-radius: 10px;
  padding: 12px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.review-main h4 {
  margin: 0 0 6px;
}

.review-meta {
  color: #8d95a3;
  margin: 0 0 8px;
  font-size: 13px;
}

.review-content {
  margin: 0;
  color: #333;
}

.review-images {
  margin-top: 8px;
}

.review-images :deep(.el-image) {
  width: 84px;
  height: 84px;
  border-radius: 8px;
}

.review-side {
  min-width: 90px;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}
</style>
