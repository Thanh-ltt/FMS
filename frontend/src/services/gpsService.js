import api from './api';

export const gpsService = {
  getLatestLocation: async (vehicleId) => {
    const response = await api.get(`/gps/vehicles/${vehicleId}/latest`);
    return response.data.result;
  },

  getTripHistory: async (tripId) => {
    const response = await api.get(`/gps/trips/${tripId}/history`);
    return response.data.result;
  },

  getVehicleHistory: async (vehicleId) => {
    const response = await api.get(`/gps/vehicles/${vehicleId}/history`);
    return response.data.result;
  },

  simulateMovement: async (vehicleId, tripId) => {
    const response = await api.post(`/gps/vehicles/${vehicleId}/simulate`, null, {
      params: { tripId }
    });
    return response.data.result;
  },

  recordLocation: async (vehicleId, locationData) => {
    const response = await api.post(`/gps/vehicles/${vehicleId}/location`, locationData);
    return response.data.result;
  }
};
