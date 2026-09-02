import { useEffect, useMemo, useState } from 'react';
import toast from 'react-hot-toast';
import { CreditCard, Eye, FileText, Map, RefreshCw, UserRound, WalletCards } from 'lucide-react';
import Modal from '../components/Modal';
import api from '../services/api';
import { cargoTypeLabel } from '../utils/cargoTypes';
import { contractValueModeLabels, isPerTripContract } from '../utils/contracts';
import { formatDate, formatDateTime } from '../utils/dates';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const hasValue = (value) => value !== null && value !== undefined && value !== '';
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const formatOptionalCurrency = (value) => hasValue(value) ? formatCurrency(value) : '-';
const formatKm = (value) => hasValue(value)
  ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} km`
  : '-';
const formatTon = (value) => hasValue(value)
  ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tấn`
  : '-';
const formatContractValue = (contract) => isPerTripContract(contract)
  ? contractValueModeLabels.PER_TRIP
  : `Thỏa thuận · ${formatOptionalCurrency(contract.contractValue)}`;
const routeText = (trip) => [trip.startLocation, trip.endLocation].filter(Boolean).join(' -> ') || '-';

const statusStyles = {
  PAID: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PENDING: 'bg-amber-50 text-amber-700 border-amber-200',
  OVERDUE: 'bg-rose-50 text-rose-700 border-rose-200',
  CREATED: 'bg-sky-50 text-sky-700 border-sky-200',
  ASSIGNED: 'bg-violet-50 text-violet-700 border-violet-200',
  IN_PROGRESS: 'bg-orange-50 text-orange-700 border-orange-200',
  COMPLETED: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  CANCELLED: 'bg-slate-100 text-slate-600 border-slate-200',
  AVAILABLE: 'bg-emerald-50 text-emerald-700 border-emerald-200',
  PARTIALLY_APPLIED: 'bg-sky-50 text-sky-700 border-sky-200',
  APPLIED: 'bg-slate-100 text-slate-700 border-slate-200',
  PARTIALLY_REFUNDED: 'bg-amber-50 text-amber-700 border-amber-200',
  REFUNDED: 'bg-violet-50 text-violet-700 border-violet-200',
};

const statusLabels = {
  PAID: 'Đã thanh toán',
  PENDING: 'Chờ thanh toán',
  OVERDUE: 'Quá hạn',
  CREATED: 'Chờ thực hiện',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang vận chuyển',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
  AVAILABLE: 'Còn khả dụng',
  PARTIALLY_APPLIED: 'Đã dùng một phần',
  APPLIED: 'Đã dùng hết',
  PARTIALLY_REFUNDED: 'Đã hoàn một phần',
  REFUNDED: 'Đã hoàn hết',
};

const paymentMethodLabels = { CASH: 'Tiền mặt', BANK_TRANSFER: 'Chuyển khoản', CARD: 'Thẻ', OTHER: 'Khác' };

const StatusBadge = ({ value }) => (
  <span className={`inline-flex whitespace-nowrap rounded-full border px-2 py-1 text-xs font-medium ${statusStyles[value] || 'border-slate-200 bg-slate-50 text-slate-600'}`}>
    {statusLabels[value] || value || '-'}
  </span>
);

const renderColumn = (row, column, isDetail = false) => {
  if (isDetail && column.detailRender) return column.detailRender(row);
  if (column.render) return column.render(row);
  return row[column.key] ?? '-';
};

function SummaryCard({ icon: Icon, label, value, tone = 'text-slate-950' }) {
  return (
    <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-slate-100 text-slate-700">
          <Icon size={18} />
        </div>
        <div className="min-w-0">
          <p className="text-xs font-medium uppercase text-slate-500">{label}</p>
          <p className={`mt-1 break-words text-lg font-semibold ${tone}`}>{value}</p>
        </div>
      </div>
    </div>
  );
}

function EmptyRow({ columns, text }) {
  return (
    <tr>
      <td colSpan={columns} className="px-4 py-8 text-center text-sm text-slate-500">{text}</td>
    </tr>
  );
}

