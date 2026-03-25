<script setup>
import {
  Form,
  FormItem,
  Input,
  InputPassword,
  Button,
  message,
  Select,
  Modal
} from 'ant-design-vue'
import { reactive, ref } from 'vue'
import { fetchLogin, fetchRegister } from '../../service'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'
import Cookies from 'js-cookie'

const useForm = Form.useForm

const formState = reactive({
  usernameOrMailOrPhone: 'admin',
  password: 'admin123456',
  code: ''
})

const state = reactive({
  open: false
})

const rulesRef = reactive({
  usernameOrMailOrPhone: [
    { required: true, message: '请输入用户名、邮箱或手机号' }
  ],
  password: [{ required: true, message: '请输入密码' }]
})

const { validate, validateInfos } = useForm(formState, rulesRef)

const registerForm = reactive({
  username: 'admin',
  password: 'admin123456',
  realName: '',
  idType: 0,
  idCard: '',
  phone: '',
  mail: ''
})

const registerRules = reactive({
  username: [{ required: true, message: '请输入用户名' }],
  password: [{ required: true, message: '请输入密码' }],
  realName: [{ required: true, message: '请输入姓名' }],
  idType: [{ required: true, message: '请选择证件类型' }],
  idCard: [{ required: true, message: '请输入证件号码' }],
  phone: [{ required: true, message: '请输入电话号码' }],
  mail: [{ required: true, message: '请输入邮箱' }]
})

const { validate: registerValidate, validateInfos: registerValidateInfos } =
  useForm(registerForm, registerRules)

const currentAction = ref('login')
const router = useRouter()

const handleFinish = () => {
  if (location.host.indexOf('12306') !== -1) {
    validate()
      .then(() => {
        state.open = true
      })
      .catch((err) => console.log(err))
    return
  }
  validate().then(() => {
    fetchLogin({ ...formState }).then((res) => {
      if (res.success) {
        Cookies.set('token', res.data?.accessToken)
        Cookies.set('username', res.data?.username)
        Cookies.set('userId', res.data?.userId ?? res.data?.id ?? '')
        router.push('/ticketSearch')
      } else {
        message.error(res.message)
      }
    })
  })
}

const handleLogin = () => {
  if (!formState.code) return message.error('请输入验证码')
  validate()
    .then(() => {
      fetchLogin({
        usernameOrMailOrPhone: formState.usernameOrMailOrPhone,
        password: formState.code
      }).then((res) => {
        if (res.success) {
          Cookies.set('token', res.data?.accessToken)
          Cookies.set('userId', res.data?.userId ?? res.data?.id ?? '')
          Cookies.set('username', res.data?.username)
          router.push('/ticketSearch')
        } else {
          message.error(res.message)
        }
      })
    })
    .catch((err) => console.log(err))
}

const registerSubmit = () => {
  if (location.host.indexOf('12306') !== -1) {
    message.info('关注公众获取验证码登录哦！')
    currentAction.value = 'login'
    return
  }
  registerValidate()
    .then(() => {
      fetchRegister(registerForm).then((res) => {
        if (res.success) {
          message.success('注册成功')
          currentAction.value = 'login'
          formState.usernameOrMailOrPhone = res.data?.username
          formState.password = ''
        } else {
          message.error(res.message)
        }
      })
    })
    .catch((err) => console.log(err))
}
</script>

