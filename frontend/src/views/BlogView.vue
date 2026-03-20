<template>
  <el-card>
    <template #header>博客发布与互动</template>
    <el-form :model="form" inline>
      <el-form-item label="标题">
        <el-input v-model="form.title" style="width: 220px" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="form.content" style="width: 320px" />
      </el-form-item>
      <el-form-item label="图片上传">
        <el-upload
          :action="uploadAction"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleCreateUploadSuccess"
        >
          <el-button>上传图片</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item>
        <el-space wrap>
          <el-image
            v-for="img in createImages"
            :key="img"
            :src="resolveImage(img)"
            style="width: 56px; height: 56px; border-radius: 6px"
            fit="cover"
          />
        </el-space>
      </el-form-item>
      <el-button type="primary" @click="createBlog">发布博客</el-button>
    </el-form>

    <el-table :data="blogs" style="margin-top: 18px">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="正文" />
      <el-table-column label="图片" width="130">
        <template #default="{ row }">
          <el-image
            v-if="firstImage(row.images)"
            :src="firstImage(row.images)"
            style="width: 56px; height: 56px; border-radius: 6px"
            fit="cover"
          />
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="likeCount" label="点赞数" width="90" />
      <el-table-column label="操作" width="360">
        <template #default="{ row }">
          <el-space>
            <el-button size="small" @click="toggleLike(row.id)">点赞/取消</el-button>
            <el-button size="small" @click="prepareEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteBlog(row.id)">删除</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
  </el-card>

  <el-dialog v-model="editVisible" title="编辑博客">
    <el-form :model="editForm" label-width="80">
      <el-form-item label="标题">
        <el-input v-model="editForm.title" />
      </el-form-item>
      <el-form-item label="正文">
        <el-input v-model="editForm.content" type="textarea" />
      </el-form-item>
      <el-form-item label="图片">
        <el-upload
          :action="uploadAction"
          :headers="uploadHeaders"
          :show-file-list="false"
          :on-success="handleEditUploadSuccess"
        >
          <el-button>上传图片</el-button>
        </el-upload>
        <el-space wrap style="margin-top: 8px">
          <el-image
            v-for="img in editImages"
            :key="img"
            :src="resolveImage(img)"
            style="width: 56px; height: 56px; border-radius: 6px"
            fit="cover"
          />
        </el-space>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" @click="submitEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue";
import http, { authHeaders, baseURL, unwrap } from "../api/http";
import { ElMessage } from "element-plus";

const blogs = ref([]);
const editVisible = ref(false);
const editForm = reactive({ id: null, title: "", content: "", images: "" });
const form = reactive({ title: "", content: "" });
const createImages = ref([]);
const editImages = ref([]);
const uploadAction = `${baseURL}/api/files/upload`;
const uploadHeaders = authHeaders();

async function loadBlogs() {
  blogs.value = await unwrap(http.get("/api/blogs"));
}

async function createBlog() {
  await unwrap(
    http.post("/api/blogs", {
      ...form,
      images: createImages.value.join(",")
    })
  );
  ElMessage.success("发布成功");
  form.title = "";
  form.content = "";
  createImages.value = [];
  await loadBlogs();
}

function prepareEdit(row) {
  editForm.id = row.id;
  editForm.title = row.title;
  editForm.content = row.content;
  editImages.value = parseImageList(row.images);
  editForm.images = editImages.value.join(",");
  editVisible.value = true;
}

async function submitEdit() {
  editForm.images = editImages.value.join(",");
  await unwrap(http.put(`/api/blogs/${editForm.id}`, editForm));
  ElMessage.success("编辑成功");
  editVisible.value = false;
  await loadBlogs();
}

async function deleteBlog(id) {
  await unwrap(http.delete(`/api/blogs/${id}`));
  ElMessage.success("删除成功");
  await loadBlogs();
}

async function toggleLike(id) {
  await unwrap(http.post(`/api/blogs/${id}/like`));
  await loadBlogs();
}

function firstImage(images) {
  const first = parseImageList(images)[0];
  return first ? resolveImage(first) : "";
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

function handleCreateUploadSuccess(response) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  createImages.value.push(response.data.url);
}

function handleEditUploadSuccess(response) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  editImages.value.push(response.data.url);
}

onMounted(loadBlogs);
</script>
