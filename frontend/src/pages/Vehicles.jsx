import { useContext, useEffect, useMemo, useState } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';
import { Check, Edit3, Eye, EyeOff, Info, ListFilter, Plus, RefreshCw, RotateCcw, Trash2, Weight, Wrench } from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import DateField from '../components/DateField';
import CheckboxGroup from '../components/CheckboxGroup';
import Modal from '../components/Modal';
import SearchableSelect from '../components/SearchableSelect';
import { toDateInputValue } from '../utils/dates';
import { getLicensePlateSuggestion } from '../utils/licensePlates';
import { VEHICLE_LICENSE_PLATE_INPUT_PATTERN } from '../utils/validation';
import {
  isKnownVehicleType,
  isVehicleCapacityOutsideRange,
  vehicleTypeCapacityRange,
  vehicleTypeGroups,
  vehicleTypeLabelWithRange,
  vehicleTypeWithCapacity,
} from '../utils/vehicleTypes';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const today = () => toDateInputValue();
const reservedTripStatuses = new Set(['CREATED', 'ASSIGNED', 'IN_PROGRESS']);

const getTripDate = (value) => {
  if (!value || Number.isNaN(Date.parse(value))) return null;
  return String(value).slice(0, 10);
};

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
  AVAILABLE: 'Sẵn sàng',
  IN_TRIP: 'Đang chạy',
  MAINTENANCE: 'Bảo dưỡng',
  INACTIVE: 'Ngưng hoạt động',
};

const statusStyles = {
  AVAILABLE: 'bg-emerald-50 text-emerald-700',
  IN_TRIP: 'bg-sky-50 text-sky-700',
  MAINTENANCE: 'bg-amber-50 text-amber-700',
  INACTIVE: 'bg-slate-100 text-slate-600',
};

const initialVehicleForm = {
  licensePlate: '',
  vehicleType: '',
  capacity: '',
  status: 'AVAILABLE',
};

const initialVehicleFilters = {
  vehicleId: '',
  status: '',
  vehicleType: '',
  capacityMin: '',
  capacityMax: '',
};

const getInitialMaintenanceForm = () => ({
  vehicleId: '',
  maintenanceTypes: ['PERIODIC'],
  cost: '',
  maintenanceDate: today(),
  nextMaintenanceDate: '',
  description: '',
});

