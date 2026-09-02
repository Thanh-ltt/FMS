import { useState } from 'react';
import toast from 'react-hot-toast';
import {
  BadgeCheck,
  BriefcaseBusiness,
  Edit3,
  Eye,
  ImagePlus,
  LockKeyhole,
  LockOpen,
  Mail,
  MapPin,
  Phone,
  UserRound,
  X,
} from 'lucide-react';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import api from '../services/api';
import { formatDate, formatDateTime, toDateInputValue } from '../utils/dates';
import {
  EMPLOYEE_CODE_INPUT_PATTERN,
  ID_NUMBER_INPUT_PATTERN,
  PHONE_INPUT_PATTERN,
  isAdultBirthDate,
  latestAdultBirthDate,
} from '../utils/validation';

const MAX_AVATAR_BYTES = 1.5 * 1024 * 1024;

const roleLabels = {
  MANAGER: 'Quản lý vận hành',
  ACCOUNTANT: 'Kế toán',
};

const genderLabels = {
  MALE: 'Nam',
  FEMALE: 'Nữ',
  OTHER: 'Khác',
};

const getInitialFormData = () => ({
  username: '',
  password: '',
  employeeCode: '',
  fullName: '',
  phone: '',
  email: '',
  address: '',
  idNumber: '',
  dob: '',
  gender: 'OTHER',
  position: '',
  hireDate: toDateInputValue(),
  avatarUrl: '',
  role: 'MANAGER',
});

const getResult = (response, fallback) => response.data?.result ?? response.data ?? fallback;

const initials = (name) => String(name || 'NV')
  .trim()
  .split(/\s+/)
  .slice(-2)
  .map((part) => part.charAt(0).toUpperCase())
  .join('');

const EmployeeAvatar = ({ employee, size = 'table' }) => {
  const sizeClass = size === 'large' ? 'h-24 w-24 text-2xl' : 'h-9 w-9 text-xs';

  return employee?.avatarUrl ? (
    <img
      src={employee.avatarUrl}
      alt={employee.fullName || 'Nhân viên'}
      className={`${sizeClass} shrink-0 rounded-lg border border-slate-200 object-cover`}
    />
  ) : (
    <span className={`${sizeClass} inline-flex shrink-0 items-center justify-center rounded-lg bg-emerald-100 font-semibold text-emerald-700`}>
      {initials(employee?.fullName)}
    </span>
  );
};

const DetailField = ({ label, value, icon: Icon }) => (
  <div className="flex items-start gap-3">
    {Icon && <Icon size={17} className="mt-0.5 shrink-0 text-emerald-600" />}
    <div className="min-w-0">
      <dt className="text-xs font-medium uppercase text-emerald-700">{label}</dt>
      <dd className="mt-1 break-words text-sm font-medium text-slate-900">{value || '-'}</dd>
    </div>
  </div>
);

