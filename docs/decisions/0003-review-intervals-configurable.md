# 0003 复习间隔与额度保持可配置，不写死默认值

日期：2026-09-22
状态：已采纳

## 背景

`PLAN.md` 与 `AGENTS.md` 都明确要求：每日学习预算、复习额度与**具体复习间隔**属于待定事项，
不能擅自固定为产品规则。同时 PLAN 又要求“复习后记录不会/模糊/掌握，据此建议下次日期”。

这两条要求需要一个折中：系统要能给出建议，但间隔参数必须由本人确定。

## 决定

- 三个档位的间隔天数存放在 `study_settings`（`review_interval_{forgot,vague,mastered}_days`），
  三列**允许为空**。
- `daily_review_minutes`（每日复习额度）同样允许为空。
- 未配置间隔时，提交复习反馈仍然正常记录，但 `nextReviewDate` 返回 `null`，
  并把 `intervalConfigured=false` 明确告诉前端；系统不编造日期。
- 未配置复习额度时，`/api/review/suggestions` 的 `dailyReviewMinutes` 与 `remainingMinutes`
  为 null，并通过 `warnings` 明确说明；此时 `/api/review/suggestions/confirm` 返回
  `REVIEW_QUOTA_REQUIRED` 而不是假装安排成功。
- 单条内容缺少预计用时时，确认安排会把它列入 `rejected` 并要求补充，
  而不是按 0 分钟计入额度。

## 理由

“按 0 处理”或“取一个常见默认值（如 1/3/7 天）”都会让页面给出看似合理、实则没有依据的结论，
与 PLAN 中“缺少…时不宣称满足额度”的要求冲突。返回空值 + 明确提示，才能让本人先补参数。

## 影响

- 首次使用复习功能前需要在设置页填写间隔与额度。
- 前端必须区分“未设置（null）”与“0”，不能把空值渲染成 0。
- 相关行为由 `ReviewFlowIntegrationTest` 覆盖（`confirmRequiresConfiguredQuota`、
  `missingEstimateMustBeSupplied`、`nextDateFollowsConfiguredInterval`）。
