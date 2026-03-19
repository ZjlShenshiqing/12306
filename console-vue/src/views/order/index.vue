<template>
  <div v-html="state.html"></div>
  <Space :style="{ width: '100%' }" direction="vertical">
    <Card>
      <div class="tip-wrapper">
        <span class="i-lock"></span>
        席位已锁定，请在提示时间内完成支付时间，完成网上购票。支付剩余时间：
        <span class="time">
          {{
            `${dayjs.duration(state.count).minutes()}分${dayjs
              .duration(state.count)
              .seconds()}秒`
          }}
        </span>
      </div>
    </Card>
    <Card title="订单信息">
      <Space :style="{ width: '100%' }" direction="vertical">
        <div>
          <span class="important-text">{{
            state.currentInfo?.ridingDate
          }}</span>
          <span class="important-text"
            >（{{
              getWeekNumber(
                dayjs(state.currentInfo?.ridingDate ?? new Date()).day()
              )
            }}）</span
          >
          <span class="important-text">{{
            state.currentInfo?.trainNumber
          }}</span>
          <span class="small-text">次</span>
          <span class="important-text">{{ state.currentInfo?.departure }}</span>
          <span class="small-text">站</span>
          <span class="important-text"
            >（{{ state.currentInfo?.departureTime }}开）——</span
          >
          <span class="important-text">{{ state.currentInfo?.arrival }}</span>
          <span class="small-text">站</span>
          <span class="important-text"
            >（{{ state.currentInfo?.arrivalTime }}到）</span
          >
        </div>
        <Table
          :columns="columns"
          :pagination="false"
          :dataSource="state.currentInfo?.passengerDetails"
        >
          <template #id="{ text, record, index }"> {{ index + 1 }}</template>
          <template #idType="{ text, record }">
            {{
              ID_CARD_TYPE.find((item) => item.value === text)?.label
            }}</template
          >
          <template #ticketType="{ text, record }">
            {{
              TICKET_TYPE_LIST.find((item) => item.value === text)?.label
            }}</template
          >
          <template #seatType="{ text, record }">
            {{
              SEAT_CLASS_TYPE_LIST.find((item) => item.code === text)?.label
            }}</template
          >
          <template #amount="{ text, record }">
            <span :style="{ color: 'rgb(252, 131, 2)' }"
              >￥{{ text / 100 }}</span
            >
          </template>
        </Table>
        <div :style="{ width: '100%', textAlign: 'end', padding: '10px' }">
          总票价：<span :style="{ color: '#ff8001' }"
            >￥{{ totalAmount }}元</span
          >
        </div>
        <Divider></Divider>
        <div
          :style="{
            width: '100%',
            display: 'flex',
            justifyContent: 'center'
          }"
        >
          <Space size="large">
            <Button
              @click="
                () => {
                  fetchOrderCancel({ orderSn: currentOrderSn }).then((res) => {
                    if (res.success) {
                      message.success('订单取消成功')
                      router.push('/ticketSearch')
                    } else {
                      message.error(res.message)
                    }
                  })
                }
              "
              >取消订单</Button
            >
            <Button
              :style="{
                backgroundColor: '#ff8001',
                color: '#fff',
                border: 'none'
              }"
              @click="state.open = true"
              >网上支付</Button
            >
            <Button @click="state.rechargeOpen = true">余额充值</Button>
          </Space>
        </div>
      </Space>
    </Card>
    <div>
      <div class="tips-txt">
        <h2>温馨提示：</h2>
        <p>
          1.
          一张有效身份证件同一乘车日期同一车次只能购买一张车票，高铁动卧列车除外。
        </p>
        <p>
          2.
          购买儿童票时，乘车儿童有有效身份证件的，请填写本人有效身份证件信息。自2023年1月1日起，每一名持票成年人旅客可免费携带一名未满6周岁且不单独占用席位的儿童乘车，超过一名时，超过人数应购买儿童优惠票。免费儿童可以在购票成功后添加。
        </p>
        <p>
          3.
          购买残疾军人（伤残警察）优待票的，须在购票后、开车前办理换票手续方可进站乘车。换票时，不符合规定的减价优待条件，没有有效"中华人民共和国残疾军人证"或"中华人民共和国伤残人民警察证"的，不予换票，所购车票按规定办理退票手续。
        </p>
        <p>
          4.一天内3次申请车票成功后取消订单（包含无座票时取消5次计为取消1次），当日将不能在12306继续购票。
        </p>
        <p>
          <strong
            >5.购买铁路乘意险的注册用户年龄须在18周岁以上，使用非中国居民身份证注册的用户如购买铁路乘意险，须在<a
              href="../view/information.html"
              shape="rect"
              >我的12306——个人信息</a
            >
            如实填写“出生日期”。</strong
          >
        </p>
        <p>
          <strong
            >6.父母为未成年子女投保，须在<a
              href="../view/passengers.html"
              shape="rect"
              >我的乘车人</a
            >
            登记未成年子女的有效身份证件信息。</strong
          >
        </p>
        <p>7.未尽事宜详见《铁路旅客运输规程》等有关规定和车站公告。</p>
        <p name="xjky" style="display: none">
          8.
          为确保乘客在旅途中有一个安全、舒适的乘坐环境，自2020年11月17日起，<span
            style="color: red"
            >旅客不得随身携带长宽高之和大于130厘米的雪具乘车</span
          >
          。您可选择雪具快运服务，请提前1-2天选择雪具快运“门到站”或“站到站”服务，中铁快运提供雪具到站后3日免费保管，请您根据出行时间，提前咨询和办理。中铁快运客服热线：95572<br
            clear="none"
          />
        </p>
      </div>
    </div>
  </Space>
  <Modal
    :visible="state.open"
    title="请付款"
    @cancel="state.open = false"
    width="40%"
    :footer="null"
  >
    <Spin :spinning="state.loading">
      <div>
        >>应付金额：<span
          :style="{ fontSize: '20px', color: ' #dc2408', fontWeight: 'bold' }"
          >{{ totalAmount }}
        </span>
        元
      </div>
      <div :style="{ marginTop: '8px' }">
        当前余额：<span :style="{ color: '#1677ff', fontWeight: 'bold' }">{{ state.balance }}</span> 元
      </div>
      <div :style="{ marginTop: '12px' }">
        <Button type="primary" @click="() => handlePay(1)">余额支付</Button>
      </div>
      <Divider dashed></Divider>
      <div :style="{ overflow: 'hidden' }">
        <div v-for="item in BANK_LIST" class="bank3">
          <div class="bank3_5" @click="() => handlePay(item.value)">
            <img :src="item.img" :alt="item.name" />
          </div>
        </div>
      </div>
    </Spin>
  </Modal>
  <Modal
    :visible="state.isPayingOpen"
    title="网上支付提示"
    :footer="null"
    style="top: 30%"
  >
    <Row :gutter="[24, 6]">
      <Col
        :span="6"
        :style="{
          display: 'flex',
          textAlign: 'end',
          alignItems: 'center',
          justifyContent: 'end'
        }"
      >
        <Spin size="large" :spinning="true"></Spin>
      </Col>
      <Col :span="14">
        <div :style="{ fontSize: '12px' }">
          支付完成后，请不要关闭此支付验证窗口
        </div>
        <div :style="{ fontSize: '12px' }">
          支付完成后请更具您支付的情况点击下面按钮。
        </div>
      </Col>
    </Row>
    <Space
      :style="{ display: 'flex', justifyContent: 'center', marginTop: '20px' }"
    >
      <Button type="default" @click="() => router.push('/ticketList')"
        >支付遇到问题</Button
      >
      <Button type="primary" @click="() => router.push('/ticketList')"
        >支付完成</Button
      >
    </Space>
  </Modal>
  <Modal
    :visible="state.rechargeOpen"
    title="余额充值"
    @cancel="state.rechargeOpen = false"
    @ok="rechargeBalance"
    :confirm-loading="state.rechargeLoading"
  >
    <div :style="{ marginBottom: '12px' }">
      当前余额：<span :style="{ color: '#1677ff', fontWeight: 'bold' }">{{ state.balance }}</span> 元
    </div>
    <InputNumber
      v-model:value="state.rechargeAmount"
      :min="0.01"
      :precision="2"
      :step="50"
      :style="{ width: '100%' }"
      placeholder="请输入充值金额"
    />
  </Modal>