<template>
  <div class="auth-shell">
    <div class="auth-shell__aurora" aria-hidden="true" />
    <div class="auth-shell__dots" aria-hidden="true" />
    <div class="auth-shell__vignette" aria-hidden="true" />

    <div
      class="auth-card"
      :class="{ 'auth-card--register': currentAction === 'register' }"
    >
      <div v-if="currentAction === 'login'" class="auth-card__body">
        <div class="auth-card__brand" aria-hidden="true">
          <span class="auth-card__brand-icon">🚄</span>
        </div>
        <header class="auth-card__head">
          <h1 class="auth-card__title">欢迎登录</h1>
          <p class="auth-card__sub">铁路购票 · 使用用户名、邮箱或手机号</p>
        </header>
        <Form name="login" autocomplete="off" class="auth-card__form">
          <FormItem v-bind="validateInfos.usernameOrMailOrPhone" class="form-gap">
            <Input
              size="large"
              v-model:value="formState.usernameOrMailOrPhone"
              placeholder="请输入账号"
              class="rounded-input"
            >
              <template #prefix>
                <UserOutlined class="input-ico" />
              </template>
            </Input>
          </FormItem>
          <FormItem v-bind="validateInfos.password" class="form-gap">
            <InputPassword
              size="large"
              v-model:value="formState.password"
              placeholder="请输入密码"
              class="rounded-input"
            >
              <template #prefix>
                <LockOutlined class="input-ico" />
              </template>
            </InputPassword>
          </FormItem>
          <div class="row-actions">
            <a href="javascript:;" class="link-muted">忘记密码？</a>
          </div>
          <Button
            type="primary"
            block
            class="btn-submit"
            size="large"
            @click="handleFinish"
          >
            登 录
          </Button>
        </Form>
        <footer class="auth-card__foot">
          <span class="auth-card__foot-text">没有账号？</span>
          <button
            type="button"
            class="auth-card__link"
            @click="currentAction = 'register'"
          >
            立即注册
          </button>
        </footer>
      </div>

      <div v-else class="auth-card__body auth-card__body--scroll">
        <div class="auth-card__brand" aria-hidden="true">
          <span class="auth-card__brand-icon">🚄</span>
        </div>
        <header class="auth-card__head">
          <h1 class="auth-card__title">注册账号</h1>
          <p class="auth-card__sub">请如实填写信息，方便购票与核验</p>
        </header>
        <Form
          name="register"
          autocomplete="off"
          :label-col="{ span: 7 }"
          :wrapper-col="{ span: 17 }"
          class="auth-card__form register-form"
        >
          <FormItem label="用户名" v-bind="registerValidateInfos.username">
            <Input v-model:value="registerForm.username" placeholder="用户名" />
          </FormItem>
          <FormItem label="密码" v-bind="registerValidateInfos.password">
            <InputPassword
              v-model:value="registerForm.password"
              placeholder="密码"
            />
          </FormItem>
          <FormItem label="姓名" v-bind="registerValidateInfos.realName">
            <Input v-model:value="registerForm.realName" placeholder="真实姓名" />
          </FormItem>
          <FormItem label="证件类型" v-bind="registerValidateInfos.idType">
            <Select
              :options="[{ value: 0, label: '中国居民身份证' }]"
              v-model:value="registerForm.idType"
              placeholder="请选择"
            />
          </FormItem>
          <FormItem label="证件号码" v-bind="registerValidateInfos.idCard">
            <Input v-model:value="registerForm.idCard" placeholder="证件号码" />
          </FormItem>
          <FormItem label="手机号码" v-bind="registerValidateInfos.phone">
            <Input v-model:value="registerForm.phone" placeholder="手机号" />
          </FormItem>
          <FormItem label="邮箱" v-bind="registerValidateInfos.mail">
            <Input v-model:value="registerForm.mail" placeholder="邮箱" />
          </FormItem>
          <FormItem :wrapper-col="{ span: 24 }" class="reg-submit-wrap">
            <Button type="primary" block class="btn-submit" size="large" @click="registerSubmit">
              完成注册
            </Button>
          </FormItem>
        </Form>
        <footer class="auth-card__foot">
          <span class="auth-card__foot-text">已有账号？</span>
          <button
            type="button"
            class="auth-card__link"
            @click="currentAction = 'login'"
          >
            返回登录
          </button>
        </footer>
      </div>
    </div>
  </div>

  <Modal
    :visible="state.open"
    title="人机认证"
    wrapClassName="code-modal"
    width="450px"
    @cancel="state.open = false"
    @ok="handleLogin"
    centered
  >
    <div class="wrapper">
      <h1 class="tip-text">
        扫码下方二维码，关注后回复「12306」获取人机验证码，用于在线购票系统验证
      </h1>
      <img
        src="https://images-machen.oss-cn-beijing.aliyuncs.com/1_990064918_171_86_3_722467528_78457b21279219802d38525d32a77f39.png"
        alt="微信公众号"
      />
      <div class="code-input">
        <label class="code-label">验证码</label>
        <Input v-model:value="formState.code" :style="{ width: '300px' }" />
      </div>
    </div>
  </Modal>
</template>

<style lang="scss" scoped>
/* 清爽浅色 + 轻微动态背景，与顶栏协调 */
.auth-shell {
  position: relative;
  isolation: isolate;
  min-height: calc(100vh - 64px);
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px 48px;
  box-sizing: border-box;
  overflow-x: hidden;
  overflow-y: auto;
  font-family:
    'PingFang SC',
    'Microsoft YaHei',
    -apple-system,
    sans-serif;
}

.auth-shell__aurora {
  position: absolute;
  inset: 0;
  z-index: 0;
  background: linear-gradient(
    125deg,
    #e8f0fe 0%,
    #f0f5ff 35%,
    #eef2ff 70%,
    #e0e7ff 100%
  );
  background-size: 200% 200%;
  animation: bg-pan 20s ease-in-out infinite;
}

