<template>
  <svg
    :width="size"
    :height="size"
    :viewBox="icon.vb"
    fill="currentColor"
    stroke="none"
    v-html="icon.body"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { MATERIAL_ICONS } from './materialIcons';

/**
 * Иконки оригинала: официальные Material Symbols outlined (те же глифы, что
 * Icons.Outlined.* в клоне и MaterialIcons-шрифт оригинала).
 * Пути — из @material-symbols/svg-400 (см. materialIcons.ts).
 */
const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 22 });

const icon = computed(() => MATERIAL_ICONS[props.name] ?? MATERIAL_ICONS.info);
</script>
