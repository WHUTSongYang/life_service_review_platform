<template>
  <div class="review-detail-page">
    <header class="detail-header">
      <el-button link @click="goBack">返回</el-button>
      <h2>点评详情</h2>
    </header>

    <el-card v-if="detail" class="detail-card" shadow="never">
      <div class="top-row">
        <h3>{{ detail.shopName }}</h3>
        <el-tag type="warning">{{ detail.score || 0 }}分</el-tag>
      </div>
      <p class="shop-meta">{{ detail.shopType || "其他" }} · {{ detail.shopAddress || "地址待完善" }}</p>
      <p class="content">{{ detail.content }}</p>
      <div v-if="detailImages.length" class="image-grid">
        <el-image v-for="img in detailImages" :key="img" :src="resolveImage(img)" fit="cover" class="detail-image" />
      </div>
      <p class="meta">
        {{ detail.userNickname || "匿名用户" }} · 点赞 {{ detail.likeCount || 0 }} · {{ formatTime(detail.createdAt) }}
      </p>
    </el-card>

    <el-card class="comment-card" shadow="never">
      <template #header>
        <div class="comment-header">
          <span>留言区（{{ comments.length }}）</span>
          <el-button size="small" @click="loadComments">刷新</el-button>
        </div>
      </template>
      <div class="comment-editor">
        <el-input
          v-model="commentContent"
          type="textarea"
          :rows="3"
          maxlength="300"
          show-word-limit
          placeholder="写下你的留言..."
        />
        <div class="comment-actions">
          <el-button type="primary" @click="submitComment">发布留言</el-button>
        </div>
      </div>

      <ul class="comment-list">
        <li v-for="item in comments" :key="item.id" class="comment-item">
          <div class="comment-user">{{ item.userNickname || "匿名用户" }}</div>
          <div class="comment-content">{{ item.content }}</div>
          <div class="comment-time">{{ formatTime(item.createdAt) }}</div>
        </li>
      </ul>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRoute, useRouter } from "vue-router";
import http, { baseURL, unwrap } from "../api/http";

const route = useRoute();
const router = useRouter();

const detail = ref(null);
const comments = ref([]);
const commentContent = ref("");

const reviewId = computed(() => Number(route.params.id));
const detailImages = computed(() => parseImageList(detail.value?.images || ""));

function goBack() {
  router.back();
}

async function loadDetail() {
  detail.value = await unwrap(http.get(`/api/reviews/${reviewId.value}`));
}

async function loadComments() {
  comments.value = await unwrap(http.get(`/api/reviews/${reviewId.value}/comments`));
}

async function submitComment() {
  const token = localStorage.getItem("token");
  if (!token) {
    ElMessage.warning("请先登录后再留言");
    router.push({ path: "/login", query: { redirect: route.fullPath } });
    return;
  }
  const content = commentContent.value.trim();
  if (!content) {
    ElMessage.warning("留言内容不能为空");
    return;
  }
  await unwrap(http.post(`/api/reviews/${reviewId.value}/comments`, { content }));
  commentContent.value = "";
  ElMessage.success("留言成功");
  await loadComments();
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

function formatTime(value) {
  if (!value) return "";
  return String(value).replace("T", " ").slice(0, 19);
}

onMounted(async () => {
  if (!reviewId.value) {
    ElMessage.error("点评ID无效");
    router.push("/shops");
    return;
  }
  await Promise.all([loadDetail(), loadComments()]);
});
</script>

<style scoped>
.review-detail-page {
  max-width: 960px;
  margin: 0 auto;
  padding: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.detail-header h2 {
  margin: 0;
  font-size: 20px;
}

.detail-card,
.comment-card {
  margin-bottom: 14px;
  border-radius: 12px;
}

.top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.shop-meta {
  color: #8a8f9d;
  margin: 8px 0 6px;
}

.content {
  line-height: 1.7;
  margin-bottom: 10px;
}

.meta {
  color: #8a8f9d;
  font-size: 13px;
}

.image-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-bottom: 10px;
}

.detail-image {
  width: 100%;
  height: 130px;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.comment-editor {
  margin-bottom: 12px;
}

.comment-actions {
  margin-top: 8px;
  display: flex;
  justify-content: flex-end;
}

.comment-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.comment-item {
  border-bottom: 1px solid #f0f0f0;
  padding: 10px 0;
}

.comment-user {
  font-weight: 600;
  margin-bottom: 6px;
}

.comment-content {
  margin-bottom: 5px;
}

.comment-time {
  color: #8a8f9d;
  font-size: 12px;
}
</style>