function PortalTable({ title, rows, columns, primaryColumns, loading, emptyText, onView, minWidth }) {
  const primaryColumnSet = new Set(primaryColumns);
  const visibleColumns = columns.filter((column) => primaryColumnSet.has(column.key));
  const columnCount = visibleColumns.length + 1;

  return (
    <div className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
      <div className="border-b border-slate-100 px-4 py-3">
        <h2 className="text-base font-semibold text-slate-950">{title}</h2>
      </div>
      <div className="overflow-x-auto">
        <table className="w-full divide-y divide-slate-200 text-sm" style={{ minWidth }}>
          <thead className="bg-slate-100">
            <tr>
              {visibleColumns.map((column) => (
                <th key={column.key} className="whitespace-nowrap px-4 py-3 text-left font-semibold text-slate-700">
                  {column.label}
                </th>
              ))}
              <th className="w-20 whitespace-nowrap px-4 py-3 text-right font-semibold text-slate-700">Chi tiết</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-100">
            {loading ? (
              <EmptyRow columns={columnCount} text={`Đang tải ${title.toLowerCase()}...`} />
            ) : rows.length > 0 ? (
              rows.map((row) => (
                <tr key={row.id} className="hover:bg-slate-50">
                  {visibleColumns.map((column) => (
                    <td key={column.key} className="max-w-80 whitespace-nowrap px-4 py-3 text-slate-700">
                      <div className="truncate">{renderColumn(row, column)}</div>
                    </td>
                  ))}
                  <td className="whitespace-nowrap px-4 py-3 text-right">
                    <button
                      type="button"
                      onClick={() => onView(row)}
                      title={`Xem chi tiết ${title.toLowerCase()}`}
                      aria-label={`Xem chi tiết ${title.toLowerCase()}`}
                      className="inline-flex h-9 w-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 text-emerald-700 transition hover:bg-emerald-100"
                    >
                      <Eye size={16} />
                    </button>
                  </td>
                </tr>
              ))
            ) : (
              <EmptyRow columns={columnCount} text={emptyText} />
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}

function PortalDetail({ detail, onClose }) {
  return (
    <Modal isOpen={Boolean(detail)} onClose={onClose} title={detail?.title || ''} size="wide" variant="detail">
      {detail && (
        <dl className="grid gap-x-8 gap-y-5 rounded-lg border border-emerald-200 bg-emerald-50/40 p-5 sm:grid-cols-2 lg:grid-cols-3">
          {detail.columns.map((column) => (
            <div key={column.key} className={`min-w-0 ${column.detailClassName || ''}`}>
              <dt className="text-xs font-medium uppercase text-emerald-700">{column.label}</dt>
              <dd className="mt-1 break-words text-sm font-medium text-slate-900">
                {renderColumn(detail.row, column, true)}
              </dd>
            </div>
          ))}
        </dl>
      )}
    </Modal>
  );
}

export default function CustomerPortal() {
  const [loading, setLoading] = useState(true);
  const [portal, setPortal] = useState({
    profile: null,
    contracts: [],
    trips: [],
    invoices: [],
    deposits: [],
  });
  const [detail, setDetail] = useState(null);

  const loadPortal = async () => {
    setLoading(true);
    try {
      const response = await api.get('/customer-portal/me');
      const data = getResult(response, {});
      setPortal({
        profile: data.profile || null,
        contracts: data.contracts || [],
        trips: data.trips || [],
        invoices: data.invoices || [],
        deposits: data.deposits || [],
      });
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải cổng khách hàng');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPortal();
  }, []);

  const summary = useMemo(() => {
    const unpaid = portal.invoices
      .filter((invoice) => invoice.status === 'PENDING' || invoice.status === 'OVERDUE')
      .reduce((total, invoice) => total + Number(invoice.amountDue ?? invoice.totalAmount ?? 0), 0);
    const availableDeposit = portal.deposits
      .reduce((total, deposit) => total + Number(deposit.availableAmount || 0), 0);

    return {
      contracts: portal.contracts.length,
      trips: portal.trips.length,
      invoices: portal.invoices.length,
      unpaid,
      availableDeposit,
    };
  }, [portal]);

  const profile = portal.profile || {};
  const contractColumns = [
    { key: 'contractCode', label: 'Mã hợp đồng', render: (contract) => contract.contractCode || shortId(contract.id) },
    { key: 'cargoType', label: 'Loại hàng', render: (contract) => cargoTypeLabel(contract.cargoType) },
    { key: 'cargoDescription', label: 'Ghi chú hàng hóa', render: (contract) => contract.cargoDescription || '-', detailClassName: 'sm:col-span-2 lg:col-span-3' },
    { key: 'freightRatePerTonKm', label: 'Đơn giá', render: (contract) => hasValue(contract.freightRatePerTonKm) ? `${formatCurrency(contract.freightRatePerTonKm)}/tấn/km` : '-' },
    { key: 'signedDate', label: 'Ngày ký', render: (contract) => formatDate(contract.signedDate) },
    { key: 'startDate', label: 'Ngày bắt đầu', render: (contract) => formatDate(contract.startDate) },
    { key: 'endDate', label: 'Ngày kết thúc', render: (contract) => formatDate(contract.endDate) },
    { key: 'estimatedDistanceKm', label: 'Quãng đường dự kiến', render: (contract) => formatKm(contract.estimatedDistanceKm) },
    { key: 'estimatedCargoWeightTon', label: 'Trọng lượng dự kiến', render: (contract) => formatTon(contract.estimatedCargoWeightTon) },
    { key: 'contractValue', label: 'Cách xác định giá trị', render: formatContractValue },
    { key: 'depositPolicy', label: 'Chính sách cọc', render: (contract) => contract.depositRequired ? `${contract.depositType === 'PERCENTAGE' ? `${contract.depositValue}%` : formatCurrency(contract.depositValue)} · ${contract.depositScope === 'TRIP' ? 'Theo chuyến' : 'Theo hợp đồng'} · ${contract.depositUsage === 'SECURITY_HOLD' ? 'Giữ bảo đảm' : 'Cấn hóa đơn'}` : 'Không yêu cầu' },
    { key: 'depositDueDays', label: 'Hạn nộp cọc', render: (contract) => contract.depositRequired ? `${Number(contract.depositDueDays || 0)} ngày sau khi ký` : '-' },
    { key: 'depositTerms', label: 'Điều khoản cọc', render: (contract) => contract.depositTerms || '-', detailClassName: 'sm:col-span-2 lg:col-span-3' },
  ];
  const invoiceColumns = [
    { key: 'invoiceNumber', label: 'Số hóa đơn', render: (invoice) => invoice.invoiceNumber || shortId(invoice.id) },
    { key: 'tripId', label: 'Mã chuyến', render: (invoice) => shortId(invoice.tripId), detailRender: (invoice) => invoice.tripId || '-' },
    { key: 'issueDate', label: 'Ngày lập', render: (invoice) => formatDate(invoice.issueDate) },
    { key: 'dueDate', label: 'Hạn thanh toán', render: (invoice) => formatDate(invoice.dueDate) },
    { key: 'totalAmount', label: 'Tổng hóa đơn', render: (invoice) => formatOptionalCurrency(invoice.totalAmount) },
    { key: 'depositAppliedAmount', label: 'Cọc đã cấn', render: (invoice) => formatCurrency(invoice.depositAppliedAmount) },
    { key: 'paidAmount', label: 'Đã thu', render: (invoice) => formatCurrency(invoice.paidAmount) },
    { key: 'amountDue', label: 'Còn phải thu', render: (invoice) => formatCurrency(invoice.amountDue) },
    { key: 'status', label: 'Trạng thái', render: (invoice) => <StatusBadge value={invoice.status} /> },
  ];
  const tripColumns = [
    { key: 'id', label: 'Mã chuyến', render: (trip) => shortId(trip.id), detailRender: (trip) => trip.id || '-' },
    { key: 'contractCode', label: 'Hợp đồng', render: (trip) => trip.contractCode || '-' },
    { key: 'driverName', label: 'Tài xế', render: (trip) => trip.driverName || shortId(trip.driverId) },
    { key: 'vehiclePlate', label: 'Phương tiện', render: (trip) => trip.vehiclePlate || shortId(trip.vehicleId) },
    { key: 'route', label: 'Tuyến đường', render: routeText, detailClassName: 'sm:col-span-2 lg:col-span-3' },
    { key: 'startLocation', label: 'Điểm đi', render: (trip) => trip.startLocation || '-', detailClassName: 'sm:col-span-2' },
    { key: 'endLocation', label: 'Điểm đến', render: (trip) => trip.endLocation || '-', detailClassName: 'sm:col-span-2' },
    { key: 'startTime', label: 'Thời gian bắt đầu', render: (trip) => formatDateTime(trip.startTime) },
    { key: 'endTime', label: 'Thời gian kết thúc', render: (trip) => formatDateTime(trip.endTime) },
    { key: 'distanceKm', label: 'Quãng đường', render: (trip) => formatKm(trip.distanceKm) },
    { key: 'cargoWeightTon', label: 'Trọng lượng', render: (trip) => formatTon(trip.cargoWeightTon) },
    { key: 'freightRatePerTonKm', label: 'Đơn giá', render: (trip) => hasValue(trip.freightRatePerTonKm) ? `${formatCurrency(trip.freightRatePerTonKm)}/tấn/km` : '-' },
    { key: 'freightAmount', label: 'Cước dự kiến', render: (trip) => formatOptionalCurrency(trip.freightAmount) },
    { key: 'depositRequired', label: 'Mức cọc yêu cầu', render: (trip) => trip.depositSummary?.required ? formatCurrency(trip.depositSummary.requiredAmount) : 'Không yêu cầu' },
    { key: 'depositAvailable', label: 'Cọc khả dụng', render: (trip) => formatCurrency(trip.depositSummary?.availableAmount) },
    { key: 'depositShortfall', label: 'Cọc còn thiếu', render: (trip) => formatCurrency(trip.depositSummary?.shortfallAmount) },
    { key: 'status', label: 'Trạng thái', render: (trip) => <StatusBadge value={trip.status} /> },
  ];
  const depositColumns = [
    { key: 'receiptNumber', label: 'Số phiếu', render: (deposit) => deposit.receiptNumber || shortId(deposit.id) },
    { key: 'target', label: 'Áp dụng cho', render: (deposit) => deposit.tripId ? `Chuyến ${shortId(deposit.tripId)}` : deposit.contractCode || 'Số dư chung' },
    { key: 'receivedDate', label: 'Ngày nhận', render: (deposit) => formatDate(deposit.receivedDate) },
    { key: 'amount', label: 'Đã nhận', render: (deposit) => formatCurrency(deposit.amount) },
    { key: 'allocatedAmount', label: 'Đã cấn hóa đơn', render: (deposit) => formatCurrency(deposit.allocatedAmount) },
    { key: 'refundedAmount', label: 'Đã hoàn', render: (deposit) => formatCurrency(deposit.refundedAmount) },
    { key: 'availableAmount', label: 'Còn khả dụng', render: (deposit) => formatCurrency(deposit.availableAmount) },
    { key: 'paymentMethod', label: 'Hình thức nhận', render: (deposit) => paymentMethodLabels[deposit.paymentMethod] || deposit.paymentMethod || '-' },
    { key: 'referenceNumber', label: 'Mã tham chiếu', render: (deposit) => deposit.referenceNumber || '-' },
    { key: 'note', label: 'Ghi chú', render: (deposit) => deposit.note || '-', detailClassName: 'sm:col-span-2 lg:col-span-3' },
    { key: 'status', label: 'Trạng thái', render: (deposit) => <StatusBadge value={deposit.status} /> },
  ];

  const openDetail = (type, row, columns) => {
    const identifiers = {
      contract: row.contractCode || shortId(row.id),
      invoice: row.invoiceNumber || shortId(row.id),
      trip: shortId(row.id),
      deposit: row.receiptNumber || shortId(row.id),
    };
    const labels = {
      contract: 'Chi tiết hợp đồng',
      invoice: 'Chi tiết hóa đơn',
      trip: 'Chi tiết chuyến đi',
      deposit: 'Chi tiết phiếu cọc',
    };

    setDetail({
      title: `${labels[type]} ${identifiers[type]}`,
      row,
      columns,
    });
  };

  return (
    <>
      <section className="space-y-5">
        <div className="flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <div>
            <h1 className="text-2xl font-semibold text-slate-950">Cổng khách hàng</h1>
            <p className="mt-1 text-sm text-slate-500">Hồ sơ, hợp đồng, chuyến đi, tiền cọc và hóa đơn của tài khoản hiện tại.</p>
          </div>
          <button
            type="button"
            onClick={loadPortal}
            className="inline-flex h-10 items-center justify-center gap-2 rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100"
          >
            <RefreshCw size={16} />
            Tải lại
          </button>
        </div>

        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
          <SummaryCard icon={FileText} label="Hợp đồng" value={loading ? '...' : summary.contracts} />
          <SummaryCard icon={Map} label="Chuyến đi" value={loading ? '...' : summary.trips} tone="text-sky-700" />
          <SummaryCard icon={CreditCard} label="Hóa đơn" value={loading ? '...' : summary.invoices} tone="text-violet-700" />
          <SummaryCard icon={CreditCard} label="Công nợ" value={loading ? '...' : formatCurrency(summary.unpaid)} tone="text-amber-700" />
          <SummaryCard icon={WalletCards} label="Cọc khả dụng" value={loading ? '...' : formatCurrency(summary.availableDeposit)} tone="text-emerald-700" />
        </div>

        <div className="rounded-lg border border-slate-200 bg-white p-4 shadow-sm">
          <div className="flex items-center gap-3 border-b border-slate-100 pb-3">
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emerald-50 text-emerald-700">
              <UserRound size={18} />
            </div>
            <div className="min-w-0">
              <h2 className="truncate text-base font-semibold text-slate-950">{profile.name || 'Khách hàng'}</h2>
              <p className="truncate text-sm text-slate-500">{profile.username || shortId(profile.id)}</p>
            </div>
          </div>
          <dl className="mt-4 grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <dt className="text-slate-500">Điện thoại</dt>
              <dd className="mt-1 break-words font-medium text-slate-900">{profile.phone || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-500">CCCD/CMND</dt>
              <dd className="mt-1 break-words font-medium text-slate-900">{profile.idNumber || '-'}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Ngày sinh</dt>
              <dd className="mt-1 font-medium text-slate-900">{formatDate(profile.dob)}</dd>
            </div>
            <div>
              <dt className="text-slate-500">Địa chỉ</dt>
              <dd className="mt-1 break-words font-medium text-slate-900">{profile.address || '-'}</dd>
            </div>
          </dl>
        </div>

        <div className="grid gap-5 xl:grid-cols-2">
          <PortalTable
            title="Hợp đồng"
            rows={portal.contracts}
            columns={contractColumns}
            primaryColumns={['contractCode', 'cargoType', 'contractValue', 'depositPolicy']}
            loading={loading}
            emptyText="Chưa có hợp đồng."
            minWidth={760}
            onView={(contract) => openDetail('contract', contract, contractColumns)}
          />
          <PortalTable
            title="Hóa đơn"
            rows={portal.invoices}
            columns={invoiceColumns}
            primaryColumns={['invoiceNumber', 'totalAmount', 'depositAppliedAmount', 'amountDue', 'status']}
            loading={loading}
            emptyText="Chưa có hóa đơn."
            minWidth={700}
            onView={(invoice) => openDetail('invoice', invoice, invoiceColumns)}
          />
        </div>

        <PortalTable
          title="Chuyến đi"
          rows={portal.trips}
          columns={tripColumns}
          primaryColumns={['id', 'route', 'freightAmount', 'status']}
          loading={loading}
          emptyText="Chưa có chuyến đi."
          minWidth={820}
          onView={(trip) => openDetail('trip', trip, tripColumns)}
        />

        <PortalTable
          title="Tiền cọc"
          rows={portal.deposits}
          columns={depositColumns}
          primaryColumns={['receiptNumber', 'target', 'amount', 'availableAmount', 'status']}
          loading={loading}
          emptyText="Chưa có phiếu cọc."
          minWidth={820}
          onView={(deposit) => openDetail('deposit', deposit, depositColumns)}
        />
      </section>

      <PortalDetail detail={detail} onClose={() => setDetail(null)} />
    </>
  );
}
