import { useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { CheckCircle2, Edit3, Play, XCircle } from 'lucide-react';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import CheckboxGroup from '../components/CheckboxGroup';
import Modal from '../components/Modal';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import { formatDate, formatDateTime, toDateInputValue } from '../utils/dates';
import { vehicleTypeWithCapacity } from '../utils/vehicleTypes';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const today = () => toDateInputValue();
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;

const maintenanceTypes = [
  { value: 'PERIODIC', label: 'Bảo dưỡng định kỳ' },
  { value: 'OIL_CHANGE', label: 'Thay dầu' },
  { value: 'TIRE', label: 'Lốp xe' },
  { value: 'BRAKE', label: 'Phanh' },
  { value: 'BATTERY_ELECTRIC', label: 'Điện / ắc quy' },
  { value: 'INSPECTION', label: 'Đăng kiểm' },
  { value: 'CLEANING', label: 'Vệ sinh xe' },
  { value: 'REPAIR', label: 'Sửa chữa đột xuất' },
  { value: 'OTHER', label: 'Khác' },
];

const statusLabels = {
  PENDING: 'Chờ xử lý',
  IN_PROGRESS: 'Đang bảo dưỡng',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
};

const statusStyles = {
  PENDING: 'bg-amber-50 text-amber-700',
  IN_PROGRESS: 'bg-sky-50 text-sky-700',
  COMPLETED: 'bg-emerald-50 text-emerald-700',
  CANCELLED: 'bg-rose-50 text-rose-700',
};

const vehicleStatusLabels = {
  AVAILABLE: 'Sẵn sàng',
  IN_TRIP: 'Đang chạy',
  MAINTENANCE: 'Bảo dưỡng',
  INACTIVE: 'Ngưng hoạt động',
};

const getInitialFormData = () => ({
  vehicleId: '',
  maintenanceTypes: ['PERIODIC'],
  cost: '',
  maintenanceDate: today(),
  nextMaintenanceDate: '',
  description: '',
});

const typeLabels = (row) => {
  const values = Array.isArray(row.maintenanceTypes) && row.maintenanceTypes.length > 0
    ? row.maintenanceTypes
    : [row.maintenanceType].filter(Boolean);
  return values
    .map((value) => maintenanceTypes.find((item) => item.value === value)?.label || value)
    .join(', ') || '-';
};

export default function Maintenance() {
  const { user } = useContext(AuthContext);
  const canManageMaintenance = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [vehiclesLoading, setVehiclesLoading] = useState(false);
  const [vehicles, setVehicles] = useState([]);
  const [formData, setFormData] = useState(getInitialFormData);
  const [editingMaintenance, setEditingMaintenance] = useState(null);
  const schedulableVehicles = vehicles.filter((vehicle) => vehicle.status !== 'INACTIVE');
  const vehicleOptions = editingMaintenance ? vehicles : schedulableVehicles;

  const loadVehicles = async () => {
    setVehiclesLoading(true);
    try {
      const response = await api.get('/vehicles');
      setVehicles(getResult(response, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách xe');
    } finally {
      setVehiclesLoading(false);
    }
  };

  useEffect(() => {
    loadVehicles();
  }, []);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const resetForm = () => {
    setFormData(getInitialFormData());
    setEditingMaintenance(null);
    setIsModalOpen(false);
  };

  const openCreateModal = () => {
    setEditingMaintenance(null);
    setFormData(getInitialFormData());
    setIsModalOpen(true);
    if (vehicles.length === 0) loadVehicles();
  };

  const openEditModal = (maintenance) => {
    setEditingMaintenance(maintenance);
    setFormData({
      vehicleId: maintenance.vehicleId || '',
      maintenanceTypes: Array.isArray(maintenance.maintenanceTypes) && maintenance.maintenanceTypes.length > 0
        ? maintenance.maintenanceTypes
        : [maintenance.maintenanceType].filter(Boolean),
      cost: maintenance.cost == null ? '' : String(maintenance.cost),
      maintenanceDate: maintenance.maintenanceDate || today(),
      nextMaintenanceDate: maintenance.nextMaintenanceDate || '',
      description: maintenance.description || '',
    });
    setIsModalOpen(true);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (formData.maintenanceTypes.length === 0) {
      toast.error('Cần chọn ít nhất một loại bảo dưỡng');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        maintenanceTypes: formData.maintenanceTypes,
        description: formData.description.trim() || null,
        cost: Number(formData.cost || 0),
        maintenanceDate: formData.maintenanceDate || null,
        nextMaintenanceDate: formData.nextMaintenanceDate || null,
      };
      if (editingMaintenance) {
        await api.put(`/maintenances/${editingMaintenance.id}`, payload);
      } else {
        await api.post('/maintenances', { ...payload, vehicleId: formData.vehicleId });
      }
      toast.success(editingMaintenance ? 'Đã cập nhật lịch bảo dưỡng' : 'Đã lập lịch bảo dưỡng');
      resetForm();
      setRefreshKey((current) => current + 1);
      loadVehicles();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể lập lịch bảo dưỡng');
    } finally {
      setIsSubmitting(false);
    }
  };

  const updateMaintenanceStatus = async (maintenanceId, action) => {
    try {
      await api.patch(`/maintenances/${maintenanceId}/${action}`);
      const messages = {
        start: 'Đã đưa xe vào bảo dưỡng',
        complete: 'Đã hoàn tất bảo dưỡng',
        cancel: 'Đã hủy bảo dưỡng',
      };
      toast.success(messages[action] || 'Đã cập nhật bảo dưỡng');
      setRefreshKey((current) => current + 1);
      loadVehicles();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật bảo dưỡng');
    }
  };

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý bảo dưỡng"
        description="Lập lịch, đưa xe vào xưởng đúng thời điểm và theo dõi chi phí bảo dưỡng."
        endpoint="/maintenances"
        deleteEndpoint="/maintenances"
        deleteLabel={(row) => `lịch bảo dưỡng xe "${row.vehiclePlate || shortId(row.vehicleId)}"`}
        deleteSuccessMessage="Đã xóa lịch bảo dưỡng"
        canDeleteRow={(row) => row.status === 'PENDING' || row.status === 'CANCELLED'}
        onDeleteSuccess={loadVehicles}
        emptyText="Chưa có lịch bảo dưỡng nào."
        primaryColumns={['vehiclePlate', 'maintenanceTypes', 'status', 'maintenanceDate', 'cost']}
        filterGridClassName="grid-cols-1 sm:grid-cols-2 xl:grid-cols-12"
        onCreate={canManageMaintenance ? openCreateModal : undefined}
        filters={[
          {
            key: 'vehicle',
            label: 'Phương tiện',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm biển số, mã xe...',
            deriveOptions: true,
            getValue: (row) => row.vehicleId || row.vehiclePlate,
            getOptionLabel: (row) => row.vehiclePlate || shortId(row.vehicleId),
            className: 'xl:col-span-4',
          },
          {
            key: 'maintenanceType',
            label: 'Loại bảo dưỡng',
            type: 'select',
            options: maintenanceTypes,
            getValue: (row) => Array.isArray(row.maintenanceTypes) && row.maintenanceTypes.length > 0
              ? row.maintenanceTypes
              : [row.maintenanceType].filter(Boolean),
            className: 'xl:col-span-4',
          },
          {
            key: 'status',
            label: 'Trạng thái',
            type: 'select',
            options: Object.entries(statusLabels).map(([value, label]) => ({ value, label })),
            className: 'xl:col-span-4',
          },
          { key: 'maintenanceFrom', label: 'Từ ngày', type: 'date', field: 'maintenanceDate', operator: 'gte', maxFilterKey: 'maintenanceTo', className: 'sm:col-start-1 xl:col-span-6 xl:col-start-1' },
          { key: 'maintenanceTo', label: 'Đến ngày', type: 'date', field: 'maintenanceDate', operator: 'lte', minFilterKey: 'maintenanceFrom', popupAlign: 'right', className: 'xl:col-span-6' },
          { key: 'costMin', label: 'Chi phí tối thiểu', type: 'number', field: 'cost', operator: 'gte', min: 0, step: 1000, placeholder: '0', className: 'sm:col-start-1 xl:col-span-6 xl:col-start-1' },
          { key: 'costMax', label: 'Chi phí tối đa', type: 'number', field: 'cost', operator: 'lte', min: 0, step: 1000, placeholder: 'Không giới hạn', className: 'xl:col-span-6' },
        ]}
        columns={[
          { key: 'vehiclePlate', label: 'Xe', render: (row) => row.vehiclePlate || shortId(row.vehicleId) },
          { key: 'maintenanceTypes', label: 'Loại bảo dưỡng', render: (row) => typeLabels(row), searchValue: (row) => typeLabels(row) },
          {
            key: 'status',
            label: 'Trạng thái',
            render: (row) => (
              <span className={`rounded-full px-3 py-1 text-xs font-medium ${statusStyles[row.status] || 'bg-slate-100 text-slate-700'}`}>
                {statusLabels[row.status] || row.status || '-'}
              </span>
            ),
          },
          { key: 'maintenanceDate', label: 'Ngày bảo dưỡng', render: (row) => formatDate(row.maintenanceDate) },
          { key: 'startedAt', label: 'Bắt đầu thực tế', render: (row) => formatDateTime(row.startedAt) },
          { key: 'completedAt', label: 'Hoàn tất thực tế', render: (row) => formatDateTime(row.completedAt) },
          { key: 'nextMaintenanceDate', label: 'Lần tiếp theo', render: (row) => formatDate(row.nextMaintenanceDate) },
          { key: 'cost', label: 'Chi phí', render: (row) => formatCurrency(row.cost) },
        ]}
        rowActions={(row) => (
          <div className="flex justify-end gap-2">
            {canManageMaintenance && row.status === 'PENDING' && (
              <button
                type="button"
                onClick={() => updateMaintenanceStatus(row.id, 'start')}
                disabled={row.maintenanceDate && row.maintenanceDate > today()}
                title={row.maintenanceDate && row.maintenanceDate > today() ? 'Chưa đến ngày bảo dưỡng' : 'Bắt đầu bảo dưỡng'}
                className="inline-flex h-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 px-3 text-sm font-medium text-sky-700 hover:bg-sky-100 disabled:cursor-not-allowed disabled:opacity-40"
              >
                <Play size={16} />
              </button>
            )}
            {canManageMaintenance && (row.status === 'PENDING' || row.status === 'IN_PROGRESS') && (
              <button
                type="button"
                onClick={() => openEditModal(row)}
                title="Cập nhật bảo dưỡng"
                aria-label="Cập nhật bảo dưỡng"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-700 hover:bg-slate-100"
              >
                <Edit3 size={16} />
              </button>
            )}
            {canManageMaintenance && row.status === 'IN_PROGRESS' && (
              <button
                type="button"
                onClick={() => updateMaintenanceStatus(row.id, 'complete')}
                title="Hoàn tất bảo dưỡng"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
              >
                <CheckCircle2 size={16} />
              </button>
            )}
            {canManageMaintenance && (row.status === 'PENDING' || row.status === 'IN_PROGRESS') && (
              <button
                type="button"
                onClick={() => updateMaintenanceStatus(row.id, 'cancel')}
                title="Hủy bảo dưỡng"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100"
              >
                <XCircle size={16} />
              </button>
            )}
          </div>
        )}
      />

      <Modal isOpen={isModalOpen} onClose={resetForm} title={editingMaintenance ? 'Cập nhật bảo dưỡng' : 'Lập lịch bảo dưỡng'} size="wide">
        <form onSubmit={handleSubmit} className="mt-2 space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-700">Xe</label>
              <select required name="vehicleId" value={formData.vehicleId} onChange={handleChange} disabled={Boolean(editingMaintenance) || vehiclesLoading || schedulableVehicles.length === 0} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 disabled:bg-slate-100 disabled:text-slate-500">
                <option value="">
                  {vehiclesLoading ? 'Đang tải...' : schedulableVehicles.length === 0 ? 'Không có xe có thể lập lịch' : 'Chọn xe'}
                </option>
                {vehicleOptions.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}>
                    {vehicle.licensePlate || shortId(vehicle.id)} - {vehicleTypeWithCapacity(vehicle.vehicleType, vehicle.capacity)} - {vehicleStatusLabels[vehicle.status] || vehicle.status || '-'}
                  </option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <CheckboxGroup
                label="Loại bảo dưỡng"
                name="maintenanceTypes"
                options={maintenanceTypes}
                values={formData.maintenanceTypes}
                onChange={handleChange}
              />
            </div>
            <DateField required disabled={editingMaintenance?.status === 'IN_PROGRESS'} label="Ngày bảo dưỡng" name="maintenanceDate" value={formData.maintenanceDate} min={today()} onChange={handleChange} />
            <DateField label="Lần tiếp theo" name="nextMaintenanceDate" value={formData.nextMaintenanceDate} min={formData.maintenanceDate} onChange={handleChange} />
            <div>
              <label className="block text-sm font-medium text-slate-700">{editingMaintenance?.status === 'IN_PROGRESS' ? 'Chi phí thực tế' : 'Chi phí dự kiến'}</label>
              <input type="number" min="0" step="1" name="cost" value={formData.cost} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Ghi chú</label>
              <input maxLength={500} name="description" value={formData.description} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetForm} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting ? 'Đang lưu...' : editingMaintenance ? 'Lưu thay đổi' : 'Lưu lịch bảo dưỡng'}
            </button>
          </div>
        </form>
      </Modal>
    </>
  );
}
