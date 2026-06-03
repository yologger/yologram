import { Tag } from 'antd'
import styles from './FilterChips.module.css'

interface FilterChipsProps {
  items: string[]
  selected: string
  onChange: (item: string) => void
}

export default function FilterChips({ items, selected, onChange }: FilterChipsProps) {
  return (
    <div className={styles.container}>
      {items.map((item) => (
        <Tag
          key={item}
          className={`${styles.chip} ${selected === item ? styles.active : ''}`}
          onClick={() => onChange(item)}
        >
          {item}
        </Tag>
      ))}
    </div>
  )
}
