<script setup lang="ts">
/**
 * 设置页：学习设置（考试时间、预算、额度、复习阶段）+ 科目 + 计量单位。
 */
import { ref } from 'vue'
import { ElTabPane, ElTabs } from 'element-plus'

import StudySettingsPanel from '../components/StudySettingsPanel.vue'
import SubjectPanel from '../components/SubjectPanel.vue'
import UnitPanel from '../components/UnitPanel.vue'

// ElTabs 的 modelValue 类型是 string | number
const activeTab = ref<string | number>('study')
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">设置</h2>
        <p class="page__subtitle">
          考试时间、每日学习预算、每日复习额度、复习阶段，以及科目与计量单位的维护。
        </p>
      </div>
    </div>

    <section class="section-card">
      <ElTabs v-model="activeTab">
        <ElTabPane label="学习设置" name="study">
          <StudySettingsPanel />
        </ElTabPane>
        <ElTabPane label="科目" name="subjects">
          <SubjectPanel />
        </ElTabPane>
        <ElTabPane label="计量单位" name="units">
          <UnitPanel />
        </ElTabPane>
      </ElTabs>
    </section>
  </div>
</template>