@keyframes bg-pan {
  0%,
  100% {
    background-position: 0% 40%;
  }
  50% {
    background-position: 100% 60%;
  }
}

.auth-shell__dots {
  position: absolute;
  inset: 0;
  z-index: 0;
  opacity: 0.4;
  background-image: radial-gradient(rgba(37, 99, 235, 0.12) 1px, transparent 1px);
  background-size: 20px 20px;
  pointer-events: none;
}

.auth-shell__vignette {
  position: absolute;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  background: radial-gradient(
    ellipse 85% 65% at 50% 40%,
    transparent 0%,
    rgba(248, 250, 252, 0.85) 100%
  );
}

.auth-card {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 420px;
  background: #fff;
  border-radius: 20px;
  border: 1px solid rgba(226, 232, 240, 0.95);
  box-shadow:
    0 4px 6px -1px rgba(15, 23, 42, 0.06),
    0 24px 48px -12px rgba(15, 23, 42, 0.12);
}

.auth-card--register {
  max-width: 520px;
}

.auth-card__brand {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.auth-card__brand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 56px;
  height: 56px;
  font-size: 28px;
  line-height: 1;
  border-radius: 16px;
  background: linear-gradient(145deg, #eff6ff 0%, #dbeafe 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.8),
    0 4px 12px rgba(37, 99, 235, 0.12);
}

.auth-card__body {
  padding: 36px 36px 28px;
  box-sizing: border-box;
}

.auth-card__body--scroll {
  max-height: min(720px, calc(100vh - 64px - 100px));
  overflow-y: auto;
  padding-bottom: 20px;
}

.auth-card__head {
  text-align: center;
  margin-bottom: 28px;
}

.auth-card__title {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
  letter-spacing: 0.02em;
}

.auth-card__sub {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: #64748b;
}

.form-gap {
  margin-bottom: 18px;
}

.input-ico {
  color: #94a3b8;
  font-size: 16px;
}

.row-actions {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.link-muted {
  color: #2563eb;
  font-size: 14px;
  font-weight: 500;
}

.link-muted:hover {
  color: #1d4ed8;
}

.btn-submit {
  height: 46px !important;
  font-weight: 600 !important;
  font-size: 16px !important;
  border-radius: 12px !important;
  border: none !important;
  background: linear-gradient(180deg, #2563eb 0%, #1d4ed8 100%) !important;
  box-shadow: 0 8px 20px -6px rgba(37, 99, 235, 0.45) !important;
}

.btn-submit:hover {
  background: linear-gradient(180deg, #3b82f6 0%, #2563eb 100%) !important;
  color: #fff !important;
}

.auth-card__foot {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #f1f5f9;
  text-align: center;
  font-size: 14px;
  color: #64748b;
}

.auth-card__foot-text {
  margin-right: 6px;
}

.auth-card__link {
  background: none;
  border: none;
  padding: 0;
  color: #2563eb;
  font-weight: 600;
  font-size: 14px;
  cursor: pointer;
}

.auth-card__link:hover {
  color: #1d4ed8;
  text-decoration: underline;
}

.register-form {
  padding-right: 4px;
}

.reg-submit-wrap {
  margin-bottom: 0 !important;
  margin-top: 12px;
}

.code-modal .wrapper {
  text-align: center;
}
.code-modal .tip-text {
  font-size: 14px;
  color: #cf1322;
  line-height: 1.6;
}
.code-modal .code-input {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 12px;
}
.code-modal .code-label {
  margin-right: 10px;
}
.code-modal .code-label::before {
  content: '*';
  color: #cf1322;
}

:deep(.rounded-input.ant-input),
:deep(.rounded-input.ant-input-affix-wrapper) {
  border-radius: 12px !important;
  border: 1px solid #e2e8f0 !important;
  background: #f8fafc !important;
  padding-top: 10px !important;
  padding-bottom: 10px !important;
  transition:
    border-color 0.2s,
    box-shadow 0.2s,
    background 0.2s;
}

:deep(.rounded-input.ant-input-affix-wrapper-focused),
:deep(.rounded-input.ant-input:focus) {
  border-color: #93c5fd !important;
  background: #fff !important;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.15) !important;
}

:deep(.register-form .ant-form-item-label > label) {
  color: #475569;
}

:deep(.register-form .ant-input),
:deep(.register-form .ant-input-password .ant-input) {
  border-radius: 10px;
  background: #f8fafc;
}

:deep(.register-form .ant-input:focus),
:deep(.register-form .ant-input-password-focused .ant-input) {
  background: #fff;
}
</style>
