<script setup lang="ts">
withDefaults(defineProps<{ name: string; size?: number }>(), { size: 20 })
const paths: Record<string, string[]> = {
  today: ['M3 10 12 3l9 7', 'M5 9v12h5v-7h4v7h5V9'],
  plan: ['M8 2v4m8-4v4M3 10h18', 'M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2', 'M7 14h3m4 0h3m-10 4h3'],
  learning: ['M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8Z', 'M14 2v6h6M8 12h8m-8 4h6'],
  progress: ['M4 20V10m8 10V4m8 16v-7M2 22h20'],
  review: ['M12 5v16', 'M3 3h5a4 4 0 0 1 4 2 4 4 0 0 1 4-2h5v16h-5a4 4 0 0 0-4 2 4 4 0 0 0-4-2H3Z'],
  memo: ['M4 3h16v15l-4 4H4ZM8 8h8m-8 4h8m-8 4h4'],
  targets: ['M20 11a8 8 0 1 1-7-7M16 12a4 4 0 1 1-4-4m0 4 9-9m-4 0h4v4'],
  settings: ['M9 3h6l1 3 3 1 2 5-2 5-3 1-1 3H9l-1-3-3-1-2-5 2-5 3-1Z', 'M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0'],
  more: ['M5 12h.01M12 12h.01M19 12h.01'],
  list: ['M4 6h.01M8 6h12M4 12h.01M8 12h12M4 18h.01M8 18h12'],
  check: ['m5 12 4 4L19 6'],
  partial: ['m9 5 10 7-10 7Z'],
  pending: ['M5 12h.01M12 12h.01M19 12h.01'],
  refresh: ['M20 7v5h-5M4 17v-5h5M6 7a7 7 0 0 1 11-1l3 6M4 12l3 6a7 7 0 0 0 11-1'],
  edit: ['m16 3 5 5-12 12-6 1 1-6Zm-3 3 5 5'],
  logout: ['M9 4H4v16h5M10 12h11m-4-4 4 4-4 4'],
  math: ['M5 2h14v20H5ZM8 6h8m-8 5h1m3 0h1m3 0h.01M8 15h1m3 0h1m3 0h.01M8 19h1m3 0h1m3 0h.01'],
  english: ['M12 5v16M3 3h5a4 4 0 0 1 4 2 4 4 0 0 1 4-2h5v16h-5a4 4 0 0 0-4 2 4 4 0 0 0-4-2H3Z'],
  politics: ['M12 3 3 8l9 5 9-5ZM5 12v7m7-3v5m7-9v7M3 22h18'],
  computer: ['M3 4h18v13H3ZM8 22h8m-4-5v5m-3-14-3 3 3 3m6-6 3 3-3 3']
}
</script>
<template>
  <svg class="app-icon" :width="size" :height="size" viewBox="0 0 24 24" fill="none" stroke="currentColor" :stroke-width="name === 'more' || name === 'pending' ? 3.5 : 1.65" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true" focusable="false"><path v-for="(path, index) in paths[name] || paths.memo" :key="index" :d="path" /></svg>
</template>
<style scoped>.app-icon { display:inline-block; flex-shrink:0; vertical-align:middle; }</style>
