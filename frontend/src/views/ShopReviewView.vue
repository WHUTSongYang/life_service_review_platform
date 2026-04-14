<template>
  <div class="consumer-page">
    <header class="topbar-wrap">
      <div class="topbar">
        <div class="topbar-left">
          <div class="brand-mark">评</div>
          <div class="logo">生活服务点评平台</div>
          <el-select v-model="currentCity" size="large" class="city-select">
            <el-option v-for="city in cityOptions" :key="city" :label="city" :value="city" />
          </el-select>
        </div>
        <div class="search-area">
          <el-input
            v-model="filters.keyword"
            size="large"
            placeholder="搜索商户名、地点或点评"
            @keyup.enter="searchFeed"
          >
            <template #append>
              <el-button class="search-btn" @click="searchFeed">搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="topbar-right">
          <el-button v-if="isLogin" text @click="goAdminPortal">管理端</el-button>
          <el-button v-if="isLogin && isUserPrincipal" text @click="goProfile">个人中心</el-button>
          <el-button text>帮助中心</el-button>
          <el-button v-if="!isLogin" text @click="goLogin">登录/注册</el-button>
          <template v-else>
            <span class="nickname">你好，{{ currentNickname }}</span>
            <el-button type="primary" class="write-btn" @click="openWriteDialog">写点评</el-button>
            <el-button text @click="logout">退出</el-button>
          </template>
        </div>
      </div>
    </header>

    <main class="main-wrapper">
      <section class="category-grid">
        <button
          v-for="item in categoryItems"
          :key="item.type"
          class="category-item"
          @click="pickType(item.type)"
        >
          <span class="emoji">{{ item.icon }}</span>
          <span class="label">{{ item.label }}</span>
        </button>
      </section>

      <section class="content-layout">
        <div class="feed-column">
          <div class="feed-header">
            <h3>{{ channelTitle }}</h3>
            <div class="feed-tools">
              <el-button size="small" @click="resetFilters">重置</el-button>
              <el-button size="small" type="primary" @click="refreshCurrentChannel">刷新</el-button>
            </div>
          </div>

          <div class="feed-waterfall">
            <article v-for="item in currentFeed" :key="item.id" class="review-card" @click="openReviewDetail(item.id)">
              <img v-if="reviewCover(item)" :src="reviewCover(item)" class="cover-image" alt="cover" />
              <div v-else class="cover-empty">{{ item.shopName }}</div>
              <div class="card-body">
                <div class="title-row">
                  <h4 class="shop-name">{{ item.shopName }}</h4>
                  <el-tag size="small" type="warning">{{ item.score || 0 }}分</el-tag>
                </div>
                <p class="content">{{ item.content || "附近优质商户，欢迎打卡体验。" }}</p>
                <p class="meta">
                  {{ item.shopType || item.type || "其他" }} · {{ item.shopAddress || item.address || "地址待完善" }}
                </p>
                <div class="footer">
                  <span class="user">{{ item.userNickname || "附近推荐" }}</span>
                  <el-button
                    text
                    @click.stop
                    @click="toggleReviewLike(item.id)"
                  >
                    ❤ {{ item.likeCount || 0 }}
                  </el-button>
                </div>
              </div>
            </article>
          </div>
        </div>

        <aside class="side-column">
          <el-card class="nearby-card" shadow="never">
            <template #header>
              <div class="side-title">
                <span>附近商户</span>
                <el-button link @click="fillCurrentLocation">定位我</el-button>
              </div>
            </template>
            <div class="nearby-form">
              <el-input-number v-model="nearbyQuery.longitude" :precision="6" :step="0.000001" />
              <el-input-number v-model="nearbyQuery.latitude" :precision="6" :step="0.000001" />
              <el-input-number v-model="nearbyQuery.radiusKm" :min="0.1" :step="0.1" />
              <el-button type="primary" @click="loadNearbyShops">查附近</el-button>
            </div>
            <ul class="shop-list">
              <li v-for="shop in nearbyShops" :key="shop.id" class="shop-item">
                <div>
                  <div class="name">{{ shop.name }}</div>
                  <div class="desc">{{ shop.type }} · {{ shop.address || "地址待完善" }}</div>
                </div>
                <div class="distance">{{ shop.distanceKm }}km</div>
              </li>
            </ul>
          </el-card>
        </aside>
      </section>
    </main>

    <el-dialog v-model="writeDialogVisible" title="写点评" width="640px" @close="handleWriteDialogClose">
      <el-form :model="reviewForm" label-width="78px">
        <el-form-item label="店铺">
          <el-select v-model="reviewForm.shopId" placeholder="请选择店铺" filterable style="width: 100%">
            <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" />
          <div class="ai-write-panel">
            <div class="ai-write-header">
              <span class="ai-write-title">AI帮写</span>
              <el-space>
                <el-button
                  size="small"
                  type="primary"
                  plain
                  :loading="aiWriteLoading"
                  @click="generateAiReviewText"
                >
                  生成文案
                </el-button>
                <el-button
                  size="small"
                  :disabled="!aiDraftText"
                  @click="applyAiDraft('replace')"
                >
                  替换内容
                </el-button>
                <el-button
                  size="small"
                  :disabled="!aiDraftText"
                  @click="applyAiDraft('append')"
                >
                  追加内容
                </el-button>
              </el-space>
            </div>
            <div class="ai-write-tip">
              {{ aiWriteTipText }}
            </div>
            <div class="ai-write-draft" :class="{ 'is-empty': !aiDraftText }">
              {{ aiDraftText || "点击“生成文案”，AI会基于店铺类型给你生成可直接发布的点评草稿。" }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleUploadSuccess"
          >
            <el-button>上传图片</el-button>
          </el-upload>
          <el-space wrap style="margin-top: 8px">
            <el-image
              v-for="img in reviewImages"
              :key="img"
              :src="resolveImage(img)"
              style="width: 64px; height: 64px; border-radius: 8px"
              fit="cover"
            />
          </el-space>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="writeDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitReview">发布点评</el-button>
      </template>
    </el-dialog>

    <div class="ai-assistant">
      <el-button class="ai-trigger" type="primary" @click="aiVisible = !aiVisible">
        {{ aiVisible ? "收起AI客服" : "AI客服" }}
      </el-button>
      <el-card v-if="aiVisible" class="ai-panel" shadow="always">
        <template #header>
          <div class="ai-title">AI 客服</div>
        </template>
        <div ref="aiBodyRef" class="ai-body">
          <div v-for="(msg, idx) in aiMessages" :key="idx" :class="['ai-msg', msg.role === 'user' ? 'is-user' : 'is-assistant']">
            {{ msg.content }}
          </div>
        </div>
        <div class="ai-input-row">
          <el-input
            v-model="aiInput"
            type="textarea"
            :rows="2"
            resize="none"
            placeholder="问我：怎么选店、怎么下单、怎么写点评..."
            @keyup.enter.exact.prevent="sendAiMessage"
          />
          <el-button :loading="aiLoading" type="primary" @click="sendAiMessage">发送</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import http, { authHeaders, baseURL, unwrap } from "../api/http";
import { useRoute, useRouter } from "vue-router";

const cityOptions = ["上海", "北京", "杭州", "深圳", "广州"];
const defaultShopTypes = ["美食", "酒店", "娱乐", "按摩", "电影院", "足疗", "丽人", "运动"];
const categoryIconMap = {
  美食: "🍜",
  景点: "🏯",
  酒店: "🏨",
  休闲: "🍸",
  电影院: "🎬",
  丽人: "💇",
  运动: "🏋️",
  按摩: "💆"
};

const router = useRouter();
const route = useRoute();
const authState = reactive({
  token: "",
  userId: "",
  nickname: "",
  principalType: "USER"
});
const currentCity = ref("上海");
const isLogin = computed(() => !!authState.token);
const isUserPrincipal = computed(() => authState.principalType !== "ADMIN");
const currentNickname = computed(() => authState.nickname || "用户");
const shops = ref([]);
const shopTypes = ref([]);
const hotReviews = ref([]);
const nearbyShops = ref([]);
const writeDialogVisible = ref(false);
const reviewImages = ref([]);
const uploadAction = `${baseURL}/api/files/upload`;
const uploadHeaders = authHeaders();
const aiVisible = ref(false);
const aiInput = ref("");
const aiLoading = ref(false);
const aiBodyRef = ref(null);
const aiMessages = ref([
  { role: "assistant", content: "你好，我是AI客服。你可以问我如何选店、下单、写点评或平台功能。" }
]);

const filters = reactive({
  keyword: ""
});
const hotQuery = reactive({
  page: 0,
  size: 24
});
const nearbyQuery = reactive({
  longitude: null,
  latitude: null,
  radiusKm: 5,
  limit: 8
});
const reviewForm = reactive({
  shopId: null,
  score: 5,
  content: "",
  images: ""
});
const aiWriteLoading = ref(false);
const aiDraftText = ref("");

const categoryItems = computed(() => [
  { type: "", label: "全部", icon: "✨" },
  ...shopTypes.value.map((type) => ({
    type,
    label: type,
    icon: categoryIconMap[type] || "🧭"
  }))
]);

const channelTitle = computed(() => "推荐点评");

const currentFeed = computed(() => hotReviews.value);
const selectedReviewShop = computed(() => shops.value.find((item) => item.id === reviewForm.shopId) || null);
const aiWriteTipText = computed(() => {
  if (!reviewForm.shopId) {
    return "请先选择店铺，再生成更贴合场景的点评文案。";
  }
  if (aiWriteLoading.value) {
    return "AI 正在生成中，请稍候...";
  }
  return "可反复生成，选择喜欢的草稿后再发布。";
});

async function loadShops() {
  shops.value = await unwrap(http.get("/api/shops"));
}

async function loadShopTypes() {
  try {
    const types = await unwrap(http.get("/api/shops/types"));
    if (Array.isArray(types) && types.length > 0) {
      shopTypes.value = types;
      return;
    }
    shopTypes.value = [...defaultShopTypes];
  } catch (_e) {
    shopTypes.value = [...defaultShopTypes];
  }
}

async function loadHotReviews() {
  hotReviews.value = await unwrap(
    http.get("/api/reviews/hot", {
      params: {
        page: hotQuery.page,
        size: hotQuery.size,
        keyword: filters.keyword || undefined
      }
    })
  );
}

async function loadNearbyShops() {
  if (nearbyQuery.longitude == null || nearbyQuery.latitude == null) {
    ElMessage.warning("请先定位或输入经纬度");
    return;
  }
  nearbyShops.value = await unwrap(
    http.get("/api/shops/nearby", {
      params: {
        longitude: nearbyQuery.longitude,
        latitude: nearbyQuery.latitude,
        radiusKm: nearbyQuery.radiusKm,
        limit: nearbyQuery.limit
      }
    })
  );
}

function pickType(type) {
  if (!type) {
    router.push("/shops/list");
    return;
  }
  router.push({ path: "/shops/list", query: { type } });
}

function searchFeed() {
  hotQuery.page = 0;
  refreshCurrentChannel();
}

function resetFilters() {
  filters.keyword = "";
  hotQuery.page = 0;
  refreshCurrentChannel();
}

async function refreshCurrentChannel() {
  await loadHotReviews();
}

function openWriteDialog() {
  if (!isLogin.value) {
    ElMessage.warning("请先登录后再发布点评");
    goLogin();
    return;
  }
  writeDialogVisible.value = true;
}

async function submitReview() {
  if (!reviewForm.shopId) {
    ElMessage.warning("请选择店铺");
    return;
  }
  reviewForm.images = reviewImages.value.join(",");
  await unwrap(
    http.post(`/api/shops/${reviewForm.shopId}/reviews`, {
      score: reviewForm.score,
      content: reviewForm.content,
      images: reviewForm.images
    })
  );
  ElMessage.success("点评发布成功");
  writeDialogVisible.value = false;
  reviewForm.shopId = null;
  reviewForm.score = 5;
  reviewForm.content = "";
  reviewForm.images = "";
  reviewImages.value = [];
  aiDraftText.value = "";
  await refreshCurrentChannel();
}

async function toggleReviewLike(reviewId) {
  if (!isLogin.value) {
    ElMessage.warning("登录后可点赞");
    goLogin();
    return;
  }
  await unwrap(http.post(`/api/reviews/${reviewId}/like`));
  await refreshCurrentChannel();
}

async function logout() {
  try {
    await unwrap(http.post("/api/auth/logout"));
  } catch (_e) {
    // Ignore backend logout failure and clear local token anyway.
  }
  localStorage.removeItem("token");
  localStorage.removeItem("userId");
  localStorage.removeItem("adminId");
  localStorage.removeItem("nickname");
  localStorage.removeItem("principalType");
  localStorage.removeItem("isSuperAdmin");
  syncAuthState();
  window.dispatchEvent(new Event("auth-change"));
  ElMessage.success("已退出登录");
  if (route.path !== "/shops") {
    router.push("/shops");
  }
}

function goLogin() {
  router.push({ path: "/login", query: { redirect: route.fullPath || "/shops" } });
}

function goProfile() {
  router.push("/me");
}

async function goAdminPortal() {
  if (!isLogin.value) {
    ElMessage.warning("请先登录后进入管理端");
    goLogin();
    return;
  }
  router.push("/admin/shops/audit");
}

async function ensureShopTypesLoaded() {
  if (shopTypes.value.length > 0) return;
  await loadShopTypes();
}

async function retryLoadShopTypes() {
  await loadShopTypes();
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

function openReviewDetail(reviewId) {
  if (!reviewId) return;
  router.push(`/reviews/${reviewId}`);
}

function handleUploadSuccess(response) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  reviewImages.value.push(response.data.url);
}

function fillCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning("浏览器不支持定位");
    return;
  }
  navigator.geolocation.getCurrentPosition(
    async (position) => {
      nearbyQuery.longitude = Number(position.coords.longitude.toFixed(6));
      nearbyQuery.latitude = Number(position.coords.latitude.toFixed(6));
      await loadNearbyShops();
    },
    () => {
      ElMessage.error("定位失败，请手动输入经纬度");
    }
  );
}

