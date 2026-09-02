import { useContext, useEffect, useState } from 'react';
import api from '../services/api';
import toast from 'react-hot-toast';
import { AlertTriangle, CalendarClock, CheckCircle2, ChevronRight, CreditCard, FileText, Map, TrendingUp, Truck, Users, WalletCards, Wrench } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { AuthContext } from '../context/auth-context';
import { toDateInputValue } from '../utils/dates';

const getResult = (response, fallback) =>
  response.status === 'fulfilled' ? response.value.data.result ?? response.value.data ?? fallback : fallback;

const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;

const isActiveInvoice = (invoice) => invoice.status !== 'CANCELLED';

const isOutstandingInvoice = (invoice) => invoice.status === 'PENDING' || invoice.status === 'OVERDUE';

const isOverdueInvoice = (invoice) => {
  return isActiveInvoice(invoice) && invoice.status === 'OVERDUE';
};

const sumInvoices = (invoices, predicate, amount = (invoice) => invoice.totalAmount) =>
  invoices
    .filter(predicate)
    .reduce((total, invoice) => total + Number(amount(invoice) || 0), 0);

const invoicePaidAmount = (invoice) => invoice.status === 'PAID'
  ? invoice.totalAmount
  : invoice.paidAmount;

const invoiceAmountDue = (invoice) => invoice.amountDue ?? (invoice.status === 'PAID' ? 0 : invoice.totalAmount);

const dateAfterDays = (days) => {
  const value = new Date();
  value.setDate(value.getDate() + days);
  return toDateInputValue(value);
};

const isLateTrip = (trip) => {
  if (!['CREATED', 'ASSIGNED', 'IN_PROGRESS'].includes(trip.status) || !trip.endTime) return false;
  const endTime = Date.parse(trip.endTime);
  return Number.isFinite(endTime) && endTime < Date.now();
};

const buildOperationalAlerts = ({ invoices, contracts, maintenances, trips, isCustomer }) => {
  const overdueInvoices = invoices.filter(isOverdueInvoice);
  const expiringContracts = contracts.filter((contract) => (
    contract.status === 'ACTIVE'
      && contract.endDate
      && String(contract.endDate).slice(0, 10) <= dateAfterDays(30)
  ));
  const dueMaintenances = maintenances.filter((maintenance) => (
    maintenance.status === 'PENDING'
      && maintenance.maintenanceDate
      && String(maintenance.maintenanceDate).slice(0, 10) <= dateAfterDays(7)
  ));
  const lateTrips = trips.filter(isLateTrip);
  const portalPath = isCustomer ? '/my-portal' : null;

  return [
    {
      key: 'overdue-invoices',
      count: overdueInvoices.length,
      title: 'Hóa đơn quá hạn',
      description: `${formatCurrency(sumInvoices(overdueInvoices, () => true, invoiceAmountDue))} cần thu`,
      path: portalPath || '/invoices',
      icon: AlertTriangle,
      tone: 'border-rose-200 bg-rose-50 text-rose-700',
    },
    {
      key: 'expiring-contracts',
      count: expiringContracts.length,
      title: 'Hợp đồng sắp hoặc đã hết hạn',
      description: 'Trong vòng 30 ngày tới',
      path: portalPath || '/contracts',
      icon: CalendarClock,
      tone: 'border-amber-200 bg-amber-50 text-amber-700',
    },
    {
      key: 'due-maintenance',
      count: dueMaintenances.length,
      title: 'Lịch bảo dưỡng đến hạn',
      description: 'Hôm nay và 7 ngày tới',
      path: '/maintenance',
      icon: Wrench,
      tone: 'border-sky-200 bg-sky-50 text-sky-700',
      hidden: isCustomer,
    },
    {
      key: 'late-trips',
      count: lateTrips.length,
      title: 'Chuyến đi chậm tiến độ',
      description: 'Đã qua thời gian kết thúc dự kiến',
      path: portalPath || '/trips',
      icon: Map,
      tone: 'border-violet-200 bg-violet-50 text-violet-700',
    },
  ].filter((alert) => !alert.hidden && alert.count > 0);
};

