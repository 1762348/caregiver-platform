<template>
  <div :class="isAdmin ? 'admin-shell' : 'app-shell'">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

// 管理后台使用全宽布局，其余端保持 480px 手机壳层
const route = useRoute()
const isAdmin = computed(() => route.path.startsWith('/admin'))
</script>
