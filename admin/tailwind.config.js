/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        'mlbb-blue': '#1E90FF',
        'mlbb-gold': '#FFD700',
        'mlbb-dark': '#0A1628',
        'mlbb-navy': '#0D1B2A',
        'mlbb-surface': '#151E2B',
      }
    },
  },
  plugins: [],
}
