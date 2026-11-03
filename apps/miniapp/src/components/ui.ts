import { defineComponent, h, type CSSProperties, type PropType, type VNodeChild } from 'vue';
import Icon from './Icon.vue';

/**
 * UI primitives of the clone — port of `tennis-ai-android/ui/common/Ui.kt` + `ui/theme/Theme.kt`.
 * Styles come from styles.css classes; colors are passed via inline CSS variables.
 */

/** Text with neon glow ("AXEL AI", "SELECT DASHBOARD", "My Profile"). */
export const GlowText = defineComponent({
  name: 'GlowText',
  props: {
    size: { type: Number, default: 24 },
    text: { type: String, required: true },
  },
  setup(props) {
    return () =>
      h(
        'div',
        {
          class: 'serif-display glow',
          style: { fontSize: `${props.size}px` } satisfies CSSProperties,
        },
        props.text,
      );
  },
});

/** Section heading: green bar + white bold ("Recent Activity", "Settings"). */
export const SectionBar = defineComponent({
  name: 'SectionBar',
  props: { title: { type: String, required: true } },
  setup(props) {
    return () => h('div', { class: 'section-bar' }, h('span', props.title));
  },
});

/** Monospace caption ("TRAINING MODULES", "no_data", label under the module line). */
export const MonoCaption = defineComponent({
  name: 'MonoCaption',
  props: {
    size: { type: Number, default: 12 },
    color: { type: String, default: 'var(--text-secondary)' },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'span',
        {
          class: 'mono-caption',
          style: { fontSize: `${props.size}px`, color: props.color } satisfies CSSProperties,
        },
        slots.default?.(),
      );
  },
});

export type TopBarVariant = 'serif' | 'mono-accent' | 'mono-white';

/** Top bar: Bar background + hairline, center — icon+title, left/right — slots. */
export const TopBar = defineComponent({
  name: 'TopBar',
  props: {
    title: { type: String, required: true },
    icon: { type: String, default: '' },
    variant: { type: String as PropType<TopBarVariant>, default: 'mono-accent' },
    titleSize: { type: Number, default: 16 },
  },
  setup(props, { slots }) {
    return () => {
      const center: VNodeChild[] = [];
      if (props.icon) center.push(h(Icon, { name: props.icon, size: 20, style: 'color:var(--accent)' }));
      const titleStyle: CSSProperties =
        props.variant === 'serif'
          ? { fontSize: `${props.titleSize}px`, color: 'var(--accent)' }
          : {
              fontFamily: 'var(--mono)',
              fontWeight: 600,
              letterSpacing: props.variant === 'mono-white' ? '2px' : '3px',
              fontSize: `${props.titleSize}px`,
              color: props.variant === 'mono-white' ? 'var(--text)' : 'var(--accent)',
            };
      center.push(
        h(
          'span',
          { class: props.variant === 'serif' ? 'serif-display glow' : '', style: titleStyle },
          props.title,
        ),
      );
      return h('div', { class: 'top-bar' }, [
        h('div', { class: 'side left' }, slots.left?.()),
        h('div', { class: 'row', style: { gap: '8px' } as CSSProperties }, center),
        h('div', { class: 'side right' }, slots.right?.()),
      ]);
    };
  },
});

/** Clone card: dark fill + thin border + radius. */
export const AppCard = defineComponent({
  name: 'AppCard',
  props: {
    accent: { type: Boolean, default: false },
    radius: { type: Number, default: 18 },
    borderColor: { type: String, default: '' },
    background: { type: String, default: '' },
    onClick: { type: Function as PropType<() => void>, default: undefined },
  },
  setup(props, { slots }) {
    return () => {
      const style: CSSProperties = { borderRadius: `${props.radius}px` };
      if (props.borderColor) style.borderColor = props.borderColor;
      if (props.background) style.background = props.background;
      return h(
        'div',
        {
          class: ['card', props.accent ? 'accent' : '', props.onClick ? 'clickable' : ''],
          style,
          onClick: props.onClick,
        },
        slots.default?.(),
      );
    };
  },
});

