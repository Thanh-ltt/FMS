import { useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Calculator, CalendarRange, CheckCircle2, CirclePlay, Edit3, FileText, Settings, WalletCards, XCircle } from 'lucide-react';
import ContractDepositsModal from '../components/ContractDepositsModal';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import { cargoTypeLabel, cargoTypes } from '../utils/cargoTypes';
import { contractValueModeLabels, getContractValueMode } from '../utils/contracts';
import { formatDate, toDateInputValue } from '../utils/dates';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;

const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const formatCurrency = (value) => value ? `${Number(value).toLocaleString('vi-VN')} đ` : '-';

const customerCode = (customer) => customer?.username || shortId(customer?.id);

const customerLabel = (customer) => {
  if (!customer) return 'Không rõ khách hàng';
  return `${customer.name || 'Khách hàng'} - ${customerCode(customer)}`;
};

const contractCustomerCode = (row) => row.customerUsername || shortId(row.customerId);

const depositScopeLabels = { CONTRACT: 'Theo hợp đồng', TRIP: 'Theo từng chuyến' };
const depositTypeLabels = { FIXED: 'Số tiền cố định', PERCENTAGE: 'Phần trăm giá trị' };
const depositUsageLabels = { APPLY_TO_INVOICE: 'Cấn trừ hóa đơn', SECURITY_HOLD: 'Giữ bảo đảm' };
const contractStatusLabels = {
  DRAFT: 'Bản nháp',
  ACTIVE: 'Đang hiệu lực',
  COMPLETED: 'Đã hoàn tất',
  CANCELLED: 'Đã hủy',
};
const contractStatusStyles = {
  DRAFT: 'border-slate-200 bg-slate-100 text-slate-700',
  ACTIVE: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  COMPLETED: 'border-sky-200 bg-sky-50 text-sky-700',
  CANCELLED: 'border-rose-200 bg-rose-50 text-rose-700',
};
const contractActionContent = {
  activate: {
    title: 'Kích hoạt hợp đồng',
    description: 'Hợp đồng sẽ được phép sử dụng để tạo và điều phối chuyến đi trong thời hạn hiệu lực.',
    confirmLabel: 'Kích hoạt',
    buttonClass: 'bg-emerald-600 hover:bg-emerald-700',
  },
  complete: {
    title: 'Hoàn tất hợp đồng',
    description: 'Chỉ có thể hoàn tất khi hợp đồng không còn chuyến mới tạo, đã phân công hoặc đang vận chuyển.',
    confirmLabel: 'Hoàn tất',
    buttonClass: 'bg-sky-600 hover:bg-sky-700',
  },
  cancel: {
    title: 'Hủy hợp đồng',
    description: 'Hợp đồng đã hủy sẽ không thể dùng để tạo hoặc bắt đầu chuyến đi mới.',
    confirmLabel: 'Hủy hợp đồng',
    buttonClass: 'bg-rose-600 hover:bg-rose-700',
  },
};

const formatDepositPolicy = (contract) => {
  if (!contract.depositRequired) return 'Không yêu cầu';
  const value = contract.depositType === 'PERCENTAGE'
    ? `${Number(contract.depositValue || 0).toLocaleString('vi-VN')}%`
    : formatCurrency(contract.depositValue);
  return `${value} · ${depositScopeLabels[contract.depositScope] || '-'} · ${depositUsageLabels[contract.depositUsage] || '-'}`;
};

const formatContractValue = (contract) => getContractValueMode(contract) === 'PER_TRIP'
  ? contractValueModeLabels.PER_TRIP
  : `Thỏa thuận · ${formatCurrency(contract.contractValue)}`;

const positiveNumber = (value) => {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : null;
};

