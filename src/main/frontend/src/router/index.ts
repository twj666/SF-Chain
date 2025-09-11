import { createRouter, createWebHistory } from 'vue-router'
import App from '../App.vue'

const router = createRouter({
  history: createWebHistory('/sf-chain/'),
  routes: [
    {
      path: '/',
      name: 'home',
      component: App
    }
  ]
})

export default router
