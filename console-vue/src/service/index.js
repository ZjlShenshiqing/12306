import http from './axios'

const fetchLogin = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/v1/login',
    data: body
  })
  http.defaults.headers.common['Authorization'] = data.data?.accessToken
  return data
}

const fetchRegister = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/register',
    data: body
  })
  return data
}

const fetchTicketSearch = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/ticket-service/ticket/query',
    params
  })
  return data
}

const fetchRegionStation = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/ticket-service/region-station/query',
    params
  })
  return data
}

const fetchPassengerList = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/user-service/passenger/query',
    params
  })
  return data
}
const fetchDeletePassenger = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/passenger/remove',
    data: body
  })
  return data
}

const fetchAddPassenger = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/passenger/save',
    data: body
  })
  return data
}

const fetchEditPassenger = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/passenger/update',
    data: body
  })
  return data
}
const fetchLogout = async (tokenOrBody) => {
  const accessToken = typeof tokenOrBody === 'string' ? tokenOrBody : tokenOrBody?.accessToken
  const { data } = await http({
    method: 'GET',
    url: '/api/user-service/logout',
    params: { accessToken }
  })
  http.defaults.headers.common['Authorization'] = null
  return data
}

const fetchBuyTicket = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/ticket-service/ticket/purchase/v2',
    data: body
  })

  return data
}

const fetchOrderBySn = async (params) => {
  const { data } = await http({
    method: 'GET',
    // 订单详情页需要按订单号查询订单及乘客明细；params 可含 orderSn/sn/userId
    url: '/api/order-service/order/item/ticket/query',
    params
  })
  return data
}

const fetchPay = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/pay-service/pay/create',
    data: body
  })
  return data
}

const fetchBalanceInfo = async () => {
  const { data } = await http({
    method: 'GET',
    url: '/api/pay-service/balance/info'
  })
  return data
}

const fetchBalanceRecharge = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/pay-service/balance/recharge',
    data: body
  })
  return data
}

const fetchStationAll = async () => {
  const { data } = await http({
    method: 'GET',
    url: '/api/ticket-service/station/all'
  })
  return data
}

const fechUserInfo = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/user-service/query',
    params
  })
  return data
}

const fetchTrainStation = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/ticket-service/train-station/query',
    params
  })
  return data
}

const fetchTicketList = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/order-service/order/ticket/page',
    params
  })
  return data
}

const fetchOrderCancel = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/ticket-service/ticket/cancel',
    data: body
  })
  return data
}

const fetchUserUpdate = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/user-service/update',
    data: body
  })
  return data
}

const fetchOrderStatus = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/pay-service/pay/query/order-sn',
    params
  })
  return data
}

const fetchMyTicket = async (params) => {
  const { data } = await http({
    method: 'GET',
    url: '/api/order-service/order/ticket/self/page',
    params
  })
  return data
}

const fetchRefundTicket = async (body) => {
  const { data } = await http({
    method: 'POST',
    url: '/api/ticket-service/ticket/refund',
    data: body
  })
}

export {
  fetchLogin,
  fetchRegister,
  fetchTicketSearch,
  fetchRegionStation,
  fetchPassengerList,
  fetchDeletePassenger,
  fetchAddPassenger,
  fetchEditPassenger,
  fetchLogout,
  fetchBuyTicket,
  fetchOrderBySn,
  fetchPay,
  fetchBalanceInfo,
  fetchBalanceRecharge,
  fetchStationAll,
  fechUserInfo,
  fetchTrainStation,
  fetchTicketList,
  fetchOrderCancel,
  fetchOrderStatus,
  fetchUserUpdate,
  fetchMyTicket,
  fetchRefundTicket
}
