import { useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { Edit3, Eye, FileText, Phone, ReceiptText, UserRound } from 'lucide-react';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import api from '../services/api';
import { cargoTypeLabel } from '../utils/cargoTypes';
import { contractValueModeLabels, isPerTripContract } from '../utils/contracts';
import { formatDate } from '../utils/dates';
import {
  ID_NUMBER_INPUT_PATTERN,
  PHONE_INPUT_PATTERN,
  latestPastDate,
} from '../utils/validation';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;

const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const formatContractValue = (contract) => isPerTripContract(contract)
  ? contractValueModeLabels.PER_TRIP
  : `Thỏa thuận · ${formatCurrency(contract.contractValue)}`;

const DetailField = ({ label, value }) => (
  <div>
    <dt className="text-xs font-medium uppercase tracking-normal text-emerald-700">{label}</dt>
    <dd className="mt-1 break-words text-sm font-medium text-slate-900">{value || '-'}</dd>
  </div>
);

const initialFormData = {
  name: '',
  phone: '',
  idNumber: '',
  dob: '',
  address: '',
  username: '',
  password: '',
};

export default function Customers() {
  const [refreshKey, setRefreshKey] = useState(0);
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [accountMode, setAccountMode] = useState('none');
  const [formData, setFormData] = useState(initialFormData);
  const [isDetailOpen, setIsDetailOpen] = useState(false);
  const [detailLoading, setDetailLoading] = useState(false);
  const [selectedCustomer, setSelectedCustomer] = useState(null);
  const [contracts, setContracts] = useState([]);
  const [invoices, setInvoices] = useState([]);

  const summary = useMemo(() => {
    const agreedContracts = contracts.filter((contract) => !isPerTripContract(contract));
    const contractValue = agreedContracts.reduce(
      (total, contract) => total + Number(contract.contractValue || 0),
      0
    );
    const invoiceValue = invoices.reduce(
      (total, invoice) => total + Number(invoice.totalAmount || 0),
      0
    );

    return {
      contractCount: contracts.length,
      agreedContractCount: agreedContracts.length,
      invoiceCount: invoices.length,
      contractValue,
      invoiceValue,
    };
  }, [contracts, invoices]);

  const handleFormChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const resetCreateForm = () => {
    setFormData(initialFormData);
    setAccountMode('none');
    setEditingCustomer(null);
    setIsCreateOpen(false);
  };

  const openCreateCustomer = () => {
    setEditingCustomer(null);
    setFormData(initialFormData);
    setAccountMode('none');
    setIsCreateOpen(true);
  };

  const openEditCustomer = (customer) => {
    setEditingCustomer(customer);
    setFormData({
      ...initialFormData,
      name: customer.name || '',
      phone: customer.phone || '',
      idNumber: customer.idNumber || '',
      dob: customer.dob || '',
      address: customer.address || '',
    });
    setAccountMode('none');
    setIsCreateOpen(true);
  };

  const handleCreateCustomer = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);

    const customerPayload = {
      name: formData.name.trim(),
      phone: formData.phone.trim(),
      idNumber: formData.idNumber.trim(),
      dob: formData.dob || null,
      address: formData.address.trim(),
    };
    const username = formData.username.trim();

    try {
      if (editingCustomer) {
        await api.put(`/customers/${editingCustomer.id}`, customerPayload);
      } else if (accountMode === 'new') {
        await api.post('/customers/with-new-account', {
          ...customerPayload,
          username,
          password: formData.password,
        });
      } else if (accountMode === 'existing') {
        await api.post('/customers/with-existing-account', {
          ...customerPayload,
          username,
        });
      } else {
        await api.post('/customers', customerPayload);
      }

      toast.success(editingCustomer ? 'Cập nhật khách hàng thành công' : 'Thêm khách hàng thành công');
      resetCreateForm();
      setRefreshKey((current) => current + 1);
    } catch (error) {
      toast.error(error.response?.data?.message || (editingCustomer
        ? 'Không thể cập nhật khách hàng'
        : 'Không thể thêm khách hàng'));
    } finally {
      setIsSubmitting(false);
    }
  };

  const openCustomerDetail = async (customer) => {
    setSelectedCustomer(customer);
    setContracts([]);
    setInvoices([]);
    setIsDetailOpen(true);
    setDetailLoading(true);

    try {
      const [customerResponse, contractResponse, invoiceResponse] = await Promise.all([
        api.get(`/customers/${customer.id}`),
        api.get(`/customers/${customer.id}/contracts`),
        api.get(`/customers/${customer.id}/invoices`),
      ]);

      setSelectedCustomer(getResult(customerResponse, customer));
      setContracts(getResult(contractResponse, []));
      setInvoices(getResult(invoiceResponse, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải hồ sơ khách hàng');
    } finally {
      setDetailLoading(false);
    }
  };

  const closeCustomerDetail = () => {
    setIsDetailOpen(false);
    setSelectedCustomer(null);
    setContracts([]);
    setInvoices([]);
  };

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý khách hàng"
        description="Quản lý hồ sơ khách hàng, thông tin liên hệ và dữ liệu liên quan đến hợp đồng, hóa đơn."
        endpoint="/customers"
        deleteEndpoint="/customers"
        deleteLabel={(row) => `khách hàng "${row.name || row.id}"`}
        deleteSuccessMessage="Đã xóa khách hàng"
        emptyText="Chưa có khách hàng nào."
        showDetails={false}
        primaryColumns={['name', 'phone', 'username', 'address']}
        onCreate={openCreateCustomer}
        filters={[
          {
            key: 'customerId',
            label: 'Khách hàng',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm tên, SĐT...',
            field: 'id',
            deriveOptions: true,
            getOptionLabel: (row) => [row.name, row.phone].filter(Boolean).join(' - ') || row.id,
          },
          {
            key: 'accountStatus',
            label: 'Liên kết tài khoản',
            type: 'select',
            options: [
              { value: 'LINKED', label: 'Đã có tài khoản' },
              { value: 'UNLINKED', label: 'Chưa có tài khoản' },
            ],
            getValue: (row) => row.username ? 'LINKED' : 'UNLINKED',
          },
          { key: 'dobFrom', label: 'Sinh từ ngày', type: 'date', field: 'dob', operator: 'gte', maxFilterKey: 'dobTo' },
          { key: 'dobTo', label: 'Sinh đến ngày', type: 'date', field: 'dob', operator: 'lte', minFilterKey: 'dobFrom', popupAlign: 'right' },
        ]}
        columns={[
          { key: 'name', label: 'Tên khách hàng' },
          { key: 'phone', label: 'Điện thoại' },
          { key: 'idNumber', label: 'CCCD/MST' },
          { key: 'dob', label: 'Ngày sinh', render: (row) => formatDate(row.dob) },
          { key: 'username', label: 'Tài khoản' },
          { key: 'address', label: 'Địa chỉ' },
        ]}
        rowActions={(row) => (
          <>
            <button
              type="button"
              onClick={() => openCustomerDetail(row)}
              title="Xem hồ sơ khách hàng"
              aria-label="Xem hồ sơ khách hàng"
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 transition hover:bg-emerald-100"
            >
              <Eye size={16} />
            </button>
            <button
              type="button"
              onClick={() => openEditCustomer(row)}
              title="Chỉnh sửa khách hàng"
              aria-label="Chỉnh sửa khách hàng"
              className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 transition hover:bg-sky-100"
            >
              <Edit3 size={16} />
            </button>
          </>
        )}
      />

      <Modal
        isOpen={isCreateOpen}
        onClose={resetCreateForm}
        title={editingCustomer ? 'Chỉnh sửa khách hàng' : 'Thêm khách hàng'}
        size="wide"
      >
        <form onSubmit={handleCreateCustomer} className="space-y-5">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-700">Tên khách hàng</label>
              <input
                required
                minLength={2}
                maxLength={100}
                name="name"
                value={formData.name}
                onChange={handleFormChange}
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Điện thoại</label>
              <input
                required
                type="tel"
                inputMode="tel"
                pattern={PHONE_INPUT_PATTERN}
                title="Nhập số điện thoại Việt Nam hợp lệ, ví dụ 0901234567"
                maxLength={20}
                name="phone"
                value={formData.phone}
                onChange={handleFormChange}
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
              />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">CCCD/MST</label>
              <input
                required
                inputMode="numeric"
                pattern={ID_NUMBER_INPUT_PATTERN}
                title="Nhập CMND 9 số, mã số thuế 10 số, CCCD 12 số hoặc MST chi nhánh dạng 0123456789-001"
                maxLength={14}
                name="idNumber"
                value={formData.idNumber}
                onChange={handleFormChange}
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
              />
            </div>
            <DateField required label="Ngày sinh" name="dob" value={formData.dob} max={latestPastDate()} quickFill={false} onChange={handleFormChange} />
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Địa chỉ</label>
              <input
                required
                minLength={5}
                maxLength={255}
                name="address"
                value={formData.address}
                onChange={handleFormChange}
                className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
              />
            </div>
          </div>

          {!editingCustomer && (
            <div className="rounded-lg border border-slate-200 p-4">
              <p className="text-sm font-semibold text-slate-900">Tài khoản đăng nhập</p>
              <div className="mt-3 grid gap-2 sm:grid-cols-3">
                {[
                  { value: 'none', label: 'Chỉ lưu hồ sơ' },
                  { value: 'new', label: 'Tạo tài khoản mới' },
                  { value: 'existing', label: 'Liên kết tài khoản có sẵn' },
                ].map((option) => (
                  <button
                    key={option.value}
                    type="button"
                    onClick={() => setAccountMode(option.value)}
                    className={`rounded-lg border px-3 py-2 text-sm font-medium transition ${
                      accountMode === option.value
                        ? 'border-emerald-600 bg-emerald-50 text-emerald-700'
                        : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    {option.label}
                  </button>
                ))}
              </div>

              {accountMode !== 'none' && (
                <div className="mt-4 grid gap-4 sm:grid-cols-2">
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Tên đăng nhập</label>
                    <input
                      required
                      minLength={6}
                      maxLength={50}
                      name="username"
                      value={formData.username}
                      onChange={handleFormChange}
                      className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                    />
                  </div>
                  {accountMode === 'new' && (
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Mật khẩu</label>
                      <input
                        required
                        minLength={8}
                        maxLength={72}
                        type="password"
                        name="password"
                        value={formData.password}
                        onChange={handleFormChange}
                        className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                      />
                    </div>
                  )}
                </div>
              )}
            </div>
          )}

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button
              type="button"
              onClick={resetCreateForm}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50"
            >
              Hủy
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {isSubmitting ? 'Đang lưu...' : editingCustomer ? 'Cập nhật khách hàng' : 'Lưu khách hàng'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={isDetailOpen}
        onClose={closeCustomerDetail}
        title="Hồ sơ khách hàng"
        size="wide"
        variant="detail"
      >
        {selectedCustomer && (
          <div className="space-y-5">
            <div className="flex flex-col gap-4 rounded-lg border border-emerald-200 bg-emerald-50/70 p-4 sm:flex-row sm:items-start sm:justify-between">
              <div className="flex gap-3">
                <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-lg bg-emerald-600 text-white">
                  <UserRound size={22} />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-slate-950">{selectedCustomer.name || 'Khách hàng'}</h2>
                  <p className="mt-1 text-sm text-slate-500">Mã khách hàng: {selectedCustomer.id || '-'}</p>
                </div>
              </div>
              {selectedCustomer.phone && (
                <a
                  href={`tel:${selectedCustomer.phone}`}
                  className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-emerald-200 bg-white px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
                >
                  <Phone size={16} />
                  Gọi khách hàng
                </a>
              )}
            </div>

            {detailLoading ? (
              <div className="rounded-lg border border-slate-200 px-4 py-10 text-center text-sm text-slate-500">
                Đang tải hồ sơ khách hàng...
              </div>
            ) : (
              <>
                <dl className="grid gap-4 rounded-lg border border-emerald-200 bg-emerald-50/40 p-4 sm:grid-cols-2 lg:grid-cols-4">
                  <DetailField label="Điện thoại" value={selectedCustomer.phone} />
                  <DetailField label="CCCD/MST" value={selectedCustomer.idNumber} />
                  <DetailField label="Ngày sinh" value={formatDate(selectedCustomer.dob)} />
                  <DetailField label="Tài khoản" value={selectedCustomer.username} />
                  <DetailField label="Địa chỉ" value={selectedCustomer.address} />
                </dl>

                <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
                  <div className="rounded-lg border border-slate-200 p-4">
                    <p className="text-xs font-medium uppercase tracking-normal text-slate-500">Hợp đồng</p>
                    <p className="mt-2 text-2xl font-semibold text-slate-950">{summary.contractCount}</p>
                  </div>
                  <div className="rounded-lg border border-slate-200 p-4">
                    <p className="text-xs font-medium uppercase tracking-normal text-slate-500">Giá trị đã thỏa thuận</p>
                    <p className="mt-2 text-lg font-semibold text-slate-950">
                      {summary.agreedContractCount > 0 ? formatCurrency(summary.contractValue) : 'Chưa cố định'}
                    </p>
                  </div>
                  <div className="rounded-lg border border-slate-200 p-4">
                    <p className="text-xs font-medium uppercase tracking-normal text-slate-500">Hóa đơn</p>
                    <p className="mt-2 text-2xl font-semibold text-slate-950">{summary.invoiceCount}</p>
                  </div>
                  <div className="rounded-lg border border-slate-200 p-4">
                    <p className="text-xs font-medium uppercase tracking-normal text-slate-500">Tổng hóa đơn</p>
                    <p className="mt-2 text-lg font-semibold text-slate-950">{formatCurrency(summary.invoiceValue)}</p>
                  </div>
                </div>

                <div className="grid gap-5 lg:grid-cols-2">
                  <section className="rounded-lg border border-slate-200">
                    <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3">
                      <FileText size={17} className="text-emerald-700" />
                      <h3 className="text-sm font-semibold text-slate-900">Hợp đồng liên quan</h3>
                    </div>
                    <div className="divide-y divide-slate-100">
                      {contracts.length > 0 ? (
                        contracts.slice(0, 5).map((contract) => (
                          <div key={contract.id} className="px-4 py-3">
                            <div className="flex items-start justify-between gap-3">
                              <p className="font-medium text-slate-900">{contract.contractCode || contract.id}</p>
                              <span className="text-sm font-semibold text-emerald-700">
                                {formatContractValue(contract)}
                              </span>
                            </div>
                            <p className="mt-1 text-xs text-slate-500">
                              {formatDate(contract.startDate)} - {formatDate(contract.endDate)}
                            </p>
                            <p className="mt-1 text-xs text-slate-600">
                              Loại hàng: {cargoTypeLabel(contract.cargoType)}
                            </p>
                            <p className="mt-1 text-xs text-slate-600">
                              {contract.cargoDescription || 'Chưa có nội dung vận chuyển'}
                            </p>
                            {contract.freightRatePerTonKm && (
                              <p className="mt-1 text-xs text-slate-600">
                                Đơn giá: {formatCurrency(contract.freightRatePerTonKm)}/tấn/km
                              </p>
                            )}
                          </div>
                        ))
                      ) : (
                        <p className="px-4 py-6 text-sm text-slate-500">Chưa có hợp đồng liên quan.</p>
                      )}
                    </div>
                  </section>

                  <section className="rounded-lg border border-slate-200">
                    <div className="flex items-center gap-2 border-b border-slate-100 px-4 py-3">
                      <ReceiptText size={17} className="text-emerald-700" />
                      <h3 className="text-sm font-semibold text-slate-900">Hóa đơn liên quan</h3>
                    </div>
                    <div className="divide-y divide-slate-100">
                      {invoices.length > 0 ? (
                        invoices.slice(0, 5).map((invoice) => (
                          <div key={invoice.id} className="px-4 py-3">
                            <div className="flex items-start justify-between gap-3">
                              <p className="font-medium text-slate-900">{invoice.id}</p>
                              <span className="text-sm font-semibold text-emerald-700">
                                {formatCurrency(invoice.totalAmount)}
                              </span>
                            </div>
                            <p className="mt-1 text-xs text-slate-500">
                              Ngày lập {formatDate(invoice.issueDate)} · Hạn {formatDate(invoice.dueDate)}
                            </p>
                          </div>
                        ))
                      ) : (
                        <p className="px-4 py-6 text-sm text-slate-500">Chưa có hóa đơn liên quan.</p>
                      )}
                    </div>
                  </section>
                </div>
              </>
            )}
          </div>
        )}
      </Modal>
    </>
  );
}
