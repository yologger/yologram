'use client'

import useIsMobile from '../../hooks/useIsMobile'
import MobileTabBar from './MobileTabBar'
import DesktopSidebar from './DesktopSidebar'
import styles from './ResponsiveLayout.module.css'

export default function ResponsiveLayout({ children }: { children: React.ReactNode }) {
  const isMobile = useIsMobile()

  if (isMobile === null) return null

  return (
    <div className={styles.layout}>
      {!isMobile && <DesktopSidebar />}
      <main className={`${styles.content} ${isMobile ? styles.mobile : styles.desktop}`}>
        <div className={styles.inner}>
          {children}
        </div>
      </main>
      {isMobile && <MobileTabBar />}
    </div>
  )
}