export default function Vehicles() {
  const { user } = useContext(AuthContext);
  const isAdmin = user?.role === 'ADMIN';
  const [vehicles, setVehicles] = useState([]);
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isMaintenanceModalOpen, setIsMaintenanceModalOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isMaintenanceSubmitting, setIsMaintenanceSubmitting] = useState(false);
  const [selectedVehicle, setSelectedVehicle] = useState(null);
  const [editingVehicle, setEditingVehicle] = useState(null);
  const [formData, setFormData] = useState(initialVehicleForm);
  const [maintenanceForm, setMaintenanceForm] = useState(getInitialMaintenanceForm);
  const [vehicleFilters, setVehicleFilters] = useState(initialVehicleFilters);
  const [hiddenPlateSuggestionPrefix, setHiddenPlateSuggestionPrefix] = useState('');
  const plateSuggestion = getLicensePlateSuggestion(formData.licensePlate);
  const isPlateSuggestionVisible = plateSuggestion
    && hiddenPlateSuggestionPrefix !== plateSuggestion.prefix;
  const selectedCapacityRange = vehicleTypeCapacityRange(formData.vehicleType);
  const isCapacityOutsideRange = isVehicleCapacityOutsideRange(
    formData.vehicleType,
    formData.capacity,
  );
  const activeVehicleFilterCount = Object.values(vehicleFilters).filter((value) => value !== '').length;
  const filteredVehicles = useMemo(() => vehicles.filter((vehicle) => {
    if (vehicleFilters.vehicleId && vehicle.id !== vehicleFilters.vehicleId) return false;
    if (vehicleFilters.status && vehicle.status !== vehicleFilters.status) return false;
    if (vehicleFilters.vehicleType && vehicle.vehicleType !== vehicleFilters.vehicleType) return false;

    const capacity = Number(vehicle.capacity);
    const minimumCapacity = Number(vehicleFilters.capacityMin);
    const maximumCapacity = Number(vehicleFilters.capacityMax);
    if (vehicleFilters.capacityMin && (!Number.isFinite(capacity) || capacity < minimumCapacity)) return false;
    if (vehicleFilters.capacityMax && (!Number.isFinite(capacity) || capacity > maximumCapacity)) return false;

    return true;
  }), [vehicleFilters, vehicles]);

  useEffect(() => {
    fetchVehicles();
  }, []);

  const fetchVehicles = async () => {
    setLoading(true);
    try {
      const [vehicleRes, tripRes] = await Promise.all([
        api.get('/vehicles'),
        api.get('/trips'),
      ]);
      setVehicles(getResult(vehicleRes, []));
      setTrips(getResult(tripRes, []));
    } catch (error) {
      toast.error('Không thể tải danh sách xe');
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const hasMaintenanceTripConflict = (vehicleId, maintenanceDate) =>
    trips.some((trip) => {
      if (trip.vehicleId !== vehicleId || !reservedTripStatuses.has(trip.status)) return false;
      if (trip.status === 'IN_PROGRESS' || !maintenanceDate) return true;

      const tripDate = getTripDate(trip.startTime);
      return !tripDate || tripDate === maintenanceDate;
    });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: name === 'licensePlate' ? value.toUpperCase() : value,
    }));
  };

  const applyPlateSuggestion = (vehicleType) => {
    if (!vehicleType) return;
    setFormData((prev) => ({
      ...prev,
      vehicleType,
    }));
    setHiddenPlateSuggestionPrefix(plateSuggestion?.prefix || '');
  };

  const handleMaintenanceChange = (e) => {
    const { name, value } = e.target;
    setMaintenanceForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleVehicleFilterChange = (event) => {
    const { name, value } = event.target;
    setVehicleFilters((current) => ({ ...current, [name]: value }));
  };

  const openCreateModal = () => {
    setEditingVehicle(null);
    setFormData(initialVehicleForm);
    setHiddenPlateSuggestionPrefix('');
    setIsModalOpen(true);
  };

  const openEditModal = (vehicle) => {
    setEditingVehicle(vehicle);
    setFormData({
      licensePlate: vehicle.licensePlate || '',
      vehicleType: vehicle.vehicleType || '',
      capacity: vehicle.capacity ?? '',
      status: vehicle.status || 'AVAILABLE',
    });
    setHiddenPlateSuggestionPrefix('');
    setIsModalOpen(true);
  };

  const closeVehicleModal = () => {
    setIsModalOpen(false);
    setEditingVehicle(null);
    setFormData(initialVehicleForm);
    setHiddenPlateSuggestionPrefix('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setIsSubmitting(true);
    try {
      const payload = {
        ...formData,
        capacity: Number(formData.capacity),
      };

      if (editingVehicle) {
        await api.put(`/vehicles/${editingVehicle.id}`, payload);
        toast.success('Cập nhật xe thành công!');
      } else {
        await api.post('/vehicles', payload);
        toast.success('Thêm xe thành công!');
      }
      closeVehicleModal();
      fetchVehicles();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể lưu xe');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async (vehicle) => {
    if (!window.confirm(`Xóa xe "${vehicle.licensePlate || vehicle.id}"? Thao tác này không thể hoàn tác.`)) {
      return;
    }

    try {
      await api.delete(`/vehicles/${vehicle.id}`);
      toast.success('Đã xóa xe');
      fetchVehicles();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể xóa xe');
    }
  };

  const openMaintenanceModal = (vehicle) => {
    setSelectedVehicle(vehicle);
    setMaintenanceForm({
      ...getInitialMaintenanceForm(),
      vehicleId: vehicle.id,
      maintenanceDate: today(),
    });
    setIsMaintenanceModalOpen(true);
  };

  const closeMaintenanceModal = () => {
    setSelectedVehicle(null);
    setMaintenanceForm(getInitialMaintenanceForm());
    setIsMaintenanceModalOpen(false);
  };

  const handleMaintenanceSubmit = async (e) => {
    e.preventDefault();
    if (maintenanceForm.maintenanceTypes.length === 0) {
      toast.error('Cần chọn ít nhất một loại bảo dưỡng');
      return;
    }

    if (hasMaintenanceTripConflict(maintenanceForm.vehicleId, maintenanceForm.maintenanceDate)) {
      toast.error('Xe đang chạy hoặc có chuyến trùng ngày bảo dưỡng');
      return;
    }

    setIsMaintenanceSubmitting(true);
    try {
      await api.post('/maintenances', {
        ...maintenanceForm,
        description: maintenanceForm.description.trim() || null,
        cost: Number(maintenanceForm.cost || 0),
        maintenanceDate: maintenanceForm.maintenanceDate || null,
        nextMaintenanceDate: maintenanceForm.nextMaintenanceDate || null,
      });
      toast.success('Đã chuyển xe sang trạng thái bảo dưỡng');
      closeMaintenanceModal();
      fetchVehicles();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể đưa xe vào bảo dưỡng');
    } finally {
      setIsMaintenanceSubmitting(false);
    }
  };

  return (
    <>
      <section className="space-y-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-slate-950">Quản lý phương tiện</h1>
            <p className="mt-1 text-sm text-slate-500">Theo dõi biển số, loại xe, sức chứa, trạng thái điều phối và chuyển xe sang bảo dưỡng.</p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={fetchVehicles}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100"
            >
              <RefreshCw size={16} />
              Tải lại
            </button>
            <button
              type="button"
              onClick={openCreateModal}
              className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white transition-colors hover:bg-emerald-700 focus:ring-4 focus:ring-emerald-100"
            >
              <Plus size={16} />
              Thêm mới
            </button>
          </div>
        </div>

        <div className="rounded-lg border border-slate-200 bg-slate-50 p-3">
          <div className="flex flex-col gap-2 sm:flex-row sm:items-center sm:justify-between">
            <div className="flex flex-wrap items-center gap-2">
              <ListFilter size={16} className="text-emerald-700" aria-hidden="true" />
              <h2 className="text-sm font-semibold text-slate-900">Bộ lọc phương tiện</h2>
              <span className="rounded-md bg-white px-2 py-0.5 text-xs font-medium text-slate-500 ring-1 ring-slate-200">
                {filteredVehicles.length}/{vehicles.length} xe
              </span>
            </div>
            <button
              type="button"
              onClick={() => setVehicleFilters(initialVehicleFilters)}
              disabled={activeVehicleFilterCount === 0}
              className="inline-flex h-8 items-center justify-center gap-1.5 self-start rounded-md border border-slate-300 bg-white px-2.5 text-xs font-medium text-slate-600 hover:bg-slate-100 disabled:cursor-not-allowed disabled:opacity-40 sm:self-auto"
            >
              <RotateCcw size={15} aria-hidden="true" />
              Xóa bộ lọc{activeVehicleFilterCount > 0 ? ` (${activeVehicleFilterCount})` : ''}
            </button>
          </div>

          <div className="mt-3 grid grid-cols-1 gap-2.5 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5">
            <SearchableSelect
              label="Phương tiện"
              name="vehicleId"
              value={vehicleFilters.vehicleId}
              allLabel="Tất cả phương tiện"
              placeholder="Tìm biển số, mã xe..."
              options={vehicles.map((vehicle) => ({
                value: vehicle.id,
                label: [vehicle.licensePlate, shortId(vehicle.id)].filter(Boolean).join(' - ') || vehicle.id,
              }))}
              onChange={handleVehicleFilterChange}
            />

            <label className="block min-w-0">
              <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">Trạng thái</span>
              <select name="status" value={vehicleFilters.status} onChange={handleVehicleFilterChange} className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100">
                <option value="">Tất cả trạng thái</option>
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
            </label>

            <label className="block min-w-0">
              <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">Loại xe</span>
              <select name="vehicleType" value={vehicleFilters.vehicleType} onChange={handleVehicleFilterChange} className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100">
                <option value="">Tất cả loại xe</option>
                {vehicleTypeGroups.map((group) => (
                  <optgroup key={group.label} label={group.label}>
                    {group.options.map((type) => (
                      <option key={type.value} value={type.value}>{type.label}</option>
                    ))}
                  </optgroup>
                ))}
              </select>
            </label>

            <label className="block min-w-0">
              <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">Tải trọng từ (tấn)</span>
              <input type="number" min="0" step="0.1" name="capacityMin" value={vehicleFilters.capacityMin} onChange={handleVehicleFilterChange} placeholder="0" className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100" />
            </label>

            <label className="block min-w-0">
              <span className="flex min-h-6 items-center text-xs font-medium text-slate-600">Tải trọng đến (tấn)</span>
              <input type="number" min="0" step="0.1" name="capacityMax" value={vehicleFilters.capacityMax} onChange={handleVehicleFilterChange} placeholder="Không giới hạn" className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100" />
            </label>
          </div>
        </div>

        {loading ? (
          <p className="text-sm text-slate-500">Đang tải dữ liệu...</p>
        ) : (
          <div className="grid grid-cols-1 gap-6 md:grid-cols-2 lg:grid-cols-3">
            {filteredVehicles.length > 0 ? filteredVehicles.map((vehicle) => {
              const hasTripConflict = hasMaintenanceTripConflict(vehicle.id, today());
              const canStartMaintenance = vehicle.status === 'AVAILABLE' && !hasTripConflict;

              return (
                <div key={vehicle.id} className="rounded-lg border border-slate-200 bg-white p-6 shadow-sm">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-xl font-semibold text-slate-950">{vehicle.licensePlate || '-'}</h3>
                      <p className="text-sm text-slate-500">{vehicleTypeWithCapacity(vehicle.vehicleType, vehicle.capacity)}</p>
                    </div>
                    <span className={`shrink-0 rounded-full px-3 py-1.5 text-sm font-medium ${statusStyles[vehicle.status] || 'bg-slate-100 text-slate-700'}`}>
                      {statusLabels[vehicle.status] || vehicle.status || '-'}
                    </span>
                  </div>

                  <div className="mt-6 space-y-2 text-sm text-slate-600">
                    <p><strong>Mã xe:</strong> {shortId(vehicle.id)}</p>
                    {hasTripConflict && <p className="text-amber-700"><strong>Điều phối:</strong> Có chuyến trùng ngày hoặc đang chạy</p>}
                  </div>

                  <div className="mt-5 grid gap-2 border-t border-slate-100 pt-4">
                    <button
                      type="button"
                      onClick={() => openMaintenanceModal(vehicle)}
                      disabled={!canStartMaintenance}
                      title={canStartMaintenance ? 'Đưa xe vào bảo dưỡng' : 'Xe hiện không thể chuyển sang bảo dưỡng'}
                      className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 text-sm font-medium text-amber-700 hover:bg-amber-100 disabled:cursor-not-allowed disabled:border-slate-200 disabled:bg-slate-100 disabled:text-slate-400"
                    >
                      <Wrench size={16} />
                      Đưa vào bảo dưỡng
                    </button>
                    <div className={`grid gap-2 ${isAdmin ? 'grid-cols-2' : ''}`}>
                      <button
                        type="button"
                        onClick={() => openEditModal(vehicle)}
                        title="Chỉnh sửa xe"
                        className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-sky-200 bg-sky-50 px-3 text-sm font-medium text-sky-700 hover:bg-sky-100"
                      >
                        <Edit3 size={16} />
                        Sửa
                      </button>
                      {isAdmin && (
                        <button
                          type="button"
                          onClick={() => handleDelete(vehicle)}
                          title="Xóa xe"
                          className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100"
                        >
                          <Trash2 size={16} />
                          Xóa
                        </button>
                      )}
                    </div>
                  </div>
                </div>
              );
            }) : (
              <p className="col-span-3 text-slate-500">
                {vehicles.length > 0 && activeVehicleFilterCount > 0
                  ? 'Không có phương tiện phù hợp với bộ lọc.'
                  : 'Chưa có dữ liệu xe nào.'}
              </p>
            )}
          </div>
        )}
      </section>

      <Modal isOpen={isModalOpen} onClose={closeVehicleModal} title={editingVehicle ? 'Chỉnh sửa xe' : 'Thêm xe mới'}>
        <form onSubmit={handleSubmit} className="mt-2 space-y-4">
          <div>
            <label className="block text-sm font-medium text-slate-700">Biển số xe</label>
            <input
              required
              type="text"
              pattern={VEHICLE_LICENSE_PLATE_INPUT_PATTERN}
              title="Nhập đúng mẫu biển số, ví dụ 51C-123.45 hoặc 29H-12345"
              maxLength={12}
              name="licensePlate"
              value={formData.licensePlate}
              onChange={handleChange}
              autoCapitalize="characters"
              autoComplete="off"
              spellCheck={false}
              placeholder="Ví dụ: 51C-123.45"
              className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 uppercase shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm"
            />
            {isPlateSuggestionVisible && (
              <div
                aria-live="polite"
                className="mt-2 flex items-start gap-2 rounded-md border border-emerald-200 bg-emerald-50 px-3 py-2.5 text-sm text-emerald-900"
              >
                <Info size={17} className="mt-0.5 shrink-0" aria-hidden="true" />
                <div className="min-w-0 flex-1">
                  <p className="font-medium">{plateSuggestion.title}</p>
                  <p className="mt-0.5 leading-5 opacity-80">{plateSuggestion.description}</p>
                  <div className="mt-2 flex flex-wrap gap-2">
                    {plateSuggestion.suggestedVehicleTypes.map((vehicleType) => {
                      const isSelected = formData.vehicleType === vehicleType;

                      return (
                        <button
                          key={vehicleType}
                          type="button"
                          aria-pressed={isSelected}
                          onClick={() => applyPlateSuggestion(vehicleType)}
                          className={`inline-flex min-h-9 items-center gap-2 rounded-md border px-3 py-1.5 font-medium ${
                            isSelected
                              ? 'border-emerald-600 bg-emerald-600 text-white'
                              : 'border-emerald-300 bg-white text-emerald-700 hover:bg-emerald-100'
                          }`}
                        >
                          <Check size={15} aria-hidden="true" />
                          {formData.capacity
                            ? vehicleTypeWithCapacity(vehicleType, formData.capacity)
                            : vehicleTypeLabelWithRange(vehicleType)}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <button
                  type="button"
                  onClick={() => setHiddenPlateSuggestionPrefix(plateSuggestion.prefix)}
                  title="Ẩn gợi ý"
                  aria-label="Ẩn gợi ý loại xe"
                  className="inline-flex size-8 shrink-0 items-center justify-center rounded-md text-emerald-700 hover:bg-emerald-100"
                >
                  <EyeOff size={17} aria-hidden="true" />
                </button>
              </div>
            )}
            {plateSuggestion && !isPlateSuggestionVisible && (
              <button
                type="button"
                onClick={() => setHiddenPlateSuggestionPrefix('')}
                className="mt-2 inline-flex min-h-9 items-center gap-2 rounded-md border border-slate-300 bg-white px-3 py-1.5 text-sm font-medium text-slate-600 hover:border-emerald-300 hover:bg-emerald-50 hover:text-emerald-700"
              >
                <Eye size={16} aria-hidden="true" />
                Hiện gợi ý cho {plateSuggestion.prefix}
              </button>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700">Loại xe</label>
            <select required name="vehicleType" value={formData.vehicleType} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm">
              <option value="">Chọn loại xe vận tải</option>
              {formData.vehicleType && !isKnownVehicleType(formData.vehicleType) && (
                <option value={formData.vehicleType}>{formData.vehicleType} (dữ liệu cũ)</option>
              )}
              {vehicleTypeGroups.map((group) => (
                <optgroup key={group.label} label={group.label}>
                  {group.options.map((type) => (
                    <option key={type.value} value={type.value}>{vehicleTypeLabelWithRange(type.value)}</option>
                  ))}
                </optgroup>
              ))}
            </select>
            {selectedCapacityRange && (
              <p className="mt-2 flex items-center gap-2 text-sm text-slate-600">
                <Weight size={16} className="shrink-0 text-emerald-600" aria-hidden="true" />
                Tải trọng hàng tham khảo: <strong>{selectedCapacityRange}</strong>
              </p>
            )}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-slate-700">Sức chứa (tấn)</label>
              <input required type="number" min="0.1" step="0.1" name="capacity" value={formData.capacity} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
              {isCapacityOutsideRange && (
                <p className="mt-1.5 text-xs leading-5 text-amber-700">
                  Ngoài khoảng tham khảo {selectedCapacityRange}; hãy đối chiếu tải trọng trên đăng kiểm.
                </p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Trạng thái</label>
              <select
                required
                name="status"
                value={formData.status}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm"
              >
                {(formData.status === 'IN_TRIP' || formData.status === 'MAINTENANCE') && (
                  <option value={formData.status}>{statusLabels[formData.status]}</option>
                )}
                <option value="AVAILABLE">Sẵn sàng</option>
                <option value="INACTIVE">Ngưng hoạt động</option>
              </select>
            </div>
          </div>
          <div className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={closeVehicleModal} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting ? 'Đang lưu...' : editingVehicle ? 'Cập nhật xe' : 'Lưu xe'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={isMaintenanceModalOpen} onClose={closeMaintenanceModal} title="Đưa xe vào bảo dưỡng" size="wide">
        <form onSubmit={handleMaintenanceSubmit} className="mt-2 space-y-4">
          <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm text-slate-700">
            <span className="font-medium">Xe:</span> {selectedVehicle?.licensePlate || '-'} - {vehicleTypeWithCapacity(selectedVehicle?.vehicleType, selectedVehicle?.capacity)}
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <CheckboxGroup
                label="Loại bảo dưỡng"
                name="maintenanceTypes"
                options={maintenanceTypes}
                values={maintenanceForm.maintenanceTypes}
                onChange={handleMaintenanceChange}
              />
            </div>
            <DateField required label="Ngày bảo dưỡng" name="maintenanceDate" value={maintenanceForm.maintenanceDate} min={today()} onChange={handleMaintenanceChange} />
            <DateField label="Lần tiếp theo" name="nextMaintenanceDate" value={maintenanceForm.nextMaintenanceDate} min={maintenanceForm.maintenanceDate} onChange={handleMaintenanceChange} />
            <div>
              <label className="block text-sm font-medium text-slate-700">Tổng chi phí</label>
              <input type="number" min="0" step="1" name="cost" value={maintenanceForm.cost} onChange={handleMaintenanceChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Ghi chú</label>
              <textarea maxLength={500} name="description" rows={3} value={maintenanceForm.description} onChange={handleMaintenanceChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={closeMaintenanceModal} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isMaintenanceSubmitting} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isMaintenanceSubmitting ? 'Đang chuyển...' : 'Chuyển sang bảo dưỡng'}
            </button>
          </div>
        </form>
      </Modal>
    </>
  );
}