export default function Dashboard() {
  const navigate = useNavigate();
  const { user } = useContext(AuthContext);
  const isCustomer = user?.role === 'CUSTOMER';
  const [stats, setStats] = useState({
    vehicles: 0,
    drivers: 0,
    trips: 0,
    contracts: 0,
    invoices: 0,
    revenue: 0,
    paidRevenue: 0,
    availableVehicles: 0,
    overdueInvoices: 0,
    completedTrips: 0,
    unpaidAmount: 0,
    totalExpense: 0,
    netProfit: 0,
    depositApplied: 0,
    availableDeposit: 0,
  });
  const [loading, setLoading] = useState(true);
  const [alerts, setAlerts] = useState([]);

  useEffect(() => {
    const loadStats = async () => {
      setLoading(true);
      try {
        if (isCustomer) {
          const response = await api.get('/customer-portal/me');
          const portal = response.data?.result || response.data || {};
          const trips = portal.trips || [];
          const invoices = portal.invoices || [];
          const activeInvoices = invoices.filter(isActiveInvoice);
          setAlerts(buildOperationalAlerts({
            invoices: activeInvoices,
            contracts: portal.contracts || [],
            maintenances: [],
            trips,
            isCustomer: true,
          }));

          setStats({
            vehicles: 0,
            drivers: 0,
            trips: trips.length,
            contracts: (portal.contracts || []).length,
            invoices: activeInvoices.length,
            revenue: sumInvoices(activeInvoices, () => true),
            paidRevenue: sumInvoices(activeInvoices, () => true, invoicePaidAmount),
            availableVehicles: 0,
            overdueInvoices: activeInvoices.filter(isOverdueInvoice).length,
            completedTrips: trips.filter((trip) => trip.status === 'COMPLETED').length,
            unpaidAmount: sumInvoices(activeInvoices, isOutstandingInvoice, invoiceAmountDue),
            totalExpense: 0,
            netProfit: 0,
            depositApplied: activeInvoices.reduce((total, invoice) => total + Number(invoice.depositAppliedAmount || 0), 0),
            availableDeposit: (portal.deposits || []).reduce((total, deposit) => total + Number(deposit.availableAmount || 0), 0),
          });
          return;
        }

        const [vehicles, drivers, trips, contracts, invoices, availableVehicles, financialReport, maintenances] = await Promise.allSettled([
          api.get('/vehicles'),
          api.get('/drivers'),
          api.get('/trips'),
          api.get('/contracts'),
          api.get('/invoices'),
          api.get('/vehicles/available/count'),
          api.get('/reports/financial'),
          api.get('/maintenances'),
        ]);

        const vehicleRows = getResult(vehicles, []);
        const driverRows = getResult(drivers, []);
        const tripRows = getResult(trips, []);
        const contractRows = getResult(contracts, []);
        const activeInvoices = getResult(invoices, []).filter(isActiveInvoice);
        const maintenanceRows = getResult(maintenances, []);
        const reportTotals = getResult(financialReport, {})?.totals || {};
        const invoiceRevenue = sumInvoices(activeInvoices, () => true);
        const paidRevenue = sumInvoices(activeInvoices, () => true, invoicePaidAmount);
        const totalExpense = Number(reportTotals.totalExpense);
        const netProfit = Number(reportTotals.netProfit);

        setAlerts(buildOperationalAlerts({
          invoices: activeInvoices,
          contracts: contractRows,
          maintenances: maintenanceRows,
          trips: tripRows,
          isCustomer: false,
        }));

        setStats({
          vehicles: vehicleRows.length || 0,
          drivers: driverRows.length || 0,
          trips: tripRows.length || 0,
          contracts: contractRows.length || 0,
          invoices: activeInvoices.length || 0,
          revenue: Number.isFinite(Number(reportTotals.recognizedRevenue)) ? Number(reportTotals.recognizedRevenue) : invoiceRevenue,
          paidRevenue: Number.isFinite(Number(reportTotals.paidRevenue)) ? Number(reportTotals.paidRevenue) : paidRevenue,
          availableVehicles: Number(getResult(availableVehicles, 0)) || 0,
          overdueInvoices: activeInvoices.filter(isOverdueInvoice).length,
          completedTrips: tripRows.filter((trip) => trip.status === 'COMPLETED').length,
          unpaidAmount: sumInvoices(activeInvoices, isOutstandingInvoice, invoiceAmountDue),
          totalExpense: Number.isFinite(totalExpense) ? totalExpense : 0,
          netProfit: Number.isFinite(netProfit) ? netProfit : invoiceRevenue,
          depositApplied: Number(reportTotals.depositApplied || 0),
          availableDeposit: Number(reportTotals.depositAvailable || 0),
        });
      } catch {
        toast.error('Không thể tải dữ liệu dashboard');
        setAlerts([]);
      } finally {
        setLoading(false);
      }
    };

    loadStats();
  }, [isCustomer]);

  const cards = isCustomer
    ? [
        { title: 'Hợp đồng', value: stats.contracts, icon: FileText, tone: 'text-emerald-700 bg-emerald-50' },
        { title: 'Chuyến đi', value: stats.trips, icon: Map, tone: 'text-sky-700 bg-sky-50' },
        { title: 'Hóa đơn', value: stats.invoices, icon: CreditCard, tone: 'text-violet-700 bg-violet-50' },
        { title: 'Công nợ', value: formatCurrency(stats.unpaidAmount), icon: AlertTriangle, tone: 'text-amber-700 bg-amber-50' },
        { title: 'Tiền cọc khả dụng', value: formatCurrency(stats.availableDeposit), icon: WalletCards, tone: 'text-emerald-700 bg-emerald-50' },
      ]
    : [
        { title: 'Tổng xe', value: stats.vehicles, icon: Truck, tone: 'text-emerald-700 bg-emerald-50' },
        { title: 'Tài xế', value: stats.drivers, icon: Users, tone: 'text-sky-700 bg-sky-50' },
        { title: 'Chuyến đi', value: stats.trips, icon: Map, tone: 'text-violet-700 bg-violet-50' },
        { title: 'Hóa đơn', value: stats.invoices, icon: FileText, tone: 'text-slate-700 bg-slate-100' },
        { title: 'Doanh thu ghi nhận', value: formatCurrency(stats.revenue), icon: CreditCard, tone: 'text-amber-700 bg-amber-50' },
        { title: 'Đã thu', value: formatCurrency(stats.paidRevenue), icon: CreditCard, tone: 'text-emerald-700 bg-emerald-50' },
        { title: 'Số tiền đã cọc', value: formatCurrency(stats.depositApplied), icon: WalletCards, tone: 'text-sky-700 bg-sky-50' },
        { title: 'Tiền cọc đang giữ', value: formatCurrency(stats.availableDeposit), icon: WalletCards, tone: 'text-violet-700 bg-violet-50' },
        { title: 'Công nợ', value: formatCurrency(stats.unpaidAmount), icon: AlertTriangle, tone: 'text-rose-700 bg-rose-50' },
        { title: 'Tổng chi phí', value: formatCurrency(stats.totalExpense), icon: WalletCards, tone: 'text-violet-700 bg-violet-50' },
        { title: 'Lợi nhuận ròng', value: formatCurrency(stats.netProfit), icon: TrendingUp, tone: stats.netProfit >= 0 ? 'text-emerald-700 bg-emerald-50' : 'text-rose-700 bg-rose-50' },
      ];

  return (
    <section className="space-y-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-950">{isCustomer ? 'Tổng quan khách hàng' : 'Tổng quan vận hành'}</h1>
        <p className="mt-1 text-sm text-slate-500">
          {isCustomer
            ? 'Theo dõi hợp đồng, chuyến đi và hóa đơn của tài khoản hiện tại.'
            : 'Theo dõi đội xe, chuyến đi, doanh thu, chi phí, công nợ và lợi nhuận ròng.'}
        </p>
      </div>

      <div className={`grid grid-cols-1 gap-6 md:grid-cols-2 ${isCustomer ? 'lg:grid-cols-5' : 'lg:grid-cols-3'}`}>
        {cards.map((card, i) => (
          <div key={i} className="bg-white rounded-lg border border-slate-200 p-6 shadow-sm">
            <div className="flex items-start justify-between gap-4">
              <div className="min-w-0 flex-1">
                <p className="text-slate-500 text-sm">{card.title}</p>
                <p className="mt-3 break-words text-2xl font-semibold text-slate-950">
                  {loading ? '...' : card.value}
                </p>
              </div>
              <div className={`shrink-0 rounded-lg p-3 ${card.tone}`}>
                <card.icon className="h-6 w-6" />
              </div>
            </div>
          </div>
        ))}
      </div>

      <section className="overflow-hidden rounded-lg border border-slate-200 bg-white shadow-sm">
        <div className="flex flex-col gap-3 border-b border-slate-200 px-5 py-4 sm:flex-row sm:items-center sm:justify-between">
          <div>
            <h2 className="text-base font-semibold text-slate-950">Việc cần xử lý</h2>
            <p className="mt-1 text-sm text-slate-500">Công nợ, hợp đồng, lịch bảo dưỡng và tiến độ chuyến đi.</p>
          </div>
          <div className="flex overflow-hidden rounded-lg border border-slate-200 bg-slate-50">
            <div className="px-4 py-2">
              <p className="text-xs text-slate-500">{isCustomer ? 'Chuyến hoàn tất' : 'Xe sẵn sàng'}</p>
              <p className="mt-0.5 font-semibold text-emerald-700">{loading ? '...' : isCustomer ? stats.completedTrips : stats.availableVehicles}</p>
            </div>
            <div className="border-l border-slate-200 px-4 py-2">
              <p className="text-xs text-slate-500">Đang cảnh báo</p>
              <p className="mt-0.5 font-semibold text-amber-700">{loading ? '...' : alerts.reduce((total, alert) => total + alert.count, 0)}</p>
            </div>
          </div>
        </div>

        {loading ? (
          <div className="px-5 py-10 text-center text-sm text-slate-500">Đang tổng hợp cảnh báo...</div>
        ) : alerts.length > 0 ? (
          <div className="divide-y divide-slate-100">
            {alerts.map((alert) => (
              <button
                key={alert.key}
                type="button"
                onClick={() => navigate(alert.path)}
                className="flex w-full items-center gap-4 px-5 py-4 text-left hover:bg-slate-50"
              >
                <span className={`inline-flex h-10 w-10 shrink-0 items-center justify-center rounded-lg border ${alert.tone}`}>
                  <alert.icon size={19} />
                </span>
                <span className="min-w-0 flex-1">
                  <span className="block font-medium text-slate-900">{alert.title}</span>
                  <span className="mt-0.5 block text-sm text-slate-500">{alert.description}</span>
                </span>
                <span className="inline-flex min-w-9 items-center justify-center rounded-full bg-slate-900 px-2.5 py-1 text-sm font-semibold text-white">{alert.count}</span>
                <ChevronRight className="shrink-0 text-slate-400" size={18} />
              </button>
            ))}
          </div>
        ) : (
          <div className="px-5 py-10 text-center">
            <CheckCircle2 className="mx-auto text-emerald-600" size={26} />
            <p className="mt-2 font-medium text-slate-900">Không có việc quá hạn hoặc sắp đến hạn</p>
          </div>
        )}
      </section>
    </section>
  );
}
