<script setup lang="ts">
import { ref } from 'vue'

defineProps<{
  id: string
  caption: string
  columns: Array<{ key: string; label: string; numeric?: boolean }>
  rows: Array<Record<string, string | number>>
}>()

const visible = ref(false)
</script>

<template>
  <div class="chart-data-table">
    <button
      type="button"
      class="table-toggle"
      :aria-expanded="visible"
      :aria-controls="id"
      @click="visible = !visible"
    >
      {{ visible ? '表を隠す' : '表で見る' }}
    </button>
    <div :class="{ 'visually-hidden': !visible, 'table-scroll': visible }">
      <table :id="id">
        <caption>
          {{
            caption
          }}
        </caption>
        <thead>
          <tr>
            <th
              v-for="column in columns"
              :key="column.key"
              scope="col"
              :class="{ numeric: column.numeric }"
            >
              {{ column.label }}
            </th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="(row, rowIndex) in rows" :key="rowIndex">
            <td v-for="column in columns" :key="column.key" :class="{ numeric: column.numeric }">
              {{ row[column.key] }}
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<style scoped>
.chart-data-table {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 0.55em;
}

.table-toggle {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--color-accent);
  cursor: pointer;
  font: inherit;
  font-size: 0.85rem;
  font-weight: 600;
  text-decoration: underline;
  text-underline-offset: 0.2em;
}

.table-toggle:focus-visible {
  outline: 2px solid var(--color-focus);
  outline-offset: 2px;
}

.table-scroll {
  width: 100%;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  color: var(--color-text);
  font-size: 0.82rem;
}

caption {
  padding-bottom: 0.45em;
  color: var(--color-text-muted);
  text-align: left;
}

th,
td {
  padding: 0.45em 0.55em;
  border-bottom: 1px solid var(--color-surface-muted);
  text-align: left;
  white-space: nowrap;
}

.numeric {
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0 0 0 0);
  white-space: nowrap;
  border: 0;
}
</style>
