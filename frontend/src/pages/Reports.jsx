import { Fragment, useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import {
  BarChart3,
  Download,
  Eye,
  Filter,
  Printer,
  RefreshCw,
  Route,
  Users,
  X,
} from 'lucide-react';
import DateField from '../components/DateField';
import SearchableSelect from '../components/SearchableSelect';
import api from '../services/api';
import { formatDateTime, toDateInputValue } from '../utils/dates';

const getResult = (response, fallback) => response.data?.result ?? response.data ?? fallback;
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const formatKm = (value) => `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} km`;
const formatTon = (value) => `${Number(value || 0).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tấn`;
const csvEscape = (value) => `"${String(value ?? '').replaceAll('"', '""')}"`;
const emptyRows = [];

const buildDatePresets = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = today.getMonth();
  const lastSevenDays = new Date(year, month, today.getDate() - 6);
  const lastThirtyDays = new Date(year, month, today.getDate() - 29);

  return [
    { key: 'all', label: 'Tất cả', fromDate: '', toDate: '' },
    { key: 'seven-days', label: '7 ngày gần nhất', fromDate: toDateInputValue(lastSevenDays), toDate: toDateInputValue(today) },
    { key: 'thirty-days', label: '30 ngày gần nhất', fromDate: toDateInputValue(lastThirtyDays), toDate: toDateInputValue(today) },
    {
      key: 'this-month',
      label: 'Tháng này',
      fromDate: toDateInputValue(new Date(year, month, 1)),
      toDate: toDateInputValue(new Date(year, month + 1, 0)),
    },
    {
      key: 'this-year',
      label: 'Năm nay',
      fromDate: toDateInputValue(new Date(year, 0, 1)),
      toDate: toDateInputValue(new Date(year, 11, 31)),
    },
  ];
};

const initialFilters = {
  fromDate: '',
  toDate: '',
  customerId: '',
  tripStatus: '',
};

const emptyTotals = {
  tripCount: 0,
  completedTripCount: 0,
  distanceKm: 0,
  recognizedRevenue: 0,
  paidRevenue: 0,
  depositApplied: 0,
  depositAvailable: 0,
  outstanding: 0,
  tripExpense: 0,
  maintenanceExpense: 0,
  totalExpense: 0,
  grossProfit: 0,
  netProfit: 0,
};

const statusLabels = {
  CREATED: 'Mới tạo',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang vận chuyển',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
};

const statusClasses = {
  CREATED: 'border-slate-200 bg-slate-100 text-slate-700',
  ASSIGNED: 'border-sky-200 bg-sky-50 text-sky-700',
  IN_PROGRESS: 'border-amber-200 bg-amber-50 text-amber-700',
  COMPLETED: 'border-emerald-200 bg-emerald-50 text-emerald-700',
  CANCELLED: 'border-rose-200 bg-rose-50 text-rose-700',
};

const Metric = ({ label, value, tone = 'text-slate-950', detail }) => (
  <article className="min-w-0 rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
    <p className="text-xs font-medium uppercase leading-5 text-slate-500">{label}</p>
    <p className={`mt-2 min-w-0 text-lg font-semibold leading-6 tabular-nums [overflow-wrap:anywhere] ${tone}`}>{value}</p>
    {detail && <p className="mt-1 text-xs leading-5 text-slate-500 [overflow-wrap:anywhere]">{detail}</p>}
  </article>
);

const StatusBadge = ({ status }) => (
  <span className={`inline-flex whitespace-nowrap rounded-md border px-2 py-1 text-xs font-medium ${statusClasses[status] || statusClasses.CREATED}`}>
    {statusLabels[status] || status || '-'}
  </span>
);

const EmptyRow = ({ colSpan, loading, label }) => (
  <tr>
    <td colSpan={colSpan} className="px-4 py-12 text-center text-sm text-slate-500">
      {loading ? 'Đang tải báo cáo...' : label}
    </td>
  </tr>
);

