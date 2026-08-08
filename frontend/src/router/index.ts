import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/projects',
  },
  {
    path: '/projects',
    name: 'project-list',
    component: () => import('@/views/ProjectListView.vue'),
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
