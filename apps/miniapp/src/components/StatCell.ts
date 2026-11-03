import { defineComponent, h, type CSSProperties } from 'vue';
import { AppCard } from './ui';
import Icon from './Icon.vue';

/** Стат-ячейка сетки 2×2 дашборда (DashboardScreens.StatCell): зелёная иконка, число 30px, метка. */
export const StatCell = defineComponent({
  name: 'StatCell',
  props: {
    icon: { type: String, required: true },
    value: { type: String, required: true },
    label: { type: String, required: true },
    mono: { type: Boolean, default: false },
  },
  setup(props) {
    return () =>
      h(
        AppCard,
        { style: { height: '150px' } as CSSProperties },
        {
          default: () =>
            h(
              'div',
              {
                style: {
                  height: '100%',
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: 'center',
                  justifyContent: 'center',
                  padding: '12px',
                } as CSSProperties,
              },
              [
                h(Icon, { name: props.icon, size: 30, style: { color: 'var(--accent)' } as CSSProperties }),
                h('div', { style: { height: '10px' } as CSSProperties }),
                h(
                  'span',
                  { style: { fontSize: '30px', fontWeight: 700, color: 'var(--text)' } as CSSProperties },
                  props.value,
                ),
                h('div', { style: { height: '8px' } as CSSProperties }),
                h(
                  'span',
                  {
                    class: props.mono ? 'mono-caption' : '',
                    style: {
                      fontSize: props.mono ? '12px' : '14px',
                      color: 'var(--text-secondary)',
                      letterSpacing: props.mono ? '2px' : undefined,
                    } as CSSProperties,
                  },
                  props.label,
                ),
              ],
            ),
        },
      );
  },
});
