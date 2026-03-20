<template>
  <div class="product-manage-page">
    <div class="header">
      <el-button link @click="goBack">返回首页</el-button>
      <h2>商品管理</h2>
    </div>

    <el-card class="section" shadow="never">
      <template #header><span>上架商品</span></template>
      <div class="create-form">
        <el-select v-model="createForm.shopId" placeholder="选择店铺" filterable>
          <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
        <el-input v-model="createForm.name" placeholder="商品名" />
        <div class="field-with-label">
          <span class="field-label">商品价格</span>
          <el-input-number v-model="createForm.price" :min="0.01" :step="1" :precision="2" />
        </div>
        <div class="field-with-label">
          <span class="field-label">商品数量</span>
          <el-input-number v-model="createForm.stock" :min="0" :step="1" />
        </div>
        <div class="upload-cell">
          <el-upload
            :action="uploadAction"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCreateImageUploadSuccess"
          >
            <el-button>上传商品图标</el-button>
          </el-upload>
          <el-image v-if="createForm.image" :src="resolveImage(createForm.image)" fit="cover" class="thumb" />
        </div>
        <el-input v-model="createForm.description" placeholder="描述（可选）" />
        <el-switch v-model="createForm.enabled" active-text="上架" inactive-text="下架" />
        <el-button type="primary" @click="createProduct">上架</el-button>
      </div>
    </el-card>

    <el-card class="section" shadow="never">
      <template #header><span>商品列表</span></template>
      <div class="filter-row">
        <el-select v-model="filters.shopId" placeholder="全部店铺" clearable @change="loadProducts">
          <el-option label="全部店铺" value="" />
          <el-option v-for="shop in shops" :key="shop.id" :label="shop.name" :value="shop.id" />
        </el-select>
        <el-input v-model="filters.keyword" placeholder="按商品名搜索" clearable @keyup.enter="loadProducts">
          <template #append>
            <el-button @click="loadProducts">查询</el-button>
          </template>
        </el-input>
      </div>

      <el-table :data="products" style="width: 100%">
        <el-table-column label="商品名" min-width="180">
          <template #default="{ row }">
            <el-input v-model="row.name" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="店铺名称" min-width="140">
          <template #default="{ row }">
            <span>{{ shopNameMap[row.shopId] || `未知店铺(#${row.shopId})` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="160">
          <template #default="{ row }">
            <el-input-number v-model="row.price" :min="0.01" :step="1" :precision="2" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="图片" width="190">
          <template #default="{ row }">
            <div class="upload-cell">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="(res) => handleRowImageUploadSuccess(res, row)"
              >
                <el-button size="small">重新上传</el-button>
              </el-upload>
              <el-image v-if="row.image" :src="resolveImage(row.image)" fit="cover" class="thumb" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="库存" width="210">
          <template #default="{ row }">
            <div class="stock-cell">
              <el-input-number v-model="row.stock" :min="0" :step="1" size="small" />
              <el-button size="small" @click="saveStock(row)">保存</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="150">
          <template #default="{ row }">
            <el-switch
              :model-value="row.enabled"
              active-text="上架"
              inactive-text="下架"
              @change="(val) => changeStatus(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column label="编辑" width="120">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="saveProduct(row)">保存编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage } from "element-plus";
import { useRouter } from "vue-router";
import http, { authHeaders, baseURL, unwrap } from "../api/http";

const router = useRouter();

const shops = ref([]);
const products = ref([]);
const filters = reactive({
  shopId: "",
  keyword: ""
});
const createForm = reactive({
  shopId: null,
  name: "",
  price: 1,
  stock: 0,
  image: "",
  description: "",
  enabled: true
});
const uploadAction = `${baseURL}/api/files/upload`;
const uploadHeaders = authHeaders();
const shopNameMap = computed(() => {
  const map = {};
  for (const shop of shops.value) {
    map[shop.id] = shop.name;
  }
  return map;
});

function goBack() {
  router.push("/admin/shops/audit");
}

async function loadShops() {
  shops.value = await unwrap(http.get("/api/products/manage/shops"));
  if (!shops.value.length) {
    ElMessage.warning("你当前没有可管理的店铺");
  }
  if (!createForm.shopId && shops.value.length) {
    createForm.shopId = shops.value[0].id;
  }
}

async function loadProducts() {
  products.value = await unwrap(
    http.get("/api/products/manage", {
      params: {
        shopId: filters.shopId || undefined,
        keyword: filters.keyword || undefined
      }
    })
  );
}

async function createProduct() {
  if (!createForm.shopId || !createForm.name.trim()) {
    ElMessage.warning("请选择店铺并填写商品名");
    return;
  }
  if (!createForm.image) {
    ElMessage.warning("请先上传商品图标");
    return;
  }
  await unwrap(
    http.post("/api/products/manage", {
      shopId: createForm.shopId,
      name: createForm.name,
      price: createForm.price,
      stock: createForm.stock,
      image: createForm.image,
      description: createForm.description,
      enabled: createForm.enabled
    })
  );
  ElMessage.success("商品上架成功");
  createForm.name = "";
  createForm.price = 1;
  createForm.stock = 0;
  createForm.image = "";
  createForm.description = "";
  await loadProducts();
}

function resolveImage(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${baseURL}${path}`;
}

function handleCreateImageUploadSuccess(response) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  createForm.image = response.data.url;
  ElMessage.success("商品图标上传成功");
}

function handleRowImageUploadSuccess(response, row) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  row.image = response.data.url;
  ElMessage.success("商品图片已更新，请点“保存编辑”提交");
}

async function saveStock(row) {
  await unwrap(http.put(`/api/products/manage/${row.id}/stock`, { stock: row.stock }));
  ElMessage.success("库存已更新");
}

async function changeStatus(row, enabled) {
  await unwrap(http.put(`/api/products/manage/${row.id}/status`, { enabled }));
  row.enabled = enabled;
  ElMessage.success(enabled ? "商品已上架" : "商品已下架");
}

async function saveProduct(row) {
  if (!row.name || !row.name.trim()) {
    ElMessage.warning("商品名不能为空");
    return;
  }
  await unwrap(
    http.put(`/api/products/manage/${row.id}`, {
      name: row.name,
      price: row.price,
      image: row.image
    })
  );
  ElMessage.success("商品信息已更新");
}

onMounted(async () => {
  await Promise.all([loadShops(), loadProducts()]);
});
</script>

<style scoped>
.product-manage-page {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}

.header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.header h2 {
  margin: 0;
}

.section {
  margin-bottom: 12px;
}

.create-form {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}

.filter-row {
  display: grid;
  grid-template-columns: 200px 1fr;
  gap: 10px;
  margin-bottom: 10px;
}

.upload-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
}

.field-with-label {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 12px;
  color: #8d95a3;
  line-height: 1;
}

.stock-cell {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
</style>
