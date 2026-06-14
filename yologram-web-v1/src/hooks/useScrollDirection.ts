import { useEffect, useState } from 'react'

/**
 * window 스크롤 방향을 감지한다.
 * 'up' | 'down'. 최상단 근처(threshold 이하)에서는 항상 'up'으로 간주해 헤더가 보이도록 한다.
 */
export default function useScrollDirection(threshold = 8): 'up' | 'down' {
  const [direction, setDirection] = useState<'up' | 'down'>('up')

  useEffect(() => {
    let lastY = window.scrollY

    const onScroll = () => {
      const currentY = window.scrollY
      const diff = currentY - lastY

      if (currentY <= 0) {
        setDirection('up')
      } else if (Math.abs(diff) >= threshold) {
        setDirection(diff > 0 ? 'down' : 'up')
      }

      lastY = currentY
    }

    window.addEventListener('scroll', onScroll, { passive: true })
    return () => window.removeEventListener('scroll', onScroll)
  }, [threshold])

  return direction
}
