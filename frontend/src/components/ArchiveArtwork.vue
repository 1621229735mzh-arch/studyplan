<script setup lang="ts">
withDefaults(defineProps<{ variant?: 'rail' | 'hero' }>(), { variant: 'rail' })
</script>
<template><div class="archive-artwork" :class="`archive-artwork--${variant}`" aria-hidden="true" /></template>
<style scoped>
.archive-artwork { pointer-events:none; background-repeat:no-repeat; }
.archive-artwork--rail { height:100%; background-image:linear-gradient(90deg,rgba(18,15,26,.24),transparent 22%),url('../assets/theme/archive-study.webp'); background-size:cover; background-position:center top; }
.archive-artwork--hero { background-image:linear-gradient(0deg,var(--kaoyan-bg),transparent 34%),url('../assets/theme/archive-hero.webp'); background-size:cover; background-position:center 42%; }
</style>
