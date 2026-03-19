import { message } from 'ant-design-vue'
import Axios from 'axios'
import Cookie from 'js-cookie'

if (Cookie.get('token')) {
}

// 开发环境用空 baseURL，走 vue.config.js 的 devServer 代理 /api -> 9000，避免跨域和 Network Error
const initAxios = Axios.create({
  timeout: 1800000, //数据响应过期时间
  // 统一走相对路径，避免生产/局域网环境固定指向 localhost 导致 404
  baseURL: ''
  // headers: ['Authorization', Cookie.get('token') ?? null]
})

//请求拦截器：每次请求都带上登录态，避免刷新后 UserContext 为空导致乘车人保存/查询异常
initAxios.interceptors.request.use(
  (config) => {
    const token = Cookie.get('token')
    if (token) {
      // 网关 JWT 解析需要 Bearer 前缀，若 Cookie 中未带则自动补全
      config.headers.Authorization = token.startsWith('Bearer ') ? token : `Bearer ${token}`
    }
    return config
  },
  (error) => {
    //对请求错误做点什么
    return error
  }
)

//响应拦截器
initAxios.interceptors.response.use(
  (response) => {
    if (response.code === 401) {
      message.error('用户未登录或已过期！')
      // location.href = '/login'
      window.location.href = 'login'
    }
    return response
  },
  (error) => {
    console.log(error, 'error')
    // 网络错误、连接失败等情况下 error.response 可能为 undefined，需先判断
    if (error.response?.status === 401) {
      message.error('用户未登录或已过期！')
      window.location.href = 'login'
    } else if (!error.response) {
      message.error('网络异常或服务未启动，请检查后端服务是否运行')
    }
    return Promise.reject(error)
  }
)

// const http = Axios()

export default initAxios
