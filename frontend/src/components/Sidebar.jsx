import { NavLink } from 'react-router-dom';
import { useContext } from 'react';
import { AuthContext } from '../context/auth-context';
import { navigationSections } from '../config/navigation';

export default function Sidebar() {
  const { user } = useContext(AuthContext);

  const allowedSections = navigationSections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.includes(user?.role)),
    }))
    .filter((section) => section.items.length > 0);

  return (
    <aside className="w-72 bg-slate-950 text-white flex flex-col h-full">
      <div className="p-6 border-b border-white/10">
        <div className="flex h-11 w-11 items-center justify-center rounded-lg bg-emerald-500 text-lg font-bold text-slate-950">
          F
        </div>
        <h2 className="mt-4 text-2xl font-bold">FMS</h2>
        <p className="text-sm text-slate-300">Fleet Management System</p>
      </div>

      <nav className="flex-1 space-y-5 overflow-y-auto p-4">
        {allowedSections.map((section) => (
          <section key={section.key} aria-labelledby={`nav-${section.key}`}>
            <p
              id={`nav-${section.key}`}
              className="mb-1.5 px-4 text-xs font-semibold uppercase text-slate-500"
            >
              {section.label}
            </p>
            <div className="space-y-1">
              {section.items.map(({ path, icon: Icon, label }) => (
                <NavLink
                  key={path}
                  to={path}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-lg px-4 py-2.5 transition-colors ${
                      isActive
                        ? 'bg-emerald-500 text-slate-950 font-semibold'
                        : 'text-slate-300 hover:bg-white/10 hover:text-white'
                    }`}
                >
                  <Icon size={20} className="shrink-0" />
                  <span className="min-w-0">{path === '/drivers' && user?.role === 'DRIVER' ? 'Hồ sơ của tôi' : label}</span>
                </NavLink>
              ))}
            </div>
          </section>
        ))}
      </nav>
    </aside>
  );
}
