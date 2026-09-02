export const contractValueModeLabels = {
  PER_TRIP: 'Tính theo từng chuyến',
  AGREED_VALUE: 'Có giá trị thỏa thuận',
};

export const getContractValueMode = (contract) => contract?.valueMode
  || (contract?.contractValue == null ? 'PER_TRIP' : 'AGREED_VALUE');

export const isPerTripContract = (contract) => getContractValueMode(contract) === 'PER_TRIP';
