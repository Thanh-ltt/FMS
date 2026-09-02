import { useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { CheckCircle2, History, Loader2, Plus, QrCode, XCircle } from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import BankSelect from '../components/BankSelect';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import VietQrModal from '../components/VietQrModal';
import api from '../services/api';
import { formatDate, toDateInputValue } from '../utils/dates';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const formatKm = (value) => value ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} km` : '-';
const formatTon = (value) => value ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tấn` : '-';

const tripStatusLabels = {
  CREATED: 'Mới tạo',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang vận chuyển',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
};

const invoiceStatusLabels = {
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  OVERDUE: 'Quá hạn',
  CANCELLED: 'Đã hủy',
};

const paymentMethodLabels = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  CARD: 'Thẻ',
  OTHER: 'Khác',
};

const toPositiveNumber = (value) => {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : null;
};

const calculateTripFreightAmount = (trip) => {
  if (!trip) return null;
  if (toPositiveNumber(trip.freightAmount)) return Number(trip.freightAmount);

  const distance = toPositiveNumber(trip.distanceKm);
  const weight = toPositiveNumber(trip.cargoWeightTon);
  const rate = toPositiveNumber(trip.freightRatePerTonKm);

  if (!distance || !weight || !rate) return null;

  return distance * weight * rate;
};

const getInitialFormData = () => ({
  invoiceNumber: '',
  tripId: '',
  totalAmount: '',
  issueDate: toDateInputValue(),
  dueDate: '',
  applyDeposit: false,
  depositAmount: '',
});

const getInitialPaymentForm = () => ({
  paymentDate: toDateInputValue(),
  paymentMethod: 'BANK_TRANSFER',
  bankName: '',
  accountHolder: '',
  accountNumber: '',
  transactionReference: '',
  note: '',
});

const tripLabel = (trip) => {
  const route = [trip.startLocation, trip.endLocation].filter(Boolean).join(' -> ');
  const customer = trip.customerName || trip.customerUsername || shortId(trip.customerId);
  const status = tripStatusLabels[trip.status] || trip.status || '-';
  return `${shortId(trip.id)} - ${status} - ${customer}${route ? ` - ${route}` : ''}`;
};