</template>

<script setup>
import {
  Card,
  Space,
  Table,
  Divider,
  Button,
  Modal,
  message,
  Spin,
  Row,
  Col,
  InputNumber
} from 'ant-design-vue'
import dayjs from 'dayjs'
import {
  fetchOrderBySn,
  fetchPay,
  fetchBalanceInfo,
  fetchBalanceRecharge,
  fetchOrderCancel,
  fetchOrderStatus,
  fetchTicketList
} from '@/service'
import { useRoute, useRouter } from 'vue-router'
import { onMounted, reactive, computed, watch, onUnmounted } from 'vue'
import Cookie from 'js-cookie'
import {
  TICKET_TYPE_LIST,
  ID_CARD_TYPE,
  BANK_LIST,
  SEAT_CLASS_TYPE_LIST
} from '@/constants'
import { getWeekNumber } from '@/utils'
let timer = undefined

const { query } = useRoute()
const router = useRouter()
const currentOrderSn = query?.orderSn || query?.sn
const state = reactive({
  count: 600000,
  currentInfo: null,
  open: false,
  html: '',
  loading: false,
  balance: 0,
  rechargeOpen: false,
  rechargeLoading: false,
  rechargeAmount: null,
  isInitiatePayment: false,
  isPaying: false,
  isPayingOpen: false
})
const columns = [
  { title: '序号', dataIndex: 'id', slots: { customRender: 'id' } },
  { title: '姓名', dataIndex: 'realName' },
  { title: '证件类型', dataIndex: 'idType', slots: { customRender: 'idType' } },
  { title: '证件号码', dataIndex: 'idCard' },
  {
    title: '票种',
    dataIndex: 'ticketType',
    slots: { customRender: 'ticketType' }
  },
  {
    title: '席别',
    dataIndex: 'seatType',
    slots: { customRender: 'seatType' }
  },
  { title: '车厢', dataIndex: 'carriageNumber' },
  { title: '席位号', dataIndex: 'seatNumber' },
  { title: '票价(元)', dataIndex: 'amount', slots: { customRender: 'amount' } }
]