function syncAuthState() {
  authState.token = localStorage.getItem("token") || "";
  authState.userId = localStorage.getItem("userId") || "";
  authState.nickname = localStorage.getItem("nickname") || "";
  authState.principalType = localStorage.getItem("principalType") || "USER";
}

function handleAuthChanged() {
  const previousToken = authState.token;
  syncAuthState();
  if (authState.token && authState.token !== previousToken) {
    loadShopTypes();
  } else {
    ensureShopTypesLoaded();
  }
}

function handleWriteDialogClose() {
  reviewForm.shopId = null;
  reviewForm.score = 5;
  reviewForm.content = "";
  reviewForm.images = "";
  reviewImages.value = [];
  aiDraftText.value = "";
}

async function generateAiReviewText() {
  const shop = selectedReviewShop.value;
  if (!shop) {
    ElMessage.warning("请先选择店铺后再使用AI帮写");
    return;
  }
  aiWriteLoading.value = true;
  try {
    const data = await unwrap(
      http.post("/api/ai/generate-review", {
        shopName: shop.name,
        shopType: shop.type || "生活服务"
      })
    );
    aiDraftText.value = (data?.content || "").trim();
    if (!aiDraftText.value) {
      ElMessage.warning("AI 暂未生成内容，请重试");
      return;
    }
    ElMessage.success("AI 文案已生成");
  } finally {
    aiWriteLoading.value = false;
  }
}

