<script setup lang="ts">
/**
 * 复习页：复习清单（加入复习 + 掌握反馈）与建议确认。
 *
 * 先学习、再按实际已学内容建立复习清单；建议经确认才进入今日安排。
 */
import { ref } from 'vue'
import { ElAlert, ElTabPane, ElTabs } from 'element-plus'

import ReviewItemPanel from '../components/ReviewItemPanel.vue'
import SuggestionPanel from '../components/SuggestionPanel.vue'

// ElTabs 的 modelValue 类型是 string | number
const activeTab = ref<string | number>('items')
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">复习</h2>
        <p class="page__subtitle">
          已学内容加入复习 → 记录掌握情况（不会 / 模糊 / 掌握）→ 查看日期建议 → 确认后进入今日安排。
        </p>
      </div>
    </div>

    <ElAlert
      type="info"
      show-icon
      :closable="false"
      title="不预先生成整套复习任务，也不擅自提高强度"
      description="超额内容进入待安排列表，保留到期信息；缺少预计用时或未设置额度时，界面不会宣称建议符合额度。"
    />

    <section class="section-card">
      <ElTabs v-model="activeTab">
        <ElTabPane label="复习清单" name="items">
          <ReviewItemPanel />
        </ElTabPane>
        <ElTabPane label="建议与确认" name="suggestions">
          <SuggestionPanel />
        </ElTabPane>
      </ElTabs>
    </section>
  </div>
</template>
