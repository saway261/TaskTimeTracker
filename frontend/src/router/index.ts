import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import { useNotificationStore } from '@/stores/notificationStore'
import type { ApiError } from '@/types/apiError'

const routes = [
  {
    path: '/',
    redirect: '/projects',
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, guestOnly: true },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, guestOnly: true },
  },
  {
    path: '/password-change',
    name: 'password-change',
    component: () => import('@/views/PasswordChangeView.vue'),
  },
  {
    path: '/password-reset-request',
    name: 'password-reset-request',
    component: () => import('@/views/PasswordResetRequestView.vue'),
    meta: { public: true },
  },
  {
    path: '/password-reset',
    name: 'password-reset',
    component: () => import('@/views/PasswordResetView.vue'),
    meta: { public: true },
  },
  {
    path: '/verify-email',
    name: 'verify-email',
    component: () => import('@/views/VerifyEmailView.vue'),
    meta: { public: true },
  },
  {
    path: '/email-verification-pending',
    name: 'email-verification-pending',
    component: () => import('@/views/EmailVerificationPendingView.vue'),
  },
  {
    path: '/email-change',
    name: 'email-change',
    component: () => import('@/views/EmailChangeView.vue'),
  },
  {
    path: '/verify-email-change',
    name: 'verify-email-change',
    component: () => import('@/views/VerifyEmailChangeView.vue'),
    meta: { public: true },
  },
  {
    path: '/projects',
    name: 'project-list',
    component: () => import('@/views/ProjectListView.vue'),
  },
  {
    path: '/reflections',
    name: 'reflection',
    component: () => import('@/views/ReflectionView.vue'),
  },
  {
    path: '/reflections/:projectId',
    name: 'reflection-detail',
    component: () => import('@/views/ReflectionDetailView.vue'),
    props: true,
  },
  {
    path: '/projects/:projectId',
    name: 'project-detail',
    component: () => import('@/views/ProjectDetailView.vue'),
    props: true,
  },
  {
    path: '/projects/:projectId/task-groups/:taskGroupId',
    name: 'task-group-detail',
    component: () => import('@/views/TaskGroupDetailView.vue'),
    props: true,
  },
  {
    path: '/projects/:projectId/tasks/:taskId',
    name: 'project-task-detail',
    component: () => import('@/views/TaskDetailView.vue'),
    props: (route: { params: Record<string, string> }) => ({
      projectId: route.params.projectId,
      taskId: route.params.taskId,
      taskGroupId: null,
    }),
  },
  {
    path: '/projects/:projectId/task-groups/:taskGroupId/tasks/:taskId',
    name: 'task-group-task-detail',
    component: () => import('@/views/TaskDetailView.vue'),
    props: (route: { params: Record<string, string> }) => ({
      projectId: route.params.projectId,
      taskGroupId: route.params.taskGroupId,
      taskId: route.params.taskId,
    }),
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('@/views/NotFoundView.vue'),
  },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  try {
    await authStore.initialize()
  } catch (e) {
    useNotificationStore().error((e as ApiError).message)
  }

  const restrictionExemptRoutes = new Set([
    'password-change',
    'password-reset-request',
    'password-reset',
    'email-change',
    'verify-email',
    'verify-email-change',
  ])

  if (
    authStore.isAuthenticated &&
    authStore.currentUser?.passwordChangeRequired &&
    !restrictionExemptRoutes.has(String(to.name))
  ) {
    return { name: 'password-change' }
  }

  const emailVerificationRoutes = new Set([
    'email-verification-pending',
    'email-change',
    'password-change',
    'password-reset-request',
    'password-reset',
    'verify-email',
    'verify-email-change',
  ])

  if (
    authStore.isAuthenticated &&
    authStore.currentUser?.emailVerified === false &&
    !emailVerificationRoutes.has(String(to.name))
  ) {
    return { name: 'email-verification-pending' }
  }

  if (!authStore.isAuthenticated && !to.meta.public) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (authStore.isAuthenticated && to.meta.guestOnly) {
    return { name: 'project-list' }
  }

  return true
})
