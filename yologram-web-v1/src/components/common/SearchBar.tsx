import { useState } from 'react'
import { Button, Input } from 'antd'
import { ArrowLeftOutlined, SearchOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router'
import useIsMobile from '../../hooks/useIsMobile'
import styles from './SearchBar.module.css'

interface SearchBarProps {
  /** 섹션 기본 경로 (예: "/tech") */
  basePath: string
  /** 키워드 페이지에서 현재 키워드 표시용 초기값 */
  initialValue?: string
}

export default function SearchBar({ basePath, initialValue = '' }: SearchBarProps) {
  const navigate = useNavigate()
  const isMobile = useIsMobile()
  const [value, setValue] = useState(initialValue)
  const [overlayOpen, setOverlayOpen] = useState(false)

  // Enter 시 검색: 공백 제거 후 비어 있으면 무시, 값이 있으면 키워드 페이지로 이동
  const submit = () => {
    const keyword = value.trim()
    if (!keyword) return
    setOverlayOpen(false)
    navigate(`${basePath}/keywords/${encodeURIComponent(keyword)}`)
  }

  // 오버레이 닫기: 입력값은 초기값으로 되돌림
  const closeOverlay = () => {
    setOverlayOpen(false)
    setValue(initialValue)
  }

  // 데스크탑: 인라인 검색바
  if (!isMobile) {
    return (
      <Input
        className={styles.desktopInput}
        prefix={<SearchOutlined />}
        placeholder="검색어를 입력하세요"
        allowClear
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onPressEnter={submit}
      />
    )
  }

  // 모바일: 돋보기 아이콘 버튼 → 탭하면 화면 상단 검색 오버레이
  return (
    <>
      <Button
        type="text"
        aria-label="검색"
        icon={<SearchOutlined />}
        onClick={() => setOverlayOpen(true)}
      />
      {overlayOpen && (
        <div className={styles.overlay}>
          <div className={styles.overlayBar}>
            <Button
              type="text"
              aria-label="뒤로"
              icon={<ArrowLeftOutlined />}
              onClick={closeOverlay}
            />
            <Input
              autoFocus
              className={styles.overlayInput}
              prefix={<SearchOutlined />}
              placeholder="검색어를 입력하세요"
              allowClear
              value={value}
              onChange={(e) => setValue(e.target.value)}
              onPressEnter={submit}
              onKeyDown={(e) => {
                if (e.key === 'Escape') closeOverlay()
              }}
            />
          </div>
        </div>
      )}
    </>
  )
}
