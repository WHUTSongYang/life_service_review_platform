<template>
  <div class="audit-page">
    <el-card shadow="never">
      <template #header>
        <span>店铺审核</span>
      </template>
      <el-tabs v-model="tab">
        <el-tab-pane label="新增店铺申请" name="apply">
          <el-form :model="applyForm" label-width="90px" class="form-wrap">
            <el-form-item label="商铺名称">
              <el-input v-model="applyForm.name" />
            </el-form-item>
            <el-form-item label="商铺类型">
              <el-select v-model="applyForm.type" style="width: 100%" placeholder="请选择类型">
                <el-option v-for="item in shopTypes" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="商铺地址">
              <el-input v-model="applyForm.address" />
            </el-form-item>
            <el-form-item label="商铺图片">
              <el-upload
                :action="uploadAction"
                :headers="uploadHeaders"
                :show-file-list="false"
                :on-success="handleApplyImageUploadSuccess"
              >
                <el-button>选择并上传图片</el-button>
              </el-upload>
              <el-image
                v-if="applyForm.image"
                :src="resolveImage(applyForm.image)"
                style="width: 88px; height: 88px; border-radius: 8px; margin-top: 8px"
                fit="cover"
              />
            </el-form-item>
            <el-form-item label="定位坐标">
              <div class="loc-wrap">
                <el-button type="primary" plain :loading="locating" @click="locateApplyShop">定位并录入经纬度</el-button>
                <span v-if="hasLocation">
                  经度 {{ applyForm.longitude }}，纬度 {{ applyForm.latitude }}
                </span>
                <span v-else>请点击按钮定位后再提交</span>
              </div>
            </el-form-item>
            <el-button type="primary" @click="submitShopApply">提交申请</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="我的申请" name="mine">
          <el-table :data="myApplyList">
            <el-table-column prop="name" label="商铺" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="address" label="地址" />
            <el-table-column label="状态" width="120">
              <template #default="{ row }">
                <el-tag :type="statusTagType(row.status)">{{ statusText(row.status) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="reviewNote" label="审批备注" width="220" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane v-if="isSuperAdmin" label="待审列表" name="pending">
          <el-table :data="pendingApplyList">
            <el-table-column prop="name" label="商铺" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="address" label="地址" />
            <el-table-column prop="applicantNickname" label="申请人" width="120" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-space>
                  <el-button size="small" type="success" @click="approveApply(row.id)">通过</el-button>
                  <el-button size="small" type="danger" @click="rejectApply(row.id)">驳回</el-button>
                </el-space>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import http, { authHeaders, baseURL, unwrap } from "../../api/http";

const tab = ref("apply");
const myApplyList = ref([]);
const pendingApplyList = ref([]);
const shopTypes = ref([]);
const locating = ref(false);
const uploadAction = `${baseURL}/api/files/upload`;
const uploadHeaders = authHeaders();
const isSuperAdmin = computed(() => localStorage.getItem("isSuperAdmin") === "true");

const applyForm = reactive({
  name: "",
  type: "",
  image: "",
  address: "",
  longitude: null,
  latitude: null
});

const hasLocation = computed(() => applyForm.longitude != null && applyForm.latitude != null);

function resolveImage(path) {
  if (!path) return "";
  if (path.startsWith("http://") || path.startsWith("https://")) return path;
  return `${baseURL}${path}`;
}

function handleApplyImageUploadSuccess(response) {
  if (!response.success) {
    ElMessage.error(response.message || "上传失败");
    return;
  }
  applyForm.image = response.data.url;
  ElMessage.success("商铺图片上传成功");
}

function locateApplyShop() {
  if (!navigator.geolocation) {
    ElMessage.warning("浏览器不支持定位");
    return;
  }
  locating.value = true;
  navigator.geolocation.getCurrentPosition(
    (position) => {
      applyForm.longitude = Number(position.coords.longitude.toFixed(6));
      applyForm.latitude = Number(position.coords.latitude.toFixed(6));
      locating.value = false;
    },
    () => {
      locating.value = false;
      ElMessage.error("定位失败，请确认浏览器定位权限");
    }
  );
}

async function loadShopTypes() {
  shopTypes.value = await unwrap(http.get("/api/shops/types"));
}

async function loadMyApplies() {
  myApplyList.value = await unwrap(http.get("/api/merchant/shops/apply/mine"));
}

async function loadPendingApplies() {
  if (!isSuperAdmin.value) return;
  pendingApplyList.value = await unwrap(http.get("/api/admin/shops/apply/pending"));
}

async function submitShopApply() {
  if (!applyForm.name || !applyForm.type || !applyForm.address || !applyForm.image) {
    ElMessage.warning("请完整填写名称、类型、地址并上传商铺图片");
    return;
  }
  if (!hasLocation.value) {
    ElMessage.warning("请先定位并录入经纬度");
    return;
  }
  await unwrap(http.post("/api/merchant/shops/apply", applyForm));
  ElMessage.success("申请已提交");
  applyForm.name = "";
  applyForm.type = "";
  applyForm.image = "";
  applyForm.address = "";
  applyForm.longitude = null;
  applyForm.latitude = null;
  tab.value = "mine";
  await loadMyApplies();
}

async function approveApply(applyId) {
  const { value } = await ElMessageBox.prompt("可填写审批备注（选填）", "通过申请", {
    confirmButtonText: "通过",
    cancelButtonText: "取消",
    inputValue: ""
  }).catch(() => ({ value: null }));
  if (value === null) return;
  await unwrap(http.post(`/api/admin/shops/apply/${applyId}/approve`, { reviewNote: value || "" }));
  ElMessage.success("审批通过");
  await Promise.all([loadPendingApplies(), loadMyApplies()]);
}

async function rejectApply(applyId) {
  const { value } = await ElMessageBox.prompt("请填写驳回原因", "驳回申请", {
    confirmButtonText: "驳回",
    cancelButtonText: "取消",
    inputValue: ""
  }).catch(() => ({ value: null }));
  if (value === null) return;
  await unwrap(http.post(`/api/admin/shops/apply/${applyId}/reject`, { reviewNote: value || "" }));
  ElMessage.success("已驳回");
  await loadPendingApplies();
}

function statusText(status) {
  if (status === "APPROVED") return "已通过";
  if (status === "REJECTED") return "已驳回";
  return "待审批";
}

function statusTagType(status) {
  if (status === "APPROVED") return "success";
  if (status === "REJECTED") return "danger";
  return "warning";
}

onMounted(async () => {
  await Promise.all([loadShopTypes(), loadMyApplies(), loadPendingApplies()]);
});
</script>

<style scoped>
.audit-page {
  padding: 4px;
}

.form-wrap {
  max-width: 760px;
}

.loc-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
