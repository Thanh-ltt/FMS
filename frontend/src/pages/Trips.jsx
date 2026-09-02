import { useCallback, useContext, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Calculator, CheckCircle2, Download, FileCheck, MapPin, Play, ReceiptText, XCircle } from 'lucide-react';
import { AuthContext } from '../context/auth-context';
import DataTablePage from '../components/DataTablePage';
import DateField from '../components/DateField';
import Modal from '../components/Modal';
import TripExpensesModal from '../components/TripExpensesModal';
import TripReadinessModal from '../components/TripReadinessModal';
import MapTracker from '../components/MapTracker';
import EpodModal from '../components/EpodModal';
import api from '../services/api';
import { cargoTypeLabel } from '../utils/cargoTypes';
import { formatDate, formatDateTime, toDateInputValue } from '../utils/dates';
import { vehicleTypeWithCapacity } from '../utils/vehicleTypes';

const getResult = (response, fallback) => response.data?.result || response.data || fallback;
const shortId = (value) => value ? String(value).slice(0, 8) : '-';
const formatKm = (value) => value ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 1 })} km` : '-';
const formatTon = (value) => value ? `${Number(value).toLocaleString('vi-VN', { maximumFractionDigits: 2 })} tấn` : '-';
const formatCurrency = (value) => `${Number(value || 0).toLocaleString('vi-VN')} đ`;
const depositSummaryLabel = (trip) => {
  const summary = trip.depositSummary;
  if (!summary?.required && Number(summary?.receivedAmount || 0) <= 0) return 'Không yêu cầu';
  if (Number(summary?.shortfallAmount || 0) > 0) return `Thiếu ${formatCurrency(summary.shortfallAmount)}`;
  return `Khả dụng ${formatCurrency(summary?.availableAmount)}`;
};
const expenseSummaryCell = (trip) => {
  const summary = trip.expenseSummary;
  if (!summary || Number(summary.totalCount || 0) === 0) return 'Chưa có';
  return (
    <div className="min-w-0">
      <p className="whitespace-nowrap font-medium text-emerald-700">{formatCurrency(summary.approvedAmount)}</p>
      {Number(summary.pendingCount || 0) > 0 && (
        <p className="mt-0.5 whitespace-nowrap text-xs text-amber-700">
          {summary.pendingCount} khoản chờ duyệt
        </p>
      )}
    </div>
  );
};
const contractCargoLabel = (contract) => {
  const type = contract?.cargoType ? cargoTypeLabel(contract.cargoType) : '';
  return [type, contract?.cargoDescription].filter(Boolean).join(' - ') || 'Chưa có hàng hóa';
};

const customerCode = (customer) => customer?.username || shortId(customer?.id);
const tripCustomerCode = (trip) => trip.customerUsername || shortId(trip.customerId);
const tripStatusLabels = {
  CREATED: 'Mới tạo',
  ASSIGNED: 'Đã phân công',
  IN_PROGRESS: 'Đang vận chuyển',
  COMPLETED: 'Hoàn tất',
  CANCELLED: 'Đã hủy',
};
const reservedTripStatuses = new Set(['CREATED', 'ASSIGNED', 'IN_PROGRESS']);
const schedulableVehicleStatuses = new Set(['AVAILABLE']);

const parseScheduleTime = (value) => {
  if (!value) return null;
  const time = Date.parse(value);
  return Number.isNaN(time) ? null : time;
};

const getScheduleWindow = (startTime, endTime) => {
  const start = parseScheduleTime(startTime);
  const end = parseScheduleTime(endTime);

  if (start === null || end === null || start >= end) return null;

  return { start, end };
};

const hasTripScheduleConflict = (trip, scheduleWindow, resourceKey, resourceId) => {
  if (!resourceId || !reservedTripStatuses.has(trip.status) || trip[resourceKey] !== resourceId) {
    return false;
  }

  if (!scheduleWindow) return true;

  const existingWindow = getScheduleWindow(trip.startTime, trip.endTime);
  if (!existingWindow) return true;

  return scheduleWindow.start < existingWindow.end && existingWindow.start < scheduleWindow.end;
};

const toPositiveNumber = (value) => {
  const number = Number(value);
  return Number.isFinite(number) && number > 0 ? number : null;
};

const calculateFreightAmount = (distanceKm, cargoWeightTon, freightRatePerTonKm) => {
  const distance = toPositiveNumber(distanceKm);
  const weight = toPositiveNumber(cargoWeightTon);
  const rate = toPositiveNumber(freightRatePerTonKm);

  if (!distance || !weight || !rate) return null;

  return distance * weight * rate;
};

const normalize = (value) =>
  String(value || '')
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'D')
    .toLowerCase();

const knownLocations = [
  { id: 'hanoi', label: 'Hà Nội', names: ['ha noi', 'hanoi'], lat: 21.0278, lon: 105.8342 },
  { id: 'hochiminh', label: 'TP.HCM', names: ['ho chi minh', 'tp hcm', 'tphcm', 'sai gon', 'saigon'], lat: 10.8231, lon: 106.6297 },
  { id: 'danang', label: 'Đà Nẵng', names: ['da nang', 'danang'], lat: 16.0471, lon: 108.2068 },
  { id: 'haiphong', label: 'Hải Phòng', names: ['hai phong', 'haiphong'], lat: 20.8449, lon: 106.6881 },
  { id: 'cantho', label: 'Cần Thơ', names: ['can tho', 'cantho'], lat: 10.0452, lon: 105.7469 },
  { id: 'nhatrang', label: 'Nha Trang', names: ['nha trang', 'khanh hoa'], lat: 12.2388, lon: 109.1967 },
  { id: 'dalat', label: 'Đà Lạt', names: ['da lat', 'dalat', 'lam dong'], lat: 11.9404, lon: 108.4583 },
  { id: 'hue', label: 'Huế', names: ['hue', 'thua thien hue'], lat: 16.4637, lon: 107.5909 },
  { id: 'quynhon', label: 'Quy Nhơn', names: ['quy nhon', 'binh dinh'], lat: 13.782, lon: 109.219 },
  { id: 'vungtau', label: 'Vũng Tàu', names: ['vung tau', 'ba ria vung tau'], lat: 10.4114, lon: 107.1362 },
  { id: 'bienhoa', label: 'Biên Hòa', names: ['bien hoa', 'dong nai'], lat: 10.9574, lon: 106.8427 },
  { id: 'thudaumot', label: 'Thủ Dầu Một', names: ['thu dau mot', 'binh duong'], lat: 10.9804, lon: 106.6519 },
  { id: 'longan', label: 'Long An', names: ['long an', 'tan an'], lat: 10.533, lon: 106.405 },
  { id: 'mytho', label: 'Mỹ Tho', names: ['my tho', 'tien giang'], lat: 10.36, lon: 106.36 },
  { id: 'rachgia', label: 'Rạch Giá', names: ['rach gia', 'kien giang'], lat: 10.0125, lon: 105.0809 },
];

const routeEdges = [
  ['hanoi', 'haiphong', 120],
  ['hanoi', 'hue', 670],
  ['hue', 'danang', 100],
  ['danang', 'quynhon', 320],
  ['quynhon', 'nhatrang', 220],
  ['nhatrang', 'dalat', 135],
  ['nhatrang', 'bienhoa', 380],
  ['dalat', 'bienhoa', 270],
  ['bienhoa', 'hochiminh', 35],
  ['hochiminh', 'thudaumot', 30],
  ['bienhoa', 'thudaumot', 35],
  ['bienhoa', 'vungtau', 95],
  ['hochiminh', 'vungtau', 100],
  ['hochiminh', 'longan', 45],
  ['longan', 'mytho', 40],
  ['mytho', 'cantho', 105],
  ['hochiminh', 'cantho', 170],
  ['cantho', 'rachgia', 115],
];

const findKnownLocation = (query) => {
  const normalized = normalize(query);
  return knownLocations.find((location) =>
    location.names.some((name) => normalized.includes(name))
  );
};

const locationById = knownLocations.reduce((map, location) => {
  map[location.id] = location;
  return map;
}, {});

const buildRouteGraph = () => routeEdges.reduce((graph, [from, to, distanceKm]) => {
  graph[from] = [...(graph[from] || []), { to, distanceKm }];
  graph[to] = [...(graph[to] || []), { to: from, distanceKm }];
  return graph;
}, {});

const findShortestKnownRoute = (start, end) => {
  if (!start || !end) return null;
  if (start.id === end.id) return { distanceKm: 0, path: [start.label] };

  const graph = buildRouteGraph();
  const distances = { [start.id]: 0 };
  const previous = {};
  const visited = new Set();
  const queue = [{ id: start.id, distanceKm: 0 }];

  while (queue.length > 0) {
    queue.sort((a, b) => a.distanceKm - b.distanceKm);
    const current = queue.shift();
    if (!current || visited.has(current.id)) continue;

    if (current.id === end.id) break;
    visited.add(current.id);

    (graph[current.id] || []).forEach((edge) => {
      if (visited.has(edge.to)) return;

      const nextDistance = current.distanceKm + edge.distanceKm;
      if (distances[edge.to] === undefined || nextDistance < distances[edge.to]) {
        distances[edge.to] = nextDistance;
        previous[edge.to] = current.id;
        queue.push({ id: edge.to, distanceKm: nextDistance });
      }
    });
  }

  if (distances[end.id] === undefined) return null;

  const path = [];
  let cursor = end.id;
  while (cursor) {
    path.unshift(locationById[cursor]?.label || cursor);
    cursor = previous[cursor];
  }

  return { distanceKm: distances[end.id], path };
};

const isSpecificAddress = (value) => {
  if (extractHouseNumberParts(value)) return true;

  const firstPart = firstAddressPart(value);
  if (!firstPart) return false;

  return firstPart.includes(' ') && !findKnownLocation(firstPart);
};

const canUseKnownLocationDistance = (startAddress, endAddress, knownStart, knownEnd) => {
  if (!knownStart || !knownEnd) return false;
  if (knownStart.id !== knownEnd.id) return true;

  return !isSpecificAddress(startAddress) && !isSpecificAddress(endAddress);
};

const toRadians = (value) => (value * Math.PI) / 180;

const estimateStraightLineKm = (start, end) => {
  const earthRadiusKm = 6371;
  const dLat = toRadians(end.lat - start.lat);
  const dLon = toRadians(end.lon - start.lon);
  const lat1 = toRadians(start.lat);
  const lat2 = toRadians(end.lat);
  const a = Math.sin(dLat / 2) ** 2
    + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) ** 2;
  const straightKm = earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  return straightKm * 1.25;
};

const removeHouseNumber = (value) =>
  String(value || '')
    .replace(/^\s*\d+[A-Za-z]?(?:[/-]\d+[A-Za-z]?)*\s*/u, '')
    .trim();

const firstAddressPart = (value) => String(value || '').split(/[,;]/)[0].trim();

const normalizedAddress = (value) => normalize(value).replace(/\s+/g, ' ').trim();

const normalizedStreetName = (value) => parseAddressParts(value).streetNormalized;

const streetDisplayName = (value) => parseAddressParts(value).streetDisplay || firstAddressPart(value);

const extractHouseNumberParts = (value) => {
  const match = firstAddressPart(value).match(/^\s*(\d+)[A-Za-z]?((?:[/-]\d+[A-Za-z]?)*)(?=\s|,|$)/u);
  if (!match) return null;

  return {
    main: Number(match[1]),
    branches: (match[2].match(/\d+/g) || []).map(Number),
  };
};

const houseNumberText = (value) => {
  const parts = extractHouseNumberParts(value);
  if (!parts) return '';

  return [parts.main, ...parts.branches].join('/');
};

const normalizeHouseNumber = (value) => String(value || '').replace(/[^0-9]/g, '');

const geocodeHasHouseNumber = (address, geocodeResult) => {
  const expectedHouseNumber = normalizeHouseNumber(houseNumberText(address));
  if (!expectedHouseNumber) return true;

  const resultHouseNumber = normalizeHouseNumber(geocodeResult?.houseNumber);
  if (resultHouseNumber && resultHouseNumber === expectedHouseNumber) return true;

  const labelHouseNumber = normalizeHouseNumber(houseNumberText(geocodeResult?.label));
  return labelHouseNumber === expectedHouseNumber;
};

const calculateBranchGap = (startBranches, endBranches) => {
  const maxLength = Math.max(startBranches.length, endBranches.length);
  let totalGap = 0;

  for (let index = 0; index < maxLength; index += 1) {
    const startValue = startBranches[index] || 0;
    const endValue = endBranches[index] || 0;
    const weight = index === 0 ? 0.006 : 0.003;
    totalGap += Math.abs(startValue - endValue) * weight;
  }

  return totalGap;
};

const estimateSameStreetDistanceKm = (startAddress, endAddress) => {
  if (normalizedAddress(startAddress) === normalizedAddress(endAddress)) return 0;

  const startStreet = normalizedStreetName(startAddress);
  const endStreet = normalizedStreetName(endAddress);
  if (!startStreet || startStreet !== endStreet) return null;

  const startNumber = extractHouseNumberParts(startAddress);
  const endNumber = extractHouseNumberParts(endAddress);

  if (!startNumber || !endNumber) return 0.3;

  const mainGapKm = Math.abs(startNumber.main - endNumber.main) * 0.01;
  const branchGapKm = calculateBranchGap(startNumber.branches, endNumber.branches);
  const oppositeSideAdjustmentKm = startNumber.main % 2 !== endNumber.main % 2 ? 0.05 : 0;
  const estimatedKm = mainGapKm + branchGapKm + oppositeSideAdjustmentKm;

  return Math.min(Math.max(estimatedKm, 0.1), 5);
};

const shouldUseSameStreetEstimate = (route, start, end, startAddress, endAddress) => {
  const sameStreetDistanceKm = estimateSameStreetDistanceKm(startAddress, endAddress);
  if (sameStreetDistanceKm === null) return null;
  if (sameStreetDistanceKm === 0) return 0;

  const coordinatesTooClose = start && end ? estimateStraightLineKm(start, end) < 0.05 : false;
  const routeTooSmall = !route || !Number.isFinite(route.distanceKm) || route.distanceKm < 0.1;
  const houseNumberIgnored = !geocodeHasHouseNumber(startAddress, start) || !geocodeHasHouseNumber(endAddress, end);

  return routeTooSmall || coordinatesTooClose || houseNumberIgnored ? sameStreetDistanceKm : null;
};

const uniqueValues = (values) => [...new Set(values.filter(Boolean).map((value) => value.trim()).filter(Boolean))];

const normalizeStreet = (value) =>
  normalize(value)
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

const normalizeAddressComponent = (value) =>
  normalize(value)
    .replace(/\b(phuong|p\.?|xa|x\.?|thi tran|quan|q\.?|huyen|thanh pho|tp\.?|tinh)\b\s*/g, '')
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();

const stripAdministrativeTail = (value) => {
  const normalizedValue = normalize(value);
  const match = normalizedValue.match(/\b(phuong|p\.?|xa|x\.?|thi tran|quan|q\.?|huyen|thanh pho|tp\.?|tinh)\b/);
  if (!match || match.index <= 0) return String(value || '').trim();

  return String(value || '').slice(0, match.index).trim();
};

const addInlineAdministrativeComponent = (components, normalizedAddress, pattern) => {
  for (const match of normalizedAddress.matchAll(pattern)) {
    const component = String(match[0] || '').trim();
    if (component) components.push(component);
  }
};

const extractInlineAdministrativeComponents = (address) => {
  const normalized = normalize(address);
  const components = [];

  addInlineAdministrativeComponent(components, normalized, /(phuong|p\.?)\s+(.+?)(?=\s+(quan|q\.?|huyen|thanh pho|tp\.?|tinh)\s+|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(xa|x\.?)\s+(.+?)(?=\s+(huyen|quan|thanh pho|tp\.?|tinh)\s+|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(thi tran)\s+(.+?)(?=\s+(huyen|quan|thanh pho|tp\.?|tinh)\s+|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(quan|q\.?)\s+(.+?)(?=\s+(thanh pho|tp\.?|tinh)\s+|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(huyen)\s+(.+?)(?=\s+(thanh pho|tp\.?|tinh)\s+|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(thanh pho|tp\.?)\s+(.+?)(?=\s+(viet nam)$|$)/g);
  addInlineAdministrativeComponent(components, normalized, /(tinh)\s+(.+?)(?=\s+(viet nam)$|$)/g);

  return components;
};

const parseAddressParts = (address) => {
  const original = String(address || '').trim();
  const segments = original.split(/[,;]+/).map((segment) => segment.trim()).filter(Boolean);
  const firstSegment = segments[0] || original;
  const house = houseNumberText(firstSegment);
  const streetDisplay = stripAdministrativeTail(removeHouseNumber(firstSegment));
  const areaComponents = segments.length > 1
    ? segments.slice(1)
    : extractInlineAdministrativeComponents(original);

  return {
    original,
    houseNumberText: house,
    streetDisplay,
    streetNormalized: normalizeStreet(streetDisplay),
    areaComponents: [...new Set(areaComponents.map((component) => component.trim()).filter(Boolean))],
  };
};

const joinAddressParts = (parts) => parts.map((part) => String(part || '').trim()).filter(Boolean).join(', ');

const buildGeocodeQueries = (address) => {
  const trimmed = String(address || '').trim();
  const parts = parseAddressParts(trimmed);
  const houseStreet = joinAddressParts([parts.houseNumberText, parts.streetDisplay]);
  const areaSuffix = joinAddressParts(parts.areaComponents);

  return uniqueValues([
    trimmed,
    !normalize(trimmed).includes('viet nam') && `${trimmed}, Việt Nam`,
    houseStreet && areaSuffix && `${houseStreet}, ${areaSuffix}`,
    houseStreet && areaSuffix && `${houseStreet}, ${areaSuffix}, Việt Nam`,
    parts.streetDisplay && areaSuffix && `${parts.streetDisplay}, ${areaSuffix}`,
    parts.streetDisplay && areaSuffix && `${parts.streetDisplay}, ${areaSuffix}, Việt Nam`,
    houseStreet,
    houseStreet && `${houseStreet}, Việt Nam`,
    parts.streetDisplay,
    parts.streetDisplay && `${parts.streetDisplay}, Việt Nam`,
  ]);
};

const scoreAddressComponents = (parts, label) => {
  let score = 0;

  parts.areaComponents.forEach((component, index) => {
    const normalizedComponent = normalizeAddressComponent(component);
    if (!normalizedComponent) return;

    const matched = label.includes(normalize(component)) || label.includes(normalizedComponent);
    const weight = Math.max(20, 70 - index * 12);
    score += matched ? weight : -Math.max(10, Math.floor(weight / 2));
  });

  return score;
};

const candidateHouseNumberText = (item, street) => {
  const addressHouseNumber = item?.address?.house_number || '';
  if (normalizeHouseNumber(addressHouseNumber)) return addressHouseNumber;

  const firstPartHouseNumber = houseNumberText(item?.display_name);
  if (normalizeHouseNumber(firstPartHouseNumber)) return firstPartHouseNumber;

  if (!street) return '';

  const label = normalize(item?.display_name || '');
  const match = label.match(new RegExp(`([0-9]+(?:[/-][0-9]+)*)\\s*,?\\s*${street.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}`));
  return match?.[1] || '';
};

const scoreHouseNumber = (address, item, street) => {
  const expected = extractHouseNumberParts(address);
  if (!expected) return 0;

  const candidate = extractHouseNumberParts(candidateHouseNumberText(item, street));
  if (!candidate) return -15;

  if (candidate.main === expected.main) {
    return JSON.stringify(candidate.branches) === JSON.stringify(expected.branches) ? 90 : 60;
  }

  const gap = Math.abs(candidate.main - expected.main);
  if (gap <= 10) return 20 - gap * 3;
  if (gap <= 50) return -60 - gap;

  return -140 - Math.min(gap, 160);
};

const scoreGeocodeCandidate = (address, query, item, queryIndex, resultIndex) => {
  const label = normalize(item?.display_name || '');
  const parts = parseAddressParts(address);
  const street = parts.streetNormalized;
  let score = Math.max(0, 30 - queryIndex * 3) + Math.max(0, 10 - resultIndex);

  if (street) {
    score += label.includes(street) ? 100 : -100;
  }

  score += scoreHouseNumber(address, item, street);
  score += scoreAddressComponents(parts, label);

  return score;
};

const geocodeAddress = async (address) => {
  const queries = buildGeocodeQueries(address);
  let bestCandidate = null;
  let bestScore = -Infinity;

  for (const [queryIndex, query] of queries.entries()) {
    const url = `https://nominatim.openstreetmap.org/search?format=json&limit=3&countrycodes=vn&addressdetails=1&q=${encodeURIComponent(query)}`;
    const response = await fetch(url, { headers: { Accept: 'application/json' } });
    if (!response.ok) continue;

    const data = await response.json();
    data?.forEach((item, resultIndex) => {
      if (!item?.lat || !item?.lon) return;

      const score = scoreGeocodeCandidate(address, query, item, queryIndex, resultIndex);
      if (score <= bestScore) return;

      bestScore = score;
      bestCandidate = {
        lat: Number(item.lat),
        lon: Number(item.lon),
        label: item.display_name || query,
        query,
        houseNumber: item.address?.house_number || '',
      };
    });
  }

  if (bestCandidate) return bestCandidate;

  throw new Error('GEOCODE_EMPTY');
};

