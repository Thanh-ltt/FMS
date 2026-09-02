import { useContext, useState } from 'react';
import { Eye, EyeOff, KeyRound, LogOut, ShieldCheck } from 'lucide-react';
import { Navigate, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { AuthContext } from '../context/auth-context';

const initialForm = {
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
};

export default function ChangePassword() {
  const { user, changePassword, logout } = useContext(AuthContext);
  const navigate = useNavigate();
  const [formData, setFormData] = useState(initialForm);
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  if (!user?.mustChangePassword) {
    return <Navigate to="/dashboard" replace />;
  }

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (formData.newPassword !== formData.confirmPassword) {
      toast.error('Mật khẩu nhập lại không khớp');
      return;
    }
    if (formData.currentPassword === formData.newPassword) {
      toast.error('Mật khẩu mới phải khác mật khẩu hiện tại');
      return;
    }

    setSubmitting(true);
    try {
      await changePassword({
        currentPassword: formData.currentPassword,
        newPassword: formData.newPassword,
      });
      toast.success('Đổi mật khẩu thành công');
      navigate('/dashboard', { replace: true });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể đổi mật khẩu');
    } finally {
      setSubmitting(false);
    }
  };

  const handleLogout = () => {
    logout();
    navigate('/login', { replace: true });
  };

  return (
    <div className="flex min-h-screen items-center justify-center bg-slate-950 p-4 sm:p-6">
      <main className="w-full max-w-lg overflow-hidden rounded-xl bg-white shadow-2xl">
        <div className="border-b border-emerald-100 bg-emerald-50 px-6 py-5 sm:px-8">
          <div className="flex items-start justify-between gap-4">
            <div className="flex min-w-0 items-center gap-3">
              <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-emerald-600 text-white">
                <ShieldCheck size={22} />
              </span>
              <div className="min-w-0">
                <h1 className="text-xl font-semibold text-emerald-950">Đổi mật khẩu lần đầu</h1>
                <p className="mt-1 break-words text-sm text-emerald-700">Tài khoản {user.username}</p>
              </div>
            </div>
            <button
              type="button"
              onClick={handleLogout}
              title="Đăng xuất"
              aria-label="Đăng xuất"
              className="inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg text-emerald-700 hover:bg-emerald-100"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5 px-6 py-6 sm:px-8">
          <p className="text-sm leading-6 text-slate-600">
            Mật khẩu vừa được cấp là mật khẩu tạm. Hãy đặt mật khẩu mới trước khi sử dụng hệ thống.
          </p>

          <label className="block">
            <span className="text-sm font-medium text-slate-700">Mật khẩu hiện tại</span>
            <div className="relative mt-1">
              <input
                required
                minLength={8}
                maxLength={72}
                autoComplete="current-password"
                type={showPassword ? 'text' : 'password'}
                name="currentPassword"
                value={formData.currentPassword}
                onChange={handleChange}
                className="block h-12 w-full rounded-lg border border-slate-300 px-3 pr-12 text-sm outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
              <button
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                title={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                className="absolute inset-y-0 right-0 inline-flex w-12 items-center justify-center text-slate-500 hover:text-slate-800"
              >
                {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
              </button>
            </div>
          </label>

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block min-w-0">
              <span className="text-sm font-medium text-slate-700">Mật khẩu mới</span>
              <input
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                type={showPassword ? 'text' : 'password'}
                name="newPassword"
                value={formData.newPassword}
                onChange={handleChange}
                className="mt-1 block h-12 w-full rounded-lg border border-slate-300 px-3 text-sm outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </label>
            <label className="block min-w-0">
              <span className="text-sm font-medium text-slate-700">Nhập lại mật khẩu</span>
              <input
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                type={showPassword ? 'text' : 'password'}
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                className="mt-1 block h-12 w-full rounded-lg border border-slate-300 px-3 text-sm outline-none focus:border-emerald-500 focus:ring-4 focus:ring-emerald-100"
              />
            </label>
          </div>

          <button
            type="submit"
            disabled={submitting}
            className="inline-flex h-12 w-full items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 text-sm font-semibold text-white hover:bg-emerald-700 disabled:opacity-60"
          >
            <KeyRound size={18} />
            {submitting ? 'Đang đổi mật khẩu...' : 'Đổi mật khẩu và tiếp tục'}
          </button>
        </form>
      </main>
    </div>
  );
}
