<template>
  <el-card class="auth-card">
    <template #header>
      <div class="auth-header">账号中心</div>
    </template>
    <el-segmented v-model="loginMode" :options="modeOptions" style="margin-bottom: 12px" />
    <el-tabs v-model="activeTab">
      <el-tab-pane label="登录" name="login">
        <el-form v-if="loginMode === 'user'" :model="loginForm" label-width="90px">
          <el-form-item label="账号">
            <el-input v-model="loginForm.account" placeholder="手机号或邮箱" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input
                v-model="loginForm.captchaCode"
                placeholder="右侧 4 位字母或数字"
                maxlength="4"
                class="captcha-input"
                @keyup.enter="loginByPassword"
              />
              <div class="captcha-img-wrap" @click="fetchCaptcha" title="点击刷新">
                <img v-if="captchaImageSrc" :src="captchaImageSrc" alt="验证码" class="captcha-img" />
                <span v-else class="captcha-placeholder">加载中…</span>
              </div>
            </div>
          </el-form-item>
          <el-button type="success" @click="loginByPassword">登录</el-button>
        </el-form>
        <el-form v-else :model="adminLoginForm" label-width="90px">
          <el-form-item label="管理员账号">
            <el-input v-model="adminLoginForm.username" placeholder="请输入管理员账号" />
          </el-form-item>
          <el-form-item label="管理员密码">
            <el-input v-model="adminLoginForm.password" type="password" show-password />
          </el-form-item>
          <el-button type="primary" @click="loginAsAdmin">管理员登录</el-button>
        </el-form>
      </el-tab-pane>

      <el-tab-pane v-if="loginMode === 'user'" label="注册" name="register">
        <el-form :model="registerForm" label-width="90px">
          <el-form-item label="账号">
            <el-input v-model="registerForm.account" placeholder="手机号或邮箱" />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="registerForm.nickname" />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="registerForm.confirmPassword" type="password" show-password />
          </el-form-item>
          <el-button type="primary" @click="register">注册</el-button>
        </el-form>
      </el-tab-pane>
    </el-tabs>
  </el-card>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from "vue";
import { ElMessage } from "element-plus";
import http, { unwrap } from "../api/http";
import { useRoute, useRouter } from "vue-router";

const router = useRouter();
const route = useRoute();
const loginMode = ref("user");
const modeOptions = [
  { label: "用户登录", value: "user" },
  { label: "管理员登录", value: "admin" }
];
const activeTab = ref("login");
const captchaId = ref("");
const captchaImageSrc = ref("");
const loginForm = reactive({
  account: "",
  password: "",
  captchaCode: ""
});
const adminLoginForm = reactive({
  username: "",
  password: ""
});
const registerForm = reactive({
  account: "",
  nickname: "",
  password: "",
  confirmPassword: ""
});

watch(loginMode, (v) => {
  activeTab.value = "login";
  if (v === "user") {
    fetchCaptcha();
  }
});

onMounted(() => {
  if (loginMode.value === "user") {
    fetchCaptcha();
  }
});

async function fetchCaptcha() {
  try {
    const data = await unwrap(http.get("/api/auth/captcha"));
    captchaId.value = data.captchaId;
    captchaImageSrc.value = "data:image/png;base64," + data.imageBase64;
    loginForm.captchaCode = "";
  } catch (_e) {
    captchaImageSrc.value = "";
    ElMessage.error("验证码加载失败");
  }
}

async function register() {
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning("两次输入的密码不一致");
    return;
  }
  const payload = {
    phone: registerForm.account.includes("@") ? null : registerForm.account,
    email: registerForm.account.includes("@") ? registerForm.account : null,
    password: registerForm.password,
    confirmPassword: registerForm.confirmPassword,
    nickname: registerForm.nickname || "新用户"
  };
  await unwrap(http.post("/api/auth/register", payload));
  ElMessage.success("注册成功，请登录");
  activeTab.value = "login";
  loginForm.account = registerForm.account;
  fetchCaptcha();
}

async function loginByPassword() {
  if (!loginForm.captchaCode || loginForm.captchaCode.trim().length < 4) {
    ElMessage.warning("请输入 4 位图形验证码");
    return;
  }
  try {
    const data = await unwrap(
      http.post("/api/auth/login", {
        account: loginForm.account,
        password: loginForm.password,
        captchaId: captchaId.value,
        captchaCode: loginForm.captchaCode.trim()
      })
    );
    applyLoginState(data, "用户");
    window.dispatchEvent(new Event("auth-change"));
    ElMessage.success("登录成功");
    router.push(route.query.redirect || "/shops");
  } catch (_e) {
    fetchCaptcha();
  }
}

async function loginAsAdmin() {
  const data = await unwrap(http.post("/api/auth/admin/login", { username: adminLoginForm.username, password: adminLoginForm.password }));
  applyLoginState(data, "管理员");
  window.dispatchEvent(new Event("auth-change"));
  ElMessage.success("管理员登录成功");
  router.push("/admin/shops/audit");
}

function applyLoginState(data, fallbackNickname) {
  localStorage.setItem("token", data.token);
  if (data.userId != null) {
    localStorage.setItem("userId", String(data.userId));
  } else {
    localStorage.removeItem("userId");
  }
  if (data.adminId != null) {
    localStorage.setItem("adminId", String(data.adminId));
  } else {
    localStorage.removeItem("adminId");
  }
  localStorage.setItem("nickname", data.nickname || fallbackNickname);
  localStorage.setItem("principalType", data.principalType || "USER");
  localStorage.setItem("isSuperAdmin", String(!!data.isSuperAdmin));
}
</script>

<style scoped>
.auth-card {
  max-width: 520px;
  margin: 20px auto;
}

.auth-header {
  font-size: 16px;
  font-weight: 600;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
}

.captcha-input {
  flex: 1;
  min-width: 0;
}

.captcha-img-wrap {
  flex-shrink: 0;
  width: 120px;
  height: 44px;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-placeholder {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
</style>