export default function Invoices() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [trips, setTrips] = useState([]);
  const [invoices, setInvoices] = useState([]);
  const [tripsLoading, setTripsLoading] = useState(false);
  const [formData, setFormData] = useState(getInitialFormData);
  const [paymentInvoice, setPaymentInvoice] = useState(null);
  const [paymentForm, setPaymentForm] = useState(getInitialPaymentForm);
  const [isPaymentSubmitting, setIsPaymentSubmitting] = useState(false);
  const [qrInvoice, setQrInvoice] = useState(null);
  const [paymentHistoryInvoice, setPaymentHistoryInvoice] = useState(null);
  const [paymentHistory, setPaymentHistory] = useState([]);
  const [paymentHistoryLoading, setPaymentHistoryLoading] = useState(false);

  const loadOptions = async () => {
    setTripsLoading(true);
    try {
      const [tripResponse, invoiceResponse] = await Promise.all([
        api.get('/trips'),
        api.get('/invoices'),
      ]);
      setTrips(getResult(tripResponse, []));
      setInvoices(getResult(invoiceResponse, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải dữ liệu lập hóa đơn');
    } finally {
      setTripsLoading(false);
    }
  };

  useEffect(() => {
    loadOptions();
  }, []);

  const handleChange = (event) => {
    const { name, value, type, checked } = event.target;

    if (name === 'tripId') {
      const trip = trips.find((item) => item.id === value);
      const freightAmount = calculateTripFreightAmount(trip);
      const depositSummary = trip?.depositSummary;
      const canApplyDeposit = depositSummary?.usage !== 'SECURITY_HOLD'
        && Number(depositSummary?.availableAmount || 0) > 0;
      const suggestedDeposit = canApplyDeposit
        ? Math.min(Number(depositSummary.availableAmount), Number(freightAmount || 0))
        : 0;
      setFormData((current) => ({
        ...current,
        tripId: value,
        totalAmount: freightAmount ? String(Math.round(freightAmount)) : '',
        applyDeposit: canApplyDeposit,
        depositAmount: suggestedDeposit > 0 ? String(Math.round(suggestedDeposit)) : '',
      }));
      return;
    }

    if (name === 'applyDeposit') {
      const availableAmount = Number(selectedTrip?.depositSummary?.availableAmount || 0);
      const invoiceAmount = Number(formData.totalAmount || 0);
      setFormData((current) => ({
        ...current,
        applyDeposit: checked,
        depositAmount: checked ? String(Math.min(availableAmount, invoiceAmount)) : '',
      }));
      return;
    }

    setFormData((current) => ({ ...current, [name]: type === 'checkbox' ? checked : value }));
  };

  const resetForm = () => {
    setFormData(getInitialFormData());
    setIsModalOpen(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setIsSubmitting(true);
    try {
      await api.post('/invoices', {
        ...formData,
        invoiceNumber: formData.invoiceNumber.trim() || null,
        totalAmount: Number(formData.totalAmount),
        applyDeposit: formData.applyDeposit,
        depositAmount: formData.applyDeposit && formData.depositAmount !== ''
          ? Number(formData.depositAmount)
          : null,
      });
      toast.success('Thêm hóa đơn thành công');
      resetForm();
      setRefreshKey((current) => current + 1);
      loadOptions();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Lỗi khi thêm hóa đơn');
    } finally {
      setIsSubmitting(false);
    }
  };

  const cancelInvoice = async (invoiceId) => {
    try {
      await api.patch(`/invoices/${invoiceId}/cancel`);
      toast.success('Đã hủy hóa đơn');
      setRefreshKey((current) => current + 1);
      loadOptions();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật hóa đơn');
    }
  };

  const openPayment = (invoice) => {
    setPaymentInvoice(invoice);
    setPaymentForm(getInitialPaymentForm());
  };

  const closePayment = () => {
    setPaymentInvoice(null);
    setPaymentForm(getInitialPaymentForm());
  };

  const handlePaymentSubmit = async (event) => {
    event.preventDefault();
    setIsPaymentSubmitting(true);
    try {
      await api.patch(`/invoices/${paymentInvoice.id}/pay`, {
        paymentDate: paymentForm.paymentDate,
        paymentMethod: paymentForm.paymentMethod,
        bankName: paymentForm.paymentMethod === 'BANK_TRANSFER' ? paymentForm.bankName.trim() : null,
        accountHolder: paymentForm.paymentMethod === 'BANK_TRANSFER' ? paymentForm.accountHolder.trim() || null : null,
        accountNumber: paymentForm.paymentMethod === 'BANK_TRANSFER' ? paymentForm.accountNumber.trim() || null : null,
        transactionReference: paymentForm.transactionReference.trim() || null,
        note: paymentForm.note.trim() || null,
      });
      toast.success('Đã ghi nhận thanh toán hóa đơn');
      closePayment();
      setRefreshKey((current) => current + 1);
      await loadOptions();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể ghi nhận thanh toán');
    } finally {
      setIsPaymentSubmitting(false);
    }
  };

  const openPaymentHistory = async (invoice) => {
    setPaymentHistoryInvoice(invoice);
    setPaymentHistory([]);
    setPaymentHistoryLoading(true);
    try {
      const response = await api.get(`/invoices/${invoice.id}/payments`);
      setPaymentHistory(getResult(response, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải lịch sử thanh toán');
    } finally {
      setPaymentHistoryLoading(false);
    }
  };

  const closePaymentHistory = () => {
    setPaymentHistoryInvoice(null);
    setPaymentHistory([]);
  };

  const activeInvoiceTripIds = new Set(
    invoices
      .filter((invoice) => invoice.status !== 'CANCELLED' && invoice.tripId)
      .map((invoice) => invoice.tripId)
  );
  const completedTrips = trips.filter((trip) => trip.status === 'COMPLETED');
  const billableTrips = completedTrips.filter((trip) => !activeInvoiceTripIds.has(trip.id));
  const selectedTrip = trips.find((trip) => trip.id === formData.tripId);
  const selectedFreightAmount = calculateTripFreightAmount(selectedTrip);
  const selectedDepositSummary = selectedTrip?.depositSummary;
  const selectedDepositAmount = formData.applyDeposit ? Number(formData.depositAmount || 0) : 0;
  const selectedAmountDue = Math.max(Number(formData.totalAmount || 0) - selectedDepositAmount, 0);
  const depositCanBeApplied = selectedDepositSummary?.usage !== 'SECURITY_HOLD'
    && Number(selectedDepositSummary?.availableAmount || 0) > 0;

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý hóa đơn"
        description="Theo dõi hóa đơn theo chuyến đi, công nợ, trạng thái thanh toán và doanh thu."
        endpoint="/invoices"
        deleteEndpoint="/invoices"
        deleteLabel={(row) => `hóa đơn "${row.invoiceNumber || shortId(row.id)}"`}
        deleteSuccessMessage="Đã xóa hóa đơn"
        onDeleteSuccess={loadOptions}
        emptyText="Chưa có hóa đơn nào."
        primaryColumns={['invoiceNumber', 'status', 'customerName', 'totalAmount', 'depositAppliedAmount', 'amountDue']}
        onCreate={() => {
          setIsModalOpen(true);
          loadOptions();
        }}
        filters={[
          {
            key: 'status',
            label: 'Trạng thái',
            type: 'select',
            options: Object.entries(invoiceStatusLabels).map(([value, label]) => ({ value, label })),
          },
          {
            key: 'customer',
            label: 'Khách hàng',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm khách hàng...',
            deriveOptions: true,
            getValue: (row) => row.customerId || row.customerUsername || row.customerName,
            getOptionLabel: (row) => row.customerName || row.customerUsername || shortId(row.customerId),
          },
          { key: 'issueFrom', label: 'Lập từ ngày', type: 'date', field: 'issueDate', operator: 'gte', maxFilterKey: 'issueTo' },
          { key: 'issueTo', label: 'Lập đến ngày', type: 'date', field: 'issueDate', operator: 'lte', minFilterKey: 'issueFrom', popupAlign: 'right' },
          { key: 'amountMin', label: 'Còn thu tối thiểu', type: 'number', field: 'amountDue', operator: 'gte', min: 0, step: 1000, placeholder: '0' },
          { key: 'amountMax', label: 'Còn thu tối đa', type: 'number', field: 'amountDue', operator: 'lte', min: 0, step: 1000, placeholder: 'Không giới hạn' },
        ]}
        columns={[
          { key: 'invoiceNumber', label: 'Số hóa đơn', render: (row) => row.invoiceNumber || shortId(row.id) },
          { key: 'status', label: 'Trạng thái', render: (row) => invoiceStatusLabels[row.status] || row.status || '-' },
          { key: 'customerName', label: 'Khách hàng', render: (row) => row.customerName || row.customerUsername || shortId(row.customerId) },
          { key: 'tripId', label: 'Chuyến đi', render: (row) => shortId(row.tripId) },
          { key: 'totalAmount', label: 'Tổng hóa đơn', render: (row) => formatCurrency(row.totalAmount) },
          { key: 'depositAppliedAmount', label: 'Tiền đã cọc', render: (row) => formatCurrency(row.depositAppliedAmount) },
          { key: 'paidAmount', label: 'Đã thu', render: (row) => formatCurrency(row.paidAmount) },
          { key: 'amountDue', label: 'Còn phải thu', render: (row) => formatCurrency(row.amountDue) },
          { key: 'issueDate', label: 'Ngày lập', render: (row) => formatDate(row.issueDate) },
          { key: 'dueDate', label: 'Hạn thanh toán', render: (row) => formatDate(row.dueDate) },
        ]}
        rowActions={(row) => (
          <div className="flex justify-end gap-2">
            {row.status !== 'CANCELLED' && (
              <button
                type="button"
                onClick={() => setQrInvoice(row)}
                title="Mã VietQR thanh toán"
                aria-label="Mã VietQR thanh toán"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-blue-200 bg-blue-50 px-3 text-sm font-medium text-blue-700 hover:bg-blue-100"
              >
                <QrCode size={16} />
              </button>
            )}
            {(row.status === 'PENDING' || row.status === 'OVERDUE') && Number(row.amountDue || 0) > 0 && (
              <button
                type="button"
                onClick={() => openPayment(row)}
                title="Ghi nhận thanh toán"
                aria-label="Ghi nhận thanh toán"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
              >
                <CheckCircle2 size={16} />
              </button>
            )}
            {row.status === 'PAID' && Number(row.paidAmount || 0) > Number(row.depositAppliedAmount || 0) && (
              <button
                type="button"
                onClick={() => openPaymentHistory(row)}
                title="Lịch sử thanh toán"
                aria-label="Lịch sử thanh toán"
                className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100"
              >
                <History size={16} />
              </button>
            )}
            {row.status !== 'CANCELLED' && row.status !== 'PAID' && (
              <button
                type="button"
                onClick={() => cancelInvoice(row.id)}
                title="Hủy hóa đơn"
                aria-label="Hủy hóa đơn"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100"
              >
                <XCircle size={16} />
              </button>
            )}
          </div>
        )}
      />

      <Modal isOpen={isModalOpen} onClose={resetForm} title="Thêm hóa đơn mới" size="wide">
        <form onSubmit={handleSubmit} className="mt-2 space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-700">Số hóa đơn</label>
              <input name="invoiceNumber" value={formData.invoiceNumber} onChange={handleChange} placeholder="Để trống để tự sinh" className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Chuyến đi</label>
              <select required name="tripId" value={formData.tripId} onChange={handleChange} disabled={tripsLoading || billableTrips.length === 0} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 disabled:bg-slate-100 disabled:text-slate-500">
                <option value="">{tripsLoading ? 'Đang tải...' : 'Chọn chuyến đã hoàn tất'}</option>
                {billableTrips.map((trip) => (
                  <option key={trip.id} value={trip.id}>
                    {tripLabel(trip)}
                  </option>
                ))}
              </select>
              {!tripsLoading && billableTrips.length === 0 && (
                <p className="mt-2 text-xs text-amber-700">
                  {completedTrips.length === 0
                    ? 'Chưa có chuyến hoàn tất để lập hóa đơn. Hãy hoàn tất chuyến đi trước.'
                    : 'Các chuyến hoàn tất hiện đã có hóa đơn chưa hủy hoặc đã thanh toán.'}
                </p>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Số tiền (VNĐ)</label>
              <input required type="number" min="1" name="totalAmount" value={formData.totalAmount} onChange={handleChange} readOnly={Boolean(selectedFreightAmount)} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500 read-only:bg-slate-100" />
            </div>
            <DateField required label="Ngày lập" name="issueDate" value={formData.issueDate} max={toDateInputValue()} onChange={handleChange} />
            <DateField required label="Hạn thanh toán" name="dueDate" value={formData.dueDate} min={formData.issueDate} onChange={handleChange} />
            {selectedTrip && (
              <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 text-sm sm:col-span-2">
                <p className="font-medium text-slate-900">Công thức tính cước</p>
                <p className="mt-1 text-slate-600">
                  {formatKm(selectedTrip.distanceKm)} x {formatTon(selectedTrip.cargoWeightTon)} x {formatCurrency(selectedTrip.freightRatePerTonKm)} / tấn / km
                </p>
                <p className="mt-2 text-base font-semibold text-emerald-700">
                  Thành tiền: {selectedFreightAmount ? formatCurrency(selectedFreightAmount) : 'Chuyến này chưa đủ dữ liệu tính cước'}
                </p>
              </div>
            )}
            {selectedTrip && (
              <section className="rounded-lg border border-emerald-200 bg-emerald-50/50 p-4 sm:col-span-2">
                <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
                  <div>
                    <p className="font-semibold text-slate-900">Tiền cọc của chuyến</p>
                    <p className="mt-1 text-xs text-slate-600">
                      Đã nhận {formatCurrency(selectedDepositSummary?.receivedAmount)} · Khả dụng {formatCurrency(selectedDepositSummary?.availableAmount)}
                      {selectedDepositSummary?.required ? ` · Mức yêu cầu ${formatCurrency(selectedDepositSummary.requiredAmount)}` : ''}
                    </p>
                  </div>
                  <label className={`flex items-center gap-2 text-sm font-medium ${depositCanBeApplied ? 'cursor-pointer text-emerald-800' : 'text-slate-400'}`}>
                    <input
                      type="checkbox"
                      name="applyDeposit"
                      checked={formData.applyDeposit}
                      onChange={handleChange}
                      disabled={!depositCanBeApplied}
                      className="h-5 w-5 rounded border-slate-300 text-emerald-600 focus:ring-emerald-500"
                    />
                    Cấn cọc vào hóa đơn
                  </label>
                </div>

                {selectedDepositSummary?.usage === 'SECURITY_HOLD' && (
                  <p className="mt-3 text-sm text-amber-700">Khoản cọc này được giữ bảo đảm theo hợp đồng nên không thể cấn vào hóa đơn.</p>
                )}
                {!depositCanBeApplied && selectedDepositSummary?.usage !== 'SECURITY_HOLD' && (
                  <p className="mt-3 text-sm text-slate-500">Chuyến này chưa có số dư cọc khả dụng.</p>
                )}

                {formData.applyDeposit && (
                  <div className="mt-4 grid gap-4 border-t border-emerald-200 pt-4 sm:grid-cols-2">
                    <div>
                      <label className="block text-sm font-medium text-slate-700">Số cọc cấn trừ (VNĐ)</label>
                      <input
                        required
                        type="number"
                        min="1"
                        max={Math.min(Number(selectedDepositSummary?.availableAmount || 0), Number(formData.totalAmount || 0))}
                        step="1"
                        name="depositAmount"
                        value={formData.depositAmount}
                        onChange={handleChange}
                        className="mt-1 block w-full rounded-md border border-emerald-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                      />
                    </div>
                    <div className="rounded-lg border border-emerald-200 bg-white px-4 py-3">
                      <p className="text-xs font-medium uppercase text-slate-500">Còn phải thu</p>
                      <p className="mt-1 text-lg font-semibold text-emerald-700">{formatCurrency(selectedAmountDue)}</p>
                    </div>
                  </div>
                )}
              </section>
            )}
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetForm} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting ? 'Đang lưu...' : 'Lưu hóa đơn'}
            </button>
          </div>
        </form>
      </Modal>

      <Modal
        isOpen={Boolean(paymentInvoice)}
        onClose={closePayment}
        title="Ghi nhận thanh toán"
        size="wide"
        variant="detail"
      >
        {paymentInvoice && (
          <form onSubmit={handlePaymentSubmit} className="mt-1 space-y-4">
            <section className="grid overflow-hidden rounded-lg border border-emerald-200 bg-emerald-50/60 sm:grid-cols-3">
              <div className="px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">Hóa đơn</p>
                <p className="mt-1 font-semibold text-slate-900">{paymentInvoice.invoiceNumber || shortId(paymentInvoice.id)}</p>
              </div>
              <div className="border-t border-emerald-200 px-4 py-3 sm:border-l sm:border-t-0">
                <p className="text-xs font-medium uppercase text-slate-500">Đã cấn cọc</p>
                <p className="mt-1 font-semibold text-sky-700">{formatCurrency(paymentInvoice.depositAppliedAmount)}</p>
              </div>
              <div className="border-t border-emerald-200 px-4 py-3 sm:border-l sm:border-t-0">
                <p className="text-xs font-medium uppercase text-slate-500">Số tiền thu</p>
                <p className="mt-1 text-lg font-semibold text-emerald-700">{formatCurrency(paymentInvoice.amountDue)}</p>
              </div>
            </section>

            <div className="grid gap-4 sm:grid-cols-2">
              <DateField
                required
                label="Ngày thanh toán"
                name="paymentDate"
                value={paymentForm.paymentDate}
                min={paymentInvoice.issueDate}
                max={toDateInputValue()}
                onChange={(event) => setPaymentForm((current) => ({ ...current, paymentDate: event.target.value }))}
              />
              <div>
                <label className="block text-sm font-medium text-slate-700">Phương thức thanh toán</label>
                <select
                  required
                  value={paymentForm.paymentMethod}
                  onChange={(event) => setPaymentForm((current) => ({ ...current, paymentMethod: event.target.value }))}
                  className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                >
                  {Object.entries(paymentMethodLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </div>

              {paymentForm.paymentMethod === 'BANK_TRANSFER' ? (
                <>
                  <BankSelect
                    required
                    value={paymentForm.bankName}
                    onChange={(bankName) => setPaymentForm((current) => ({ ...current, bankName }))}
                  />
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Mã giao dịch</label>
                    <input
                      required
                      value={paymentForm.transactionReference}
                      onChange={(event) => setPaymentForm((current) => ({ ...current, transactionReference: event.target.value }))}
                      placeholder="Mã đối soát ngân hàng"
                      className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Chủ tài khoản chuyển</label>
                    <input
                      value={paymentForm.accountHolder}
                      onChange={(event) => setPaymentForm((current) => ({ ...current, accountHolder: event.target.value }))}
                      placeholder="Không bắt buộc"
                      className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-slate-700">Số tài khoản chuyển</label>
                    <input
                      value={paymentForm.accountNumber}
                      onChange={(event) => setPaymentForm((current) => ({ ...current, accountNumber: event.target.value }))}
                      placeholder="Không bắt buộc"
                      className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                    />
                  </div>
                </>
              ) : (
                <div className="sm:col-span-2">
                  <label className="block text-sm font-medium text-slate-700">Mã tham chiếu</label>
                  <input
                    value={paymentForm.transactionReference}
                    onChange={(event) => setPaymentForm((current) => ({ ...current, transactionReference: event.target.value }))}
                    placeholder="Không bắt buộc"
                    className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                  />
                </div>
              )}

              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-slate-700">Ghi chú</label>
                <textarea
                  rows={3}
                  value={paymentForm.note}
                  onChange={(event) => setPaymentForm((current) => ({ ...current, note: event.target.value }))}
                  className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
                />
              </div>
            </div>

            <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
              <button type="button" onClick={closePayment} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Hủy</button>
              <button type="submit" disabled={isPaymentSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">
                {isPaymentSubmitting ? 'Đang lưu...' : 'Xác nhận thanh toán'}
              </button>
            </div>
          </form>
        )}
      </Modal>

      <Modal
        isOpen={Boolean(paymentHistoryInvoice)}
        onClose={closePaymentHistory}
        title="Lịch sử thanh toán"
        size="wide"
        variant="detail"
      >
        {paymentHistoryInvoice && (
          <div className="space-y-4">
            <div className="flex flex-col gap-1 border-b border-slate-200 pb-3 sm:flex-row sm:items-center sm:justify-between">
              <p className="font-semibold text-slate-900">Hóa đơn {paymentHistoryInvoice.invoiceNumber || shortId(paymentHistoryInvoice.id)}</p>
              <p className="text-sm text-slate-500">Đã thu: {formatCurrency(paymentHistoryInvoice.paidAmount)}</p>
            </div>
            <div className="overflow-hidden rounded-lg border border-slate-200">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-slate-200 text-sm">
                  <thead className="bg-slate-100">
                    <tr>
                      <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Ngày thu</th>
                      <th className="whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Số tiền</th>
                      <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Phương thức</th>
                      <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Ngân hàng</th>
                      <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Người chuyển</th>
                      <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Mã giao dịch</th>
                      <th className="min-w-48 px-4 py-3 text-left font-semibold text-slate-700">Ghi chú</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 bg-white">
                    {paymentHistoryLoading ? (
                      <tr><td colSpan={7} className="px-4 py-10 text-center text-slate-500"><Loader2 className="mx-auto animate-spin" size={22} /></td></tr>
                    ) : paymentHistory.length > 0 ? paymentHistory.map((payment) => (
                      <tr key={payment.id}>
                        <td className="whitespace-nowrap px-4 py-3 text-slate-700">{formatDate(payment.paymentDate)}</td>
                        <td className="whitespace-nowrap px-4 py-3 text-right font-semibold text-emerald-700">{formatCurrency(payment.amount)}</td>
                        <td className="whitespace-nowrap px-4 py-3 text-slate-700">{paymentMethodLabels[payment.paymentMethod] || payment.paymentMethod || '-'}</td>
                        <td className="whitespace-nowrap px-4 py-3 text-slate-700">{payment.bankName || '-'}</td>
                        <td className="px-4 py-3 text-slate-700">
                          <p className="whitespace-nowrap">{payment.accountHolder || '-'}</p>
                          {payment.accountNumber && <p className="mt-0.5 whitespace-nowrap text-xs text-slate-500">{payment.accountNumber}</p>}
                        </td>
                        <td className="whitespace-nowrap px-4 py-3 text-slate-700">{payment.transactionReference || '-'}</td>
                        <td className="px-4 py-3 text-slate-700"><p className="max-w-72 break-words">{payment.note || '-'}</p></td>
                      </tr>
                    )) : (
                      <tr><td colSpan={7} className="px-4 py-10 text-center text-slate-500">Chưa có giao dịch thanh toán được lưu.</td></tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        )}
      </Modal>
      {qrInvoice && (
        <VietQrModal
          invoiceId={qrInvoice.id}
          invoiceCode={qrInvoice.invoiceNumber}
          onClose={() => setQrInvoice(null)}
        />
      )}
    </>
  );
}
