import { useEffect, useState } from 'react'
import { UpOutlined } from '@ant-design/icons'
import styles from './ScrollToTopButton.module.css'

/**
 * 일정 이상 스크롤하면 우하단에 나타나는 "맨 위로" 버튼.
 */
export default function ScrollToTopButton({ threshold = 300 }: { threshold?: number }) {
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    const onScroll = () => setVisible(window.scrollY > threshold)
    onScroll()
    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold])

  if (!visible) return null

  return (
    <button
      type="button"
      className={styles.button}
      aria-label="맨 위로"
      onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}
    >
      <UpOutlined />
    </button>
  )
}
