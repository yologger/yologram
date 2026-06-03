import { Outlet } from 'react-router'
import useIsMobile from '../../hooks/useIsMobile'
import MobileTabBar from './MobileTabBar'
import DesktopSidebar from './DesktopSidebar'
import styles from './ResponsiveLayout.module.css'

export default function ResponsiveLayout() {
  const isMobile = useIsMobile()

  return (
    <div className={styles.layout}>
      {!isMobile && <DesktopSidebar />}
      <main className={`${styles.content} ${isMobile ? styles.mobile : styles.desktop}`}>
        <Outlet />
      </main>
      {isMobile && <MobileTabBar />}
    </div>
  )
}
