import { useCallback, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import {
  CheckCircle2,
  Clock3,
  FileText,
  Loader2,
  MapPinned,
  MinusCircle,
  Play,
  RefreshCw,
  Truck,
  UserRound,
  WalletCards,
  XCircle,
} from 'lucide-react';
import api from '../services/api';
import { formatDateTime } from '../utils/dates';
import Modal from './Modal';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';

const groupOrder = ['TRIP', 'CONTRACT', 'VEHICLE', 'DRIVER', 'FINANCE'];
const groupMeta = {
  TRIP: { label: 'Thông tin chuyến', icon: MapPinned },
  CONTRACT: { label: 'Hợp đồng', icon: FileText },
  VEHICLE: { label: 'Phương tiện', icon: Truck },
  DRIVER: { label: 'Tài xế', icon: UserRound },
  FINANCE: { label: 'Tài chính', icon: WalletCards },
};

const statusMeta = {
  PASSED: {
    label: 'Đạt',
    icon: CheckCircle2,
    iconClass: 'text-emerald-600',
    badgeClass: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  },
  BLOCKED: {
    label: 'Đang chặn',
    icon: XCircle,
    iconClass: 'text-rose-600',
    badgeClass: 'border-rose-200 bg-rose-50 text-rose-700',
  },
  WAITING: {
    label: 'Chờ kiểm tra',
    icon: Clock3,
    iconClass: 'text-amber-600',
    badgeClass: 'border-amber-200 bg-amber-50 text-amber-700',
  },
  NOT_APPLICABLE: {
    label: 'Không áp dụng',
    icon: MinusCircle,
    iconClass: 'text-slate-400',
    badgeClass: 'border-slate-200 bg-slate-50 text-slate-600',
  },
};

function ReadinessCheck({ check }) {
  const meta = statusMeta[check.status] || statusMeta.WAITING;
  const Icon = meta.icon;

  return (
    <div className="grid min-w-0 grid-cols-[auto_minmax(0,1fr)] gap-3 px-4 py-3">
      <Icon size={20} className={`mt-0.5 shrink-0 ${meta.iconClass}`} aria-hidden="true" />
      <div className="min-w-0">
        <div className="flex min-w-0 flex-wrap items-center justify-between gap-2">
          <p className="min-w-0 font-medium text-slate-900">{check.label}</p>
          <span className={`inline-flex shrink-0 rounded-full border px-2 py-0.5 text-xs font-medium ${meta.badgeClass}`}>
            {meta.label}
          </span>
        </div>
        <p className="mt-1 break-words text-sm leading-6 text-slate-600">{check.message}</p>
        {check.resolution && (
          <p className="mt-1 break-words text-sm font-medium leading-6 text-rose-700">
            Cần làm: {check.resolution}
          </p>
        )}
      </div>
    </div>
  );
}

export default function TripReadinessModal({ trip, onClose, onStarted }) {
  const [readiness, setReadiness] = useState(null);
  const [loading, setLoading] = useState(false);
  const [starting, setStarting] = useState(false);
  const [loadError, setLoadError] = useState('');

  const loadReadiness = useCallback(async () => {
    if (!trip?.id) return;
    setLoading(true);
    setLoadError('');
    setReadiness(null);
    try {
      const response = await api.get(`/trips/${trip.id}/readiness`);
      setReadiness(getResult(response, null));
    } catch (error) {
      const message = error.response?.data?.message || 'Không thể kiểm tra điều kiện khởi hành';
      setLoadError(message);
      setReadiness(null);
    } finally {
      setLoading(false);
    }
  }, [trip?.id]);

  useEffect(() => {
    if (trip?.id) loadReadiness();
  }, [trip?.id, loadReadiness]);

  const groupedChecks = useMemo(() => {
    const groups = new Map(groupOrder.map((group) => [group, []]));
    (readiness?.checks || []).forEach((check) => {
      const group = groups.has(check.group) ? check.group : 'TRIP';
      groups.get(group).push(check);
    });
    return groupOrder
      .map((group) => ({ group, checks: groups.get(group) }))
      .filter((item) => item.checks.length > 0);
  }, [readiness]);

  const handleStart = async () => {
    if (!trip?.id || !readiness?.ready) return;
    setStarting(true);
    try {
      await api.patch(`/trips/${trip.id}/start`);
      toast.success('Đã bắt đầu chuyến đi');
      onStarted();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Điều kiện đã thay đổi, chưa thể bắt đầu chuyến');
      await loadReadiness();
    } finally {
      setStarting(false);
    }
  };

  const closeModal = () => {
    if (starting) return;
    setReadiness(null);
    setLoadError('');
    onClose();
  };

  const applicableCount = Number(readiness?.passedCount || 0)
    + Number(readiness?.blockedCount || 0)
    + Number(readiness?.waitingCount || 0);

  return (
    <Modal
      isOpen={Boolean(trip)}
      onClose={closeModal}
      title="Điều kiện khởi hành"
      size="wide"
      variant="detail"
    >
      <div className="space-y-5">
        <div className="-mx-6 -mt-4 border-b border-slate-200 bg-slate-50 px-6 py-4">
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div className="min-w-0">
              <p className="text-xs font-medium uppercase text-slate-500">Chuyến {shortId(trip?.id)}</p>
              <p className="mt-1 break-words font-semibold text-slate-950">
                {[trip?.startLocation, trip?.endLocation].filter(Boolean).join(' → ') || 'Chưa có tuyến đường'}
              </p>
              <p className="mt-1 text-sm text-slate-600">
                {trip?.contractCode ? `Hợp đồng ${trip.contractCode}` : 'Không gắn hợp đồng'}
                {' · '}{trip?.vehiclePlate || 'Chưa có xe'}
                {' · '}{trip?.driverName || 'Chưa có tài xế'}
              </p>
            </div>

            {readiness && (
              <span className={`inline-flex shrink-0 items-center gap-2 rounded-full border px-3 py-1.5 text-sm font-semibold ${
                readiness.ready
                  ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                  : 'border-rose-200 bg-rose-50 text-rose-700'
              }`}>
                {readiness.ready ? <CheckCircle2 size={16} /> : <XCircle size={16} />}
                {readiness.ready ? 'Sẵn sàng khởi hành' : 'Chưa đủ điều kiện'}
              </span>
            )}
          </div>
        </div>

        {loading ? (
          <div className="py-14 text-center text-sm text-slate-500">
            <Loader2 size={24} className="mx-auto mb-3 animate-spin text-emerald-600" />
            Đang kiểm tra dữ liệu mới nhất...
          </div>
        ) : loadError ? (
          <div className="border-l-4 border-rose-500 bg-rose-50 px-4 py-3">
            <p className="font-medium text-rose-800">{loadError}</p>
            <button
              type="button"
              onClick={loadReadiness}
              className="mt-2 inline-flex items-center gap-2 text-sm font-semibold text-rose-700 hover:text-rose-900"
            >
              <RefreshCw size={15} />
              Kiểm tra lại
            </button>
          </div>
        ) : readiness ? (
          <>
            <div className="grid grid-cols-2 gap-px overflow-hidden rounded-lg border border-slate-200 bg-slate-200 lg:grid-cols-4">
              <div className="bg-white px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">Đã đạt</p>
                <p className="mt-1 text-xl font-semibold text-emerald-700">
                  {readiness.passedCount}/{applicableCount}
                </p>
              </div>
              <div className="bg-white px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">Đang chặn</p>
                <p className={`mt-1 text-xl font-semibold ${readiness.blockedCount > 0 ? 'text-rose-700' : 'text-slate-900'}`}>
                  {readiness.blockedCount}
                </p>
              </div>
              <div className="bg-white px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">Đang chờ</p>
                <p className={`mt-1 text-xl font-semibold ${readiness.waitingCount > 0 ? 'text-amber-700' : 'text-slate-900'}`}>
                  {readiness.waitingCount}
                </p>
              </div>
              <div className="bg-white px-4 py-3">
                <p className="text-xs font-medium uppercase text-slate-500">Không áp dụng</p>
                <p className="mt-1 text-xl font-semibold text-slate-900">{readiness.notApplicableCount}</p>
              </div>
            </div>

            <p className="text-right text-xs text-slate-500">
              Kiểm tra lần cuối: <span className="font-medium text-slate-700">{formatDateTime(readiness.checkedAt)}</span>
            </p>

            {readiness.primaryBlockerMessage && (
              <div className="border-l-4 border-rose-500 bg-rose-50 px-4 py-3">
                <p className="font-semibold text-rose-900">Vướng mắc cần xử lý trước</p>
                <p className="mt-1 text-sm leading-6 text-rose-800">{readiness.primaryBlockerMessage}</p>
                {readiness.primaryResolution && (
                  <p className="mt-1 text-sm font-medium leading-6 text-rose-900">
                    Cần làm: {readiness.primaryResolution}
                  </p>
                )}
              </div>
            )}

            <div className="space-y-5">
              {groupedChecks.map(({ group, checks }) => {
                const meta = groupMeta[group] || groupMeta.TRIP;
                const Icon = meta.icon;
                return (
                  <section key={group}>
                    <div className="mb-2 flex items-center gap-2">
                      <Icon size={17} className="text-emerald-700" aria-hidden="true" />
                      <h4 className="text-sm font-semibold text-slate-900">{meta.label}</h4>
                    </div>
                    <div className="divide-y divide-slate-100 overflow-hidden rounded-lg border border-slate-200 bg-white">
                      {checks.map((check) => <ReadinessCheck key={check.key} check={check} />)}
                    </div>
                  </section>
                );
              })}
            </div>
          </>
        ) : null}

        <div className="flex flex-wrap items-center justify-between gap-3 border-t border-slate-200 pt-4">
          <button
            type="button"
            onClick={loadReadiness}
            disabled={loading || starting}
            title="Kiểm tra lại điều kiện"
            className="inline-flex h-10 w-10 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-50"
          >
            <RefreshCw size={17} className={loading ? 'animate-spin' : ''} />
          </button>
          <div className="flex gap-3">
            <button
              type="button"
              onClick={closeModal}
              disabled={starting}
              className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50 disabled:opacity-50"
            >
              Đóng
            </button>
            <button
              type="button"
              onClick={handleStart}
              disabled={!readiness?.ready || loading || starting}
              className="inline-flex min-w-40 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:bg-slate-300"
            >
              {starting ? <Loader2 size={16} className="animate-spin" /> : <Play size={16} />}
              {starting ? 'Đang bắt đầu...' : readiness?.ready ? 'Bắt đầu chuyến' : 'Chưa thể bắt đầu'}
            </button>
          </div>
        </div>
      </div>
    </Modal>
  );
}