function applyAiDraft(mode) {
  if (!aiDraftText.value) {
    ElMessage.warning("请先生成AI文案");
    return;
  }
  if (mode === "append") {
    reviewForm.content = reviewForm.content
      ? `${reviewForm.content}\n${aiDraftText.value}`
      : aiDraftText.value;
    ElMessage.success("已追加到点评内容");
    return;
  }
  reviewForm.content = aiDraftText.value;
  ElMessage.success("已替换点评内容");
}

function toAiHistory() {
  return aiMessages.value
    .slice(-12)
    .filter((item) => item.content && item.content.trim())
    .map((item) => ({
      role: item.role,
      content: item.content
    }));
}

async function scrollAiToBottom() {
  await nextTick();
  if (aiBodyRef.value) {
    aiBodyRef.value.scrollTop = aiBodyRef.value.scrollHeight;
  }
}

/** 可选：供 AI 客服「附近店铺」意图使用；用户拒绝或超时时返回空对象 */
function getOptionalCoords() {
  if (typeof navigator === "undefined" || !navigator.geolocation) {
    return Promise.resolve({});
  }
  return new Promise((resolve) => {
    navigator.geolocation.getCurrentPosition(
      (pos) =>
        resolve({
          latitude: pos.coords.latitude,
          longitude: pos.coords.longitude
        }),
      () => resolve({}),
      { timeout: 4000, maximumAge: 120000 }
    );
  });
}