export default function Reports() {
  const [loading, setLoading] = useState(true);
  const [customers, setCustomers] = useState([]);
  const [report, setReport] = useState(null);
  const [draftFilters, setDraftFilters] = useState(initialFilters);
  const [appliedFilters, setAppliedFilters] = useState(initialFilters);
  const [viewMode, setViewMode] = useState('trips');
  const [expandedTripId, setExpandedTripId] = useState(null);

  const loadReport = async (filters = appliedFilters) => {
    setLoading(true);
    try {
      const params = Object.fromEntries(
        Object.entries(filters).filter(([, value]) => value !== '')
      );
      const response = await api.get('/reports/financial', { params });
      setReport(getResult(response, null));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải báo cáo');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        const response = await api.get('/customers');
        setCustomers(getResult(response, []));
      } catch {
        setCustomers([]);
      }
    };

    loadInitialData();
    loadReport(initialFilters);
  }, []);

  const totals = report?.totals || emptyTotals;
  const tripRows = report?.trips || emptyRows;
  const customerRows = report?.customers || emptyRows;
  const monthlyRows = report?.monthly || emptyRows;
  const datePresets = buildDatePresets();
  const activeDatePreset = datePresets.find((preset) => (
    preset.fromDate === draftFilters.fromDate && preset.toDate === draftFilters.toDate
  ))?.key;

  const maxMonthlyAmount = useMemo(() => Math.max(
    1,
    ...monthlyRows.flatMap((row) => [
      Number(row.revenue || 0),
      Number(row.tripCost || 0) + Number(row.maintenanceCost || 0),
      Math.abs(Number(row.netProfit || 0)),
    ])
  ), [monthlyRows]);

  const handleFilterChange = (event) => {
    const { name, value } = event.target;
    setDraftFilters((current) => ({ ...current, [name]: value }));
  };

  const applyFilters = (event) => {
    event.preventDefault();
    if (draftFilters.fromDate && draftFilters.toDate && draftFilters.fromDate > draftFilters.toDate) {
      toast.error('Ngày bắt đầu không được sau ngày kết thúc');
      return;
    }
    setAppliedFilters(draftFilters);
    setExpandedTripId(null);
    loadReport(draftFilters);
  };

  const clearFilters = () => {
    setDraftFilters(initialFilters);
    setAppliedFilters(initialFilters);
    setExpandedTripId(null);
    loadReport(initialFilters);
  };

  const applyDatePreset = (preset) => {
    const nextFilters = {
      ...draftFilters,
      fromDate: preset.fromDate,
      toDate: preset.toDate,
    };

    setDraftFilters(nextFilters);
    setAppliedFilters(nextFilters);
    setExpandedTripId(null);
    loadReport(nextFilters);
  };

  const exportCsv = () => {
    let header;
    let rows;

    if (viewMode === 'customers') {
      header = ['Khach hang', 'So chuyen', 'Chuyen hoan tat', 'Tong km', 'Doanh thu', 'Da thu', 'Coc da can', 'Cong no', 'Chi phi chuyen', 'Loi nhuan gop'];
      rows = customerRows.map((row) => [
        row.customerName,
        row.tripCount,
        row.completedTripCount,
        row.distanceKm,
        row.revenue,
        row.paidRevenue,
        row.depositApplied,
        row.outstanding,
        row.tripCost,
        row.profit,
      ]);
    } else if (viewMode === 'monthly') {
      header = ['Thang', 'Doanh thu', 'Da thu', 'Coc da can', 'Cong no', 'Chi phi chuyen', 'Bao duong', 'Loi nhuan rong'];
      rows = monthlyRows.map((row) => [
        row.label,
        row.revenue,
        row.paidRevenue,
        row.depositApplied,
        row.outstanding,
        row.tripCost,
        row.maintenanceCost,
        row.netProfit,
      ]);
    } else {
      header = ['Ma chuyen', 'Trang thai', 'Khach hang', 'Xe', 'Bat dau', 'Ket thuc', 'Tuyen duong', 'Quang duong km', 'Trong luong tan', 'Cuoc du kien', 'Doanh thu', 'Da thu', 'Coc da can', 'Cong no', 'Chi phi', 'Loi nhuan gop'];
      rows = tripRows.map((row) => [
        row.code,
        statusLabels[row.status] || row.status,
        row.customerName,
        row.vehiclePlate,
        row.startTime,
        row.endTime,
        row.route,
        row.distanceKm,
        row.cargoWeightTon,
        row.freightAmount,
        row.revenue,
        row.paidRevenue,
        row.depositApplied,
        row.outstanding,
        row.cost,
        row.profit,
      ]);
    }

    const csv = `\uFEFF${[header, ...rows].map((row) => row.map(csvEscape).join(',')).join('\n')}`;
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `bao-cao-tai-chinh-${viewMode}.csv`;
    link.click();
    link.remove();
    URL.revokeObjectURL(url);
  };

  const tabs = [
    { key: 'trips', label: 'Theo chuyến', icon: Route, count: tripRows.length },
    { key: 'customers', label: 'Theo khách hàng', icon: Users, count: customerRows.length },
    { key: 'monthly', label: 'Theo tháng', icon: BarChart3, count: monthlyRows.length },
  ];

  return (
    <section className="space-y-5">
      <div className="flex flex-col gap-4 xl:flex-row xl:items-end xl:justify-between">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">Báo cáo tài chính vận tải</h1>
          <p className="mt-1 text-sm text-slate-500">
            Đối chiếu doanh thu, thực thu, cọc đã cấn, công nợ, chi phí và lợi nhuận từ cùng một nguồn dữ liệu.
          </p>
        </div>
        <div className="flex flex-wrap gap-2 print:hidden">
          <button type="button" onClick={() => loadReport()} title="Tải lại dữ liệu" className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} />
            Tải lại
          </button>
          <button type="button" onClick={exportCsv} title="Xuất dữ liệu đang xem" disabled={loading} className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100 disabled:opacity-50">
            <Download size={16} />
            Xuất CSV
          </button>
          <button type="button" onClick={() => window.print()} title="In báo cáo" className="inline-flex h-10 items-center justify-center gap-2 rounded-lg bg-emerald-600 px-3 text-sm font-medium text-white hover:bg-emerald-700">
            <Printer size={16} />
            In báo cáo
          </button>
        </div>
      </div>

      <form onSubmit={applyFilters} className="rounded-lg border border-slate-200 bg-white p-3 shadow-sm print:hidden">
        <div className="mb-3 flex flex-col gap-2 xl:flex-row xl:items-center xl:justify-between">
          <div className="flex flex-wrap items-center gap-2">
            <Filter size={18} className="text-emerald-700" />
            <h2 className="text-sm font-semibold text-slate-950">Bộ lọc báo cáo</h2>
            <span className="text-xs text-slate-500">Ngày lọc được tính theo thời điểm bắt đầu chuyến.</span>
          </div>
          <div className="flex flex-wrap gap-1" aria-label="Chọn nhanh khoảng ngày">
            {datePresets.map((preset) => (
              <button
                key={preset.key}
                type="button"
                disabled={loading}
                aria-pressed={activeDatePreset === preset.key}
                onClick={() => applyDatePreset(preset)}
                className={`min-h-7 whitespace-nowrap rounded-md border px-2 text-xs font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${
                  activeDatePreset === preset.key
                    ? 'border-emerald-600 bg-emerald-600 text-white'
                    : 'border-slate-200 bg-slate-50 text-slate-600 hover:border-emerald-200 hover:bg-emerald-50 hover:text-emerald-700'
                }`}
              >
                {preset.label}
              </button>
            ))}
          </div>
        </div>
        <div className="grid grid-cols-1 gap-2.5 sm:grid-cols-2 xl:grid-cols-[minmax(210px,1fr)_minmax(210px,1fr)_minmax(180px,0.9fr)_minmax(180px,0.9fr)_auto] xl:items-end">
          <DateField compact label="Chuyến bắt đầu từ ngày" name="fromDate" value={draftFilters.fromDate} max={draftFilters.toDate || undefined} onChange={handleFilterChange} />
          <DateField compact label="Chuyến bắt đầu đến ngày" name="toDate" value={draftFilters.toDate} min={draftFilters.fromDate || undefined} popupAlign="right" onChange={handleFilterChange} />
          <div className="min-w-0">
            <SearchableSelect
              label="Khách hàng"
              name="customerId"
              value={draftFilters.customerId}
              allLabel="Tất cả khách hàng"
              placeholder="Tìm khách hàng..."
              options={customers.map((customer) => ({
                value: customer.id,
                label: [customer.name, customer.username].filter(Boolean).join(' - ') || customer.id,
              }))}
              onChange={handleFilterChange}
            />
          </div>
          <div className="min-w-0">
              <label className="flex min-h-6 items-center text-xs font-medium text-slate-600">Trạng thái chuyến</label>
              <select name="tripStatus" value={draftFilters.tripStatus} onChange={handleFilterChange} className="mt-1 block h-11 w-full min-w-0 rounded-md border border-slate-300 bg-white px-3 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-2 focus:ring-emerald-100">
                <option value="">Tất cả trạng thái</option>
                {Object.entries(statusLabels).map(([value, label]) => (
                  <option key={value} value={value}>{label}</option>
                ))}
              </select>
          </div>
          <div className="flex min-w-0 gap-2 sm:col-span-2 xl:col-span-1 xl:justify-end">
              <button type="submit" disabled={loading} className="inline-flex h-11 min-w-0 flex-1 items-center justify-center gap-2 whitespace-nowrap rounded-lg bg-emerald-600 px-4 text-sm font-medium text-white hover:bg-emerald-700 disabled:opacity-50 xl:flex-none">
                <Filter size={16} />
                Áp dụng
              </button>
              <button type="button" onClick={clearFilters} title="Xóa bộ lọc" className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg border border-slate-300 bg-white text-slate-600 hover:bg-slate-100">
                <X size={17} />
              </button>
          </div>
        </div>
      </form>

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <Metric label="Doanh thu ghi nhận" value={loading ? '...' : formatCurrency(totals.recognizedRevenue)} />
        <Metric label="Đã thu" value={loading ? '...' : formatCurrency(totals.paidRevenue)} tone="text-emerald-700" />
        <Metric label="Cọc đã cấn" value={loading ? '...' : formatCurrency(totals.depositApplied)} tone="text-sky-700" />
        <Metric label="Cọc đang giữ" value={loading ? '...' : formatCurrency(totals.depositAvailable)} tone="text-violet-700" detail="Số dư hiện tại, không tính là doanh thu." />
        <Metric label="Công nợ" value={loading ? '...' : formatCurrency(totals.outstanding)} tone="text-amber-700" />
        <Metric label="Chi phí chuyến" value={loading ? '...' : formatCurrency(totals.tripExpense)} tone="text-rose-700" />
        <Metric
          label="Chi phí bảo dưỡng"
          value={loading ? '...' : formatCurrency(totals.maintenanceExpense)}
          tone="text-violet-700"
          detail={report && !report.maintenanceIncluded ? 'Không phân bổ khi lọc theo khách hàng hoặc trạng thái.' : undefined}
        />
        <Metric label="Tổng chi phí" value={loading ? '...' : formatCurrency(totals.totalExpense)} tone="text-rose-700" />
        <Metric label="Lợi nhuận gộp chuyến" value={loading ? '...' : formatCurrency(totals.grossProfit)} tone={totals.grossProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'} />
        <Metric
          label="Lợi nhuận ròng"
          value={loading ? '...' : formatCurrency(totals.netProfit)}
          tone={totals.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'}
          detail={`${totals.completedTripCount || 0}/${totals.tripCount || 0} chuyến hoàn tất · ${formatKm(totals.distanceKm)}`}
        />
      </div>

      <div className="flex flex-col gap-3 border-b border-slate-200 pb-3 xl:flex-row xl:items-center xl:justify-between print:hidden">
        <div className="grid w-full shrink-0 grid-cols-1 gap-1 rounded-lg border border-slate-200 bg-slate-100 p-1 sm:grid-cols-3 xl:w-auto" role="tablist" aria-label="Chế độ xem báo cáo">
          {tabs.map((tab) => (
            <button
              key={tab.key}
              type="button"
              role="tab"
              aria-selected={viewMode === tab.key}
              onClick={() => setViewMode(tab.key)}
              className={`grid min-h-10 min-w-0 grid-cols-[auto_minmax(0,1fr)_auto] items-center gap-2 whitespace-nowrap rounded-md px-3 text-sm font-medium transition ${
                viewMode === tab.key
                  ? 'bg-white text-emerald-700 shadow-sm'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <tab.icon size={16} className="shrink-0" />
              <span className="min-w-0 text-center">{tab.label}</span>
              <span className="shrink-0 rounded bg-slate-200 px-1.5 py-0.5 text-[11px] text-slate-600">{tab.count}</span>
            </button>
          ))}
        </div>
        <p className="min-w-0 text-xs leading-5 text-slate-500 xl:max-w-xl xl:text-right">
          Hóa đơn đã hủy không được tính vào doanh thu. Lợi nhuận theo khách hàng chỉ trừ chi phí trực tiếp của chuyến.
        </p>
      </div>

      {viewMode === 'trips' && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1320px] table-fixed divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-100">
                <tr>
                  <th className="w-32 whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Mã chuyến</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Trạng thái</th>
                  <th className="w-56 whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Khách hàng</th>
                  <th className="w-80 whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Tuyến đường</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Doanh thu</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Chi phí</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Lợi nhuận</th>
                  <th className="w-24 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Chi tiết</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading || tripRows.length === 0 ? (
                  <EmptyRow colSpan={8} loading={loading} label="Không có chuyến đi phù hợp bộ lọc." />
                ) : tripRows.map((row) => {
                  const expanded = expandedTripId === row.id;
                  return (
                    <Fragment key={row.id}>
                      <tr className="hover:bg-slate-50">
                        <td className="whitespace-nowrap px-4 py-3 font-medium text-slate-900">{row.code}</td>
                        <td className="px-4 py-3"><StatusBadge status={row.status} /></td>
                        <td className="max-w-52 px-4 py-3 text-slate-700"><div className="truncate" title={row.customerName}>{row.customerName || '-'}</div></td>
                        <td className="max-w-80 px-4 py-3 text-slate-700"><div className="truncate" title={row.route}>{row.route || '-'}</div></td>
                        <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{formatCurrency(row.revenue)}</td>
                        <td className="whitespace-nowrap px-4 py-3 text-right text-rose-700">{formatCurrency(row.cost)}</td>
                        <td className={`whitespace-nowrap px-4 py-3 text-right font-semibold ${row.profit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{formatCurrency(row.profit)}</td>
                        <td className="px-4 py-3 text-right">
                          <button type="button" onClick={() => setExpandedTripId(expanded ? null : row.id)} title={expanded ? 'Thu gọn' : 'Xem đầy đủ'} aria-label={expanded ? 'Thu gọn chi tiết chuyến đi' : 'Xem chi tiết chuyến đi'} className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 transition hover:bg-emerald-100">
                            <Eye size={16} />
                          </button>
                        </td>
                      </tr>
                      {expanded && (
                        <tr className="bg-emerald-50/60">
                          <td colSpan={8} className="px-4 py-4">
                            <dl className="grid gap-x-6 gap-y-4 border-l-4 border-emerald-400 pl-4 sm:grid-cols-2 lg:grid-cols-4">
                              {[
                                ['Xe', row.vehiclePlate],
                                ['Bắt đầu', formatDateTime(row.startTime)],
                                ['Kết thúc', formatDateTime(row.endTime)],
                                ['Quãng đường', formatKm(row.distanceKm)],
                                ['Trọng lượng', formatTon(row.cargoWeightTon)],
                                ['Cước dự kiến', formatCurrency(row.freightAmount)],
                                ['Đã thu', formatCurrency(row.paidRevenue)],
                                ['Cọc đã cấn', formatCurrency(row.depositApplied)],
                                ['Công nợ', formatCurrency(row.outstanding)],
                              ].map(([label, value]) => (
                                <div key={label}>
                                  <dt className="text-xs font-medium uppercase text-emerald-700">{label}</dt>
                                  <dd className="mt-1 break-words font-medium text-slate-900">{value}</dd>
                                </div>
                              ))}
                              <div className="sm:col-span-2 lg:col-span-4">
                                <dt className="text-xs font-medium uppercase text-emerald-700">Tuyến đường đầy đủ</dt>
                                <dd className="mt-1 break-words font-medium text-slate-900">{row.route || '-'}</dd>
                              </div>
                            </dl>
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {viewMode === 'customers' && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full min-w-[1320px] table-fixed divide-y divide-slate-200 text-sm">
              <thead className="bg-slate-100">
                <tr>
                  <th className="w-64 whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">Khách hàng</th>
                  <th className="w-28 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Số chuyến</th>
                  <th className="w-28 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Hoàn tất</th>
                  <th className="w-32 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Tổng km</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Doanh thu</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Đã thu</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Cọc đã cấn</th>
                  <th className="w-40 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Công nợ</th>
                  <th className="w-44 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Chi phí chuyến</th>
                  <th className="w-44 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Lợi nhuận gộp</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {loading || customerRows.length === 0 ? (
                  <EmptyRow colSpan={10} loading={loading} label="Không có dữ liệu khách hàng phù hợp bộ lọc." />
                ) : customerRows.map((row) => (
                  <tr key={row.customerId || row.customerName} className="hover:bg-slate-50">
                    <td className="px-4 py-3 font-medium text-slate-900"><div className="truncate" title={row.customerName}>{row.customerName || '-'}</div></td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{row.tripCount}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{row.completedTripCount}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{formatKm(row.distanceKm)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-slate-700">{formatCurrency(row.revenue)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-emerald-700">{formatCurrency(row.paidRevenue)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-sky-700">{formatCurrency(row.depositApplied)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-amber-700">{formatCurrency(row.outstanding)}</td>
                    <td className="whitespace-nowrap px-4 py-3 text-right text-rose-700">{formatCurrency(row.tripCost)}</td>
                    <td className={`whitespace-nowrap px-4 py-3 text-right font-semibold ${row.profit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{formatCurrency(row.profit)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {viewMode === 'monthly' && (
        <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
          <div className="border-b border-slate-200 px-4 py-4">
            <h2 className="text-sm font-semibold text-slate-950">Xu hướng doanh thu và chi phí</h2>
            <p className="mt-1 text-xs text-slate-500">Các khoản của chuyến được gom theo tháng bắt đầu chuyến; bảo dưỡng theo ngày bảo dưỡng.</p>
          </div>
          {loading || monthlyRows.length === 0 ? (
            <div className="px-4 py-12 text-center text-sm text-slate-500">{loading ? 'Đang tải báo cáo...' : 'Chưa có dữ liệu theo tháng.'}</div>
          ) : (
            <div className="divide-y divide-slate-100">
              {monthlyRows.map((row) => {
                const cost = Number(row.tripCost || 0) + Number(row.maintenanceCost || 0);
                return (
                  <div key={row.period} className="grid min-w-0 gap-4 px-4 py-4 sm:grid-cols-2 xl:grid-cols-[90px_minmax(240px,1fr)_repeat(4,minmax(130px,auto))] xl:items-center">
                    <p className="font-semibold text-slate-900">{row.label}</p>
                    <div className="min-w-0 space-y-2 sm:col-span-2 xl:col-span-1">
                      <div className="flex items-center gap-2">
                        <span className="w-16 text-xs text-slate-500">Doanh thu</span>
                        <div className="h-2 flex-1 overflow-hidden rounded bg-slate-100">
                          <div className="h-full rounded bg-emerald-500" style={{ width: Number(row.revenue || 0) > 0 ? `${Math.max(1, Number(row.revenue || 0) / maxMonthlyAmount * 100)}%` : '0%' }} />
                        </div>
                      </div>
                      <div className="flex items-center gap-2">
                        <span className="w-16 text-xs text-slate-500">Chi phí</span>
                        <div className="h-2 flex-1 overflow-hidden rounded bg-slate-100">
                          <div className="h-full rounded bg-rose-400" style={{ width: cost > 0 ? `${Math.max(1, cost / maxMonthlyAmount * 100)}%` : '0%' }} />
                        </div>
                      </div>
                    </div>
                    <div className="min-w-0 xl:text-right">
                      <p className="text-xs text-slate-500">Doanh thu</p>
                      <p className="mt-1 font-medium text-slate-900 [overflow-wrap:anywhere]">{formatCurrency(row.revenue)}</p>
                    </div>
                    <div className="min-w-0 xl:text-right">
                      <p className="text-xs text-slate-500">Cọc đã cấn</p>
                      <p className="mt-1 font-medium text-sky-700 [overflow-wrap:anywhere]">{formatCurrency(row.depositApplied)}</p>
                    </div>
                    <div className="min-w-0 xl:text-right">
                      <p className="text-xs text-slate-500">Tổng chi phí</p>
                      <p className="mt-1 font-medium text-rose-700 [overflow-wrap:anywhere]">{formatCurrency(cost)}</p>
                    </div>
                    <div className="min-w-0 xl:text-right">
                      <p className="text-xs text-slate-500">Lợi nhuận ròng</p>
                      <p className={`mt-1 font-semibold [overflow-wrap:anywhere] ${row.netProfit >= 0 ? 'text-emerald-700' : 'text-rose-700'}`}>{formatCurrency(row.netProfit)}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}
    </section>
  );
}