/** Colored icon tile (Home modules, Analyses/Videos cards, Ambassador). */
export const IconTile = defineComponent({
  name: 'IconTile',
  props: {
    icon: { type: String, required: true },
    color: { type: String, required: true },
    size: { type: Number, default: 52 },
    radius: { type: Number, default: 14 },
    iconScale: { type: Number, default: 0.5 },
  },
  setup(props) {
    return () =>
      h(
        'div',
        {
          class: 'icon-tile',
          style: {
            '--tile-color': props.color,
            width: `${props.size}px`,
            height: `${props.size}px`,
            borderRadius: `${props.radius}px`,
          } satisfies CSSProperties,
        },
        h(Icon, { name: props.icon, size: Math.round(props.size * props.iconScale) }),
      );
  },
});

/** Percent ring (Home modules): gray track + colored arc + "N%" in the center. */
export const RingPercent = defineComponent({
  name: 'RingPercent',
  props: {
    percent: { type: Number, default: 0 },
    color: { type: String, required: true },
    size: { type: Number, default: 44 },
  },
  setup(props) {
    return () => {
      const r = 50 - 3; // viewBox 100, stroke 6
      const c = 2 * Math.PI * r;
      const filled = (Math.min(100, Math.max(0, props.percent)) / 100) * c;
      return h(
        'div',
        {
          style: {
            position: 'relative',
            width: `${props.size}px`,
            height: `${props.size}px`,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            flex: 'none',
          } satisfies CSSProperties,
        },
        [
          h('svg', { width: props.size, height: props.size, viewBox: '0 0 100 100' }, [
            h('circle', {
              cx: 50,
              cy: 50,
              r,
              fill: 'none',
              stroke: 'var(--track)',
              'stroke-width': 6,
            }),
            props.percent > 0
              ? h('circle', {
                  cx: 50,
                  cy: 50,
                  r,
                  fill: 'none',
                  stroke: props.color,
                  'stroke-width': 6,
                  'stroke-linecap': 'round',
                  'stroke-dasharray': `${filled} ${c - filled}`,
                  transform: 'rotate(-90 50 50)',
                })
              : null,
          ]),
          h(
            'span',
            {
              style: {
                position: 'absolute',
                fontSize: `${Math.max(10, Math.round(props.size * 0.27))}px`,
                fontWeight: 600,
                color: props.color,
              } satisfies CSSProperties,
            },
            `${props.percent}%`,
          ),
        ],
      );
    };
  },
});

/** Axel FAB: circle with a green border and a "sparkles" icon. */
export const AxelFab = defineComponent({
  name: 'AxelFab',
  emits: ['click'],
  setup(_, { emit }) {
    return () =>
      h(
        'div',
        {
          class: 'fab-axel',
          onClick: () => emit('click'),
        },
        h(Icon, { name: 'sparkles', size: 24 }),
      );
  },
});

/** Pill badge ("✓ 0", "🕐 0") — thin colored border. */
export const PillBadge = defineComponent({
  name: 'PillBadge',
  props: {
    color: { type: String, required: true },
    icon: { type: String, default: '' },
  },
  setup(props, { slots }) {
    return () =>
      h(
        'span',
        {
          class: 'pill-badge',
          style: { '--badge-color': props.color } satisfies CSSProperties,
        },
        [
          props.icon ? h(Icon, { name: props.icon, size: 13 }) : null,
          h('span', slots.default?.()),
        ],
      );
  },
});

/** Settings row with a toggle. */
export const ToggleRow = defineComponent({
  name: 'ToggleRow',
  props: {
    icon: { type: String, required: true },
    label: { type: String, required: true },
    modelValue: { type: Boolean, required: true },
  },
  emits: ['update:modelValue'],
  setup(props, { emit }) {
    return () =>
      h(
        AppCard,
        { radius: 16, style: { padding: '0' } as CSSProperties },
        {
          default: () =>
            h(
              'div',
              {
                class: 'row',
                style: {
                  padding: '6px 16px',
                  height: '52px',
                  gap: '14px',
                } satisfies CSSProperties,
              },
              [
                h(Icon, { name: props.icon, size: 22, style: 'color:var(--text)' }),
                h('span', { class: 'grow', style: { fontSize: '16px' } as CSSProperties }, props.label),
                h('div', {
                  class: ['toggle', props.modelValue ? 'on' : ''],
                  onClick: () => emit('update:modelValue', !props.modelValue),
                }),
              ],
            ),
        },
      );
  },
});