const depositPolicySummary = (form) => {
  const depositValue = positiveNumber(form.depositValue);
  const handling = form.depositUsage === 'SECURITY_HOLD'
    ? 'Khoản cọc được giữ bảo đảm và không cấn vào hóa đơn.'
    : 'Khoản cọc có thể được cấn vào hóa đơn.';

  if (form.depositScope === 'TRIP' && form.depositType === 'PERCENTAGE') {
    return `Mỗi chuyến yêu cầu ${depositValue == null ? 'một tỷ lệ' : `${depositValue}%`} trên cước thực tế. ${handling}`;
  }
  if (form.depositScope === 'TRIP') {
    return `Mỗi chuyến yêu cầu cọc cố định ${depositValue == null ? '-' : formatCurrency(depositValue)}. ${handling}`;
  }
  if (form.depositType === 'FIXED') {
    return `Toàn hợp đồng yêu cầu cọc cố định ${depositValue == null ? '-' : formatCurrency(depositValue)}. ${handling}`;
  }

  const contractValue = positiveNumber(form.contractValue);
  const requiredAmount = contractValue != null && depositValue != null
    ? contractValue * depositValue / 100
    : null;
  return `Toàn hợp đồng yêu cầu ${depositValue == null ? 'một tỷ lệ' : `${depositValue}%`} trên giá trị thỏa thuận${requiredAmount == null ? '' : `, tương đương ${formatCurrency(requiredAmount)}`}. ${handling}`;
};

const matchesContractPeriod = (row, period) => {
  const today = toDateInputValue();
  const startDate = row.startDate ? String(row.startDate).slice(0, 10) : '';
  const endDate = row.endDate ? String(row.endDate).slice(0, 10) : '';

  if (period === 'UPCOMING') return Boolean(startDate && startDate > today);
  if (period === 'EXPIRED') return Boolean(endDate && endDate < today);
  if (period === 'ACTIVE') {
    return (!startDate || startDate <= today) && (!endDate || endDate >= today);
  }

  return true;
};

const emptyContractForm = {
  contractCode: '',
  customerId: '',
  signedDate: '',
  startDate: '',
  endDate: '',
  cargoType: 'DRY',
  cargoDescription: '',
  freightRatePerTonKm: '',
  valueMode: 'PER_TRIP',
  contractValue: '',
  depositRequired: false,
  depositScope: 'TRIP',
  depositType: 'PERCENTAGE',
  depositValue: '30',
  depositUsage: 'APPLY_TO_INVOICE',
  depositDueDays: '0',
  depositTerms: '',
};

const getCargoRate = (rates, cargoType) => {
  const configuredRate = rates.find((rate) => rate.cargoType === cargoType)?.ratePerTonKm;
  const defaultRate = cargoTypes.find((type) => type.value === cargoType)?.defaultRate;
  return configuredRate ?? defaultRate ?? '';
};

const buildRateForm = (rates) => cargoTypes.reduce((form, type) => {
  form[type.value] = String(getCargoRate(rates, type.value) || '');
  return form;
}, {});

const CONTRACT_CODE_PATTERN = /^HD(\d+)$/i;

const generateNextContractCode = (existingContracts) => {
  let maxIndex = 0;
  for (const contract of existingContracts) {
    const code = contract.contractCode || '';
    const match = code.trim().match(CONTRACT_CODE_PATTERN);
    if (match) {
      const num = parseInt(match[1], 10);
      if (num > maxIndex) maxIndex = num;
    }
  }
  const next = maxIndex + 1;
  return `HD${String(next).padStart(2, '0')}`;
};