export default function Employees() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingEmployee, setEditingEmployee] = useState(null);
  const [formData, setFormData] = useState(getInitialFormData);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedEmployee, setSelectedEmployee] = useState(null);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const openCreateForm = () => {
    setEditingEmployee(null);
    setFormData(getInitialFormData());
    setIsFormOpen(true);
  };

  const openEditForm = (employee) => {
    setEditingEmployee(employee);
    setFormData({
      username: employee.username || '',
      password: '',
      employeeCode: employee.employeeCode || '',
      fullName: employee.fullName || '',
      phone: employee.phone || '',
      email: employee.email || '',
      address: employee.address || '',
      idNumber: employee.idNumber || '',
      dob: employee.dob || '',
      gender: employee.gender || 'OTHER',
      position: employee.position || '',
      hireDate: employee.hireDate || '',
      avatarUrl: employee.avatarUrl || '',
      role: employee.role || 'MANAGER',
    });
    setIsFormOpen(true);
  };

  const closeForm = () => {
    setIsFormOpen(false);
    setEditingEmployee(null);
    setFormData(getInitialFormData());
  };

  const handleAvatarChange = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;

    if (!file.type.startsWith('image/') || file.size > MAX_AVATAR_BYTES) {
      toast.error('Ảnh phải đúng định dạng và không vượt quá 1,5 MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => {
      setFormData((current) => ({ ...current, avatarUrl: String(reader.result || '') }));
    };
    reader.onerror = () => toast.error('Không thể đọc ảnh đã chọn');
    reader.readAsDataURL(file);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (!isAdultBirthDate(formData.dob)) {
      toast.error('Nhân viên phải đủ 18 tuổi');
      return;
    }
    setIsSubmitting(true);

    const payload = {
      ...formData,
      username: formData.username.trim(),
      employeeCode: formData.employeeCode.trim(),
      fullName: formData.fullName.trim(),
      phone: formData.phone.trim(),
      email: formData.email.trim() || null,
      address: formData.address.trim() || null,
      idNumber: formData.idNumber.trim() || null,
      dob: formData.dob || null,
      hireDate: formData.hireDate || null,
      position: formData.position.trim() || null,
      avatarUrl: formData.avatarUrl || null,
      password: formData.password || null,
    };

    try {
      if (editingEmployee) {
        await api.put(`/users/employees/${editingEmployee.id}`, payload);
        toast.success('Cập nhật nhân viên thành công');
      } else {
        await api.post('/users/employees', payload);
        toast.success('Thêm nhân viên thành công');
      }
      closeForm();
      setRefreshKey((current) => current + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể lưu hồ sơ nhân viên');
    } finally {
      setIsSubmitting(false);
    }
  };

  const openDetail = async (employee) => {
    setSelectedEmployee(employee);
    setIsDetailOpen(true);
    setDetailLoading(true);
    try {
      const response = await api.get(`/users/employees/${employee.id}`);
      setSelectedEmployee(getResult(response, employee));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải hồ sơ nhân viên');
    } finally {
      setDetailLoading(false);
    }
  };

  const closeDetail = () => {
    setIsDetailOpen(false);
    setSelectedEmployee(null);
  };

  const updateStatus = async (employee) => {
    const isActive = employee.active !== false;
    const action = isActive ? 'khóa' : 'mở lại';
    if (!window.confirm(`Bạn có chắc muốn ${action} tài khoản của ${employee.fullName || employee.username}?`)) {
      return;
    }

    try {
      await api.patch(`/users/employees/${employee.id}/status`, { active: !isActive });
      toast.success(isActive ? 'Đã khóa tài khoản nhân viên' : 'Đã mở lại tài khoản nhân viên');
      setRefreshKey((current) => current + 1);
      if (selectedEmployee?.id === employee.id) closeDetail();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái tài khoản');
    }
  };

  const activeLabel = (employee) => employee?.active === false ? 'Đã khóa' : 'Đang hoạt động';

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý nhân viên"
        description="Quản lý hồ sơ, tài khoản và quyền truy cập của nhân sự nội bộ."
        endpoint="/users/employees"
        deleteEndpoint="/users/employees"
        deleteLabel={(row) => `nhân viên "${row.fullName || row.username || row.employeeCode || row.id}"`}
        deleteSuccessMessage="Đã xóa nhân viên và tài khoản đăng nhập"
        emptyText="Chưa có nhân viên nội bộ nào."
        onCreate={openCreateForm}
        showDetails={false}
        primaryColumns={['avatarUrl', 'employeeCode', 'fullName', 'role', 'phone', 'active']}
        filters={[
          {
            key: 'employeeId',
            label: 'Nhân viên',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm mã NV, họ tên...',
            field: 'id',
            deriveOptions: true,
            getOptionLabel: (row) => [row.employeeCode, row.fullName].filter(Boolean).join(' - ') || row.id,
          },
          {
            key: 'role',
            label: 'Vai trò',
            type: 'select',
            options: Object.entries(roleLabels).map(([value, label]) => ({ value, label })),
          },
          {
            key: 'active',
            label: 'Trạng thái tài khoản',
            type: 'select',
            options: [
              { value: 'true', label: 'Đang hoạt động' },
              { value: 'false', label: 'Đã khóa' },
            ],
            getValue: (row) => row.active !== false,
          },
          {
            key: 'gender',
            label: 'Giới tính',
            type: 'select',
            options: Object.entries(genderLabels).map(([value, label]) => ({ value, label })),
          },
          { key: 'hireFrom', label: 'Ngày vào làm: từ ngày', type: 'date', field: 'hireDate', operator: 'gte', maxFilterKey: 'hireTo', className: 'sm:col-span-2' },
          { key: 'hireTo', label: 'Ngày vào làm: đến ngày', type: 'date', field: 'hireDate', operator: 'lte', minFilterKey: 'hireFrom', popupAlign: 'right', className: 'sm:col-span-2' },
        ]}
        columns={[
          { key: 'avatarUrl', label: 'Ảnh', searchValue: () => '', render: (row) => <EmployeeAvatar employee={row} /> },
          { key: 'employeeCode', label: 'Mã nhân viên' },
          { key: 'fullName', label: 'Họ tên' },
          { key: 'role', label: 'Quyền', render: (row) => roleLabels[row.role] || row.role || '-' },
          { key: 'position', label: 'Chức danh' },
          { key: 'phone', label: 'Điện thoại' },
          { key: 'email', label: 'Email' },
          { key: 'username', label: 'Tên đăng nhập' },
          { key: 'idNumber', label: 'CCCD/Mã định danh' },
          { key: 'dob', label: 'Ngày sinh', render: (row) => formatDate(row.dob) },
          { key: 'gender', label: 'Giới tính', render: (row) => genderLabels[row.gender] || '-' },
          { key: 'hireDate', label: 'Ngày vào làm', render: (row) => formatDate(row.hireDate) },
          { key: 'address', label: 'Địa chỉ' },
          {
            key: 'active',
            label: 'Trạng thái',
            render: (row) => (
              <span className={`inline-flex rounded-md border px-2 py-1 text-xs font-medium ${
                row.active === false
                  ? 'border-rose-200 bg-rose-50 text-rose-700'
                  : 'border-emerald-200 bg-emerald-50 text-emerald-700'
              }`}>
                {activeLabel(row)}
              </span>
            ),
          },
        ]}
        rowActions={(row) => (
          <>
            <button type="button" onClick={() => openDetail(row)} title="Xem hồ sơ nhân viên" className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100">
              <Eye size={16} />
            </button>
            <button type="button" onClick={() => openEditForm(row)} title="Chỉnh sửa nhân viên" className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100">
              <Edit3 size={16} />
            </button>
            <button type="button" onClick={() => updateStatus(row)} title={row.active === false ? 'Mở lại tài khoản' : 'Khóa tài khoản'} className={`inline-flex h-9 w-9 items-center justify-center rounded-lg border ${
              row.active === false
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                : 'border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100'
            }`}>
              {row.active === false ? <LockOpen size={16} /> : <LockKeyhole size={16} />}
            </button>
          </>
        )}
      />

      <Modal isOpen={isFormOpen} onClose={closeForm} title={editingEmployee ? 'Chỉnh sửa nhân viên' : 'Thêm nhân viên'} size="wide">
        <form onSubmit={handleSubmit} className="space-y-6">
          <div className="flex flex-col gap-5 border-b border-slate-100 pb-5 sm:flex-row sm:items-center">
            <EmployeeAvatar employee={{ fullName: formData.fullName, avatarUrl: formData.avatarUrl }} size="large" />
            <div className="min-w-0 flex-1">
              <p className="text-sm font-semibold text-slate-900">Ảnh nhân viên</p>
              <p className="mt-1 text-xs text-slate-500">JPG, PNG hoặc WebP; dung lượng tối đa 1,5 MB.</p>
              <div className="mt-3 flex flex-wrap gap-2">
                <label className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50">
                  <ImagePlus size={16} />
                  Chọn ảnh
                  <input type="file" accept="image/*" onChange={handleAvatarChange} className="sr-only" />
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

          <div>
            <h4 className="text-sm font-semibold text-slate-950">Thông tin nhân sự</h4>
            <div className="mt-3 grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700">Mã nhân viên</label>
                <input required minLength={2} maxLength={30} pattern={EMPLOYEE_CODE_INPUT_PATTERN} title="Mã nhân viên chỉ gồm chữ, số, dấu chấm, gạch ngang hoặc gạch dưới" name="employeeCode" value={formData.employeeCode} onChange={handleChange} placeholder="VD: NV0001" className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Họ và tên</label>
                <input required minLength={2} maxLength={100} name="fullName" value={formData.fullName} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Giới tính</label>
                <select required name="gender" value={formData.gender} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100">
                  <option value="MALE">Nam</option>
                  <option value="FEMALE">Nữ</option>
                  <option value="OTHER">Khác</option>
                </select>
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">CCCD/Mã định danh</label>
                <input required inputMode="numeric" pattern={ID_NUMBER_INPUT_PATTERN} title="Nhập CMND 9 số, mã số thuế 10 số, CCCD 12 số hoặc MST chi nhánh" maxLength={14} name="idNumber" value={formData.idNumber} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Chức danh</label>
                <input name="position" value={formData.position} onChange={handleChange} placeholder="Tự điền theo quyền nếu để trống" className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Quyền hệ thống</label>
                <select required name="role" value={formData.role} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100">
                  <option value="MANAGER">Quản lý vận hành</option>
                  <option value="ACCOUNTANT">Kế toán</option>
                </select>
              </div>
              <div className="border-t border-slate-100 pt-4 sm:col-span-2">
                <p className="mb-3 text-sm font-semibold text-slate-900">Mốc thời gian nhân sự</p>
                <div className="grid gap-4 lg:grid-cols-2">
                  <DateField
                    required
                    label="Ngày sinh"
                    name="dob"
                    value={formData.dob}
                    max={latestAdultBirthDate()}
                    onChange={handleChange}
                  />
                  <DateField
                    required
                    label="Ngày vào làm"
                    name="hireDate"
                    value={formData.hireDate}
                    max={toDateInputValue()}
                    popupAlign="right"
                    onChange={handleChange}
                  />
                </div>
              </div>
            </div>
          </div>

          <div>
            <h4 className="text-sm font-semibold text-slate-950">Liên hệ và tài khoản</h4>
            <div className="mt-3 grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700">Số điện thoại</label>
                <input required type="tel" inputMode="tel" pattern={PHONE_INPUT_PATTERN} title="Nhập số điện thoại Việt Nam hợp lệ, ví dụ 0901234567" maxLength={20} name="phone" value={formData.phone} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Email</label>
                <input required type="email" maxLength={254} name="email" value={formData.email} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-slate-700">Địa chỉ</label>
                <input required minLength={5} maxLength={255} name="address" value={formData.address} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">Tên đăng nhập</label>
                <input required minLength={6} maxLength={50} name="username" value={formData.username} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
              <div>
                <label className="block text-sm font-medium text-slate-700">{editingEmployee ? 'Mật khẩu mới' : 'Mật khẩu'}</label>
                <input required={!editingEmployee} minLength={8} maxLength={72} type="password" name="password" value={formData.password} onChange={handleChange} placeholder={editingEmployee ? 'Để trống nếu không đổi' : 'Tối thiểu 8 ký tự'} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100" />
              </div>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={closeForm} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Hủy</button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting ? 'Đang lưu...' : editingEmployee ? 'Cập nhật nhân viên' : 'Lưu nhân viên'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={isDetailOpen} onClose={closeDetail} title="Hồ sơ nhân viên" size="wide" variant="detail">
        {detailLoading && !selectedEmployee ? (
          <p className="py-10 text-center text-sm text-slate-500">Đang tải hồ sơ...</p>
        ) : selectedEmployee && (
          <div className="space-y-6">
            <div className="flex flex-col gap-5 border-b border-emerald-100 pb-5 sm:flex-row sm:items-center">
              <EmployeeAvatar employee={selectedEmployee} size="large" />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-center gap-2">
                  <h3 className="text-xl font-semibold text-slate-950">{selectedEmployee.fullName || selectedEmployee.username}</h3>
                  <span className={`rounded-md border px-2 py-1 text-xs font-medium ${selectedEmployee.active === false ? 'border-rose-200 bg-rose-50 text-rose-700' : 'border-emerald-200 bg-emerald-50 text-emerald-700'}`}>
                    {activeLabel(selectedEmployee)}
                  </span>
                </div>
                <p className="mt-1 text-sm text-slate-500">{selectedEmployee.position || roleLabels[selectedEmployee.role] || '-'}</p>
                <p className="mt-2 text-sm font-medium text-emerald-700">{selectedEmployee.employeeCode || '-'}</p>
              </div>
              <button type="button" onClick={() => { closeDetail(); openEditForm(selectedEmployee); }} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100">
                <Edit3 size={16} />
                Chỉnh sửa
              </button>
            </div>

            <dl className="grid gap-x-8 gap-y-5 rounded-lg border border-emerald-200 bg-emerald-50/40 p-5 sm:grid-cols-2 lg:grid-cols-3">
              <DetailField icon={UserRound} label="Tên đăng nhập" value={selectedEmployee.username} />
              <DetailField icon={BadgeCheck} label="Quyền hệ thống" value={roleLabels[selectedEmployee.role] || selectedEmployee.role} />
              <DetailField icon={BriefcaseBusiness} label="Ngày vào làm" value={formatDate(selectedEmployee.hireDate)} />
              <DetailField icon={Phone} label="Số điện thoại" value={selectedEmployee.phone} />
              <DetailField icon={Mail} label="Email" value={selectedEmployee.email} />
              <DetailField icon={BadgeCheck} label="CCCD/Mã định danh" value={selectedEmployee.idNumber} />
              <DetailField label="Ngày sinh" value={formatDate(selectedEmployee.dob)} />
              <DetailField label="Giới tính" value={genderLabels[selectedEmployee.gender] || '-'} />
              <DetailField label="Ngày tạo tài khoản" value={formatDateTime(selectedEmployee.createdAt)} />
              <div className="sm:col-span-2 lg:col-span-3">
                <DetailField icon={MapPin} label="Địa chỉ" value={selectedEmployee.address} />
              </div>
            </dl>
          </div>
        )}
      </Modal>
    </>
  );
}
