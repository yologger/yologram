'use client'

import { useEffect, useState } from 'react'
import { useRouter } from 'next/navigation'
import { Button, Input } from 'antd'
import { ArrowLeftOutlined, SearchOutlined } from '@ant-design/icons'
import useIsMobile from '@/hooks/useIsMobile'
import styles from './SearchBar.module.css'

interface SearchBarProps {
  basePath: string
  initialValue?: string
}

// 섹션 공용 검색바
// - 데스크탑: 타이틀과 탭 사이 인라인 검색바
// - 모바일: 돋보기 아이콘 버튼(타이틀 행 오른쪽 상단) → 화면 상단 검색 오버레이
export default function SearchBar({ basePath, initialValue = '' }: SearchBarProps) {
  const router = useRouter()
  const isMobile = useIsMobile()
  const [value, setValue] = useState(initialValue)
  const [overlayOpen, setOverlayOpen] = useState(false)

  // ESC로 모바일 오버레이 닫기
  useEffect(() => {
    if (!overlayOpen) return
    const handler = (e: KeyboardEvent) => {
      if (e.key === 'Escape') setOverlayOpen(false)
    }
    document.addEventListener('keydown', handler)
    return () => document.removeEventListener('keydown', handler)
  }, [overlayOpen])

  // 빈 값(공백만 포함)은 무시, 한글 등은 인코딩해 키워드 경로로 이동
  const search = () => {
    const keyword = value.trim()
    if (!keyword) return
    setOverlayOpen(false)
    router.push(`${basePath}/keywords/${encodeURIComponent(keyword)}`)
  }

  // 뷰포트 판별 전(최초 렌더)에는 그리지 않는다 — ResponsiveLayout과 동일 패턴
  if (isMobile === null) return null

  if (!isMobile) {
    return (
      <Input
        className={styles.inline}
        prefix={<SearchOutlined />}
        placeholder="검색어를 입력하세요"
        allowClear
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onPressEnter={search}
      />
    )
  }

  return (
    <>
      <Button
        type="text"
        className={styles.trigger}
        aria-label="검색"
        icon={<SearchOutlined />}
        onClick={() => setOverlayOpen(true)}
      />
      {overlayOpen && (
        <div className={styles.overlay}>
          <Button
            type="text"
            aria-label="뒤로"
            icon={<ArrowLeftOutlined />}
            onClick={() => setOverlayOpen(false)}
          />
          <Input
            autoFocus
            className={styles.overlayInput}
            prefix={<SearchOutlined />}
            placeholder="검색어를 입력하세요"
            allowClear
            value={value}
            onChange={(e) => setValue(e.target.value)}
            onPressEnter={search}
          />
        </div>
      )}
    </>
  )
}
