import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { ArrowLeft, History, Loader2, Plus, Trash2, Undo2 } from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import { formatDate, toDateInputValue } from '../utils/dates';
import BankSelect from './BankSelect';
import DateField from './DateField';
import Modal from './Modal';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;

const statusLabels = {
  AVAILABLE: 'Còn khả dụng',
  PARTIALLY_APPLIED: 'Đã dùng một phần',
  APPLIED: 'Đã dùng hết',
  PARTIALLY_REFUNDED: 'Đã hoàn một phần',
  REFUNDED: 'Đã hoàn hết',
  CANCELLED: 'Đã hủy',
};

const statusStyles = {
  AVAILABLE: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  PARTIALLY_APPLIED: 'border-sky-200 bg-sky-50 text-sky-700',
  APPLIED: 'border-slate-200 bg-slate-100 text-slate-700',
  PARTIALLY_REFUNDED: 'border-amber-200 bg-amber-50 text-amber-700',
  REFUNDED: 'border-violet-200 bg-violet-50 text-violet-700',
  CANCELLED: 'border-rose-200 bg-rose-50 text-rose-700',
};

const paymentMethodLabels = {
  CASH: 'Tiền mặt',
  BANK_TRANSFER: 'Chuyển khoản',
  CARD: 'Thẻ',
  OTHER: 'Khác',
};

const depositScopeLabels = {
  CONTRACT: 'Theo hợp đồng',
  TRIP: 'Theo từng chuyến',
};

const depositUsageLabels = {
  APPLY_TO_INVOICE: 'Cấn trừ hóa đơn',
  SECURITY_HOLD: 'Giữ bảo đảm',
};

const buildCreateForm = (contract) => ({
  targetType: contract?.depositScope === 'TRIP' ? 'TRIP' : 'CONTRACT',
  tripId: '',
  receiptNumber: '',
  amount: contract?.depositScope === 'CONTRACT' && Number(contract?.requiredDepositAmount || 0) > 0
    ? String(Math.round(Number(contract.requiredDepositAmount)))
    : '',
  receivedDate: toDateInputValue(),
  paymentMethod: 'BANK_TRANSFER',
  bankName: '',
  accountHolder: '',
  accountNumber: '',
  referenceNumber: '',
  note: '',
});

const buildRefundForm = () => ({
  amount: '',
  refundDate: toDateInputValue(),
  paymentMethod: 'BANK_TRANSFER',
  bankName: '',
  accountHolder: '',
  accountNumber: '',
  referenceNumber: '',
  note: '',
});

function StatusBadge({ status }) {
  return (
    <span className={`inline-flex whitespace-nowrap rounded-full border px-2 py-1 text-xs font-medium ${statusStyles[status] || statusStyles.APPLIED}`}>
      {statusLabels[status] || status || '-'}
    </span>
  );
}

function BackButton({ onClick }) {
  return (
    <button
      type="button"
      onClick={onClick}
      title="Quay lại sổ tiền cọc"
      aria-label="Quay lại sổ tiền cọc"
      className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 hover:bg-slate-50"
    >
      <ArrowLeft size={17} />
    </button>
  );
}

function TransferFields({ form, setForm, referenceLabel = 'Mã giao dịch' }) {
  if (form.paymentMethod !== 'BANK_TRANSFER') {
    return (
      <div className="sm:col-span-2">
        <label className="block text-sm font-medium text-slate-700">Mã tham chiếu</label>
        <input
          value={form.referenceNumber}
          onChange={(event) => setForm((current) => ({ ...current, referenceNumber: event.target.value }))}
          placeholder="Không bắt buộc"
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
        />
      </div>
    );
  }

  return (
    <>
      <BankSelect
        required
        value={form.bankName}
        onChange={(bankName) => setForm((current) => ({ ...current, bankName }))}
      />
      <div>
        <label className="block text-sm font-medium text-slate-700">{referenceLabel}</label>
        <input
          required
          value={form.referenceNumber}
          onChange={(event) => setForm((current) => ({ ...current, referenceNumber: event.target.value }))}
          placeholder="Mã đối soát ngân hàng"
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700">Chủ tài khoản chuyển</label>
        <input
          value={form.accountHolder}
          onChange={(event) => setForm((current) => ({ ...current, accountHolder: event.target.value }))}
          placeholder="Không bắt buộc"
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
        />
      </div>
      <div>
        <label className="block text-sm font-medium text-slate-700">Số tài khoản chuyển</label>
        <input
          value={form.accountNumber}
          onChange={(event) => setForm((current) => ({ ...current, accountNumber: event.target.value }))}
          placeholder="Không bắt buộc"
          className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500"
        />
      </div>
    </>
  );
}

