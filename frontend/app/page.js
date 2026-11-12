// The URL is public: it is used for links to the existing Spring application too.
// Never put credentials in NEXT_PUBLIC_ variables.
export const dynamic = 'force-dynamic';

const configuredUrl = process.env.NEXT_PUBLIC_API_URL;

function backendOrigin() {
  if (!configuredUrl) return null;
  try {
    const url = new URL(configuredUrl);
    if (!['http:', 'https:'].includes(url.protocol) || url.username || url.password || url.pathname !== '/' || url.search || url.hash) return null;
    return url.origin;
  } catch {
    return null;
  }
}

async function getHome(origin) {
  if (!origin) return null;
  try {
    const response = await fetch(`${origin}/api/public/home`, {
      cache: 'no-store',
      signal: AbortSignal.timeout(5000),
    });
    if (!response.ok) return null;
    const data = await response.json();
    if (!data.metrics || !Array.isArray(data.popularSkills) || !Array.isArray(data.categories)) return null;
    return data;
  } catch {
    return null;
  }
}

function BackendLink({ origin, path, children, className }) {
  return origin
    ? <a className={className} href={`${origin}${path}`}>{children}</a>
    : <span className={`${className || ''} disabled`} aria-disabled="true" title="Backend not configured">{children}</span>;
}

export default async function Home() {
  const origin = backendOrigin();
  const data = await getHome(origin);
  return <>
    <header className="header"><div className="wrap nav">
      <a className="brand" href="/" aria-label="SkillSwap home"><span className="brand-mark">↔</span> Skill<span>Swap</span></a>
      <nav aria-label="Main navigation"><a href="#how-it-works">How it works</a><a href="#skills">Explore skills</a><BackendLink origin={origin} path="/about">About</BackendLink></nav>
      <div className="nav-actions"><BackendLink origin={origin} path="/login" className="login">Log in</BackendLink><BackendLink origin={origin} path="/register" className="button button-small">Join SkillSwap <span>↗</span></BackendLink></div>
    </div></header>
    <main>
      <section className="hero wrap">
        <div className="hero-copy"><p className="eyebrow"><span></span> A BETTER WAY TO LEARN TOGETHER</p>
          <h1>What you know<br/>can take someone <em>further.</em></h1>
          <p className="hero-description">Share a skill, learn something new, and grow alongside people who are curious just like you.</p>
          <div className="actions"><BackendLink origin={origin} path="/register" className="button">Get started <span>↗</span></BackendLink><a className="button button-outline" href="#skills">Explore skills <span>↓</span></a></div>
          <p className="hero-note"><span>✳</span> Teach one thing. Learn another. No money changes hands.</p>
        </div>
        <div className="hero-art" aria-label="An illustration of sharing knowledge">
          <div className="art-circle circle-one"></div><div className="art-circle circle-two"></div>
          <div className="art-tag top-tag"><span>✳</span> A little knowledge goes a long way</div>
          <div className="art-card"><div className="art-card-top"><span className="art-icon">↗</span><span>THE JOY OF<br/>LEARNING TOGETHER</span></div><div className="art-main">Good things<br/>happen when<br/><i>we share.</i></div><div className="art-card-bottom"><span>SKILLSWAP COMMUNITY</span><span>✺</span></div></div>
          <div className="art-tag bottom-tag"><span>↔</span><div><strong>Give a little. Grow a lot.</strong><small>Everyone has something to share.</small></div></div>
        </div>
      </section>
      <div className="ticker"><div className="wrap ticker-inner"><span>LEARN SOMETHING NEW</span><b>✳</b><span>SHARE WHAT YOU KNOW</span><b>✳</b><span>GROW TOGETHER</span><b>✳</b><span>REPEAT</span></div></div>
      <section className="section wrap" id="how-it-works"><div className="section-header"><div><p className="eyebrow"><span></span> THE IDEA IS SIMPLE</p><h2>Knowledge is better<br/><em>when it’s shared.</em></h2></div><p>We’re building a place for students to exchange knowledge, one conversation at a time.</p></div>
        <div className="steps"><article><div className="step-top"><span>01 / SHARE</span><b>↗</b></div><h3>Bring what you know.</h3><p>Everyone has something worth sharing. Start by creating your account.</p></article><article><div className="step-top"><span>02 / DISCOVER</span><b>✳</b></div><h3>Find your curiosity.</h3><p>Browse the skill catalog and discover something you’d love to learn.</p></article><article><div className="step-top"><span>03 / GROW</span><b>↔</b></div><h3>Learn together.</h3><p>Our goal is to make learning from your peers feel a little more human.</p></article></div>
        <p className="development-note">Skill profiles, matching, and sessions are still in development.</p>
      </section>
      <section className="catalog" id="skills"><div className="wrap"><div className="section-header"><div><p className="eyebrow"><span></span> FIND YOUR NEXT THING</p><h2>A world of things<br/><em>to learn.</em></h2></div><div><p>From code to communication, every skill is a place to start.</p><BackendLink origin={origin} path="/skills" className="text-link">Browse the full catalog ↗</BackendLink></div></div>
        {!data ? <div className="status" role="status"><strong>Catalog currently unavailable.</strong><p>{origin ? 'We couldn’t reach the SkillSwap backend. Please try again later.' : 'The SkillSwap backend has not been configured yet.'}</p></div> : data.popularSkills.length === 0 ? <div className="status"><strong>The catalog is getting ready.</strong><p>Check back soon for skills to explore.</p></div> : <div className="skill-grid">{data.popularSkills.map((skill, index) => <BackendLink key={skill.id} origin={origin} path={`/skills?q=${encodeURIComponent(skill.name)}`} className="skill"><span className="skill-number">{String(index + 1).padStart(2, '0')}</span><span className="skill-title">{skill.name}<small>{skill.categoryName}</small></span><span className="skill-arrow">↗</span><span className="skill-count">{skill.teacherCount} teaching · {skill.learnerCount} learning</span></BackendLink>)}</div>}
      </div></section>
      <section className="closing wrap"><div><p className="eyebrow"><span></span> LET’S LEARN TOGETHER</p><h2>A little curiosity<br/><em>goes a long way.</em></h2><p>Join a community built around the idea that we all have something to teach, and something to learn.</p><BackendLink origin={origin} path="/register" className="button button-light">Join SkillSwap <span>↗</span></BackendLink></div><div className="closing-star" aria-hidden="true">✳</div></section>
    </main>
    <footer className="footer wrap"><div><a className="brand" href="/"> <span className="brand-mark">↔</span> Skill<span>Swap</span></a><p>Learn together. Grow together.</p></div><div className="footer-links"><BackendLink origin={origin} path="/about">About</BackendLink><BackendLink origin={origin} path="/contact">Contact</BackendLink><BackendLink origin={origin} path="/privacy">Privacy</BackendLink><BackendLink origin={origin} path="/terms">Terms</BackendLink></div><small>Built for curious minds, everywhere.</small></footer>
  </>;
}
