import api from './api';

export const driverPortalService = {
  getProof: async (tripId) => {
    const response = await api.get(`/driver-portal/trips/${tripId}/epod`);
    return response.data.result;
  },

  createProof: async (tripId, proofData) => {
    const response = await api.post(`/driver-portal/trips/${tripId}/epod`, proofData);
    return response.data.result;
  },

  startTrip: async (tripId) => {
    const response = await api.post(`/driver-portal/trips/${tripId}/start`);
    return response.data.result;
  },

  completeTripWithEpod: async (tripId, proofData) => {
    const response = await api.post(`/driver-portal/trips/${tripId}/complete-with-epod`, proofData);
    return response.data.result;
  }
};
