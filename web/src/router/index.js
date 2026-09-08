import { createRouter, createWebHistory } from 'vue-router'
import PKIndexView from '@/views/pk/PKIndexView.vue'
import RecordIndexView from '@/views/record/RecordIndexView.vue'
import RanklistIndexView from '@/views/ranklist/RanklistIndexView.vue'
import NotFound from '@/views/error/NotFound.vue'
import UserBotsIndexView from '@/views/user/bots/UserBotsIndexView.vue'
import UserAccountLoginView from '@/views/user/account/UserAccountLoginView.vue'
import UserAccountRegisterView from '@/views/user/account/UserAccountRegisterView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/pk',
    },
    {
      path: '/pk',
      name: 'pk',
      component: PKIndexView,
    },
    {
      path: '/record',
      name: 'record',
      component: RecordIndexView,
    },
    {
      path: '/ranklist',
      name: 'ranklist',
      component: RanklistIndexView,
    },
    {
      path: '/user/bots',
      name: 'userbots',
      component: UserBotsIndexView,
    },
    {
      path: '/user/account/login',
      name: 'useraccountlogin',
      component: UserAccountLoginView,
    },
    {
      path: '/user/account/register',
      name: 'useraccountregister',
      component: UserAccountRegisterView,
    },
    {
      path: '/:pathMatch(.*)*',
      name: '404',
      component: NotFound,
    },
  ],
})

export default router