async function sendAiMessage() {
  const question = aiInput.value.trim();
  if (!question || aiLoading.value) {
    return;
  }
  aiMessages.value.push({ role: "user", content: question });
  aiInput.value = "";
  aiLoading.value = true;
  const assistantMsg = { role: "assistant", content: "" };
  aiMessages.value.push(assistantMsg);
  await scrollAiToBottom();
  try {
    const coords = await getOptionalCoords();
    const token = localStorage.getItem("token");
    const response = await fetch(`${baseURL}/api/ai/chat/stream`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({
        message: question,
        history: toAiHistory(),
        ...coords
      })
    });
    if (!response.ok || !response.body) {
      throw new Error(`AI服务不可用（HTTP ${response.status}）`);
    }
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");
    let buffer = "";
    while (true) {
      const { value, done } = await reader.read();
      if (done) {
        break;
      }
      buffer += decoder.decode(value, { stream: true });
      const events = buffer.split("\n\n");
      buffer = events.pop() || "";
      for (const eventBlock of events) {
        const lines = eventBlock
          .split("\n")
          .map((line) => line.trim())
          .filter(Boolean);
        let dataText = "";
        for (const line of lines) {
          if (line.startsWith("data:")) {
            dataText += line.slice(5).trim();
          }
        }
        if (!dataText || dataText === "[DONE]") {
          continue;
        }
        assistantMsg.content += dataText;
      }
      await scrollAiToBottom();
    }
    if (!assistantMsg.content) {
      assistantMsg.content = "暂时没有拿到回复，请稍后重试。";
    }
  } catch (e) {
    assistantMsg.content = e?.message || "AI服务异常，请稍后重试。";
  } finally {
    aiLoading.value = false;
    await scrollAiToBottom();
  }
}

