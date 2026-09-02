import { useCallback, useContext, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import {
  Check,
  ImagePlus,
  Loader2,
  Plus,
  ReceiptText,
  RefreshCw,
  Trash2,
  X,
} from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import api from '../services/api';
import { formatDate, formatDateTime, toDateInputValue } from '../utils/dates';
import CheckboxGroup from './CheckboxGroup';
import DateField from './DateField';
import Modal from './Modal';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const MAX_RECEIPT_BYTES = 1.5 * 1024 * 1024;
const supportedReceiptTypes = new Set(['image/jpeg', 'image/png', 'image/webp']);

const expenseTypes = [
  { value: 'FUEL', label: 'Nhiên liệu' },
  { value: 'TOLL', label: 'Cầu đường' },
  { value: 'MAINTENANCE', label: 'Bảo trì dọc đường' },
  { value: 'OTHER', label: 'Khác' },
];

const statusMeta = {
  PENDING: {
    label: 'Chờ duyệt',
    className: 'border-amber-200 bg-amber-50 text-amber-700',
  },
  APPROVED: {
    label: 'Đã duyệt',
    className: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  },
  REJECTED: {
    label: 'Từ chối',
    className: 'border-rose-200 bg-rose-50 text-rose-700',
  },
};

const roleLabels = {
  ADMIN: 'Quản trị viên',
  MANAGER: 'Quản lý',
  ACCOUNTANT: 'Kế toán',
  DRIVER: 'Tài xế',
};

const typeLabel = (expense) => {
  const values = Array.isArray(expense.expenseTypes) && expense.expenseTypes.length > 0
    ? expense.expenseTypes
    : [expense.expenseType].filter(Boolean);
  return values
    .map((value) => expenseTypes.find((item) => item.value === value)?.label || value)
    .join(', ') || '-';
};

const tripDate = (value) => value ? String(value).slice(0, 10) : '';

const initialForm = (trip) => {
  const today = toDateInputValue();
  const endDate = tripDate(trip?.endTime);
  const expenseDate = endDate && endDate < today ? endDate : today;
  return {
    expenseTypes: ['FUEL'],
    amount: '',
    expenseDate,
    description: '',
    receiptImageUrl: '',
  };
};

export default function TripExpensesModal({ trip, onClose, onChanged }) {
  const { user } = useContext(AuthContext);
  const [expenses, setExpenses] = useState([]);
  const [loading, setLoading] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [reviewingId, setReviewingId] = useState('');
  const [deletingId, setDeletingId] = useState('');
  const [loadError, setLoadError] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState(() => initialForm(null));
  const [rejectTarget, setRejectTarget] = useState(null);
  const [rejectNote, setRejectNote] = useState('');

  const canReview = ['ADMIN', 'MANAGER', 'ACCOUNTANT'].includes(user?.role);
  const canDelete = user?.role === 'ADMIN';
  const startDate = tripDate(trip?.startTime);
  const endDate = tripDate(trip?.endTime);
  const today = toDateInputValue();
  const maxExpenseDate = endDate && endDate < today ? endDate : today;
  const statusAllowsRecording = trip?.status === 'IN_PROGRESS' || trip?.status === 'COMPLETED';
  const canRecord = statusAllowsRecording && (!startDate || startDate <= today);

  const loadExpenses = useCallback(async () => {
    if (!trip?.id) return;
    setLoading(true);
    setLoadError('');
    try {
      const response = await api.get(`/expenses/trip/${trip.id}`);
      setExpenses(getResult(response, []));
    } catch (error) {
      setExpenses([]);
      setLoadError(error.response?.data?.message || 'Không thể tải chi phí của chuyến');
    } finally {
      setLoading(false);
    }
  }, [trip?.id]);

  useEffect(() => {
    if (!trip?.id) return;
    setFormData(initialForm(trip));
    setShowForm(user?.role === 'DRIVER' && canRecord);
    setRejectTarget(null);
    setRejectNote('');
    loadExpenses();
  }, [trip, user?.role, canRecord, loadExpenses]);

  const totals = useMemo(() => expenses.reduce((result, expense) => {
    const amount = Number(expense.amount || 0);
    if (expense.status === 'PENDING') result.pending += amount;
    if (expense.status === 'APPROVED' || !expense.status) result.approved += amount;
    if (expense.status === 'REJECTED') result.rejected += 1;
    return result;
  }, { approved: 0, pending: 0, rejected: 0 }), [expenses]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
  };

  const handleReceiptChange = (event) => {
    const file = event.target.files?.[0];
    event.target.value = '';
    if (!file) return;
    if (!supportedReceiptTypes.has(file.type) || file.size > MAX_RECEIPT_BYTES) {
      toast.error('Ảnh biên lai phải là JPG, PNG hoặc WebP và không vượt quá 1,5 MB');
      return;
    }

    const reader = new FileReader();
    reader.onload = () => setFormData((current) => ({
      ...current,
      receiptImageUrl: String(reader.result || ''),
    }));
    reader.onerror = () => toast.error('Không thể đọc ảnh biên lai');
    reader.readAsDataURL(file);
  };

  const refreshAfterChange = async () => {
    await loadExpenses();
    onChanged?.();
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    if (formData.expenseTypes.length === 0) {
      toast.error('Cần chọn ít nhất một loại chi phí');
      return;
    }
    if (formData.expenseTypes.includes('OTHER') && !formData.description.trim()) {
      toast.error('Cần ghi rõ nội dung cho loại chi phí khác');
      return;
    }

    setSubmitting(true);
    try {
      await api.post('/expenses', {
        tripId: trip.id,
        ...formData,
        amount: Number(formData.amount),
      });
      toast.success('Đã gửi chi phí và đang chờ duyệt');
      setFormData(initialForm(trip));
      setShowForm(false);
      await refreshAfterChange();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể ghi nhận chi phí');
    } finally {
      setSubmitting(false);
    }
  };

  const approveExpense = async (expense) => {
    setReviewingId(expense.id);
    try {
      await api.patch(`/expenses/${expense.id}/approve`, {});
      toast.success('Đã duyệt khoản chi');
      await refreshAfterChange();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể duyệt khoản chi');
    } finally {
      setReviewingId('');
    }
  };

  const rejectExpense = async () => {
    if (!rejectTarget || !rejectNote.trim()) {
      toast.error('Cần nhập lý do từ chối');
      return;
    }
    setReviewingId(rejectTarget.id);
    try {
      await api.patch(`/expenses/${rejectTarget.id}/reject`, { reviewNote: rejectNote.trim() });
      toast.success('Đã từ chối khoản chi');
      setRejectTarget(null);
      setRejectNote('');
      await refreshAfterChange();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể từ chối khoản chi');
    } finally {
      setReviewingId('');
    }
  };

  const deleteExpense = async (expense) => {
    const confirmed = window.confirm(`Xóa khoản chi ${formatCurrency(expense.amount)}?`);
    if (!confirmed) return;
    setDeletingId(expense.id);
    try {
      await api.delete(`/expenses/${expense.id}`);
      toast.success('Đã xóa khoản chi');
      await refreshAfterChange();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể xóa khoản chi');
    } finally {
      setDeletingId('');
    }
  };

  const closeModal = () => {
    if (submitting || reviewingId || deletingId) return;
    setExpenses([]);
    setShowForm(false);
    onClose();
  };

  return (
    <Modal
      isOpen={Boolean(trip)}
      onClose={closeModal}
      title="Chi phí phát sinh"
      size="wide"
      variant="detail"
    >
      <div className="space-y-5">
        <div className="-mx-6 -mt-4 border-b border-emerald-100 bg-emerald-50 px-6 py-4">
          <p className="text-xs font-medium uppercase text-emerald-700">Chuyến {shortId(trip?.id)}</p>
          <p className="mt-1 break-words font-semibold text-emerald-950">
            {[trip?.startLocation, trip?.endLocation].filter(Boolean).join(' → ') || 'Chưa có tuyến đường'}
          </p>
          <p className="mt-1 text-sm text-emerald-800">
            {trip?.driverName || 'Chưa có tài xế'} · {trip?.vehiclePlate || 'Chưa có xe'}
          </p>
        </div>

        <div className="grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-slate-200 bg-slate-200 sm:grid-cols-4">
          <div className="bg-white px-4 py-3">
            <p className="text-xs font-medium uppercase text-slate-500">Đã duyệt</p>
            <p className="mt-1 break-words text-sm font-semibold text-emerald-700">{formatCurrency(totals.approved)}</p>
          </div>
          <div className="bg-white px-4 py-3">
            <p className="text-xs font-medium uppercase text-slate-500">Chờ duyệt</p>
            <p className="mt-1 break-words text-sm font-semibold text-amber-700">{formatCurrency(totals.pending)}</p>
          </div>
          <div className="bg-white px-4 py-3">
            <p className="text-xs font-medium uppercase text-slate-500">Khoản chi</p>
            <p className="mt-1 font-semibold text-slate-900">{expenses.length}</p>
          </div>
          <div className="bg-white px-4 py-3">
            <p className="text-xs font-medium uppercase text-slate-500">Bị từ chối</p>
            <p className="mt-1 font-semibold text-rose-700">{totals.rejected}</p>
          </div>
        </div>

        <div className="flex flex-wrap items-center justify-between gap-3">
          <div>
            <h4 className="font-semibold text-slate-900">Lịch sử chi phí</h4>
            <p className="mt-1 text-sm text-slate-500">Chỉ khoản đã duyệt mới được tính vào báo cáo.</p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={loadExpenses}
              disabled={loading}
              title="Tải lại chi phí"
              aria-label="Tải lại chi phí"
              className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50"
            >
              <RefreshCw size={17} className={loading ? 'animate-spin' : ''} />
            </button>
            {canRecord && !showForm && (
              <button
                type="button"
                onClick={() => setShowForm(true)}
                className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700"
              >
                <Plus size={17} />
                Ghi chi phí
              </button>
            )}
          </div>
        </div>

        {!canRecord && (
          <div className="border-l-4 border-amber-400 bg-amber-50 px-4 py-3 text-sm text-amber-800">
            Chỉ có thể ghi chi phí khi chuyến đang vận chuyển hoặc đã hoàn tất và đã đến ngày bắt đầu.
          </div>
        )}

        {loadError ? (
          <div className="border-l-4 border-rose-500 bg-rose-50 px-4 py-3 text-sm text-rose-800">{loadError}</div>
        ) : loading ? (
          <div className="py-10 text-center text-sm text-slate-500">
            <Loader2 size={22} className="mx-auto mb-2 animate-spin text-emerald-600" />
            Đang tải chi phí...
          </div>
        ) : expenses.length === 0 ? (
          <div className="border border-dashed border-slate-300 px-4 py-10 text-center text-sm text-slate-500">
            Chuyến này chưa ghi nhận chi phí phát sinh.
          </div>
        ) : (
          <div className="divide-y divide-slate-100 overflow-hidden rounded-lg border border-slate-200">
            {expenses.map((expense) => {
              const status = statusMeta[expense.status] || statusMeta.APPROVED;
              const busy = reviewingId === expense.id || deletingId === expense.id;
              return (
                <article key={expense.id} className="min-w-0 bg-white p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div className="min-w-0">
                      <p className="break-words font-semibold text-slate-900">{typeLabel(expense)}</p>
                      <p className="mt-1 text-lg font-semibold text-emerald-700">{formatCurrency(expense.amount)}</p>
                    </div>
                    <span className={`shrink-0 rounded-full border px-2.5 py-1 text-xs font-semibold ${status.className}`}>
                      {status.label}
                    </span>
                  </div>

                  <div className="mt-3 grid gap-3 text-sm sm:grid-cols-3">
                    <div>
                      <p className="text-xs font-medium uppercase text-slate-500">Ngày chi</p>
                      <p className="mt-1 text-slate-800">{formatDate(expense.expenseDate)}</p>
                    </div>
                    <div>
                      <p className="text-xs font-medium uppercase text-slate-500">Người ghi</p>
                      <p className="mt-1 break-words text-slate-800">{expense.recordedByName || '-'}</p>
                      <p className="mt-0.5 text-xs text-slate-500">{roleLabels[expense.recordedByRole] || expense.recordedByRole || '-'}</p>
                    </div>
                    <div>
                      <p className="text-xs font-medium uppercase text-slate-500">Gửi lúc</p>
                      <p className="mt-1 text-slate-800">{formatDateTime(expense.createdAt)}</p>
                    </div>
                  </div>

                  {expense.description && <p className="mt-3 break-words text-sm leading-6 text-slate-600">{expense.description}</p>}
                  {expense.reviewNote && (
                    <p className={`mt-2 break-words text-sm leading-6 ${expense.status === 'REJECTED' ? 'text-rose-700' : 'text-slate-600'}`}>
                      Phản hồi: {expense.reviewNote}
                    </p>
                  )}

                  <div className="mt-3 flex flex-wrap items-center justify-between gap-3 border-t border-slate-100 pt-3">
                    <div>
                      {expense.receiptImageUrl ? (
                        <a
                          href={expense.receiptImageUrl}
                          target="_blank"
                          rel="noreferrer"
                          className="inline-flex items-center gap-2 text-sm font-medium text-sky-700 hover:text-sky-900"
                        >
                          <ReceiptText size={16} />
                          Xem ảnh biên lai
                        </a>
                      ) : <span className="text-xs text-slate-400">Không có ảnh biên lai</span>}
                    </div>

                    <div className="flex gap-2">
                      {canReview && expense.status === 'PENDING' && (
                        <>
                          <button
                            type="button"
                            onClick={() => approveExpense(expense)}
                            disabled={busy}
                            title="Duyệt khoản chi"
                            aria-label="Duyệt khoản chi"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 hover:bg-emerald-100 disabled:opacity-50"
                          >
                            {reviewingId === expense.id ? <Loader2 size={16} className="animate-spin" /> : <Check size={16} />}
                          </button>
                          <button
                            type="button"
                            onClick={() => { setRejectTarget(expense); setRejectNote(''); }}
                            disabled={busy}
                            title="Từ chối khoản chi"
                            aria-label="Từ chối khoản chi"
                            className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 text-rose-700 hover:bg-rose-100 disabled:opacity-50"
                          >
                            <X size={16} />
                          </button>
                        </>
                      )}
                      {canDelete && (
                        <button
                          type="button"
                          onClick={() => deleteExpense(expense)}
                          disabled={busy}
                          title="Xóa khoản chi"
                          aria-label="Xóa khoản chi"
                          className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 hover:bg-slate-50 disabled:opacity-50"
                        >
                          {deletingId === expense.id ? <Loader2 size={16} className="animate-spin" /> : <Trash2 size={16} />}
                        </button>
                      )}
                    </div>
                  </div>
                </article>
              );
            })}
          </div>
        )}

        {rejectTarget && (
          <div className="border-l-4 border-rose-500 bg-rose-50 px-4 py-4">
            <p className="font-semibold text-rose-900">Từ chối khoản chi {formatCurrency(rejectTarget.amount)}</p>
            <label className="mt-3 block text-sm font-medium text-rose-900">
              Lý do từ chối
              <textarea
                required
                maxLength={500}
                rows={3}
                value={rejectNote}
                onChange={(event) => setRejectNote(event.target.value)}
                className="mt-1 block w-full resize-y rounded-md border border-rose-200 bg-white px-3 py-2 text-sm text-slate-900 outline-none focus:border-rose-400 focus:ring-2 focus:ring-rose-100"
              />
            </label>
            <div className="mt-3 flex justify-end gap-2">
              <button type="button" onClick={() => setRejectTarget(null)} className="rounded-lg border border-slate-300 bg-white px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                Hủy
              </button>
              <button type="button" onClick={rejectExpense} disabled={Boolean(reviewingId)} className="rounded-lg bg-rose-600 px-3 py-2 text-sm font-medium text-white hover:bg-rose-700 disabled:opacity-50">
                Xác nhận từ chối
              </button>
            </div>
          </div>
        )}

        {showForm && canRecord && (
          <form onSubmit={handleSubmit} className="space-y-4 border-t border-slate-200 pt-5">
            <div className="flex items-center justify-between gap-3">
              <div>
                <h4 className="font-semibold text-slate-900">Ghi chi phí mới</h4>
                <p className="mt-1 text-sm text-slate-500">Khoản chi sẽ được gửi chờ quản lý hoặc kế toán duyệt.</p>
              </div>
              <button type="button" onClick={() => setShowForm(false)} title="Đóng biểu mẫu" aria-label="Đóng biểu mẫu" className="inline-flex h-9 w-9 items-center justify-center rounded-lg text-slate-500 hover:bg-slate-100">
                <X size={18} />
              </button>
            </div>

            <CheckboxGroup
              label="Loại chi phí"
              name="expenseTypes"
              options={expenseTypes}
              values={formData.expenseTypes}
              onChange={handleChange}
            />

            <div className="grid gap-4 sm:grid-cols-2">
              <label className="block min-w-0">
                <span className="text-sm font-medium text-slate-700">Số tiền (VNĐ)</span>
                <input
                  required
                  type="number"
                  min="1"
                  step="1"
                  name="amount"
                  value={formData.amount}
                  onChange={handleChange}
                  className="mt-1 block h-11 w-full rounded-md border border-slate-300 px-3 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
                />
              </label>
              <DateField
                required
                label="Ngày chi"
                name="expenseDate"
                value={formData.expenseDate}
                min={startDate || undefined}
                max={maxExpenseDate || undefined}
                onChange={handleChange}
              />
            </div>

            <label className="block">
              <span className="text-sm font-medium text-slate-700">
                Ghi chú {formData.expenseTypes.includes('OTHER') ? '(bắt buộc)' : ''}
              </span>
              <textarea
                required={formData.expenseTypes.includes('OTHER')}
                maxLength={500}
                rows={3}
                name="description"
                value={formData.description}
                onChange={handleChange}
                className="mt-1 block w-full resize-y rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm outline-none focus:border-emerald-500 focus:ring-2 focus:ring-emerald-100"
              />
            </label>

            <div className="grid gap-4 sm:grid-cols-[minmax(0,1fr)_160px] sm:items-center">
              <div>
                <p className="text-sm font-medium text-slate-700">Ảnh hóa đơn hoặc biên lai</p>
                <p className="mt-1 text-xs text-slate-500">JPG, PNG hoặc WebP; tối đa 1,5 MB.</p>
                <div className="mt-2 flex flex-wrap gap-2">
                  <label className="inline-flex h-10 cursor-pointer items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-50">
                    <ImagePlus size={16} />
                    Chọn ảnh
                    <input type="file" accept="image/jpeg,image/png,image/webp" onChange={handleReceiptChange} className="sr-only" />
                  </label>
                  {formData.receiptImageUrl && (
                    <button type="button" onClick={() => setFormData((current) => ({ ...current, receiptImageUrl: '' }))} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100">
                      <X size={16} />
                      Bỏ ảnh
                    </button>
                  )}
                </div>
              </div>
              {formData.receiptImageUrl && (
                <img src={formData.receiptImageUrl} alt="Biên lai đã chọn" className="h-28 w-full rounded-lg border border-slate-200 object-cover" />
              )}
            </div>

            <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
              <button type="button" onClick={() => setShowForm(false)} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
                Hủy
              </button>
              <button type="submit" disabled={submitting} className="inline-flex min-w-32 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50">
                {submitting ? <Loader2 size={16} className="animate-spin" /> : <ReceiptText size={16} />}
                {submitting ? 'Đang gửi...' : 'Gửi chi phí'}
              </button>
            </div>
          </form>
        )}

        <div className="flex justify-end border-t border-slate-200 pt-4">
          <button type="button" onClick={closeModal} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
            Đóng
          </button>
        </div>
      </div>
    </Modal>
  );
}
