<template>
  <el-card>
    <template #header>关注 / 粉丝</template>
    <el-form inline>
      <el-form-item label="目标用户ID">
        <el-input v-model="targetUserId" style="width: 200px" />
      </el-form-item>
      <el-space>
        <el-button type="primary" @click="followUser">关注</el-button>
        <el-button type="danger" @click="unfollowUser">取消关注</el-button>
      </el-space>
    </el-form>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>我的关注</template>
          <el-table :data="following">
            <el-table-column prop="id" label="用户ID" width="90" />
            <el-table-column prop="nickname" label="昵称" />
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>我的粉丝</template>
          <el-table :data="followers">
            <el-table-column prop="id" label="用户ID" width="90" />
            <el-table-column prop="nickname" label="昵称" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from "vue";
import { ElMessage } from "element-plus";
import http, { unwrap } from "../api/http";

const targetUserId = ref("");
const following = ref([]);
const followers = ref([]);

async function loadRelations() {
  following.value = await unwrap(http.get("/api/users/me/following"));
  followers.value = await unwrap(http.get("/api/users/me/followers"));
}

async function followUser() {
  await unwrap(http.post(`/api/users/${targetUserId.value}/follow`));
  ElMessage.success("关注成功");
  targetUserId.value = "";
  await loadRelations();
}

async function unfollowUser() {
  await unwrap(http.delete(`/api/users/${targetUserId.value}/follow`));
  ElMessage.success("取消关注成功");
  targetUserId.value = "";
  await loadRelations();
}

onMounted(loadRelations);
</script>
