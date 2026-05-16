import type { Metadata } from 'next'
import './globals.css'

export const metadata: Metadata = {
  title: 'MLBB Scrim Host - Admin',
  description: 'Admin dashboard for MLBB Scrim Host',
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className="bg-gray-900 text-white">{children}</body>
    </html>
  )
}
