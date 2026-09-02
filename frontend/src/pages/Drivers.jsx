import { useContext, useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import {
  Calendar,
  Clock,
  Edit3,
  Eye,
  EyeOff,
  FileText,
  ImagePlus,
  KeyRound,
  MapPin,
  Phone,
  UserPlus,
  UserRound,
  X,
} from 'lucide-react';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import { formatDate, formatDateTime, toDateInputValue } from '../utils/dates';
import {
  DRIVER_LICENSE_INPUT_PATTERN,
  PHONE_INPUT_PATTERN,
  isAdultBirthDate,
  latestAdultBirthDate,
} from '../utils/validation';

const MAX_AVATAR_BYTES = 1.5 * 1024 * 1024;
const supportedAvatarTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);

const initialDriverForm = {
  name: '',
  dob: '',
  phone: '',
  licenseNumber: '',
  licenseExpiration: '',
  address: '',
  avatarUrl: '',
};

const initialAccountForm = {
  username: '',
  password: '',
  confirmPassword: '',
};

const roleLabels = {
  ADMIN: 'Quản trị viên',
  MANAGER: 'Quản lý vận hành',
};

const initials = (name) => String(name || 'TX')
  .trim()
  .split(/\s+/)
  .slice(-2)
  .map((part) => part.charAt(0).toUpperCase())
  .join('');

const DriverAvatar = ({ driver, size = 'table' }) => {
  const sizeClass = size === 'large'
    ? 'h-24 w-24 text-2xl'
    : size === 'profile'
      ? 'h-16 w-16 text-lg'
      : 'h-9 w-9 text-xs';

  return driver?.avatarUrl ? (
    <img
      src={driver.avatarUrl}
      alt={driver.name || 'Tài xế'}
      className={`${sizeClass} shrink-0 rounded-lg border border-emerald-200 object-cover`}
    />
  ) : (
    <span className={`${sizeClass} inline-flex shrink-0 items-center justify-center rounded-lg bg-emerald-100 font-semibold text-emerald-700`}>
      {initials(driver?.name)}
    </span>
  );
};

const matchesLicenseState = (row, state) => {
  const expiration = row.licenseExpiration ? String(row.licenseExpiration).slice(0, 10) : '';
  if (state === 'MISSING') return !expiration;
  if (!expiration) return false;

  const today = new Date();
  const nextThirtyDays = new Date(today);
  nextThirtyDays.setDate(nextThirtyDays.getDate() + 30);
  const todayValue = toDateInputValue(today);
  const nextThirtyDaysValue = toDateInputValue(nextThirtyDays);

  if (state === 'EXPIRED') return expiration < todayValue;
  if (state === 'EXPIRING') return expiration >= todayValue && expiration <= nextThirtyDaysValue;
  if (state === 'VALID') return expiration > nextThirtyDaysValue;
  return true;
};

