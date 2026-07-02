<template>
  <div class="student-row" @click.stop>
    <el-checkbox
      :model-value="isGroupAllSelected(group, type)"
      :indeterminate="isGroupIndeterminate(group, type)"
      @change="$emit('toggle-group', group, type)"
      class="student-row-checkbox"
    />
    <div class="student-row-body">
      <span class="student-row-name">{{ group.studentName }}</span>
      <div class="student-row-parents">
        <label
          v-for="parent in group.parents"
          :key="parent.treeKey"
          class="parent-tag"
          :class="{ 'is-selected': isItemSelected(parent, type) }"
          @click.stop="$emit('toggle-parent', parent, type)"
        >
          <el-checkbox
            :model-value="isItemSelected(parent, type)"
            @change="$emit('toggle-parent', parent, type)"
            @click.stop
            class="parent-tag-checkbox"
          />
          <span class="parent-tag-label">{{ getRelationLabel(parent.name) }}</span>
        </label>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StudentTreeNode',
  props: {
    group: { type: Object, required: true },
    type: { type: Number, required: true },
    isItemSelected: { type: Function, required: true },
    isGroupAllSelected: { type: Function, required: true },
    isGroupIndeterminate: { type: Function, required: true },
    getRelationLabel: { type: Function, required: true }
  },
  emits: ['toggle-group', 'toggle-parent']
}
</script>

<style scoped>
.student-row {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 8px 10px;
  margin: 2px 0;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px solid #edf0f4;
  transition: border-color 0.2s, background 0.2s;
}

.student-row:hover {
  background: #f1f5f9;
  border-color: #cbd5e1;
}

.student-row-checkbox {
  flex-shrink: 0;
  margin: 0;
}

.student-row-body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px 12px;
}

.student-row-name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
  flex-shrink: 0;
  line-height: 1;
}

.student-row-parents {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.parent-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 5px 12px;
  min-height: 28px;
  box-sizing: border-box;
  border-radius: 999px;
  background: #fff;
  border: 1px solid #e2e8f0;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  user-select: none;
  margin: 0;
  line-height: 1;
  transition: all 0.15s;
}

.parent-tag-checkbox {
  margin: 0;
  height: 14px;
  --el-checkbox-height: 14px;
  display: inline-flex;
  align-items: center;
}

.parent-tag-checkbox :deep(.el-checkbox__inner) {
  width: 14px;
  height: 14px;
}

.parent-tag-checkbox :deep(.el-checkbox__input) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
  vertical-align: middle;
}

.parent-tag-label {
  display: inline-flex;
  align-items: center;
  line-height: 1;
  padding-top: 1px;
}

.parent-tag:hover {
  border-color: #93c5fd;
  color: #2563eb;
}

.parent-tag.is-selected {
  background: #eff6ff;
  border-color: #3b82f6;
  color: #1d4ed8;
  font-weight: 500;
}
</style>