onMounted(() => {
  timer = setInterval(() => {
    state.count -= 1000
    getOrderStatus()
  }, 1000)
  dayjs.duration(state.count).minutes()
  getOrder()
  loadBalance()
})
onUnmounted(() => {
  clearInterval(timer)
})

const getOrder = () => {
  fetchOrderBySn({ orderSn: currentOrderSn }).then((res) => {
    if (res.success && res.data) {
      state.currentInfo = res.data
    } else {
      fallbackGetOrder()
    }
  }).catch(() => {
    fallbackGetOrder()
  })
}

const fallbackGetOrder = () => {
  const userId = Cookie.get('userId')
  if (!userId || !currentOrderSn) {
    message.error('订单查询失败，请返回订单列表重试')
    return
  }
  const statusTypes = [0, 1, 2]
  const tryFetchByStatus = (index = 0) => {
    if (index >= statusTypes.length) {
      message.error('未查询到对应订单')
      return
    }
    fetchTicketList({
      userId,
      current: 1,
      size: 100,
      statusType: statusTypes[index]
    }).then((res) => {
      const records = res?.data?.records ?? []
      const matched = records.find((item) => item.orderSn === currentOrderSn)
      if (matched) {
        state.currentInfo = matched
      } else {
        tryFetchByStatus(index + 1)
      }
    }).catch(() => {
      tryFetchByStatus(index + 1)
    })
  }
  tryFetchByStatus()
}

watch(
  () => state.isPaying,
  (newV) => {
    state.isPayingOpen = newV
  }
)
const totalAmount = computed(() => {
  let amount = 0
  state.currentInfo?.passengerDetails?.map((item) => {
    amount += item?.amount ?? 0
  })
  return amount / 100
})

const loadBalance = () => {
  fetchBalanceInfo()
    .then((res) => {
      if (res?.success) {
        state.balance = Number(res?.data?.balance ?? 0)
      }
    })
    .catch(() => {
      state.balance = 0
    })
}

