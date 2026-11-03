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
 * Original app icons: official Material Symbols outlined (the same glyphs as
 * Icons.Outlined.* in the clone and the original's MaterialIcons font).
 * Paths come from @material-symbols/svg-400 (see materialIcons.ts).
 */
const props = withDefaults(defineProps<{ name: string; size?: number }>(), { size: 22 });

const icon = computed(() => MATERIAL_ICONS[props.name] ?? MATERIAL_ICONS.info);
</script>