export default function ContractDepositsModal({ contract, isOpen, onClose, onChanged }) {
  const { user } = useContext(AuthContext);
  const isAdmin = user?.role === 'ADMIN';
  const [view, setView] = useState('LIST');
  const [deposits, setDeposits] = useState([]);
  const [trips, setTrips] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [selectedDeposit, setSelectedDeposit] = useState(null);
  const [refundHistory, setRefundHistory] = useState([]);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [createForm, setCreateForm] = useState(() => buildCreateForm(null));
  const [refundForm, setRefundForm] = useState(buildRefundForm);
  const contractId = contract?.id;

  const loadData = useCallback(async () => {
    if (!contractId) return;
    setLoading(true);
    try {
      const [depositResponse, tripResponse] = await Promise.all([
        api.get(`/deposits/contract/${contractId}`),
        api.get('/trips'),
      ]);
      setDeposits(getResult(depositResponse, []));
      setTrips(getResult(tripResponse, []).filter((trip) => (
        trip.contractId === contractId && trip.status !== 'CANCELLED'
      )));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải sổ tiền cọc của hợp đồng');
      setDeposits([]);
      setTrips([]);
    } finally {
      setLoading(false);
    }
  }, [contractId]);

  useEffect(() => {
    if (!isOpen || !contract) return;
    setView('LIST');
    setSelectedDeposit(null);
    setRefundHistory([]);
    setCreateForm(buildCreateForm(contract));
    setRefundForm(buildRefundForm());
    loadData();
  }, [isOpen, contract, loadData]);

  const totals = useMemo(() => deposits.reduce((summary, deposit) => ({
    received: summary.received + Number(deposit.amount || 0),
    allocated: summary.allocated + Number(deposit.allocatedAmount || 0),
    refunded: summary.refunded + Number(deposit.refundedAmount || 0),
    available: summary.available + Number(deposit.availableAmount || 0),
  }), { received: 0, allocated: 0, refunded: 0, available: 0 }), [deposits]);

  const selectedTrip = trips.find((trip) => trip.id === createForm.tripId);

  const resetToList = () => {
    setView('LIST');
    setSelectedDeposit(null);
    setRefundHistory([]);
    setCreateForm(buildCreateForm(contract));
    setRefundForm(buildRefundForm());
  };

  const handleTargetChange = (targetType) => {
    setCreateForm((current) => ({
      ...current,
      targetType,
      tripId: '',
      amount: targetType === 'CONTRACT' && Number(contract?.requiredDepositAmount || 0) > 0
        ? String(Math.round(Number(contract.requiredDepositAmount)))
        : '',
    }));
  };

  const handleTripChange = (event) => {
    const tripId = event.target.value;
    const trip = trips.find((item) => item.id === tripId);
    const suggestedAmount = Number(trip?.depositSummary?.requiredAmount || 0);
    setCreateForm((current) => ({
      ...current,
      tripId,
      amount: suggestedAmount > 0 ? String(Math.round(suggestedAmount)) : current.amount,
    }));
  };

  const handleCreate = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await api.post('/deposits', {
        receiptNumber: createForm.receiptNumber.trim() || null,
        customerId: contract.customerId,
        contractId: contract.id,
        tripId: createForm.targetType === 'TRIP' ? createForm.tripId : null,
        amount: Number(createForm.amount),
        receivedDate: createForm.receivedDate,
        paymentMethod: createForm.paymentMethod,
        bankName: createForm.paymentMethod === 'BANK_TRANSFER' ? createForm.bankName.trim() : null,
        accountHolder: createForm.paymentMethod === 'BANK_TRANSFER' ? createForm.accountHolder.trim() || null : null,
        accountNumber: createForm.paymentMethod === 'BANK_TRANSFER' ? createForm.accountNumber.trim() || null : null,
        referenceNumber: createForm.referenceNumber.trim() || null,
        note: createForm.note.trim() || null,
      });
      toast.success('Đã ghi nhận tiền cọc cho hợp đồng');
      resetToList();
      await loadData();
      onChanged?.();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể ghi nhận tiền cọc');
    } finally {
      setSubmitting(false);
    }
  };

  const openRefund = (deposit) => {
    setSelectedDeposit(deposit);
    setRefundForm(buildRefundForm());
    setView('REFUND');
  };

  const handleRefund = async (event) => {
    event.preventDefault();
    setSubmitting(true);
    try {
      await api.post(`/deposits/${selectedDeposit.id}/refunds`, {
        amount: Number(refundForm.amount),
        refundDate: refundForm.refundDate,
        paymentMethod: refundForm.paymentMethod,
        bankName: refundForm.paymentMethod === 'BANK_TRANSFER' ? refundForm.bankName.trim() : null,
        accountHolder: refundForm.paymentMethod === 'BANK_TRANSFER' ? refundForm.accountHolder.trim() || null : null,
        accountNumber: refundForm.paymentMethod === 'BANK_TRANSFER' ? refundForm.accountNumber.trim() || null : null,
        referenceNumber: refundForm.referenceNumber.trim() || null,
        note: refundForm.note.trim() || null,
      });
      toast.success('Đã ghi nhận hoàn tiền cọc');
      resetToList();
      await loadData();
      onChanged?.();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể hoàn tiền cọc');
    } finally {
      setSubmitting(false);
    }
  };

  const openHistory = async (deposit) => {
    setSelectedDeposit(deposit);
    setRefundHistory([]);
    setView('HISTORY');
    setHistoryLoading(true);
    try {
      const response = await api.get(`/deposits/${deposit.id}/refunds`);
      setRefundHistory(getResult(response, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải lịch sử hoàn cọc');
    } finally {
      setHistoryLoading(false);
    }
  };

  const handleDelete = async () => {
    setSubmitting(true);
    try {
      await api.delete(`/deposits/${selectedDeposit.id}`);
      toast.success('Đã xóa phiếu cọc chưa phát sinh');
      resetToList();
      await loadData();
      onChanged?.();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể xóa phiếu cọc');
    } finally {
      setSubmitting(false);
    }
  };

  const closeModal = () => {
    resetToList();
    onClose();
  };

  if (!contract) return null;

  return (
    <Modal
      isOpen={isOpen}
      onClose={closeModal}
      title={`Tiền cọc · ${contract.contractCode || shortId(contract.id)}`}
      size="wide"
      variant="detail"
    >
      {view === 'LIST' && (
        <div className="space-y-4">
          <section className="rounded-lg border border-emerald-200 bg-emerald-50/60 p-4">
            <div className="flex flex-col gap-3 sm:flex-row sm:items-start sm:justify-between">
              <div>
                <p className="font-semibold text-emerald-950">{contract.customerName || contract.customerUsername || 'Khách hàng'}</p>
                <p className="mt-1 text-sm text-emerald-800">
                  {contract.depositRequired
                    ? `${depositScopeLabels[contract.depositScope] || '-'} · ${depositUsageLabels[contract.depositUsage] || '-'}`
                    : 'Hợp đồng không bắt buộc tiền cọc'}
                </p>
              </div>
              <button
                type="button"
                onClick={() => setView('CREATE')}
                className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700"
              >
                <Plus size={17} />
                Ghi nhận cọc
              </button>
            </div>
          </section>

          <div className="grid gap-px overflow-hidden rounded-lg border border-slate-200 bg-slate-200 sm:grid-cols-2 lg:grid-cols-4">
            {[
              ['Đã nhận', totals.received, 'text-slate-900'],
              ['Đã cấn hóa đơn', totals.allocated, 'text-sky-700'],
              ['Đã hoàn', totals.refunded, 'text-amber-700'],
              ['Còn khả dụng', totals.available, 'text-emerald-700'],
            ].map(([label, value, color]) => (
              <div key={label} className="bg-white px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
                <p className={`mt-1 text-base font-semibold ${color}`}>{formatCurrency(value)}</p>
              </div>
            ))}
          </div>

          <div className="overflow-hidden rounded-lg border border-slate-200">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-100">
                  <tr>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Số phiếu</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Phạm vi</th>
                    <th className="whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Đã nhận</th>
                    <th className="whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Khả dụng</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Ngày nhận</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Hình thức</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Đối soát</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Trạng thái</th>
                    <th className="px-4 py-3 text-right font-semibold text-slate-700">Thao tác</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 bg-white">
                  {loading ? (
                    <tr><td colSpan={9} className="px-4 py-10 text-center text-slate-500"><Loader2 className="mx-auto animate-spin" size={22} /></td></tr>
                  ) : deposits.length > 0 ? deposits.map((deposit) => (
                    <tr key={deposit.id}>
                      <td className="whitespace-nowrap px-4 py-3 font-medium text-slate-900">{deposit.receiptNumber || shortId(deposit.id)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{deposit.tripId ? `Chuyến ${shortId(deposit.tripId)}` : 'Hợp đồng'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{formatCurrency(deposit.amount)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-medium text-emerald-700">{formatCurrency(deposit.availableAmount)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{formatDate(deposit.receivedDate)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{paymentMethodLabels[deposit.paymentMethod] || deposit.paymentMethod || '-'}</td>
                      <td className="px-4 py-3 text-slate-700">
                        <p className="whitespace-nowrap">{deposit.bankName || deposit.referenceNumber || '-'}</p>
                        {deposit.bankName && deposit.referenceNumber && <p className="mt-0.5 whitespace-nowrap text-xs text-slate-500">{deposit.referenceNumber}</p>}
                      </td>
                      <td className="whitespace-nowrap px-4 py-3"><StatusBadge status={deposit.status} /></td>
                      <td className="px-4 py-3">
                        <div className="flex justify-end gap-2">
                          {Number(deposit.availableAmount || 0) > 0 && deposit.status !== 'CANCELLED' && (
                            <button type="button" onClick={() => openRefund(deposit)} title="Hoàn tiền cọc" aria-label="Hoàn tiền cọc" className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-amber-200 bg-amber-50 text-amber-700 hover:bg-amber-100">
                              <Undo2 size={16} />
                            </button>
                          )}
                          {Number(deposit.refundedAmount || 0) > 0 && (
                            <button type="button" onClick={() => openHistory(deposit)} title="Lịch sử hoàn cọc" aria-label="Lịch sử hoàn cọc" className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 text-sky-700 hover:bg-sky-100">
                              <History size={16} />
                            </button>
                          )}
                          {isAdmin && Number(deposit.allocatedAmount || 0) === 0 && Number(deposit.refundedAmount || 0) === 0 && (
                            <button type="button" onClick={() => { setSelectedDeposit(deposit); setView('DELETE'); }} title="Xóa phiếu cọc" aria-label="Xóa phiếu cọc" className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100">
                              <Trash2 size={16} />
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  )) : (
                    <tr><td colSpan={9} className="px-4 py-10 text-center text-slate-500">Hợp đồng chưa có phiếu cọc.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {view === 'CREATE' && (
        <form onSubmit={handleCreate} className="space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-200 pb-3">
            <BackButton onClick={resetToList} />
            <div>
              <p className="font-semibold text-slate-900">Ghi nhận tiền cọc</p>
              <p className="text-sm text-slate-500">{contract.customerName || contract.customerUsername || '-'}</p>
            </div>
          </div>

          <div>
            <span className="block text-sm font-medium text-slate-700">Phạm vi phiếu cọc</span>
            <div className="mt-2 grid grid-cols-2 overflow-hidden rounded-lg border border-slate-300 bg-white p-1">
              {Object.entries(depositScopeLabels).map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  disabled={value === 'TRIP' && trips.length === 0}
                  onClick={() => handleTargetChange(value)}
                  className={`min-h-10 px-3 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-40 ${createForm.targetType === value ? 'rounded-md bg-emerald-600 text-white' : 'text-slate-600 hover:bg-slate-100'}`}
                >
                  {label}
                </button>
              ))}
            </div>
          </div>

          <div className="grid gap-4 sm:grid-cols-2">
            {createForm.targetType === 'TRIP' && (
              <div className="sm:col-span-2">
                <label className="block text-sm font-medium text-slate-700">Chuyến đi</label>
                <select required value={createForm.tripId} onChange={handleTripChange} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                  <option value="">Chọn chuyến thuộc hợp đồng</option>
                  {trips.map((trip) => (
                    <option key={trip.id} value={trip.id}>{shortId(trip.id)} - {trip.startLocation || '-'} → {trip.endLocation || '-'}</option>
                  ))}
                </select>
                {selectedTrip?.depositSummary?.requiredAmount > 0 && (
                  <p className="mt-2 text-xs text-slate-500">Mức cọc chuyến: {formatCurrency(selectedTrip.depositSummary.requiredAmount)}</p>
                )}
              </div>
            )}
            <div>
              <label className="block text-sm font-medium text-slate-700">Số phiếu</label>
              <input value={createForm.receiptNumber} onChange={(event) => setCreateForm((current) => ({ ...current, receiptNumber: event.target.value }))} placeholder="Để trống để tự sinh" className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Số tiền nhận (VNĐ)</label>
              <input required type="number" min="1" step="1" value={createForm.amount} onChange={(event) => setCreateForm((current) => ({ ...current, amount: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <DateField required label="Ngày nhận" name="receivedDate" value={createForm.receivedDate} max={toDateInputValue()} onChange={(event) => setCreateForm((current) => ({ ...current, receivedDate: event.target.value }))} />
            <div>
              <label className="block text-sm font-medium text-slate-700">Phương thức nhận</label>
              <select required value={createForm.paymentMethod} onChange={(event) => setCreateForm((current) => ({ ...current, paymentMethod: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                {Object.entries(paymentMethodLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <TransferFields form={createForm} setForm={setCreateForm} />
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Ghi chú</label>
              <textarea rows={3} value={createForm.note} onChange={(event) => setCreateForm((current) => ({ ...current, note: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
          </div>
          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetToList} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Hủy</button>
            <button type="submit" disabled={submitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">{submitting ? 'Đang lưu...' : 'Lưu phiếu cọc'}</button>
          </div>
        </form>
      )}

      {view === 'REFUND' && selectedDeposit && (
        <form onSubmit={handleRefund} className="space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-200 pb-3">
            <BackButton onClick={resetToList} />
            <div>
              <p className="font-semibold text-slate-900">Hoàn tiền cọc · {selectedDeposit.receiptNumber}</p>
              <p className="text-sm text-slate-500">Có thể hoàn {formatCurrency(selectedDeposit.availableAmount)}</p>
            </div>
          </div>
          <div className="grid gap-4 sm:grid-cols-2">
            <div>
              <label className="block text-sm font-medium text-slate-700">Số tiền hoàn (VNĐ)</label>
              <input required type="number" min="1" max={selectedDeposit.availableAmount} step="1" value={refundForm.amount} onChange={(event) => setRefundForm((current) => ({ ...current, amount: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <DateField required label="Ngày hoàn" name="refundDate" value={refundForm.refundDate} min={selectedDeposit.receivedDate} max={toDateInputValue()} onChange={(event) => setRefundForm((current) => ({ ...current, refundDate: event.target.value }))} />
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Phương thức hoàn</label>
              <select required value={refundForm.paymentMethod} onChange={(event) => setRefundForm((current) => ({ ...current, paymentMethod: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 bg-white px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                {Object.entries(paymentMethodLabels).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </div>
            <TransferFields form={refundForm} setForm={setRefundForm} referenceLabel="Mã giao dịch hoàn" />
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Lý do hoàn</label>
              <textarea rows={3} value={refundForm.note} onChange={(event) => setRefundForm((current) => ({ ...current, note: event.target.value }))} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
          </div>
          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetToList} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Hủy</button>
            <button type="submit" disabled={submitting} className="rounded-lg bg-amber-600 px-4 py-2 text-sm font-medium text-white hover:bg-amber-700 disabled:opacity-50">{submitting ? 'Đang lưu...' : 'Xác nhận hoàn cọc'}</button>
          </div>
        </form>
      )}

      {view === 'HISTORY' && selectedDeposit && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-200 pb-3">
            <BackButton onClick={resetToList} />
            <div>
              <p className="font-semibold text-slate-900">Lịch sử hoàn · {selectedDeposit.receiptNumber}</p>
              <p className="text-sm text-slate-500">Đã hoàn {formatCurrency(selectedDeposit.refundedAmount)}</p>
            </div>
          </div>
          <div className="overflow-hidden rounded-lg border border-slate-200">
            <div className="overflow-x-auto">
              <table className="min-w-full divide-y divide-slate-200 text-sm">
                <thead className="bg-slate-100">
                  <tr>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Ngày hoàn</th>
                    <th className="whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Số tiền</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Hình thức</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Ngân hàng</th>
                    <th className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Mã giao dịch</th>
                    <th className="min-w-48 px-4 py-3 text-left font-semibold text-slate-700">Ghi chú</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {historyLoading ? (
                    <tr><td colSpan={6} className="px-4 py-10 text-center text-slate-500"><Loader2 className="mx-auto animate-spin" size={22} /></td></tr>
                  ) : refundHistory.length > 0 ? refundHistory.map((refund) => (
                    <tr key={refund.id}>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{formatDate(refund.refundDate)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-right font-medium text-amber-700">{formatCurrency(refund.amount)}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{paymentMethodLabels[refund.paymentMethod] || refund.paymentMethod || '-'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{refund.bankName || '-'}</td>
                      <td className="whitespace-nowrap px-4 py-3 text-slate-700">{refund.referenceNumber || '-'}</td>
                      <td className="px-4 py-3 text-slate-700"><p className="max-w-72 break-words">{refund.note || '-'}</p></td>
                    </tr>
                  )) : (
                    <tr><td colSpan={6} className="px-4 py-10 text-center text-slate-500">Chưa có lần hoàn cọc nào.</td></tr>
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {view === 'DELETE' && selectedDeposit && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 border-b border-slate-200 pb-3">
            <BackButton onClick={resetToList} />
            <p className="font-semibold text-slate-900">Xóa phiếu cọc</p>
          </div>
          <div className="rounded-lg border border-rose-200 bg-rose-50 p-4 text-sm text-rose-900">
            Phiếu <strong>{selectedDeposit.receiptNumber || shortId(selectedDeposit.id)}</strong> trị giá <strong>{formatCurrency(selectedDeposit.amount)}</strong> sẽ bị xóa.
          </div>
          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetToList} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">Hủy</button>
            <button type="button" onClick={handleDelete} disabled={submitting} className="rounded-lg bg-rose-600 px-4 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50">{submitting ? 'Đang xóa...' : 'Xóa phiếu cọc'}</button>
          </div>
        </div>
      )}
    </Modal>
  );
}