const rechargeBalance = () => {
  const amount = Number(state.rechargeAmount)
  if (!amount || amount <= 0) {
    return message.error('请输入正确的充值金额')
  }
  state.rechargeLoading = true
  fetchBalanceRecharge({ amount }).then((res) => {
    if (res?.success) {
      state.balance = Number(res?.data?.balance ?? state.balance)
      state.rechargeOpen = false
      state.rechargeAmount = null
      message.success('充值成功')
    } else {
      message.error(res?.message || '充值失败')
    }
  }).catch(() => {
    message.error('充值失败，请稍后重试')
  }).finally(() => {
    state.rechargeLoading = false
  })
}

const handlePay = (channel) => {
  if (channel !== 0 && channel !== 1) {
    return message.error('该支付方式暂未对接，请稍后')
  }
  if (channel === 1 && state.balance < totalAmount.value) {
    state.open = false
    state.rechargeOpen = true
    return message.error('余额不足，请先充值')
  }
  state.isInitiatePayment = true
  state.open = false
  state.isPayingOpen = true
  const body = {
    channel,
    tradeType: 0,
    orderSn: currentOrderSn,
    totalAmount: totalAmount.value,
    outOrderSn: currentOrderSn,
    subject: `${state.currentInfo.departure}-${state.currentInfo.arrival}`
  }
  fetchPay(body).then((res) => {
    if (channel === 1) {
      if (!res?.success) {
        state.isPayingOpen = false
        state.isInitiatePayment = false
        return message.error(res?.message || '余额支付失败')
      }
      message.success('余额支付成功')
      loadBalance()
      router.push(`/paySuccess?orderSn=${currentOrderSn}`)
      return
    }
    const payBody = res?.data?.body
    if (!res?.success || !payBody) {
      state.isPayingOpen = false
      state.isInitiatePayment = false
      message.error(res?.message || '未获取到支付宝支付表单，请检查支付服务日志')
      return
    }
    state.html = payBody
    state.loading = true
    setTimeout(() => {
      state.loading = false
      window.open(`/aliPay?body=${encodeURIComponent(payBody)}`)
    }, 500)
  }).catch(() => {
    state.isPayingOpen = false
    state.isInitiatePayment = false
    message.error('发起支付失败，请稍后重试')
  })
}

const getOrderStatus = () => {
  state.isInitiatePayment &&
    fetchOrderStatus({ orderSn: currentOrderSn })
      .then((res) => {
        state.isPaying = res.data.status === 0
        res.data.status === 20 &&
          router.push(`/paySuccess?orderSn=${res.data.orderSn}`)
      })
      .catch((error) => {
        console.log('error:::', error)
      })
}
</script>

<style lang="scss" scoped>
.tip-wrapper {
  display: flex;
  align-items: center;
  .i-lock {
    display: inline-block;
    width: 42px;
    height: 42px;
    background: url(https://kyfw.12306.cn/otn/resources/images/bg02.png)
      repeat-x;
    background-position: 0 -250px;
    margin-right: 20px;
  }
  .time {
    color: #fd6a09;
    font-weight: bolder;
  }
}

.tips-txt {
  background: #fffbe5;
  border: 1px solid #fbd800;
  padding: 5px;
  border-radius: 5px;
  -moz-border-radius: 5px;
  -ms-border-radius: 5px;
  -o-border-radius: 5px;
  -webkit-border-radius: 5px;
}
.important-text {
  font-size: 16px;
  font-weight: bolder;
}
.bank3 {
  float: left;
  width: 170px;
  padding: 8px 0;
  margin-left: 16px;
  .bank3_5 {
    float: left;
    width: 160px;
    cursor: pointer;
  }
}
.cyx-hd {
  position: relative;
  line-height: 20px;
  padding: 5px 5px 5px 65px;
  background-image: linear-gradient(45deg, #fef9ff 0%, #fff 100%);
  border: 1px solid #bfd7e3;
  .cyx-hd-label {
    position: absolute;
    width: 40px;
    top: 50%;
    left: 18px;
    margin-top: -8px;
    height: 16px;
    line-height: 16px;
    display: flex;
    justify-content: space-around;
    align-items: center;
    .icon-cyx {
      float: left;
      width: 16px;
      height: 16px;
      background-image: url(https://kyfw.12306.cn/otn/resources/images/icon-cyx.png);
    }
  }
}
::v-deep {
  .ant-table-thead {
    .ant-table-cell {
      background: #f6f6f6;
    }
  }
}
</style>
