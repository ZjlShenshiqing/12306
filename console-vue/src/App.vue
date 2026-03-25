<script setup>
import { Layout, ConfigProvider, Space } from 'ant-design-vue'
import { useRoute } from 'vue-router'
import { computed, onMounted, reactive } from 'vue'
import BreadHeader from '@/components/bread-header'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import Header from './components/header'
import Sider from './components/sider'
import jsCookie from 'js-cookie'
import axios from './service/axios'
import dayjs from 'dayjs'
import duration from 'dayjs/plugin/duration'
dayjs.extend(duration)

const state = reactive()

const { Content } = Layout
const route = useRoute()

/** 登录页 / 首页：不显示侧栏、面包屑 */
const hideAppChrome = computed(
  () => route.path === '/login' || route.path === '/'
)
/** 仅登录页：顶栏使用浅色玻璃样式 */
const isLoginPage = computed(() => route.path === '/login')

onMounted(() => {
  const token = jsCookie.get('token')
  if (token) {
    axios.defaults.headers.common['Authorization'] = token
  }
  window.addEventListener('hashchange', () => {
    console.log(location.pathname, 'change')
  })
})
</script>
<template>
  <ConfigProvider :locale="zhCN">
    <Layout>
      <Header :isLogin="isLoginPage" />
      <Layout class="page-wrapper" :class="{ isLogin: hideAppChrome }">
        <Sider v-if="!hideAppChrome" />
        <Content class="app-wrapper" :class="{ isLogin: hideAppChrome }">
          <ConfigProvider :locale="zhCN">
            <BreadHeader v-if="!hideAppChrome" /> <router-view /> </ConfigProvider
        ></Content>
      </Layout>
    </Layout>
  </ConfigProvider>
</template>

<style lang="scss" scoped>
.page-wrapper {
  box-sizing: border-box;
  padding-top: 64px;
  /* min-width: 1230px; */
  .app-wrapper {
    box-sizing: border-box;
    margin: 20px;
    transition: all 0.2s;
  }
}

.isLogin.page-wrapper {
  padding: 64px 0 0;
  margin: 0;
}
.ant-layout-content {
  min-height: calc(100vh - 64px);
}
.isLogin.ant-layout-content {
  min-height: calc(100vh - 64px);
  height: auto;
  margin: 0;
}
::v-deep {
  .ant-layout-content {
    transition: all 0.2s;
  }
}
</style>
