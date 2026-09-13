/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      fontFamily: {
        heading: ['Space Grotesk', 'sans-serif'],
        body: ['Inter', 'sans-serif'],
        mono: ['IBM Plex Mono', 'monospace'],
      },
      colors: {
        brand: {
          50: '#E6F4F5',
          100: '#CBE8EC',
          500: '#0E6E76',
          600: '#0B575D',
          700: '#084146',
        }
      }
    },
  },
  plugins: [],
};
