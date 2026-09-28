<script setup lang="ts">
/**
 * 计划页：阶段目标 → 任务 → 周计划 → 每日安排。
 *
 * 明确提示：未完成任务不会自动顺延；计划只做安排，完成量由学习记录决定。
 */
import { ref } from 'vue'
import { ElAlert, ElTabPane, ElTabs } from 'element-plus'

import DayPlanPanel from '../components/DayPlanPanel.vue'
import StageGoalPanel from '../components/StageGoalPanel.vue'
import TaskPanel from '../components/TaskPanel.vue'
import WeekPlanPanel from '../components/WeekPlanPanel.vue'

// ElTabs 的 modelValue 类型是 string | number，因此这里用联合类型接收，避免类型不兼容
const activeTab = ref<string | number>('tasks')
</script>

<template>
  <div class="page">
    <div class="page__header">
      <div>
        <h2 class="page__title">计划管理</h2>
        <p class="page__subtitle">
          阶段目标 → 任务 → 周计划 → 每日安排。任务、周计划与今日安排引用同一个任务，避免重复统计。
        </p>
      </div>
    </div>

    <ElAlert
      type="warning"
      show-icon
      :closable="false"
      title="未完成任务不会自动顺延"
      description="当天或当周没做完的任务会保持原状，需要本人决定是继续安排到后面、调整计划量还是放弃。"
    />

    <section class="section-card">
      <ElTabs v-model="activeTab">
        <ElTabPane label="任务" name="tasks">
          <TaskPanel />
        </ElTabPane>
        <ElTabPane label="周计划" name="week">
          <WeekPlanPanel />
        </ElTabPane>
        <ElTabPane label="每日安排" name="day">
          <DayPlanPanel />
        </ElTabPane>
        <ElTabPane label="阶段目标" name="goals">
          <StageGoalPanel />
        </ElTabPane>
      </ElTabs>
    </section>
  </div>
</template>
