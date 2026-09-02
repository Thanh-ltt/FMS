import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import Sidebar from './Sidebar';
import { useContext } from 'react';
import { AuthContext } from '../context/auth-context';
import { LogOut } from 'lucide-react';
import { navigationItems } from '../config/navigation';

import NotificationBell from './NotificationBell';

export default function Layout() {
  const { user, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const location = useLocation();

  const currentPage = navigationItems.find((item) => item.path === location.pathname);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <div className="flex h-screen bg-slate-50">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <header className="bg-white border-b border-slate-200 px-8 py-4 flex justify-between items-center">
          <div>
            <h1 className="text-xl font-semibold text-slate-950">{currentPage?.label || 'Fleet Management System'}</h1>
            <p className="mt-0.5 text-xs text-slate-500">Hệ thống quản lý vận hành đội xe</p>
          </div>
          <div className="flex items-center gap-4">
            <NotificationBell />
            <div className="text-right">
              <p className="text-sm font-medium text-slate-800">Xin chào, {user?.username || 'Admin'}</p>
              <p className="text-xs text-slate-500">Vai trò: {user?.role || '-'}</p>
            </div>
            <button
              onClick={handleLogout}
              className="inline-flex h-10 items-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100"
            >
              <LogOut size={16} />
              Đăng xuất
            </button>
          </div>
        </header>
        <main className="flex-1 overflow-auto p-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
