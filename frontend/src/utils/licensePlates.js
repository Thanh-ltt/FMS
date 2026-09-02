const hoChiMinhCityCodes = new Set([
  '41', '50', '51', '52', '53', '54', '55', '56', '57', '58', '59',
]);

const haNoiCodes = new Set(['29', '30', '31', '32', '33', '40']);

const cargoTruck = 'CARGO_TRUCK';
const pickupTruck = 'PICKUP_TRUCK';
const cargoVan = 'CARGO_VAN';

// These are market heuristics for plates commonly issued before 2025, not a
// definitive vehicle classification. The selected type always remains editable.
const marketProfiles = [
  {
    localityCodes: hoChiMinhCityCodes,
    localityName: 'TP.HCM',
    series: {
      C: {
        categoryLabel: 'xe tải / bán tải',
        suggestedVehicleTypes: [cargoTruck, pickupTruck],
      },
      D: {
        categoryLabel: 'xe tải',
        suggestedVehicleTypes: [cargoTruck, cargoVan],
      },
      M: {
        categoryLabel: 'xe tải',
        suggestedVehicleTypes: [cargoTruck],
      },
    },
  },
  {
    localityCodes: haNoiCodes,
    localityName: 'Hà Nội',
    series: {
      C: {
        categoryLabel: 'xe tải / bán tải',
        suggestedVehicleTypes: [cargoTruck, pickupTruck],
      },
      H: {
        categoryLabel: 'xe tải / bán tải',
        suggestedVehicleTypes: [cargoTruck, pickupTruck],
      },
      K: {
        categoryLabel: 'xe tải / bán tải',
        suggestedVehicleTypes: [cargoTruck, pickupTruck],
      },
    },
  },
];

const commonMarketSeries = {
  C: {
    categoryLabel: 'xe tải / bán tải',
    suggestedVehicleTypes: [cargoTruck, pickupTruck],
  },
  D: {
    categoryLabel: 'xe tải van',
    suggestedVehicleTypes: [cargoVan, cargoTruck],
  },
};

const normalizePlate = (value) => String(value || '')
  .toUpperCase()
  .replace(/[^A-Z0-9]/g, '');

export const getLicensePlateSuggestion = (value) => {
  const normalized = normalizePlate(value);
  if (!/^\d{2}[A-Z]/.test(normalized)) return null;

  const localityCode = normalized.slice(0, 2);
  const series = normalized.charAt(2);
  const prefix = `${localityCode}${series}`;
  const profile = marketProfiles.find((item) => item.localityCodes.has(localityCode));
  const match = profile?.series[series] || commonMarketSeries[series];

  if (!match) return null;

  return {
    prefix,
    title: `Gợi ý theo mẫu biển ${prefix}: ${match.categoryLabel}`,
    description: profile?.series[series]
      ? `Mẫu này thường gặp trên ${match.categoryLabel} đã đăng ký trước 2025 tại ${profile.localityName}. Bạn vẫn có thể chọn lại theo xe thực tế.`
      : `Mẫu này thường gặp trên ${match.categoryLabel} đã đăng ký trước 2025. Bạn vẫn có thể chọn lại theo xe thực tế.`,
    suggestedVehicleTypes: match.suggestedVehicleTypes,
  };
};
