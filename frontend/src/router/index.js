import { createRouter, createWebHistory } from "vue-router";
import LoginView from "../views/LoginView.vue";
import BlogView from "../views/BlogView.vue";
import ShopReviewView from "../views/ShopReviewView.vue";
import FollowView from "../views/FollowView.vue";
import ReviewDetailView from "../views/ReviewDetailView.vue";
import ShopListView from "../views/ShopListView.vue";
import ShopDetailView from "../views/ShopDetailView.vue";
import ProductManageView from "../views/ProductManageView.vue";
import UserProfileView from "../views/UserProfileView.vue";
import AdminLayout from "../views/admin/AdminLayout.vue";
import ShopAuditView from "../views/admin/ShopAuditView.vue";
import ShopManageView from "../views/admin/ShopManageView.vue";
import DashboardView from "../views/admin/DashboardView.vue";

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: "/", redirect: "/shops" },
    { path: "/login", component: LoginView },
    { path: "/blogs", component: BlogView },
    { path: "/shops", component: ShopReviewView },
    { path: "/shops/list", component: ShopListView },
    { path: "/shops/:id", component: ShopDetailView },
    { path: "/me", component: UserProfileView },
    { path: "/reviews/:id", component: ReviewDetailView },
    { path: "/follows", component: FollowView },
    {
      path: "/admin",
      component: AdminLayout,
      children: [
        { path: "", redirect: "/admin/dashboard" },
        { path: "dashboard", component: DashboardView },
        { path: "shops/audit", component: ShopAuditView },
        { path: "shops/manage", component: ShopManageView },
        { path: "products", component: ProductManageView }
      ]
    },
    { path: "/products/manage", redirect: "/admin/products" }
  ]
});

router.beforeEach((to, from, next) => {
  if (to.path === "/login" || to.path === "/shops" || to.path === "/shops/list" || to.path.startsWith("/shops/") || to.path.startsWith("/reviews/")) {
    return next();
  }
  if (!localStorage.getItem("token")) {
    return next({ path: "/login", query: { redirect: to.fullPath || from.fullPath || "/shops" } });
  }
  next();
});

export default router;
