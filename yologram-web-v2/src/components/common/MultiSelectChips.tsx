'use client'

import styles from './FilterChips.module.css'
import type { ChipItem } from './FilterChips'

interface MultiSelectChipsProps<T> {
  items: Array<ChipItem<T> | string>
  selected: T[]
  onToggle: (value: T) => void
}

function normalize<T>(item: ChipItem<T> | string): ChipItem<T> {
  return typeof item === 'string' ? { label: item, value: item as T } : item
}

export default function MultiSelectChips<T>({ items, selected, onToggle }: MultiSelectChipsProps<T>) {
  return (
    <div className={styles.container}>
      {items.map((raw) => {
        const { label, value } = normalize(raw)
        return (
          <span
            key={String(value)}
            className={`${styles.chip} ${selected.includes(value) ? styles.active : ''}`}
            onClick={() => onToggle(value)}
          >
            {label}
          </span>
        )
      })}
    </div>
  )
}
