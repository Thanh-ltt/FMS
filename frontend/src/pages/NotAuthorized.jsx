import { Link } from 'react-router-dom';
import { ShieldAlert } from 'lucide-react';

export default function NotAuthorized() {
  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center px-6">
      <div className="w-full max-w-md rounded-lg bg-white border border-slate-200 p-8 text-center shadow-sm">
        <div className="mx-auto mb-5 flex h-12 w-12 items-center justify-center rounded-lg bg-rose-50 text-rose-600">
          <ShieldAlert size={24} />
        </div>
        <h1 className="text-2xl font-semibold text-slate-950">Không có quyền truy cập</h1>
        <p className="mt-3 text-sm leading-6 text-slate-500">
          Tài khoản hiện tại không được phép xem màn hình này. Hãy đăng nhập bằng tài khoản có vai trò phù hợp.
        </p>
        <Link
          to="/dashboard"
          className="mt-6 inline-flex h-10 items-center rounded-lg bg-emerald-700 px-4 text-sm font-medium text-white hover:bg-emerald-800"
        >
          Về Dashboard
        </Link>
      </div>
    </div>
  );
}
