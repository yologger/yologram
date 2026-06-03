import ResponsiveLayout from '@/components/layout/ResponsiveLayout'

export default function MainLayout({ children }: { children: React.ReactNode }) {
  return <ResponsiveLayout>{children}</ResponsiveLayout>
}
