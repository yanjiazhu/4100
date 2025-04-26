import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import PerformanceDashboard from '../views/PerformanceDashboard.vue'
import DataUpload from '../views/DataUpload.vue'
import LoginPage from '../views/LoginPage.vue'

const routes = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'LoginPage',
    component: LoginPage,
    meta: {
      title: 'Login'
    }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      {
        path: 'performance',
        name: 'PerformanceDashboard',
        component: PerformanceDashboard,
        meta: {
          title: 'Performance Dashboard'
        }
      },
      {
        path: 'upload',
        name: 'DataUpload',
        component: DataUpload,
        meta: {
          title: 'Upload Performance Data'
        }
      }
    ]
  },
  // Fallback route
  {
    path: '/:pathMatch(.*)*',
    redirect: '/'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// Global navigation guard to set document title
router.beforeEach((to, from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - Performance Track` : 'Performance Track'
  next()
})

export default router 