const routeDistanceKm = async (start, end) => {
  const url = `https://router.project-osrm.org/route/v1/driving/${start.lon},${start.lat};${end.lon},${end.lat}?overview=false&alternatives=true&steps=false`;
  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error('ROUTE_FAILED');
  const data = await response.json();
  const routes = data?.routes || [];
  const shortestRoute = routes
    .filter((route) => Number.isFinite(route.distance) && route.distance > 0)
    .sort((a, b) => a.distance - b.distance)[0];
  if (!shortestRoute) throw new Error('ROUTE_EMPTY');

  return {
    distanceKm: shortestRoute.distance / 1000,
    durationMinutes: shortestRoute.duration ? shortestRoute.duration / 60 : null,
    routeCount: routes.length,
  };
};

const initialFormData = {
  vehicleId: '',
  driverId: '',
  customerId: '',
  contractId: '',
  startLocation: '',
  endLocation: '',
  startTime: '',
  endTime: '',
  distanceKm: '',
  cargoWeightTon: '',
  freightRatePerTonKm: '',
};

export default function Trips() {
  const { user } = useContext(AuthContext);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [refreshKey, setRefreshKey] = useState(0);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [optionsLoading, setOptionsLoading] = useState(false);
  const [drivers, setDrivers] = useState([]);
  const [vehicles, setVehicles] = useState([]);
  const [customers, setCustomers] = useState([]);
  const [contracts, setContracts] = useState([]);
  const [trips, setTrips] = useState([]);
  const [formData, setFormData] = useState(initialFormData);
  const [isCalculatingDistance, setIsCalculatingDistance] = useState(false);
  const [routeInfo, setRouteInfo] = useState(null);
  const [readinessTrip, setReadinessTrip] = useState(null);
  const [expenseTrip, setExpenseTrip] = useState(null);
  const [gpsTrip, setGpsTrip] = useState(null);
  const [epodTrip, setEpodTrip] = useState(null);
  const isDriver = user?.role === 'DRIVER';
  const canCreateTrip = user?.role === 'ADMIN' || user?.role === 'MANAGER';
  const canOperateTrip = ['ADMIN', 'MANAGER', 'DRIVER'].includes(user?.role);
  const tripEndpoint = isDriver ? '/trips/my' : '/trips';

  const loadOptions = useCallback(async () => {
    setOptionsLoading(true);
    try {
      if (!canCreateTrip) {
        const tripRes = await api.get(tripEndpoint);
        setTrips(getResult(tripRes, []));
        return;
      }

      const [driverRes, vehicleRes, customerRes, contractRes, tripRes] = await Promise.all([
        api.get('/drivers'),
        api.get('/vehicles'),
        api.get('/customers'),
        api.get('/contracts'),
        api.get('/trips'),
      ]);

      setDrivers(getResult(driverRes, []));
      setVehicles(getResult(vehicleRes, []));
      setCustomers(getResult(customerRes, []));
      setContracts(getResult(contractRes, []));
      setTrips(getResult(tripRes, []));
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tải dữ liệu tạo chuyến đi');
    } finally {
      setOptionsLoading(false);
    }
  }, [canCreateTrip, tripEndpoint]);

  useEffect(() => {
    loadOptions();
  }, [loadOptions]);

  const handleChange = (event) => {
    const { name, value } = event.target;
    setFormData((current) => ({ ...current, [name]: value }));
    if (name === 'startLocation' || name === 'endLocation' || name === 'distanceKm') {
      setRouteInfo(null);
    }
  };

  const handleContractChange = (event) => {
    const contractId = event.target.value;
    const contract = contracts.find((item) => item.id === contractId);
    setFormData((current) => ({
      ...current,
      contractId,
      customerId: contract?.customerId || current.customerId,
      freightRatePerTonKm: contract?.freightRatePerTonKm
        ? String(contract.freightRatePerTonKm)
        : current.freightRatePerTonKm,
    }));
  };

  const resetForm = () => {
    setFormData(initialFormData);
    setRouteInfo(null);
    setIsModalOpen(false);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (!getScheduleWindow(formData.startTime, formData.endTime)) {
      toast.error('Thời gian kết thúc phải sau thời gian bắt đầu');
      return;
    }

    if (!freightAmount) {
      toast.error('Cần nhập quãng đường, trọng lượng và đơn giá lớn hơn 0');
      return;
    }

    setIsSubmitting(true);
    try {
      await api.post('/trips', {
        ...formData,
        distanceKm: formData.distanceKm === '' ? null : Number(formData.distanceKm),
        cargoWeightTon: formData.cargoWeightTon === '' ? null : Number(formData.cargoWeightTon),
        freightRatePerTonKm: formData.freightRatePerTonKm === '' ? null : Number(formData.freightRatePerTonKm),
      });
      toast.success('Tạo chuyến đi thành công');
      resetForm();
      setRefreshKey((current) => current + 1);
      loadOptions();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể tạo chuyến đi');
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleCalculateDistance = async () => {
    if (!formData.startLocation.trim() || !formData.endLocation.trim()) {
      toast.error('Cần nhập điểm đi và điểm đến');
      return;
    }

    setIsCalculatingDistance(true);
    let start = null;
    let end = null;

    try {
      const response = await api.post('/routes/distance', {
        startLocation: formData.startLocation,
        endLocation: formData.endLocation,
      });
      const route = getResult(response, null);

      if (route?.distanceKm !== undefined && route?.distanceKm !== null) {
        setFormData((current) => ({ ...current, distanceKm: Number(route.distanceKm).toFixed(1) }));
        setRouteInfo({
          title: route.title || 'Tuyến ngắn nhất theo đường bộ',
          detail: route.detail || [
            route.startLabel ? `Điểm đi: ${route.startLabel}` : null,
            route.endLabel ? `Điểm đến: ${route.endLabel}` : null,
          ].filter(Boolean).join(' | '),
        });
        const message = route.source === 'SAME_STREET_ESTIMATE'
          ? (Number(route.distanceKm) === 0 ? 'Hai địa chỉ trùng nhau' : 'Đã ước tính quãng đường theo số nhà')
          : (route.source === 'STRAIGHT_LINE_ESTIMATE' ? 'Đã ước tính quãng đường' : 'Đã tính tuyến đường ngắn nhất');
        toast.success(message);
        setIsCalculatingDistance(false);
        return;
      }
    } catch {
      // Fallback to browser-side geocoding below when the backend cannot reach the map APIs.
    }

    try {
      [start, end] = await Promise.all([
        geocodeAddress(formData.startLocation),
        geocodeAddress(formData.endLocation),
      ]);
      const route = await routeDistanceKm(start, end);
      const sameStreetDistanceKm = shouldUseSameStreetEstimate(
        route,
        start,
        end,
        formData.startLocation,
        formData.endLocation
      );

      if (sameStreetDistanceKm !== null) {
        setFormData((current) => ({ ...current, distanceKm: sameStreetDistanceKm.toFixed(1) }));
        setRouteInfo({
          title: sameStreetDistanceKm === 0 ? 'Hai địa chỉ trùng nhau' : 'Ước tính theo số nhà cùng tuyến đường',
          detail: sameStreetDistanceKm === 0
            ? 'Điểm đi và điểm đến giống nhau nên quãng đường được tính là 0 km.'
            : [
              `Tuyến: ${streetDisplayName(formData.startLocation)}`,
              'Dữ liệu bản đồ không trả về đúng tọa độ từng số nhà hoặc trả hai điểm quá gần nhau.',
              'Hệ thống ước tính theo chênh lệch số nhà/hẻm để tránh kết quả sai.',
            ].join(' | '),
        });
        toast.success(sameStreetDistanceKm === 0 ? 'Hai địa chỉ trùng nhau' : 'Đã ước tính quãng đường theo số nhà');
        return;
      }

      setFormData((current) => ({ ...current, distanceKm: route.distanceKm.toFixed(1) }));
      const routeDetail = [
        `Điểm đi: ${start.label || start.query || formData.startLocation}`,
        `Điểm đến: ${end.label || end.query || formData.endLocation}`,
        route.durationMinutes
        ? `Đã chọn tuyến ngắn nhất trong ${route.routeCount || 1} phương án, thời gian ước tính khoảng ${Math.round(route.durationMinutes)} phút.`
        : `Đã chọn tuyến ngắn nhất trong ${route.routeCount || 1} phương án.`,
      ];

      if (!geocodeHasHouseNumber(formData.startLocation, start) || !geocodeHasHouseNumber(formData.endLocation, end)) {
        routeDetail.push('Dữ liệu bản đồ chưa xác nhận đúng số nhà, quãng đường là ước tính theo điểm gần nhất trên tuyến.');
      }

      setRouteInfo({
        title: 'Tuyến ngắn nhất theo đường bộ',
        detail: routeDetail.join(' | '),
      });
      toast.success('Đã tính tuyến đường ngắn nhất');
    } catch {
      const sameStreetDistanceKm = shouldUseSameStreetEstimate(
        null,
        start,
        end,
        formData.startLocation,
        formData.endLocation
      );

      if (sameStreetDistanceKm !== null) {
        setFormData((current) => ({ ...current, distanceKm: sameStreetDistanceKm.toFixed(1) }));
        setRouteInfo({
          title: sameStreetDistanceKm === 0 ? 'Hai địa chỉ trùng nhau' : 'Ước tính theo số nhà cùng tuyến đường',
          detail: sameStreetDistanceKm === 0
            ? 'Điểm đi và điểm đến giống nhau nên quãng đường được tính là 0 km.'
            : [
              `Tuyến: ${streetDisplayName(formData.startLocation)}`,
              'Không lấy được tuyến đường chi tiết theo đúng số nhà từ bản đồ.',
              'Hệ thống ước tính theo chênh lệch số nhà/hẻm trên cùng tuyến đường.',
            ].join(' | '),
        });
        toast.success(sameStreetDistanceKm === 0 ? 'Hai địa chỉ trùng nhau' : 'Đã ước tính quãng đường theo số nhà');
        return;
      }

      const knownStart = findKnownLocation(formData.startLocation);
      const knownEnd = findKnownLocation(formData.endLocation);
      const canUseKnownDistance = canUseKnownLocationDistance(
        formData.startLocation,
        formData.endLocation,
        knownStart,
        knownEnd
      );
      const shortestKnownRoute = canUseKnownDistance ? findShortestKnownRoute(knownStart, knownEnd) : null;

      if (shortestKnownRoute) {
        setFormData((current) => ({ ...current, distanceKm: shortestKnownRoute.distanceKm.toFixed(1) }));
        setRouteInfo({
          title: 'Tuyến ngắn nhất theo Dijkstra',
          detail: shortestKnownRoute.path.join(' -> '),
        });
        toast.success('Đã tính tuyến ngắn nhất bằng Dijkstra');
      } else if (start && end) {
        const distance = estimateStraightLineKm(start, end);
        setFormData((current) => ({ ...current, distanceKm: distance.toFixed(1) }));
        setRouteInfo({
          title: 'Ước tính theo khoảng cách địa lý',
          detail: 'Không lấy được tuyến đường bộ chi tiết, hệ thống dùng khoảng cách đường thẳng có hệ số quy đổi.',
        });
        toast.success('Đã ước tính quãng đường');
      } else if (canUseKnownDistance && knownStart && knownEnd) {
        const distance = estimateStraightLineKm(knownStart, knownEnd);
        setFormData((current) => ({ ...current, distanceKm: distance.toFixed(1) }));
        setRouteInfo({
          title: 'Ước tính theo khoảng cách địa lý',
          detail: 'Không tìm được tuyến trong mạng đường nội bộ, hệ thống dùng khoảng cách đường thẳng có hệ số quy đổi.',
        });
        toast.success('Đã ước tính quãng đường');
      } else {
        setRouteInfo(null);
        toast.error('Không thể tự tính quãng đường, bạn có thể nhập km thủ công');
      }
    } finally {
      setIsCalculatingDistance(false);
    }
  };

  const updateTripStatus = async (tripId, action) => {
    try {
      await api.patch(`/trips/${tripId}/${action}`);
      const messages = {
        start: 'Đã bắt đầu chuyến đi',
        complete: 'Đã hoàn tất chuyến đi',
        cancel: 'Đã hủy chuyến đi',
      };
      toast.success(messages[action] || 'Đã cập nhật chuyến đi');
      setRefreshKey((current) => current + 1);
      loadOptions();
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể cập nhật chuyến đi');
    }
  };

  const downloadReport = async (tripId) => {
    try {
      const response = await api.get(`/trips/${tripId}/report`, { responseType: 'blob' });
      const url = URL.createObjectURL(response.data);
      const link = document.createElement('a');
      link.href = url;
      link.download = `trip_${shortId(tripId)}_report.xlsx`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      toast.error(error.response?.data?.message || 'Không thể xuất báo cáo chuyến đi');
    }
  };

  const selectedScheduleWindow = getScheduleWindow(formData.startTime, formData.endTime);
  const selectedContract = contracts.find((contract) => contract.id === formData.contractId);
  const activeContracts = contracts.filter((contract) => (
    contract.status === 'ACTIVE'
      && (!contract.endDate || String(contract.endDate).slice(0, 10) >= toDateInputValue())
  ));
  const freightAmount = calculateFreightAmount(
    formData.distanceKm,
    formData.cargoWeightTon,
    formData.freightRatePerTonKm
  );
  const availableVehicles = vehicles.filter((vehicle) =>
    schedulableVehicleStatuses.has(vehicle.status)
      && !trips.some((trip) => hasTripScheduleConflict(trip, selectedScheduleWindow, 'vehicleId', vehicle.id))
  );
  const availableDrivers = drivers.filter((driver) =>
    !trips.some((trip) => hasTripScheduleConflict(trip, selectedScheduleWindow, 'driverId', driver.id))
  );

  return (
    <>
      <DataTablePage
        key={refreshKey}
        title="Quản lý chuyến đi"
        description="Theo dõi lịch trình, tài xế, xe, khách hàng và trạng thái từng chuyến."
        endpoint={tripEndpoint}
        deleteEndpoint="/trips"
        deleteLabel={(row) => `chuyến đi "${shortId(row.id)}"`}
        deleteSuccessMessage="Đã xóa chuyến đi"
        onDeleteSuccess={loadOptions}
        emptyText="Chưa có chuyến đi nào."
        primaryColumns={['id', 'status', 'customerName', 'startLocation', 'endLocation', 'freightAmount', 'expenseStatus']}
        filterGridClassName="grid-cols-1 sm:grid-cols-2 xl:grid-cols-12"
        onCreate={canCreateTrip ? () => {
          setIsModalOpen(true);
          if (drivers.length === 0 || vehicles.length === 0 || customers.length === 0 || trips.length === 0) {
            loadOptions();
          }
        } : undefined}
        filters={[
          {
            key: 'status',
            label: 'Trạng thái',
            type: 'select',
            options: Object.entries(tripStatusLabels).map(([value, label]) => ({ value, label })),
            className: 'xl:col-span-3',
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
            className: 'xl:col-span-3',
          },
          {
            key: 'driver',
            label: 'Tài xế',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm tài xế...',
            deriveOptions: true,
            getValue: (row) => row.driverId || row.driverName,
            getOptionLabel: (row) => row.driverName || shortId(row.driverId),
            className: 'xl:col-span-3',
          },
          {
            key: 'vehicle',
            label: 'Phương tiện',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm xe, biển số...',
            deriveOptions: true,
            getValue: (row) => row.vehicleId || row.vehiclePlate,
            getOptionLabel: (row) => row.vehiclePlate || shortId(row.vehicleId),
            className: 'xl:col-span-3',
          },
          {
            key: 'contract',
            label: 'Hợp đồng',
            type: 'select',
            searchable: true,
            placeholder: 'Tìm hợp đồng...',
            deriveOptions: true,
            getValue: (row) => row.contractId || row.contractCode,
            getOptionLabel: (row) => row.contractCode || shortId(row.contractId),
            className: 'xl:col-span-3',
          },
          {
            key: 'depositState',
            label: 'Tiền cọc',
            type: 'select',
            options: [
              { value: 'ENOUGH', label: 'Đã đủ cọc' },
              { value: 'MISSING', label: 'Còn thiếu cọc' },
              { value: 'NOT_REQUIRED', label: 'Không yêu cầu cọc' },
            ],
            match: (row, value) => {
              const summary = row.depositSummary;
              if (value === 'MISSING') return Number(summary?.shortfallAmount || 0) > 0;
              if (value === 'ENOUGH') return Boolean(summary?.required) && Number(summary?.shortfallAmount || 0) <= 0;
              return !summary?.required;
            },
            className: 'xl:col-span-3',
          },
          {
            key: 'expenseState',
            label: 'Chi phí',
            type: 'select',
            options: [
              { value: 'PENDING', label: 'Có khoản chờ duyệt' },
              { value: 'APPROVED', label: 'Có khoản đã duyệt' },
              { value: 'NONE', label: 'Chưa có chi phí' },
            ],
            match: (row, value) => {
              const summary = row.expenseSummary;
              if (value === 'PENDING') return Number(summary?.pendingCount || 0) > 0;
              if (value === 'APPROVED') return Number(summary?.approvedCount || 0) > 0;
              return Number(summary?.totalCount || 0) === 0;
            },
            className: 'xl:col-span-3',
          },
          {
            key: 'startDate',
            label: 'Từ ngày',
            type: 'date',
            field: 'startTime',
            maxFilterKey: 'endDate',
            className: 'sm:col-start-1 xl:col-span-6 xl:col-start-1',
          },
          {
            key: 'endDate',
            label: 'Đến ngày',
            type: 'date',
            field: 'endTime',
            minFilterKey: 'startDate',
            popupAlign: 'right',
            className: 'xl:col-span-6',
          },
        ]}
        columns={[
          { key: 'id', label: 'Mã chuyến', render: (row) => shortId(row.id) },
          { key: 'status', label: 'Trạng thái', render: (row) => tripStatusLabels[row.status] || row.status || '-' },
          { key: 'driverName', label: 'Tài xế', render: (row) => row.driverName || shortId(row.driverId) },
          { key: 'vehiclePlate', label: 'Xe', render: (row) => row.vehiclePlate || shortId(row.vehicleId) },
          { key: 'customerName', label: 'Khách hàng', render: (row) => row.customerName || tripCustomerCode(row) },
          { key: 'contractCode', label: 'Hợp đồng', render: (row) => row.contractCode || '-' },
          { key: 'startLocation', label: 'Điểm đi' },
          { key: 'endLocation', label: 'Điểm đến' },
          { key: 'startTime', label: 'Bắt đầu', render: (row) => formatDateTime(row.startTime) },
          { key: 'endTime', label: 'Kết thúc', render: (row) => formatDateTime(row.endTime) },
          { key: 'distanceKm', label: 'Quãng đường', render: (row) => formatKm(row.distanceKm) },
          { key: 'cargoWeightTon', label: 'Trọng lượng', render: (row) => formatTon(row.cargoWeightTon) },
          { key: 'freightAmount', label: 'Cước dự kiến', render: (row) => formatCurrency(row.freightAmount) },
          { key: 'depositStatus', label: 'Tiền cọc', render: depositSummaryLabel },
          { key: 'expenseStatus', label: 'Chi phí đã duyệt', render: expenseSummaryCell },
        ]}
        rowActions={(row) => (
          <div className="flex justify-end gap-2">
            <button
              type="button"
              onClick={() => setGpsTrip(row)}
              title="Bản đồ GPS & Lộ trình"
              aria-label="Bản đồ GPS & Lộ trình"
              className="inline-flex h-9 items-center justify-center rounded-lg border border-blue-200 bg-blue-50 px-3 text-sm font-medium text-blue-700 hover:bg-blue-100"
            >
              <MapPin size={16} />
            </button>
            {(row.status === 'IN_PROGRESS' || row.status === 'COMPLETED') && (
              <button
                type="button"
                onClick={() => setEpodTrip(row)}
                title="Chứng từ e-POD & Chữ ký"
                aria-label="Chứng từ e-POD & Chữ ký"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
              >
                <FileCheck size={16} />
              </button>
            )}
            {(row.status === 'IN_PROGRESS'
              || row.status === 'COMPLETED'
              || Number(row.expenseSummary?.totalCount || 0) > 0) && (
              <button
                type="button"
                onClick={() => setExpenseTrip(row)}
                title="Chi phí phát sinh"
                aria-label="Chi phí phát sinh"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-amber-200 bg-amber-50 px-3 text-sm font-medium text-amber-700 hover:bg-amber-100"
              >
                <ReceiptText size={16} />
              </button>
            )}
            {canOperateTrip && (row.status === 'CREATED' || row.status === 'ASSIGNED') && (
              <button
                type="button"
                onClick={() => setReadinessTrip(row)}
                title="Kiểm tra điều kiện khởi hành"
                aria-label="Kiểm tra điều kiện khởi hành"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-sky-200 bg-sky-50 px-3 text-sm font-medium text-sky-700 hover:bg-sky-100"
              >
                <Play size={16} />
              </button>
            )}
            {canOperateTrip && row.status === 'IN_PROGRESS' && (
              <button
                type="button"
                onClick={() => updateTripStatus(row.id, 'complete')}
                title="Hoàn tất chuyến đi"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-emerald-200 bg-emerald-50 px-3 text-sm font-medium text-emerald-700 hover:bg-emerald-100"
              >
                <CheckCircle2 size={16} />
              </button>
            )}
            {canOperateTrip && (row.status === 'CREATED' || row.status === 'ASSIGNED' || row.status === 'IN_PROGRESS') && (
              <button
                type="button"
                onClick={() => updateTripStatus(row.id, 'cancel')}
                title="Hủy chuyến đi"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-rose-200 bg-rose-50 px-3 text-sm font-medium text-rose-700 hover:bg-rose-100"
              >
                <XCircle size={16} />
              </button>
            )}
            {row.status === 'COMPLETED' && (
              <button
                type="button"
                onClick={() => downloadReport(row.id)}
                title="Xuất báo cáo Excel"
                className="inline-flex h-9 items-center justify-center rounded-lg border border-slate-300 bg-white px-3 text-sm font-medium text-slate-700 hover:bg-slate-100"
              >
                <Download size={16} />
              </button>
            )}
          </div>
        )}
      />

      <TripReadinessModal
        trip={readinessTrip}
        onClose={() => setReadinessTrip(null)}
        onStarted={() => {
          setReadinessTrip(null);
          setRefreshKey((current) => current + 1);
          loadOptions();
        }}
      />

      <TripExpensesModal
        trip={expenseTrip}
        onClose={() => setExpenseTrip(null)}
        onChanged={() => setRefreshKey((current) => current + 1)}
      />

      <Modal isOpen={isModalOpen} onClose={resetForm} title="Tạo chuyến đi" size="wide">
        <form onSubmit={handleSubmit} className="mt-2 space-y-4">
          <div className="grid gap-4 sm:grid-cols-2">
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Hợp đồng</label>
              <select name="contractId" value={formData.contractId} onChange={handleContractChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                <option value="">{activeContracts.length > 0 ? 'Không chọn hợp đồng' : 'Chưa có hợp đồng đang hiệu lực'}</option>
                {activeContracts.map((contract) => (
                  <option key={contract.id} value={contract.id}>
                    {contract.contractCode} - {contract.customerName || contract.customerUsername || shortId(contract.customerId)} - {contractCargoLabel(contract)} - {formatDate(contract.startDate)} đến {formatDate(contract.endDate)}{contract.freightRatePerTonKm ? ` - ${formatCurrency(contract.freightRatePerTonKm)}/tấn/km` : ''}
                  </option>
                ))}
              </select>
              {(selectedContract?.cargoType || selectedContract?.cargoDescription || selectedContract?.freightRatePerTonKm) && (
                <div className="mt-3 grid gap-2 rounded-lg border border-emerald-100 bg-emerald-50 p-3 text-sm text-emerald-900 sm:grid-cols-2 lg:grid-cols-4">
                  <div>
                    <p className="text-xs font-medium uppercase text-emerald-600">Loại hàng</p>
                    <p className="mt-1 font-semibold">{selectedContract.cargoType ? cargoTypeLabel(selectedContract.cargoType) : '-'}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium uppercase text-emerald-600">Đơn giá</p>
                    <p className="mt-1 font-semibold">
                      {selectedContract.freightRatePerTonKm ? `${formatCurrency(selectedContract.freightRatePerTonKm)}/tấn/km` : '-'}
                    </p>
                  </div>
                  <div>
                    <p className="text-xs font-medium uppercase text-emerald-600">Ghi chú hàng hóa</p>
                    <p className="mt-1 line-clamp-2 font-semibold">{selectedContract.cargoDescription || '-'}</p>
                  </div>
                  <div>
                    <p className="text-xs font-medium uppercase text-emerald-600">Chính sách cọc</p>
                    <p className="mt-1 font-semibold">
                      {selectedContract.depositRequired
                        ? `${selectedContract.depositType === 'PERCENTAGE' ? `${selectedContract.depositValue}%` : formatCurrency(selectedContract.depositValue)} · ${selectedContract.depositScope === 'TRIP' ? 'theo chuyến' : 'theo hợp đồng'}`
                        : 'Không yêu cầu'}
                    </p>
                  </div>
                </div>
              )}
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Khách hàng</label>
              <select required name="customerId" value={formData.customerId} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                <option value="">{optionsLoading ? 'Đang tải...' : 'Chọn khách hàng'}</option>
                {customers.map((customer) => (
                  <option key={customer.id} value={customer.id}>
                    {customer.name || 'Khách hàng'} - {customerCode(customer)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Tài xế</label>
              <select required name="driverId" value={formData.driverId} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                <option value="">{optionsLoading ? 'Đang tải...' : 'Chọn tài xế phù hợp lịch'}</option>
                {availableDrivers.map((driver) => (
                  <option key={driver.id} value={driver.id}>
                    {driver.name || shortId(driver.id)}
                  </option>
                ))}
              </select>
            </div>
            <div className="sm:col-span-2">
              <label className="block text-sm font-medium text-slate-700">Xe</label>
              <select required name="vehicleId" value={formData.vehicleId} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500">
                <option value="">{optionsLoading ? 'Đang tải...' : 'Chọn xe phù hợp lịch'}</option>
                {availableVehicles.map((vehicle) => (
                  <option key={vehicle.id} value={vehicle.id}>
                    {vehicle.licensePlate || shortId(vehicle.id)} - {vehicleTypeWithCapacity(vehicle.vehicleType, vehicle.capacity)}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Điểm đi</label>
              <input required maxLength={255} name="startLocation" value={formData.startLocation} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Điểm đến</label>
              <input required maxLength={255} name="endLocation" value={formData.endLocation} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 p-4 sm:col-span-2">
              <div className="mb-3 flex items-center justify-between gap-3">
                <div>
                  <p className="text-sm font-semibold text-slate-900">Lịch trình vận chuyển</p>
                  <p className="mt-1 text-xs text-slate-500">Chọn rõ thời điểm bắt đầu và thời điểm kết thúc dự kiến của chuyến.</p>
                </div>
              </div>
              <div className="grid gap-4 lg:grid-cols-2">
                <div className="rounded-lg border border-emerald-200 bg-white p-3">
                  <div className="mb-3 flex items-center gap-2">
                    <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-emerald-50 text-emerald-700">
                      <Play size={16} />
                    </span>
                    <div>
                      <p className="text-xs font-semibold uppercase text-emerald-700">Bắt đầu</p>
                      <p className="text-xs text-slate-500">Thời điểm xe bắt đầu nhận hoặc chạy hàng</p>
                    </div>
                  </div>
                  <DateField required type="datetime-local" label="Ngày giờ bắt đầu" name="startTime" value={formData.startTime} onChange={handleChange} />
                </div>
                <div className="rounded-lg border border-sky-200 bg-white p-3">
                  <div className="mb-3 flex items-center gap-2">
                    <span className="inline-flex h-8 w-8 items-center justify-center rounded-lg bg-sky-50 text-sky-700">
                      <CheckCircle2 size={16} />
                    </span>
                    <div>
                      <p className="text-xs font-semibold uppercase text-sky-700">Kết thúc dự kiến</p>
                      <p className="text-xs text-slate-500">Thời điểm dự kiến giao xong chuyến hàng</p>
                    </div>
                  </div>
                  <DateField required type="datetime-local" label="Ngày giờ kết thúc" name="endTime" value={formData.endTime} min={formData.startTime} onChange={handleChange} />
                </div>
              </div>
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Quãng đường (km)</label>
              <input required type="number" min="0.1" step="0.1" name="distanceKm" value={formData.distanceKm} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div className="flex items-end">
              <button
                type="button"
                onClick={handleCalculateDistance}
                disabled={isCalculatingDistance}
                className="inline-flex h-10 w-full items-center justify-center gap-2 rounded-lg border border-sky-200 bg-sky-50 px-3 text-sm font-medium text-sky-700 hover:bg-sky-100 disabled:cursor-not-allowed disabled:opacity-50"
              >
                <Calculator size={16} />
                {isCalculatingDistance ? 'Đang tính...' : 'Tính quãng đường'}
              </button>
            </div>
            {routeInfo && (
              <div className="rounded-lg border border-sky-100 bg-sky-50 px-4 py-3 text-sm text-sky-800 sm:col-span-2">
                <p className="font-medium">{routeInfo.title}</p>
                <p className="mt-1">{routeInfo.detail}</p>
              </div>
            )}
            <div>
              <label className="block text-sm font-medium text-slate-700">Trọng lượng hàng (tấn)</label>
              <input required type="number" min="0.01" step="0.01" name="cargoWeightTon" value={formData.cargoWeightTon} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div>
              <label className="block text-sm font-medium text-slate-700">Đơn giá (VNĐ/tấn/km)</label>
              <input required type="number" min="1" step="1" name="freightRatePerTonKm" value={formData.freightRatePerTonKm} onChange={handleChange} className="mt-1 block w-full rounded-md border border-slate-300 px-3 py-2 text-sm shadow-sm focus:border-emerald-500 focus:outline-none focus:ring-emerald-500" />
            </div>
            <div className="rounded-lg border border-slate-200 bg-slate-50 px-4 py-3 sm:col-span-2">
              <p className="text-xs font-medium uppercase text-slate-500">Cước vận chuyển dự kiến</p>
              <p className="mt-1 text-lg font-semibold text-emerald-700">
                {freightAmount ? formatCurrency(freightAmount) : 'Nhập đủ km, tấn và đơn giá'}
              </p>
            </div>
          </div>

          <div className="flex justify-end gap-3 border-t border-slate-100 pt-4">
            <button type="button" onClick={resetForm} className="rounded-lg border border-slate-300 bg-white px-4 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
              Hủy
            </button>
            <button type="submit" disabled={isSubmitting} className="rounded-lg bg-emerald-600 px-4 py-2 text-sm font-medium text-white hover:bg-emerald-700 disabled:cursor-not-allowed disabled:opacity-50">
              {isSubmitting ? 'Đang lưu...' : 'Lưu chuyến đi'}
            </button>
          </div>
        </form>
      </Modal>

      {gpsTrip && (
        <MapTracker
          tripId={gpsTrip.id}
          vehicleId={gpsTrip.vehicleId}
          licensePlate={gpsTrip.vehiclePlate}
          startLocation={gpsTrip.startLocation}
          endLocation={gpsTrip.endLocation}
          onClose={() => setGpsTrip(null)}
        />
      )}

      {epodTrip && (
        <EpodModal
          trip={epodTrip}
          onClose={() => setEpodTrip(null)}
          onSuccess={() => setRefreshKey(prev => prev + 1)}
        />
      )}
    </>
  );
}
