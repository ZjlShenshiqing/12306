<template>
  <Header :class="{ isLogin }">
    <div class="header-wrapper">
      <div class="header-left">
        <router-link to="/" class="logo-link">
          <div class="logo">12306购票</div>
        </router-link>
        <ul class="nav-list-wrapper">
          <router-link to="/">
            <li class="nav-item">首页</li>
          </router-link>
          <router-link to="/ticketSearch">
            <li class="nav-item">车票</li>
          </router-link>
          <router-link to="/order-list">
            <li class="nav-item">服务</li>
          </router-link>
        </ul>
      </div>
      
      <div class="header-right">
        <a v-if="route.path !== '/login' && route.path !== '/'">
          <Dropdown :trigger="['click']" placement="bottomRight">
            <div class="user-menu">
              <Avatar shape="circle" style="background-color: #1890ff; cursor: pointer;"
                >{{ state.username?.slice(0, 1)?.toUpperCase() }} 
              </Avatar>
            </div>
            <template #overlay>
              <Menu>
                <MenuItem>
                  <a @click="() => router.push('/user-info')">个人信息</a>
                </MenuItem>
                <MenuItem>
                  <a @click="() => router.push('/passenger')">乘车人管理</a>
                </MenuItem>
                <MenuItem>
                  <a @click="() => router.push('/order-list')">我的订单</a>
                </MenuItem>
                <MenuItem>
                  <a @click="() => logout()">退出登录</a>
                </MenuItem>
              </Menu>
            </template>
          </Dropdown>
        </a>
        <router-link v-else to="/login" class="login-link">
          <div class="login-btn">登录</div>
        </router-link>
      </div>
    </div>
  </Header>
</template>

<script setup>
import {
  Layout,
  Avatar,
  Dropdown,
  Menu,
  MenuItem,
  message
} from 'ant-design-vue'
import { useRouter, useRoute } from 'vue-router'
import { defineProps, reactive, toRefs, watch } from 'vue'
import { fetchLogout } from '@/service'
import Cookie from 'js-cookie'
const username = Cookie.get('username')

const { Header } = Layout
const props = defineProps({
  isLogin: Boolean
})

const { isLogin } = toRefs(props)

const state = reactive({
  username: username
})

const router = useRouter()
const route = useRoute()

watch(
  () => route.fullPath,
  (newValue) => {
    state.username = username
  },
  { immediate: true }
)

const logout = () => {
  const token = Cookie.get('token')
  fetchLogout({ accessToken: token }).then((res) => {
    if (res.success) {
      message.success('退出成功')
      location.href = 'login'
      Cookie.remove('token')
      Cookie.remove('username')
    }
  })
}
</script>

<style lang="scss" scoped>
.ant-layout-header {
  position: fixed;
  width: 100%;
  min-width: 900px;
  height: 64px;
  top: 0;
  z-index: 100;
  background-color: #0066cc;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.isLogin.ant-layout-header {
  background-color: rgba(0, 102, 204, 0.95);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.2);
}

.header-wrapper {
  display: flex;
  flex: 1;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  flex-wrap: nowrap;
  color: #fff;
  padding: 0 40px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 30px;
}

.logo-link,
.login-link {
  text-decoration: none;
  color: inherit;
  display: inline-block;
}

.logo {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  font-family: Helvetica, Tahoma, Arial, 'PingFang SC', 'Hiragino Sans GB', 'Heiti SC', 'Microsoft YaHei', 'WenQuanYi Micro Hei';
  cursor: pointer;
  letter-spacing: 1px;
  text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.logo:hover {
  color: #f0f9ff;
}

/* 导航列表 */
.nav-list-wrapper {
  display: flex;
  text-decoration: none;
  list-style: none;
  margin: 0;
  align-items: center;
  gap: 10px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nav-item {
  padding: 0 16px;
  line-height: 64px;
  transition: all 0.3s ease;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  position: relative;
  color: #fff;
  
  &:hover {
    color: #f0f9ff;
    background-color: rgba(255, 255, 255, 0.1);
  }
  
  &::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 0;
    height: 3px;
    background-color: #fff;
    transition: width 0.3s ease;
  }
  
  &:hover::after {
    width: 80%;
  }
}

.user-menu {
  padding: 0 24px;
  line-height: 64px;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: rgba(255, 255, 255, 0.1);
  }
}

.login-btn {
  padding: 0 20px;
  line-height: 36px;
  margin: 14px 0;
  border-radius: 18px;
  background-color: #ff9500;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  
  &:hover {
    background-color: #ffb74d;
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3);
  }
  
  &::after {
    display: none;
  }
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-wrapper {
    padding: 0 30px;
  }

  .nav-item {
    padding: 0 20px;
  }
}

@media (max-width: 1200px) {
  .ant-layout-header {
    min-width: 800px;
  }

  .nav-item {
    padding: 0 16px;
  }
}

@media (max-width: 1000px) {
  .ant-layout-header {
    min-width: 700px;
  }

  .logo {
    font-size: 18px;
  }
}
</style>
