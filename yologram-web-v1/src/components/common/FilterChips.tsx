import styles from './FilterChips.module.css'

export type ChipItem<T> = { label: string; value: T }

interface FilterChipsProps<T> {
  items: Array<ChipItem<T> | string>
  selected: T
  onChange: (value: T) => void
}

function normalize<T>(item: ChipItem<T> | string): ChipItem<T> {
  return typeof item === 'string' ? { label: item, value: item as T } : item
}

export default function FilterChips<T>({ items, selected, onChange }: FilterChipsProps<T>) {
  return (
    <div className={styles.container}>
      {items.map((raw) => {
        const { label, value } = normalize(raw)
        return (
          <span
            key={String(value)}
            className={`${styles.chip} ${selected === value ? styles.active : ''}`}
            onClick={() => onChange(value)}
          >
            {label}
          </span>
        )
      })}
    </div>
  )
}
