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
          <el-form-item label="验证码登录">
            <el-input v-model="loginForm.code" placeholder="演示验证码 123456" />
          </el-form-item>
          <el-space>
            <el-button type="success" @click="loginByPassword">密码登录</el-button>
            <el-button @click="loginByCode">验证码登录</el-button>
          </el-space>
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
import { ref, reactive, watch } from "vue";
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
const loginForm = reactive({
  account: "",
  password: "",
  code: "123456"
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

watch(loginMode, () => {
  activeTab.value = "login";
});

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
}

async function loginByPassword() {
  const data = await unwrap(http.post("/api/auth/login", { account: loginForm.account, password: loginForm.password }));
  applyLoginState(data, "用户");
  window.dispatchEvent(new Event("auth-change"));
  ElMessage.success("登录成功");
  router.push(route.query.redirect || "/shops");
}

async function loginByCode() {
  const data = await unwrap(http.post("/api/auth/code-login", { account: loginForm.account, code: loginForm.code }));
  applyLoginState(data, "用户");
  window.dispatchEvent(new Event("auth-change"));
  ElMessage.success("登录成功");
  router.push(route.query.redirect || "/shops");
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
</style>
