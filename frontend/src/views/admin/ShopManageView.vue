<template>
  <div class="shop-manage-page">
    <el-card shadow="never">
      <template #header>
        <span>店铺管理</span>
      </template>

      <div class="toolbar">
        <el-button @click="loadShops">刷新</el-button>
      </div>

      <el-table :data="shops" style="width: 100%">
        <el-table-column prop="id" label="店铺ID" width="90" />
        <el-table-column label="店铺名称" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="店铺类型" width="160">
          <template #default="{ row }">
            <el-select v-model="row.type" size="small" style="width: 100%">
              <el-option v-for="item in shopTypes" :key="item" :label="item" :value="item" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="店铺地址" min-width="220">
          <template #default="{ row }">
            <el-input v-model="row.address" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="店铺图片" width="220">
          <template #default="{ row }">
            <div class="upload-cell">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="(res) => handleRowImageUploadSuccess(res, row)"
              >
                <el-button size="small">上传图片</el-button>
              </el-upload>
              <el-image v-if="row.image" :src="resolveImage(row.image)" fit="cover" class="thumb" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="经纬度" min-width="240">
          <template #default="{ row }">
            <div class="coord-wrap">
              <el-input-number v-model="row.longitude" :precision="6" :step="0.000001" size="small" />
              <el-input-number v-model="row.latitude" :precision="6" :step="0.000001" size="small" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="saveShop(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from "vue";
import { ElMessage } from "element-plus";
import http, { authHeaders, baseURL, unwrap } from "../../api/http";

const shops = ref([]);
const shopTypes = ref([]);
const uploadAction = `${baseURL}/api/files/upload`;
const uploadHeaders = authHeaders();

function resolveImage(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${baseURL}${path}`;
}

function handleRowImageUploadSuccess(response, row) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  row.image = response.data.url;
  ElMessage.success("店铺图片已更新，请点保存提交");
}

async function loadShops() {
  shops.value = await unwrap(http.get("/api/shops/manage"));
}

async function loadShopTypes() {
  shopTypes.value = await unwrap(http.get("/api/shops/types"));
}

async function saveShop(row) {
  await unwrap(
    http.put(`/api/shops/manage/${row.id}`, {
      name: row.name,
      type: row.type,
      image: row.image,
      address: row.address,
      longitude: row.longitude,
      latitude: row.latitude
    })
  );
  ElMessage.success("店铺信息已更新");
}

onMounted(async () => {
  await Promise.all([loadShops(), loadShopTypes()]);
});
</script>

<style scoped>
.shop-manage-page {
  padding: 4px;
}

.toolbar {
  margin-bottom: 10px;
}

.upload-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
}

.coord-wrap {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
