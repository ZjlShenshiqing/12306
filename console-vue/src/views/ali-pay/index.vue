<template>
  <div v-if="decodedBody" v-html="decodedBody"></div>
  <div v-else>支付参数缺失，请返回订单页重新发起支付。</div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'

const { query } = useRoute()
const decodedBody = computed(() => {
  if (typeof query.body !== 'string' || !query.body.trim()) {
    return ''
  }
  try {
    return decodeURIComponent(query.body)
  } catch (error) {
    return ''
  }
})

onMounted(() => {
  if (!decodedBody.value) {
    return
  }
  setTimeout(() => {
    document.forms[0]?.submit()
  }, 100)
})
// const state = reactive({
//   body: ''
// })

// onMounted(() => {
//   //   const profile = Vue.extend({
//   //     template
//   //   })
//   //   state.body = query
// })
</script>

<style lang="scss" scoped></style>
