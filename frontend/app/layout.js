import './style.css';

export const metadata = {
  title: 'SkillSwap | Learn together. Grow together.',
  description: 'A student community for sharing skills and learning together.',
};

export default function RootLayout({ children }) {
  return <html lang="en"><body>{children}</body></html>;
}
