import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'reimburse-list',
      component: () => import('@/views/ReimburseList.vue'),
    },
    {
      path: '/reimburse/form',
      name: 'reimburse-form-new',
      component: () => import('@/views/ReimburseForm.vue'),
    },
    {
      path: '/reimburse/form/:id',
      name: 'reimburse-form-edit',
      component: () => import('@/views/ReimburseForm.vue'),
    },
  ],
})

export default router
