'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import {
  LayoutDashboard,
  Users,
  Shield,
  Swords,
  Trophy,
  MessageSquare,
  Image,
  LogOut
} from 'lucide-react'

const navItems = [
  { href: '/', label: 'Dashboard', icon: LayoutDashboard },
  { href: '/players', label: 'Players', icon: Users },
  { href: '/teams', label: 'Teams', icon: Shield },
  { href: '/scrims', label: 'Scrims', icon: Swords },
  { href: '/matches', label: 'Matches', icon: Trophy },
  { href: '/chat', label: 'Chat Logs', icon: MessageSquare },
  { href: '/screenshots', label: 'Screenshots', icon: Image },
]

export default function Sidebar() {
  const pathname = usePathname()

  return (
    <aside className="w-64 bg-mlbb-dark border-r border-gray-800 min-h-screen flex flex-col">
      {/* Logo */}
      <div className="p-6 border-b border-gray-800">
        <h1 className="text-xl font-bold text-mlbb-gold">MLBB Scrim Host</h1>
        <p className="text-xs text-gray-500 mt-1">Admin Panel</p>
      </div>

      {/* Navigation */}
      <nav className="flex-1 p-4">
        <ul className="space-y-1">
          {navItems.map((item) => {
            const Icon = item.icon
            const isActive = pathname === item.href

            return (
              <li key={item.href}>
                <Link
                  href={item.href}
                  className={`flex items-center gap-3 px-4 py-3 rounded-lg transition-colors ${
                    isActive
                      ? 'bg-mlbb-blue/20 text-mlbb-blue'
                      : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                  }`}
                >
                  <Icon size={18} />
                  <span className="text-sm font-medium">{item.label}</span>
                </Link>
              </li>
            )
          })}
        </ul>
      </nav>

      {/* Footer */}
      <div className="p-4 border-t border-gray-800">
        <button className="flex items-center gap-3 px-4 py-3 w-full text-gray-400 hover:text-red-400 transition-colors rounded-lg hover:bg-gray-800">
          <LogOut size={18} />
          <span className="text-sm font-medium">Logout</span>
        </button>
      </div>
    </aside>
  )
}
