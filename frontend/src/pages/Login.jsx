import { useState, useContext } from 'react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import toast from 'react-hot-toast';
import { LockKeyhole, ShieldCheck, UserRound } from 'lucide-react';

const initialFormData = {
  username: '',
  password: '',
  confirmPassword: '',
};

export default function Login() {
  const [isLogin, setIsLogin] = useState(true);
  const [formData, setFormData] = useState(initialFormData);
  const [loading, setLoading] = useState(false);

  const { login } = useContext(AuthContext);
  const navigate = useNavigate();

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    try {
      if (isLogin) {
        const userData = await login({ username: formData.username, password: formData.password });
        toast.success(userData.mustChangePassword ? 'Hãy đổi mật khẩu tạm để tiếp tục' : 'Đăng nhập thành công!');
        navigate(userData.mustChangePassword ? '/change-password' : '/dashboard');
      } else {
        if (formData.password !== formData.confirmPassword) {
          toast.error('Mật khẩu nhập lại không khớp');
          return;
        }

        await api.post('/auth/register', {
          username: formData.username.trim(),
          password: formData.password,
        });
        toast.success('Đăng ký tài khoản thành công! Vui lòng đăng nhập.');
        setIsLogin(true);
        setFormData(initialFormData);
      }
    } catch (error) {
      const serverMessage =
        error.response?.data?.message ||
        error.response?.data?.error ||
        error.response?.data?.result;
      const networkMessage = error.request && !error.response
        ? 'Không kết nối được backend. Hãy kiểm tra server 8080 và CORS.'
        : null;

      toast.error(serverMessage || networkMessage || error.message || 'Có lỗi xảy ra!');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-6">
      <div className="grid w-full max-w-5xl overflow-hidden rounded-2xl bg-white shadow-2xl md:grid-cols-[1.05fr_0.95fr]">
        <div className="hidden bg-slate-900 p-10 text-white md:flex md:flex-col md:justify-between">
          <div>
            <div className="flex h-12 w-12 items-center justify-center rounded-xl bg-emerald-400 text-xl font-bold text-slate-950">
              F
            </div>
            <h1 className="mt-8 text-4xl font-semibold leading-tight">
              Quản lý đội xe rõ ràng, nhanh và an toàn hơn.
            </h1>
            <p className="mt-4 max-w-md text-sm leading-6 text-slate-300">
              Theo dõi phương tiện, tài xế, chuyến đi, hợp đồng và hóa đơn trong một không gian vận hành thống nhất.
            </p>
          </div>

          <div className="grid grid-cols-3 gap-3 text-sm">
            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="text-2xl font-semibold text-emerald-300">24/7</p>
              <p className="mt-1 text-slate-300">Theo dõi vận hành</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="text-2xl font-semibold text-emerald-300">5</p>
              <p className="mt-1 text-slate-300">Vai trò hệ thống</p>
            </div>
            <div className="rounded-xl border border-white/10 bg-white/5 p-4">
              <p className="text-2xl font-semibold text-emerald-300">JWT</p>
              <p className="mt-1 text-slate-300">Bảo mật truy cập</p>
            </div>
          </div>
        </div>

        <div className="p-8 sm:p-10">
          <div className="mb-8">
            <div className="inline-flex items-center gap-2 rounded-full bg-emerald-50 px-3 py-1 text-xs font-medium text-emerald-700">
              <ShieldCheck size={14} />
              Fleet Management System
            </div>
            <h2 className="mt-5 text-3xl font-semibold text-slate-950">
              {isLogin ? 'Đăng nhập' : 'Đăng ký'}
            </h2>
            <p className="mt-2 text-sm text-slate-500">
              {isLogin
                ? 'Nhập thông tin tài khoản để tiếp tục quản lý vận hành.'
                : 'Tạo tài khoản mới để bắt đầu sử dụng hệ thống.'}
            </p>
          </div>

          <div className="mb-6 grid grid-cols-2 rounded-lg bg-slate-100 p-1">
            <button
              type="button"
              onClick={() => {
                setIsLogin(true);
                setFormData(initialFormData);
              }}
              className={`h-10 rounded-md text-sm font-medium transition-colors ${isLogin ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500 hover:text-slate-800'}`}
            >
              Đăng nhập
            </button>
            <button
              type="button"
              onClick={() => {
                setIsLogin(false);
                setFormData(initialFormData);
              }}
              className={`h-10 rounded-md text-sm font-medium transition-colors ${!isLogin ? 'bg-white text-slate-950 shadow-sm' : 'text-slate-500 hover:text-slate-800'}`}
            >
              Đăng ký
            </button>
          </div>

          <form onSubmit={handleSubmit} className="space-y-5">
            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Tên đăng nhập</label>
              <div className="relative">
                <UserRound className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                <input
                  type="text"
                  name="username"
                  value={formData.username}
                  onChange={handleChange}
                  minLength={6}
                  className="h-12 w-full rounded-lg border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100"
                  placeholder="Nhập tên đăng nhập"
                  required
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-slate-700 mb-2">Mật khẩu</label>
              <div className="relative">
                <LockKeyhole className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                <input
                  type="password"
                  name="password"
                  value={formData.password}
                  onChange={handleChange}
                  minLength={8}
                  className="h-12 w-full rounded-lg border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100"
                  placeholder="Nhập mật khẩu"
                  required
                />
              </div>
            </div>

            {!isLogin && (
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-700">Nhập lại mật khẩu</label>
                <div className="relative">
                  <LockKeyhole className="pointer-events-none absolute left-3 top-1/2 h-5 w-5 -translate-y-1/2 text-slate-400" />
                  <input
                    type="password"
                    name="confirmPassword"
                    value={formData.confirmPassword}
                    onChange={handleChange}
                    minLength={8}
                    className="h-12 w-full rounded-lg border border-slate-300 bg-white pl-11 pr-4 text-sm outline-none transition focus:border-emerald-600 focus:ring-4 focus:ring-emerald-100"
                    placeholder="Nhập lại mật khẩu"
                    required
                  />
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="h-12 w-full rounded-lg bg-emerald-600 text-sm font-semibold text-white transition hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-70"
            >
              {loading ? 'Đang xử lý...' : (isLogin ? 'Đăng nhập' : 'Đăng ký')}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
