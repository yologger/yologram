import styles from './FilterChips.module.css'

interface MultiSelectChipsProps {
  items: string[]
  selected: string[]
  onToggle: (item: string) => void
}

export default function MultiSelectChips({ items, selected, onToggle }: MultiSelectChipsProps) {
  return (
    <div className={styles.container}>
      {items.map((item) => (
        <span
          key={item}
          className={`${styles.chip} ${selected.includes(item) ? styles.active : ''}`}
          onClick={() => onToggle(item)}
        >
          {item}
        </span>
      ))}
    </div>
  )
}
