import { createRouter, createWebHistory } from 'vue-router'
import PKIndexView from '@/views/pk/PKIndexView.vue'
import RecordIndexView from '@/views/record/RecordIndexView.vue'
import RanklistIndexView from '@/views/ranklist/RanklistIndexView.vue'
import NotFound from '@/views/error/NotFound.vue'
import UserBotsIndexView from '@/views/user/bots/UserBotsIndexView.vue'
import UserAccountLoginView from '@/views/user/account/UserAccountLoginView.vue'
import UserAccountRegisterView from '@/views/user/account/UserAccountRegisterView.vue'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      redirect: '/pk',
      meta: {
        requestAuth: true,
      }
    },
    {
      path: '/pk',
      name: 'pk',
      component: PKIndexView,
      meta: {
        requestAuth: true,
      }
    },
    {
      path: '/record',
      name: 'record',
      component: RecordIndexView,
      meta: {
        requestAuth: false,
      }
    },
    {
      path: '/ranklist',
      name: 'ranklist',
      component: RanklistIndexView,
      meta: {
        requestAuth: false,
      }
    },
    {
      path: '/user/bots',
      name: 'userbots',
      component: UserBotsIndexView,
      meta: {
        requestAuth: true,
      }
    },
    {
      path: '/user/account/login',
      name: 'useraccountlogin',
      component: UserAccountLoginView,
      meta: {
        requestAuth: false,
      }
    },
    {
      path: '/user/account/register',
      name: 'useraccountregister',
      component: UserAccountRegisterView,
      meta: {
        requestAuth: false,
      }
    },
    {
      path: '/:pathMatch(.*)*',
      name: '404',
      component: NotFound,
    },
  ],
})

router.beforeEach(async (to, from) => {
  const userStore = useUserStore();
  const jwt_token = localStorage.getItem('jwt_token');
  if (jwt_token && !userStore.is_login) {
    try {
      await userStore.getInfo();
    } catch (error) {
      userStore.logout();
    }
  }

  if (!userStore.is_login && to.meta.requestAuth) {
    return { name: 'useraccountlogin' }
  }
})

export default router
