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

  if (
    authStore.isAuthenticated &&
    authStore.currentUser?.passwordChangeRequired &&
    to.name !== 'password-change'
  ) {
    return { name: 'password-change' }
  }

  if (!authStore.isAuthenticated && !to.meta.public) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  if (authStore.isAuthenticated && to.meta.guestOnly) {
    return { name: 'project-list' }
  }

  return true
})