export default function Contracts() {
  const { user } = useContext(AuthContext);
  const canManageContracts = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isRateModalOpen, setIsRateModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isRateSaving, setIsRateSaving] = useState(false);
  const [customers, setCustomers] = useState([]);
  const [cargoRates, setCargoRates] = useState([]);
  const [rateForm, setRateForm] = useState({});
  const [isLoadingCustomers, setIsLoadingCustomers] = useState(false);
  const [isGeneratingCode, setIsGeneratingCode] = useState(false);
  const [formData, setFormData] = useState(emptyContractForm);
  const [editingContract, setEditingContract] = useState(null);
  const [depositContract, setDepositContract] = useState(null);
  const [statusAction, setStatusAction] = useState(null);
  const [isStatusSubmitting, setIsStatusSubmitting] = useState(false);

  const loadCustomers = async () => {
    setIsLoadingCustomers(true);
    try {
      const response = await api.get('/customers');
      setCustomers(getResult(response, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải danh sách khách hàng');
      setCustomers([]);
    } finally {
      setIsLoadingCustomers(false);
    }
  };

  const loadCargoRates = async () => {
    try {
      const response = await api.get('/cargo-rates');
      const rates = getResult(response, []);
      setCargoRates(rates);
      setRateForm(buildRateForm(rates));
      setFormData((current) => {
        if (current.freightRatePerTonKm !== '') return current;

        const rate = getCargoRate(rates, current.cargoType);
        return rate === '' ? current : { ...current, freightRatePerTonKm: String(rate) };
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải đơn giá loại hàng');
      setRateForm(buildRateForm([]));
    }
  };

  useEffect(() => {
    loadCustomers();
    loadCargoRates();
  }, []);

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    if (name === 'cargoType') {
      const rate = getCargoRate(cargoRates, value);
      setFormData((prev) => ({
        ...prev,
        cargoType: value,
        freightRatePerTonKm: rate === '' ? prev.freightRatePerTonKm : String(rate),
      }));
      return;
    }

    if (name === 'valueMode') {
      setFormData((prev) => ({
        ...prev,
        valueMode: value,
        contractValue: value === 'PER_TRIP' ? '' : prev.contractValue,
        depositScope: value === 'PER_TRIP'
          && prev.depositScope === 'CONTRACT'
          && prev.depositType === 'PERCENTAGE'
          ? 'TRIP'
          : prev.depositScope,
      }));
      return;
    }

    if (name === 'depositScope') {
      setFormData((prev) => {
        const mustUseFixedAmount = value === 'CONTRACT'
          && prev.valueMode === 'PER_TRIP'
          && prev.depositType === 'PERCENTAGE';
        return {
          ...prev,
          depositScope: value,
          depositType: mustUseFixedAmount ? 'FIXED' : prev.depositType,
          depositValue: mustUseFixedAmount ? '' : prev.depositValue,
        };
      });
      return;
    }

    if (name === 'depositType' && value === 'PERCENTAGE') {
      setFormData((prev) => ({
        ...prev,
        depositType: value,
        depositScope: prev.valueMode === 'PER_TRIP' ? 'TRIP' : prev.depositScope,
      }));
      return;
    }

    setFormData((prev) => ({ ...prev, [name]: type === 'checkbox' ? checked : value }));
  };

  const openCreateModal = async () => {
    setEditingContract(null);
    setFormData({
      ...emptyContractForm,
      freightRatePerTonKm: String(getCargoRate(cargoRates, 'DRY') || ''),
      contractCode: 'Đang tính...',
    });
    setIsModalOpen(true);
    setIsGeneratingCode(true);
    if (customers.length === 0) loadCustomers();
    if (cargoRates.length === 0) loadCargoRates();
    try {
      const response = await api.get('/contracts');
      const existing = getResult(response, []);
      const nextCode = generateNextContractCode(existing);
      setFormData((prev) => ({ ...prev, contractCode: nextCode }));
    } catch {
      setFormData((prev) => ({ ...prev, contractCode: 'HD01' }));
    } finally {
      setIsGeneratingCode(false);
    }
  };

  const openEditModal = (contract) => {
    setEditingContract(contract);
    setFormData({
      contractCode: contract.contractCode || '',
      customerId: contract.customerId || '',
      signedDate: contract.signedDate || '',
      startDate: contract.startDate || '',
      endDate: contract.endDate || '',
      cargoType: contract.cargoType || 'DRY',
      cargoDescription: contract.cargoDescription || '',
      freightRatePerTonKm: contract.freightRatePerTonKm == null ? '' : String(contract.freightRatePerTonKm),
      valueMode: getContractValueMode(contract),
      contractValue: contract.contractValue == null ? '' : String(contract.contractValue),
      depositRequired: Boolean(contract.depositRequired),
      depositScope: contract.depositScope || 'TRIP',
      depositType: contract.depositType || 'PERCENTAGE',
      depositValue: contract.depositValue == null ? '30' : String(contract.depositValue),
      depositUsage: contract.depositUsage || 'APPLY_TO_INVOICE',
      depositDueDays: contract.depositDueDays == null ? '0' : String(contract.depositDueDays),
      depositTerms: contract.depositTerms || '',
    });
    setIsModalOpen(true);
  };

  const closeCreateModal = () => {
    setIsModalOpen(false);
    setEditingContract(null);
    setFormData({
      ...emptyContractForm,
      freightRatePerTonKm: String(getCargoRate(cargoRates, 'DRY') || ''),
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const contractValue = formData.valueMode === 'AGREED_VALUE'
      ? positiveNumber(formData.contractValue)
      : null;
    if (formData.valueMode === 'AGREED_VALUE' && contractValue == null) {
      toast.error('Giá trị thỏa thuận phải lớn hơn 0');
      return;
    }
    if (formData.depositRequired
        && formData.depositScope === 'CONTRACT'
        && formData.depositType === 'PERCENTAGE'
        && formData.valueMode !== 'AGREED_VALUE') {
      toast.error('Cọc theo % toàn hợp đồng cần có giá trị thỏa thuận');
      return;
    }

    setIsSubmitting(true);
    try {
      const payload = {
        ...formData,
        freightRatePerTonKm: formData.freightRatePerTonKm === '' ? null : Number(formData.freightRatePerTonKm),
        valueMode: formData.valueMode,
        contractValue,
        depositValue: formData.depositRequired && formData.depositValue !== '' ? Number(formData.depositValue) : null,
        depositDueDays: formData.depositRequired && formData.depositDueDays !== '' ? Number(formData.depositDueDays) : null,
      };
      if (editingContract) {
        await api.put(`/contracts/${editingContract.id}`, payload);
      } else {
        await api.post('/contracts', payload);
      }
      toast.success(editingContract ? 'Đã cập nhật hợp đồng' : 'Thêm hợp đồng thành công!');
      closeCreateModal();
      setRefreshKey((prev) => prev + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể lưu hợp đồng');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleRateChange = (cargoType, value) => {
    setRateForm((current) => ({ ...current, [cargoType]: value }));
  };

  const handleRateSubmit = async (event) => {
    event.preventDefault();

    const invalidRate = cargoTypes.some((type) => {
      const rate = Number(rateForm[type.value]);
      return !Number.isFinite(rate) || rate <= 0;
    });

    if (invalidRate) {
      toast.error('Đơn giá từng loại hàng phải lớn hơn 0');
      return;
    }

    setIsRateSaving(true);
    try {
      await Promise.all(cargoTypes.map((type) => (
        api.put(`/cargo-rates/${type.value}`, {
          cargoType: type.value,
          cargoLabel: type.label,
          ratePerTonKm: Number(rateForm[type.value]),
        })
      )));
      toast.success('Đã cập nhật đơn giá loại hàng');
      setIsRateModalOpen(false);
      await loadCargoRates();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật đơn giá loại hàng');
    } finally {
      setIsRateSaving(false);
    }
  };

  const handleContractStatus = async () => {
    if (!statusAction) return;
    setIsStatusSubmitting(true);
    try {
      await api.patch(`/contracts/${statusAction.contract.id}/${statusAction.action}`);
      const messages = {
        activate: 'Đã kích hoạt hợp đồng',
        complete: 'Đã hoàn tất hợp đồng',
        cancel: 'Đã hủy hợp đồng',
      };
      toast.success(messages[statusAction.action] || 'Đã cập nhật hợp đồng');
      setStatusAction(null);
      setRefreshKey((current) => current + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật trạng thái hợp đồng');
    } finally {
      setIsStatusSubmitting(false);
    }
  };

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý hợp đồng"
        description="Theo dõi hợp đồng khung, khách hàng, hàng hóa, đơn giá và thời hạn thực hiện."
        endpoint="/contracts"
        deleteEndpoint="/contracts"
        deleteLabel={(row) => `hợp đồng "${row.contractCode || row.id}"`}
        deleteSuccessMessage="Đã xóa hợp đồng"
        canDeleteRow={(row) => row.status === 'DRAFT' || row.status === 'CANCELLED'}
        emptyText="Chưa có hợp đồng nào."
        primaryColumns={['contractCode', 'status', 'customerName', 'cargoType', 'contractValue', 'depositPolicy']}
        filterGridClassName="grid-cols-1 sm:grid-cols-2 xl:grid-cols-12"
        onCreate={canManageContracts ? openCreateModal : undefined}
        headerActions={canManageContracts ? (
          <button
            type="button"
            onClick={() => {
              setIsRateModalOpen(true);
              if (cargoRates.length === 0) loadCargoRates();
            }}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-sky-200 bg-sky-50 px-3 text-sm font-medium text-sky-700 hover:bg-sky-100"
          >
            <Settings size={16} />
            Đơn giá loại hàng
          </button>
        ) : null}
        filters={[
          {
            key: 'status',
            label: 'Trạng thái',
            type: 'select',
            options: Object.entries(contractStatusLabels).map(([value, label]) => ({ value, label })),
            className: 'xl:col-span-3',
          },
          {
            key: 'contractCode',
            label: 'Hợp đồng',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm mã hợp đồng...',
            field: 'contractCode',
            deriveOptions: true,
            className: 'xl:col-span-3',
          },
          {
            key: 'customer',
            label: 'Khách hàng',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm tên, mã KH...',
            deriveOptions: true,
            getValue: (row) => row.customerId || row.customerUsername || row.customerName,
            getOptionLabel: (row) => row.customerName || row.customerUsername || shortId(row.customerId),
            className: 'xl:col-span-3',
          },
          {
            key: 'cargoType',
            label: 'Loại hàng',
            type: 'select',
            options: cargoTypes,
            className: 'xl:col-span-3',
          },
          {
            key: 'period',
            label: 'Theo mốc thời hạn',
            type: 'select',
            allLabel: 'Tất cả mốc thời hạn',
            options: [
              { value: 'UPCOMING', label: 'Chưa tới ngày bắt đầu' },
              { value: 'ACTIVE', label: 'Đang trong thời hạn' },
              { value: 'EXPIRED', label: 'Đã qua ngày kết thúc' },
            ],
            match: matchesContractPeriod,
            className: 'sm:col-span-2 xl:col-span-4',
          },
          { key: 'signedFrom', label: 'Ngày ký từ', type: 'date', field: 'signedDate', operator: 'gte', maxFilterKey: 'signedTo', className: 'xl:col-span-4' },
          { key: 'signedTo', label: 'Ngày ký đến', type: 'date', field: 'signedDate', operator: 'lte', minFilterKey: 'signedFrom', popupAlign: 'right', className: 'xl:col-span-4' },
        ]}
        columns={[
          { key: 'contractCode', label: 'Mã hợp đồng' },
          {
            key: 'status',
            label: 'Trạng thái',
            render: (row) => (
              <span className={`inline-flex whitespace-nowrap rounded-full border px-2 py-1 text-xs font-medium ${contractStatusStyles[row.status] || contractStatusStyles.DRAFT}`}>
                {contractStatusLabels[row.status] || row.status || '-'}
              </span>
            ),
          },
          { key: 'customerUsername', label: 'Mã KH', render: (row) => contractCustomerCode(row) },
          { key: 'customerName', label: 'Khách hàng', render: (row) => row.customerName || '-' },
          { key: 'cargoType', label: 'Loại hàng', render: (row) => cargoTypeLabel(row.cargoType), searchValue: (row) => cargoTypeLabel(row.cargoType) },
          { key: 'cargoDescription', label: 'Ghi chú hàng hóa', render: (row) => row.cargoDescription || '-' },
          { key: 'freightRatePerTonKm', label: 'Đơn giá', render: (row) => row.freightRatePerTonKm ? `${formatCurrency(row.freightRatePerTonKm)}/tấn/km` : '-' },
          { key: 'signedDate', label: 'Ngày ký', render: (row) => formatDate(row.signedDate) },
          { key: 'startDate', label: 'Ngày bắt đầu', render: (row) => formatDate(row.startDate) },
          { key: 'endDate', label: 'Ngày kết thúc', render: (row) => formatDate(row.endDate) },
          { key: 'contractValue', label: 'Cách xác định giá trị', render: formatContractValue, searchValue: formatContractValue },
          { key: 'depositPolicy', label: 'Chính sách cọc', render: formatDepositPolicy, detailClassName: 'sm:col-span-2' },
          { key: 'requiredDepositAmount', label: 'Mức cọc hợp đồng', render: (row) => row.depositScope === 'TRIP' && row.depositRequired ? 'Tính theo từng chuyến' : formatCurrency(row.requiredDepositAmount) },
          { key: 'depositDueDays', label: 'Hạn nộp cọc', render: (row) => row.depositRequired ? `${Number(row.depositDueDays || 0)} ngày sau khi ký` : '-' },
          { key: 'depositTerms', label: 'Điều khoản cọc', render: (row) => row.depositTerms || '-', detailClassName: 'sm:col-span-2 lg:col-span-3' },
        ]}
        rowActions={(row) => (
          <div className="flex justify-end gap-2">
            {canManageContracts && row.status === 'DRAFT' && (
              <button
                type="button"
                onClick={() => setStatusAction({ contract: row, action: 'activate' })}
                title="Kích hoạt hợp đồng"
                aria-label="Kích hoạt hợp đồng"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
              >
                <CirclePlay size={16} />
              </button>
            )}
            {canManageContracts && row.status === 'ACTIVE' && (
              <button
                type="button"
                onClick={() => setStatusAction({ contract: row, action: 'complete' })}
                title="Hoàn tất hợp đồng"
                aria-label="Hoàn tất hợp đồng"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100"
              >
                <CheckCircle2 size={16} />
              </button>
            )}
            {canManageContracts && (row.status === 'DRAFT' || row.status === 'ACTIVE') && (
              <button
                type="button"
                onClick={() => setStatusAction({ contract: row, action: 'cancel' })}
                title="Hủy hợp đồng"
                aria-label="Hủy hợp đồng"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100"
              >
                <XCircle size={16} />
              </button>
            )}
            <button
              type="button"
              onClick={() => setDepositContract(row)}
              title="Quản lý tiền cọc"
              aria-label="Quản lý tiền cọc"
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100"
            >
              <WalletCards size={16} />
            </button>
            {canManageContracts && row.status === 'DRAFT' && (
              <button
                type="button"
                onClick={() => openEditModal(row)}
                title="Sửa hợp đồng"
                aria-label="Sửa hợp đồng"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100"
              >
                <Edit3 size={16} />
              </button>
            )}
          </div>
        )}
      />

      <Modal isOpen={isModalOpen} onClose={closeCreateModal} title={editingContract ? 'Sửa hợp đồng' : 'Thêm hợp đồng mới'} size="wide">
        <form onSubmit={handleSubmit} className="space-y-4 mt-2">
          <div>
            <label className="block text-sm font-medium text-slate-700">Mã hợp đồng</label>
            <input
              disabled
              type="text"
              name="contractCode"
              value={isGeneratingCode && !editingContract ? 'Đang tính...' : formData.contractCode}
              className="mt-1 block w-full rounded-md border border-slate-300 bg-slate-100 px-3 py-2 text-sm font-medium text-slate-700 shadow-sm sm:text-sm"
            />
            {!editingContract && (
              <p className="mt-1 text-xs text-slate-400">Mã hợp đồng được tự động sinh, không thể chỉnh sửa.</p>
            )}
          </div>
          <div>
            <label className="block text-sm font-medium text-slate-700">Khách hàng</label>
            <select
              required
              name="customerId"
              value={formData.customerId}
              onChange={handleChange}
              disabled={Boolean(editingContract) || isLoadingCustomers || customers.length === 0}
              className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 disabled:bg-slate-100 disabled:text-slate-500 sm:text-sm"
            >
              <option value="">
                {isLoadingCustomers ? 'Đang tải khách hàng...' : 'Chọn khách hàng'}
              </option>
              {customers.map((customer) => (
                <option key={customer.id} value={customer.id}>
                  {customerLabel(customer)}
                </option>
              ))}
            </select>
            {customers.length === 0 && !isLoadingCustomers && (
              <p className="mt-2 text-xs text-slate-500">
                Chưa có khách hàng để liên kết. Hãy thêm khách hàng trước khi tạo hợp đồng.
              </p>
            )}
          </div>
          <fieldset className="rounded-lg border border-slate-200 px-4 pb-4 pt-2">
            <legend className="px-2">
              <span className="inline-flex items-center gap-2 text-sm font-semibold text-slate-800">
                <CalendarRange size={17} className="text-emerald-600" aria-hidden="true" />
                Thời hạn hợp đồng
              </span>
            </legend>
            <div className="grid gap-4 pt-1 md:grid-cols-3">
              <DateField
                required
                label="Ngày ký kết"
                name="signedDate"
                value={formData.signedDate}
                max={toDateInputValue()}
                onChange={handleChange}
              />
              <DateField
                required
                label="Hiệu lực từ ngày"
                name="startDate"
                value={formData.startDate}
                min={formData.signedDate}
                onChange={handleChange}
              />
              <DateField
                required
                label="Hiệu lực đến ngày"
                name="endDate"
                value={formData.endDate}
                min={formData.startDate || formData.signedDate}
                popupAlign="right"
                onChange={handleChange}
              />
            </div>
          </fieldset>
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Loại hàng hóa</label>
              <select
                required
                name="cargoType"
                value={formData.cargoType}
                onChange={handleChange}
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm"
              >
                {cargoTypes.map((type) => (
                  <option key={type.value} value={type.value}>{type.label}</option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Ghi chú hàng hóa vận chuyển</label>
              <textarea
                required
                maxLength={500}
                name="cargoDescription"
                rows={3}
                value={formData.cargoDescription}
                onChange={handleChange}
                placeholder="Ví dụ: linh kiện điện tử, hàng đông lạnh 0-5°C, vật liệu đóng bao..."
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm"
              />
            </div>
          </div>
          <section className="rounded-lg border border-slate-200 p-4">
            <div className="flex items-center gap-2">
              <Calculator size={17} className="text-emerald-600" aria-hidden="true" />
              <h3 className="text-sm font-semibold text-slate-900">Cách xác định giá trị hợp đồng</h3>
            </div>

            <div className="mt-3 grid gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1 sm:grid-cols-2">
              {[
                {
                  value: 'PER_TRIP',
                  label: contractValueModeLabels.PER_TRIP,
                  description: 'Chưa xác định tổng tiền khi ký',
                  icon: Calculator,
                },
                {
                  value: 'AGREED_VALUE',
                  label: contractValueModeLabels.AGREED_VALUE,
                  description: 'Dùng làm giá trị tham chiếu và tính cọc',
                  icon: FileText,
                },
              ].map((option) => {
                const Icon = option.icon;
                const selected = formData.valueMode === option.value;
                return (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => handleChange({ target: { name: 'valueMode', value: option.value } })}
                    className={`flex min-h-16 min-w-0 items-center gap-3 rounded-md px-3 py-2 text-left transition ${
                      selected
                        ? 'bg-white text-emerald-800 shadow-sm ring-1 ring-emerald-200'
                        : 'text-slate-600 hover:bg-white/70 hover:text-slate-900'
                    }`}
                  >
                    <Icon size={18} className="shrink-0" aria-hidden="true" />
                    <span className="min-w-0">
                      <span className="block text-sm font-semibold leading-5">{option.label}</span>
                      <span className="block text-xs leading-5 text-slate-500">{option.description}</span>
                    </span>
                  </button>
                );
              })}
            </div>

            <div className="mt-4 grid gap-4 sm:grid-cols-2">
              <div>
                <label className="block text-sm font-medium text-slate-700">Đơn giá (VNĐ/tấn/km)</label>
                <input required type="number" min="1" step="1" name="freightRatePerTonKm" value={formData.freightRatePerTonKm} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
              </div>
              {formData.valueMode === 'AGREED_VALUE' ? (
                <div>
                  <label className="block text-sm font-medium text-slate-700">Giá trị thỏa thuận (VNĐ)</label>
                  <input required type="number" min="1" step="1" name="contractValue" value={formData.contractValue} onChange={handleChange} placeholder="Ví dụ: 50000000" className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm" />
                  <p className="mt-1 text-xs leading-5 text-slate-500">
                    Dùng để tham chiếu và tính cọc toàn hợp đồng; cước thực tế vẫn được chốt theo từng chuyến.
                  </p>
                </div>
              ) : (
                <div className="border-l-2 border-emerald-400 pl-3 sm:self-end sm:pb-1">
                  <p className="text-xs font-medium uppercase text-slate-500">Công thức mỗi chuyến</p>
                  <p className="mt-1 text-sm font-semibold text-slate-800">Quãng đường × Trọng lượng × Đơn giá</p>
                </div>
              )}
            </div>
          </section>
          <section className="rounded-lg border border-slate-200 bg-slate-50 p-4">
            <label className="flex cursor-pointer items-start gap-3">
              <input
                type="checkbox"
                name="depositRequired"
                checked={formData.depositRequired}
                onChange={handleChange}
                className="mt-0.5 h-5 w-5 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
              />
              <span>
                <span className="block text-sm font-semibold text-slate-900">Yêu cầu tiền cọc</span>
                <span className="mt-0.5 block text-xs text-slate-500">Quy định mức cọc; tiền thực nhận được quản lý ngay trên hợp đồng.</span>
              </span>
            </label>

            {formData.depositRequired && (
              <div className="mt-4 grid gap-4 border-t border-slate-200 pt-4 sm:grid-cols-2">
                <div>
                  <label className="block text-sm font-medium text-slate-700">Phạm vi áp dụng</label>
                  <select required name="depositScope" value={formData.depositScope} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                    {Object.entries(depositScopeLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Cách tính</label>
                  <select required name="depositType" value={formData.depositType} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                    {Object.entries(depositTypeLabels).map(([value, label]) => (
                      <option
                        key={value}
                        value={value}
                        disabled={value === 'PERCENTAGE' && formData.valueMode === 'PER_TRIP' && formData.depositScope === 'CONTRACT'}
                      >
                        {label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">{formData.depositType === 'PERCENTAGE' ? 'Tỷ lệ cọc (%)' : 'Số tiền cọc (VNĐ)'}</label>
                  <input required type="number" min="0.01" max={formData.depositType === 'PERCENTAGE' ? 100 : undefined} step={formData.depositType === 'PERCENTAGE' ? '0.01' : '1'} name="depositValue" value={formData.depositValue} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Xử lý tiền cọc</label>
                  <select required name="depositUsage" value={formData.depositUsage} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                    {Object.entries(depositUsageLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700">Hạn nộp sau khi ký (ngày)</label>
                  <input required type="number" min="0" step="1" name="depositDueDays" value={formData.depositDueDays} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
                </div>
                <div className="border-l-2 border-emerald-400 pl-3 sm:col-span-2">
                  <p className="text-xs font-medium uppercase text-slate-500">Cách áp dụng tiền cọc</p>
                  <p className="mt-1 text-sm leading-6 text-slate-700">{depositPolicySummary(formData)}</p>
                </div>
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-slate-700">Điều khoản hoàn/khấu trừ cọc</label>
                  <textarea maxLength={1000} name="depositTerms" rows={3} value={formData.depositTerms} onChange={handleChange} placeholder="Ví dụ: hoàn sau khi đối soát công nợ và không còn chi phí phát sinh." className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
                </div>
              </div>
            )}
          </section>
          <div className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={closeCreateModal} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed">
              {isSubmitting ? 'Đang lưu...' : 'Lưu hợp đồng'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal isOpen={isRateModalOpen} onClose={() => setIsRateModalOpen(false)} title="Đơn giá theo loại hàng" size="wide">
        <form onSubmit={handleRateSubmit} className="mt-2 space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            {cargoTypes.map((type) => (
              <div key={type.value}>
                <label className="block text-sm font-medium text-slate-700">{type.label}</label>
                <input
                  required
                  type="number"
                  min="1"
                  step="1"
                  value={rateForm[type.value] || ''}
                  onChange={(event) => handleRateChange(type.value, event.target.value)}
                  className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 sm:text-sm"
                />
              </div>
            ))}
          </div>
          <div className="mt-6 flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={() => setIsRateModalOpen(false)} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isRateSaving} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed">
              {isRateSaving ? 'Đang lưu...' : 'Lưu đơn giá'}
            </button>
          </div>
        </form>
      </Modal>

      <ContractDepositsModal
        contract={depositContract}
        isOpen={Boolean(depositContract)}
        onClose={() => setDepositContract(null)}
        onChanged={() => setRefreshKey((current) => current + 1)}
      />

      <Modal
        isOpen={Boolean(statusAction)}
        onClose={() => setStatusAction(null)}
        title={statusAction ? contractActionContent[statusAction.action].title : 'Cập nhật hợp đồng'}
        variant="detail"
      >
        {statusAction && (
          <div className="space-y-4">
            <div className="rounded-lg border border-emerald-200 bg-emerald-50 p-4">
              <p className="font-semibold text-emerald-950">{statusAction.contract.contractCode}</p>
              <p className="mt-1 text-sm text-emerald-800">{statusAction.contract.customerName || statusAction.contract.customerUsername || '-'}</p>
            </div>
            <p className="text-sm leading-6 text-slate-600">{contractActionContent[statusAction.action].description}</p>
            <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
              <button type="button" onClick={() => setStatusAction(null)} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Đóng</button>
              <button
                type="button"
                onClick={handleContractStatus}
                disabled={isStatusSubmitting}
                className={`rounded-lg px-4 py-2 text-sm font-medium text-white disabled:cursor-not-allowed disabled:opacity-50 ${contractActionContent[statusAction.action].buttonClass}`}
              >
                {isStatusSubmitting ? 'Đang xử lý...' : contractActionContent[statusAction.action].confirmLabel}
              </button>
            </div>
          </div>
        )}
      </Modal>
    </>
  );
}