onMounted(async () => {
  syncAuthState();
  window.addEventListener("storage", handleAuthChanged);
  window.addEventListener("auth-change", handleAuthChanged);
  await loadShops();
  await loadShopTypes();
  await loadHotReviews();
});

onUnmounted(() => {
  window.removeEventListener("storage", handleAuthChanged);
  window.removeEventListener("auth-change", handleAuthChanged);
});
</script>

<style scoped>
.consumer-page {
  min-height: 100vh;
  background: linear-gradient(180deg, #fff7f0 0%, #f7f8fc 120px, #f7f8fc 100%);
}

.topbar-wrap {
  background: #ffffff;
  border-bottom: 1px solid #f1e8e0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.topbar {
  max-width: 1260px;
  margin: 0 auto;
  height: 74px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 20px;
}

.topbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.logo {
  font-size: 24px;
  font-weight: 700;
  color: #ff6a00;
}

.brand-mark {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  background: linear-gradient(135deg, #ff8a2a, #ff5a00);
  font-weight: 700;
  box-shadow: 0 6px 14px rgba(255, 106, 0, 0.24);
}

.city-select {
  width: 116px;
}

.search-area {
  flex: 1;
  max-width: 620px;
}

.search-btn {
  color: #fff;
  background: #ff7a1a;
  border-color: #ff7a1a;
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 2px;
}

.nickname {
  color: #7b6b58;
  font-size: 14px;
  margin: 0 4px 0 8px;
}

.write-btn {
  border-radius: 18px;
  background: #ff7a1a;
  border-color: #ff7a1a;
}

.main-wrapper {
  max-width: 1260px;
  margin: 0 auto;
  padding: 16px 20px 28px;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.category-item {
  border: none;
  background: #fff;
  border-radius: 12px;
  height: 68px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.category-item.active,
.category-item:hover {
  box-shadow: 0 8px 18px rgba(255, 122, 26, 0.14);
  transform: translateY(-1px);
}

.emoji {
  font-size: 20px;
}

.label {
  font-size: 15px;
  color: #1f2430;
}

.content-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
}

.feed-column {
  background: #fff;
  border-radius: 14px;
  padding: 14px;
}

.feed-header {
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 14px;
  margin-bottom: 14px;
}

.feed-header h3 {
  margin: 0;
  font-size: 20px;
}

.channel-tabs {
  display: flex;
  gap: 8px;
}

.channel-tab {
  border: 1px solid #f1e3d7;
  background: #fff;
  color: #7a6b5d;
  border-radius: 999px;
  padding: 6px 14px;
  cursor: pointer;
}

.channel-tab.active {
  border-color: #ff7a1a;
  color: #ff7a1a;
  background: #fff3e7;
}

.feed-waterfall {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.review-card {
  border-radius: 12px;
  background: #fff;
  border: 1px solid #efe6dc;
  display: flex;
  flex-direction: column;
  padding: 8px;
  min-height: 268px;
}

.cover-image {
  display: block;
  width: 100%;
  height: 120px;
  border-radius: 8px;
  object-fit: cover;
}

.cover-empty {
  width: 100%;
  height: 120px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(145deg, #ffe8d4, #fff4e8);
  color: #c18148;
  font-weight: 600;
}

.card-body {
  display: flex;
  flex-direction: column;
  min-width: 0;
  padding: 8px 4px 2px;
}

.title-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.shop-name {
  margin: 0;
  font-size: 14px;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content {
  margin: 6px 0 4px;
  color: #333;
  line-height: 1.45;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 40px;
  font-size: 13px;
}

.meta {
  color: #8d95a3;
  font-size: 11px;
  margin: 0 0 6px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: auto;
  border-top: 1px dashed #f1e6d8;
  padding-top: 6px;
}

.user {
  color: #666;
  font-size: 12px;
}

.nearby-card {
  border-radius: 14px;
}

.side-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.nearby-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  margin-bottom: 12px;
}

.nearby-form :deep(.el-input-number) {
  width: 100%;
}

.shop-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.shop-item {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 0;
  border-bottom: 1px solid #f2f2f2;
}

.shop-item:last-child {
  border-bottom: none;
}

.shop-item .name {
  font-weight: 600;
  margin-bottom: 3px;
}

.shop-item .desc {
  color: #8892a0;
  font-size: 12px;
}

.shop-item .distance {
  color: #ff6a00;
  font-weight: 600;
}

.shop-type-hint-row {
  margin-top: 6px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.shop-type-hint {
  font-size: 12px;
  color: #8a8f9d;
}

.shop-type-hint.is-warning {
  color: #d48806;
}

.apply-location-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.apply-location-value {
  color: #2f8a38;
  font-size: 13px;
}

.apply-location-empty {
  color: #909399;
  font-size: 12px;
}

.ai-write-panel {
  margin-top: 10px;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #ffe2c7;
  background: linear-gradient(180deg, #fffaf5, #fff);
}

.ai-write-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}

.ai-write-title {
  font-weight: 600;
  color: #ff6a00;
}

.ai-write-tip {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
}

.ai-write-draft {
  margin-top: 8px;
  min-height: 68px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #fff;
  border: 1px solid #f2e4d6;
  white-space: pre-wrap;
  line-height: 1.6;
  color: #303133;
}

.ai-write-draft.is-empty {
  color: #a0a5af;
}

.ai-assistant {
  position: fixed;
  right: 18px;
  bottom: 18px;
  z-index: 30;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.ai-trigger {
  border-radius: 999px;
  background: linear-gradient(135deg, #ff8a2a, #ff5a00);
  border-color: transparent;
}

.ai-panel {
  width: 340px;
}

.ai-title {
  font-weight: 600;
}

.ai-body {
  max-height: 320px;
  overflow-y: auto;
  padding-right: 2px;
  margin-bottom: 8px;
  display: grid;
  gap: 8px;
}

.ai-msg {
  padding: 8px 10px;
  border-radius: 10px;
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.ai-msg.is-user {
  background: #fff1e5;
}

.ai-msg.is-assistant {
  background: #f4f6fb;
}

.ai-input-row {
  display: grid;
  gap: 8px;
}

@media (max-width: 1200px) {
  .feed-waterfall {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 992px) {
  .content-layout {
    grid-template-columns: 1fr;
  }
  .feed-waterfall {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .topbar {
    flex-wrap: wrap;
    height: auto;
    padding: 10px 16px;
  }
  .search-area {
    width: 100%;
    max-width: none;
    order: 3;
  }
  .topbar-right {
    margin-left: auto;
  }
  .category-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .feed-header {
    grid-template-columns: 1fr;
    gap: 10px;
  }
  .feed-waterfall {
    grid-template-columns: 1fr;
  }
}
</style>