export default function Drivers() {
  const { user } = useContext(AuthContext);
  const isDriver = user?.role === 'DRIVER';
  const isAdmin = user?.role === 'ADMIN';
  const canProvisionAccount = isAdmin || user?.role === 'MANAGER';

  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(isDriver);
  
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingDriver, setEditingDriver] = useState(null);
  const [formData, setFormData] = useState(initialDriverForm);
  const [accountDriver, setAccountDriver] = useState(null);
  const [accountForm, setAccountForm] = useState(initialAccountForm);
  const [accountSubmitting, setAccountSubmitting] = useState(false);
  const [showAccountPassword, setShowAccountPassword] = useState(false);

  useEffect(() => {
    if (isDriver) {
      fetchMyProfile();
    }
  }, [isDriver]);

  const fetchMyProfile = async () => {
    try {
      const res = await api.get('/drivers/my-profile');
      setProfile(res.data.result || res.data);
    } catch {
      toast.error('Không thể tải hồ sơ cá nhân');
    } finally {
      setLoading(false);
    }
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
  };

  const openCreateModal = () => {
    setEditingDriver(null);
    setFormData(initialDriverForm);
    setIsModalOpen(true);
  };

  const openEditModal = (driver) => {
    setEditingDriver(driver);
    setFormData({
      name: driver.name || '',
      dob: driver.dob || '',
      phone: driver.phone || '',
      licenseNumber: driver.licenseNumber || '',
      licenseExpiration: driver.licenseExpiration || '',
      address: driver.address || '',
      avatarUrl: driver.avatarUrl || '',
    });
    setIsModalOpen(true);
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setEditingDriver(null);
    setFormData(initialDriverForm);
  };

  const openAccountModal = (driver) => {
    setAccountDriver(driver);
    setAccountForm({
      username: driver.username || '',
      password: '',
      confirmPassword: '',
    });
    setShowAccountPassword(false);
  };

  const closeAccountModal = () => {
    if (accountSubmitting) return;
    setAccountDriver(null);
    setAccountForm(initialAccountForm);
    setShowAccountPassword(false);
  };

  const handleAccountChange = (event) => {
    const { name, value } = event.target;
    setAccountForm((current) => ({ ...current, [name]: value }));
  };

  const handleAccountSubmit = async (event) => {
    event.preventDefault();
    if (!accountDriver) return;
    if (accountDriver.username && !isAdmin) {
      toast.error('Chỉ quản trị viên được đặt lại mật khẩu');
      return;
    }
    if (accountForm.password !== accountForm.confirmPassword) {
      toast.error('Mật khẩu nhập lại không khớp');
      return;
    }

    setAccountSubmitting(true);
    try {
      if (accountDriver.username) {
        await api.patch(`/drivers/${accountDriver.id}/account/password`, {
          password: accountForm.password,
        });
        toast.success(`Đã đặt lại mật khẩu cho tài khoản ${accountDriver.username}`);
      } else {
        await api.post(`/drivers/${accountDriver.id}/account`, {
          username: accountForm.username.trim(),
          password: accountForm.password,
        });
        toast.success(`Đã cấp tài khoản ${accountForm.username.trim()} cho ${accountDriver.name}`);
      }
      setAccountDriver(null);
      setAccountForm(initialAccountForm);
      setRefreshKey((current) => current + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật tài khoản tài xế');
    } finally {
      setAccountSubmitting(false);
    }
  };

  const revokeDriverAccount = async () => {
    if (!accountDriver?.username) return;
    const confirmed = window.confirm(
      `Thu hồi tài khoản ${accountDriver.username}? Tài xế sẽ không thể đăng nhập cho đến khi được cấp tài khoản mới.`
    );
    if (!confirmed) return;

    setAccountSubmitting(true);
    try {
      await api.delete(`/drivers/${accountDriver.id}/account`);
      toast.success(`Đã thu hồi tài khoản ${accountDriver.username}`);
      setAccountDriver(null);
      setAccountForm(initialAccountForm);
      setRefreshKey((current) => current + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể thu hồi tài khoản tài xế');
    } finally {
      setAccountSubmitting(false);
    }
  };

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    if (!supportedAvatarTypes.has(file.type) || file.size > MAX_AVATAR_BYTES) {
      toast.error('Ảnh phải là JPG, PNG hoặc WebP và không vượt quá 1,5 MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setFormData((current) => ({ ...current, avatarUrl: String(reader.result || '') }));
    };
    reader.onerror = () => toast.error('Không thể đọc ảnh đã chọn');
    reader.readAsDataURL(file);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!isAdultBirthDate(formData.dob)) {
      toast.error('Tài xế phải đủ 18 tuổi');
      return;
    }
    setIsSubmitting(true);
    try {
      if (editingDriver) {
        const { name, dob, phone, licenseNumber, licenseExpiration, address, avatarUrl } = formData;
        await api.put(`/drivers/${editingDriver.id}`, { name, dob, phone, licenseNumber, licenseExpiration, address, avatarUrl });
        toast.success('Cập nhật tài xế thành công!');
      } else {
        await api.post('/drivers', formData);
        toast.success('Đã thêm hồ sơ tài xế');
      }
      closeModal();
      setRefreshKey((prev) => prev + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể lưu tài xế');
    } finally {
      setIsSubmitting(false);
    }
  };

  if (isDriver) {
    if (loading) return <p className="text-slate-500">Đang tải hồ sơ...</p>;
    if (!profile) return <p className="text-slate-500">Không tìm thấy thông tin hồ sơ.</p>;

    return (
      <div className="max-w-3xl mx-auto space-y-6">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">Hồ sơ của tôi</h1>
          <p className="mt-1 text-sm text-slate-500">Quản lý thông tin cá nhân và bằng lái của bạn.</p>
        </div>
        
        <div className="bg-white rounded-xl border border-slate-200 overflow-hidden shadow-sm">
          <div className="bg-slate-50 px-6 py-4 border-b border-slate-100 flex items-center gap-4">
            <DriverAvatar driver={profile} size="profile" />
            <div>
              <h2 className="text-xl font-bold text-slate-900">{profile.name}</h2>
              <p className="text-slate-500 text-sm">Tài xế</p>
            </div>
          </div>
          
          <div className="p-6 grid grid-cols-1 md:grid-cols-2 gap-6">
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <Calendar className="text-slate-400 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-slate-900">Ngày sinh</p>
                  <p className="text-sm text-slate-500">{profile.dob ? formatDate(profile.dob) : 'Chưa cập nhật'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <UserRound className="text-slate-400 mt-0.5" size={18} />
                <div className="min-w-0">
                  <p className="text-sm font-medium text-slate-900">Tên đăng nhập</p>
                  <p className="break-words text-sm text-slate-500">{profile.username || 'Chưa liên kết tài khoản'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Phone className="text-slate-400 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-slate-900">Số điện thoại</p>
                  <p className="text-sm text-slate-500">{profile.phone || 'Chưa cập nhật'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <MapPin className="text-slate-400 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-slate-900">Địa chỉ</p>
                  <p className="text-sm text-slate-500">{profile.address || 'Chưa cập nhật'}</p>
                </div>
              </div>
            </div>
            
            <div className="space-y-4">
              <div className="flex items-start gap-3">
                <FileText className="text-slate-400 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-slate-900">Số bằng lái</p>
                  <p className="text-sm text-slate-500">{profile.licenseNumber || 'Chưa cập nhật'}</p>
                </div>
              </div>
              <div className="flex items-start gap-3">
                <Clock className="text-slate-400 mt-0.5" size={18} />
                <div>
                  <p className="text-sm font-medium text-slate-900">Ngày hết hạn bằng lái</p>
                  <p className="text-sm text-slate-500">{profile.licenseExpiration ? formatDate(profile.licenseExpiration) : 'Chưa cập nhật'}</p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý tài xế"
        description="Theo dõi tài khoản đăng nhập, thông tin liên hệ và bằng lái của từng tài xế."
        endpoint="/drivers"
        deleteEndpoint="/drivers"
        deleteLabel={(row) => `tài xế "${row.name || row.id}"`}
        deleteSuccessMessage="Đã xóa tài xế và tài khoản đăng nhập"
        emptyText="Chưa có tài xế nào."
        onCreate={isAdmin ? openCreateModal : undefined}
        filterGridClassName="grid-cols-1 sm:grid-cols-2 xl:grid-cols-5"
        filters={[
          {
            key: 'driverId',
            label: 'Tài xế',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm tên, username, SĐT...',
            field: 'id',
            deriveOptions: true,
            getOptionLabel: (row) => [row.name, row.username, row.phone].filter(Boolean).join(' - ') || row.id,
          },
          {
            key: 'licenseState',
            label: 'Tình trạng bằng lái',
            type: 'select',
            options: [
              { value: 'VALID', label: 'Còn hạn trên 30 ngày' },
              { value: 'EXPIRING', label: 'Sắp hết hạn trong 30 ngày' },
              { value: 'EXPIRED', label: 'Đã hết hạn' },
              { value: 'MISSING', label: 'Chưa có ngày hết hạn' },
            ],
            match: matchesLicenseState,
          },
          {
            key: 'accountState',
            label: 'Tài khoản đăng nhập',
            type: 'select',
            options: [
              { value: 'LINKED', label: 'Đã cấp tài khoản' },
              { value: 'UNLINKED', label: 'Chưa cấp tài khoản' },
            ],
            match: (row, value) => value === 'LINKED' ? Boolean(row.username) : !row.username,
          },
          { key: 'licenseFrom', label: 'Bằng lái hết hạn từ ngày', type: 'date', field: 'licenseExpiration', operator: 'gte', maxFilterKey: 'licenseTo' },
          { key: 'licenseTo', label: 'Bằng lái hết hạn đến ngày', type: 'date', field: 'licenseExpiration', operator: 'lte', minFilterKey: 'licenseFrom', popupAlign: 'right' },
        ]}
        primaryColumns={['avatarUrl', 'name', 'username', 'phone', 'licenseExpiration']}
        columns={[
          {
            key: 'avatarUrl',
            label: 'Ảnh',
            searchValue: () => '',
            render: (row) => <DriverAvatar driver={row} />,
            detailRender: (row) => <DriverAvatar driver={row} size="large" />,
          },
          { key: 'name', label: 'Họ tên' },
          {
            key: 'username',
            label: 'Tên đăng nhập',
            render: (row) => row.username
              ? <span className="font-medium text-slate-800">{row.username}</span>
              : <span className="font-medium text-amber-700">Chưa cấp</span>,
          },
          { key: 'phone', label: 'Điện thoại' },
          { key: 'licenseNumber', label: 'Số bằng lái' },
          { key: 'licenseExpiration', label: 'Hết hạn bằng lái', render: (row) => formatDate(row.licenseExpiration) },
          { key: 'address', label: 'Địa chỉ' },
        ]}
        rowActions={(row) => (
          <>
            {((!row.username && canProvisionAccount) || (row.username && isAdmin)) && (
              <button
                type="button"
                onClick={() => openAccountModal(row)}
                title={row.username ? 'Quản lý tài khoản tài xế' : 'Cấp tài khoản tài xế'}
                aria-label={row.username ? 'Quản lý tài khoản tài xế' : 'Cấp tài khoản tài xế'}
                className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border transition ${
                  row.username
                    ? 'border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100'
                    : 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                }`}
              >
                {row.username ? <KeyRound size={16} /> : <UserPlus size={16} />}
              </button>
            )}
            <button
              type="button"
              onClick={() => openEditModal(row)}
              title="Chỉnh sửa tài xế"
              aria-label="Chỉnh sửa tài xế"
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100"
            >
              <Edit3 size={16} />
            </button>
          </>
        )}
      />

      <Modal isOpen={isModalOpen} onClose={closeModal} title={editingDriver ? 'Chỉnh sửa tài xế' : 'Thêm tài xế mới'} size="wide">
        <form onSubmit={handleSubmit} className="mt-2 space-y-5">
          <div className="flex flex-col gap-5 border-b border-slate-100 pb-5 sm:flex-row sm:items-center">
            <DriverAvatar driver={{ name: formData.name, avatarUrl: formData.avatarUrl }} size="large" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-slate-900">Ảnh tài xế</p>
              <p className="mt-1 text-xs text-slate-500">JPG, PNG hoặc WebP; dung lượng tối đa 1,5 MB.</p>
              <div className="mt-3 flex flex-wrap gap-2">
                <label className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50">
                  <ImagePlus size={16} />
                  Chọn ảnh
                  <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleAvatarChange} className="sr-only" />
                </label>
                {formData.avatarUrl && (
                  <button type="button" onClick={() => setFormData((current) => ({ ...current, avatarUrl: '' }))} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100">
                    <X size={16} />
                    Xóa ảnh
                  </button>
                )}
              </div>
            </div>
          </div>

          {editingDriver && (
            <div className="rounded-lg border border-emerald-100 bg-emerald-50 p-4">
              <h4 className="text-sm font-semibold text-emerald-900">Tài khoản đăng nhập</h4>
              <div className="mt-3 flex min-w-0 items-center gap-3">
                <UserRound size={18} className="shrink-0 text-emerald-700" />
                <div className="min-w-0">
                  <p className="break-words font-semibold text-emerald-950">
                    {editingDriver.username || 'Chưa liên kết tài khoản'}
                  </p>
                  <p className="mt-0.5 text-xs text-emerald-700">Mật khẩu được bảo mật và không hiển thị lại.</p>
                </div>
              </div>
            </div>
          )}
          
          <div className="pt-2">
            <h4 className="text-sm font-semibold text-slate-800 mb-4">Thông tin cá nhân & Bằng lái</h4>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-slate-700">Họ và tên</label>
                <input required minLength={2} maxLength={100} type="text" name="name" value={formData.name} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <DateField required label="Ngày sinh" name="dob" value={formData.dob} max={latestAdultBirthDate()} quickFill={false} onChange={handleChange} />
                <div>
                  <label className="block text-sm font-medium text-slate-700">Số điện thoại</label>
                  <input required type="tel" inputMode="tel" pattern={PHONE_INPUT_PATTERN} title="Nhập số điện thoại Việt Nam hợp lệ, ví dụ 0901234567" maxLength={20} name="phone" value={formData.phone} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
                </div>
              </div>
              <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Số bằng lái</label>
                  <input required type="text" pattern={DRIVER_LICENSE_INPUT_PATTERN} title="Số bằng lái gồm 5-20 ký tự chữ, số, dấu chấm, gạch chéo hoặc gạch ngang" maxLength={20} name="licenseNumber" value={formData.licenseNumber} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
                </div>
                <DateField required label="Ngày hết hạn bằng lái" name="licenseExpiration" value={formData.licenseExpiration} min={toDateInputValue()} onChange={handleChange} />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Địa chỉ</label>
                <input required minLength={5} maxLength={255} type="text" name="address" value={formData.address} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
              </div>
            </div>
          </div>
          
          <div className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={closeModal} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed">
              {isSubmitting ? 'Đang lưu...' : editingDriver ? 'Cập nhật tài xế' : 'Lưu tài xế'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(accountDriver)}
        onClose={closeAccountModal}
        title={accountDriver?.username ? 'Quản lý tài khoản tài xế' : 'Cấp tài khoản tài xế'}
        size="medium"
        variant="detail"
      >
        <form onSubmit={handleAccountSubmit} className="space-y-5">
          <div className="-mx-6 -mt-4 border-b border-emerald-100 bg-emerald-50 px-6 py-4">
            <div className="flex min-w-0 items-center gap-3">
              <DriverAvatar driver={accountDriver} size="profile" />
              <div className="min-w-0">
                <p className="break-words font-semibold text-emerald-950">{accountDriver?.name || 'Tài xế'}</p>
                <p className="mt-0.5 break-words text-sm text-emerald-700">
                  {accountDriver?.licenseNumber || 'Chưa có số bằng lái'}
                </p>
              </div>
            </div>
          </div>

          {accountDriver?.username ? (
            <div className="border-l-4 border-emerald-500 bg-emerald-50 px-4 py-3">
              <p className="text-xs font-medium uppercase text-emerald-700">Tài khoản hiện tại</p>
              <p className="mt-1 break-words font-semibold text-emerald-950">{accountDriver.username}</p>
              {accountDriver.accountProvisionedByName && (
                <p className="mt-1 break-words text-xs leading-5 text-emerald-700">
                  Cấp bởi {accountDriver.accountProvisionedByName}
                  {accountDriver.accountProvisionedByRole
                    ? ` (${roleLabels[accountDriver.accountProvisionedByRole] || accountDriver.accountProvisionedByRole})`
                    : ''}
                  {accountDriver.accountProvisionedAt
                    ? ` lúc ${formatDateTime(accountDriver.accountProvisionedAt)}`
                    : ''}
                </p>
              )}
            </div>
          ) : (
            <label className="block min-w-0">
              <span className="text-sm font-medium text-slate-700">Tên đăng nhập</span>
              <input
                required
                minLength={6}
                maxLength={50}
                autoComplete="off"
                type="text"
                name="username"
                value={accountForm.username}
                onChange={handleAccountChange}
                className="mt-1 block h-11 w-full rounded-md border border-slate-300 px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
            </label>
          )}

          <div className="grid gap-4 sm:grid-cols-2">
            <label className="block min-w-0">
              <span className="text-sm font-medium text-slate-700">
                {accountDriver?.username ? 'Mật khẩu mới' : 'Mật khẩu'}
              </span>
              <div className="relative mt-1">
                <input
                  required
                  minLength={8}
                  maxLength={72}
                  autoComplete="new-password"
                  type={showAccountPassword ? 'text' : 'password'}
                  name="password"
                  value={accountForm.password}
                  onChange={handleAccountChange}
                  className="block h-11 w-full rounded-md border border-slate-300 px-3 pr-11 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
                />
                <button
                  type="button"
                  onClick={() => setShowAccountPassword((current) => !current)}
                  title={showAccountPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  aria-label={showAccountPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  className="absolute inset-y-0 right-0 inline-flex w-11 items-center justify-center text-slate-500 hover:text-slate-800"
                >
                  {showAccountPassword ? <EyeOff size={17} /> : <Eye size={17} />}
                </button>
              </div>
            </label>

            <label className="block min-w-0">
              <span className="text-sm font-medium text-slate-700">Nhập lại mật khẩu</span>
              <input
                required
                minLength={8}
                maxLength={72}
                autoComplete="new-password"
                type={showAccountPassword ? 'text' : 'password'}
                name="confirmPassword"
                value={accountForm.confirmPassword}
                onChange={handleAccountChange}
                className="mt-1 block h-11 w-full rounded-md border border-slate-300 px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
            </label>
          </div>

          <p className="text-sm leading-6 text-slate-500">
            Mật khẩu được mã hóa ngay khi lưu và sẽ không thể xem lại trên hệ thống.
          </p>

          <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 pt-4">
            <div>
              {isAdmin && accountDriver?.username && (
                <button
                  type="button"
                  onClick={revokeDriverAccount}
                  disabled={accountSubmitting}
                  className="rounded-lg border border-rose-200 bg-rose-50 px-3 py-2 text-sm font-medium text-rose-700 hover:bg-rose-100 disabled:opacity-50"
                >
                  Thu hồi tài khoản
                </button>
              )}
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={closeAccountModal}
                disabled={accountSubmitting}
                className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
              >
                Hủy
              </button>
              <button
                type="submit"
                disabled={accountSubmitting}
                className="inline-flex min-w-32 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50"
              >
                <KeyRound size={16} />
                {accountSubmitting
                  ? 'Đang lưu...'
                  : accountDriver?.username
                    ? 'Đặt lại mật khẩu'
                    : 'Cấp tài khoản'}
              </button>
            </div>
          </div>
        </form>
      </Modal>
    </>
  );
}
