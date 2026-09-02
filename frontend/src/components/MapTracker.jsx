import React, { useEffect, useRef, useState } from 'react';
import { gpsService } from '../services/gpsService';
import { Play, RefreshCw, MapPin, Navigation, Truck, AlertCircle } from 'lucide-react';
import toast from 'react-hot-toast';

export default function MapTracker({ vehicleId, tripId, licensePlate, startLocation, endLocation, onClose }) {
  const mapContainerRef = useRef(null);
  const mapInstanceRef = useRef(null);
  const markerRef = useRef(null);
  const polylineRef = useRef(null);

  const [location, setLocation] = useState(null);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(true);
  const [isSimulating, setIsSimulating] = useState(false);
  const [leafletLoaded, setLeafletLoaded] = useState(false);

  // Load Leaflet dynamically
  useEffect(() => {
    if (window.L) {
      setLeafletLoaded(true);
      return;
    }

    const cssLink = document.createElement('link');
    cssLink.rel = 'stylesheet';
    cssLink.href = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.css';
    document.head.appendChild(cssLink);

    const script = document.createElement('script');
    script.src = 'https://unpkg.com/leaflet@1.9.4/dist/leaflet.js';
    script.onload = () => setLeafletLoaded(true);
    document.body.appendChild(script);

    return () => {
      // cleanup if needed
    };
  }, []);

  // Fetch initial location & history
  const fetchData = async () => {
    setLoading(true);
    try {
      if (vehicleId) {
        const latest = await gpsService.getLatestLocation(vehicleId);
        setLocation(latest);
      }

      if (tripId) {
        const hist = await gpsService.getTripHistory(tripId);
        setHistory(hist);
      } else if (vehicleId) {
        const hist = await gpsService.getVehicleHistory(vehicleId);
        setHistory(hist);
      }
    } catch (err) {
      console.error("Lỗi khi tải dữ liệu GPS:", err);
      toast.error("Không thể tải vị trí GPS phương tiện");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, [vehicleId, tripId]);

  // Initialize Map
  useEffect(() => {
    if (!leafletLoaded || !mapContainerRef.current || mapInstanceRef.current) return;

    const L = window.L;
    const initialLat = location?.latitude || 10.7769;
    const initialLng = location?.longitude || 106.7009;

    const map = L.map(mapContainerRef.current).setView([initialLat, initialLng], 13);
    mapInstanceRef.current = map;

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '&copy; OpenStreetMap contributors',
      maxZoom: 19
    }).addTo(map);

    // Custom Truck Icon
    const truckIcon = L.divIcon({
      className: 'custom-truck-icon',
      html: `
        <div style="background-color: #2563eb; color: white; padding: 6px; border-radius: 50%; box-shadow: 0 4px 6px -1px rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; width: 36px; height: 36px; border: 2px solid white;">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="1" y="3" width="15" height="13"></rect><polygon points="16 8 20 8 23 11 23 16 16 16 16 8"></polygon><circle cx="5.5" cy="18.5" r="2.5"></circle><circle cx="18.5" cy="18.5" r="2.5"></circle></svg>
        </div>
      `,
      iconSize: [36, 36],
      iconAnchor: [18, 18]
    });

    const marker = L.marker([initialLat, initialLng], { icon: truckIcon }).addTo(map);
    markerRef.current = marker;

    const popupContent = `
      <div style="font-family: sans-serif; font-size: 13px; line-height: 1.4;">
        <strong style="color: #1e3a8a; font-size: 14px;">Xe: ${licensePlate || location?.licensePlate || 'Chưa rõ'}</strong><br/>
        <span>Vận tốc: <b>${location?.speed || 0} km/h</b></span><br/>
        <span style="color: #6b7280; font-size: 11px;">Cập nhật: ${location?.recordedAt ? new Date(location.recordedAt).toLocaleTimeString('vi-VN') : 'Mới đây'}</span>
      </div>
    `;
    marker.bindPopup(popupContent);

    return () => {
      if (mapInstanceRef.current) {
        mapInstanceRef.current.remove();
        mapInstanceRef.current = null;
      }
    };
  }, [leafletLoaded]);

  // Update map marker and polyline when location/history changes
  useEffect(() => {
    if (!leafletLoaded || !mapInstanceRef.current || !location) return;

    const L = window.L;
    const map = mapInstanceRef.current;
    const latLng = [location.latitude, location.longitude];

    if (markerRef.current) {
      markerRef.current.setLatLng(latLng);
      markerRef.current.setPopupContent(`
        <div style="font-family: sans-serif; font-size: 13px; line-height: 1.4;">
          <strong style="color: #1e3a8a; font-size: 14px;">Xe: ${licensePlate || location.licensePlate || 'N/A'}</strong><br/>
          <span>Vận tốc: <b>${location.speed || 0} km/h</b></span><br/>
          <span style="color: #6b7280; font-size: 11px;">Cập nhật: ${location.recordedAt ? new Date(location.recordedAt).toLocaleTimeString('vi-VN') : 'Mới đây'}</span>
        </div>
      `);
    }

    // Draw route trail
    if (history.length > 0) {
      const latLngs = history.map(h => [h.latitude, h.longitude]);
      latLngs.push(latLng);

      if (polylineRef.current) {
        polylineRef.current.setLatLngs(latLngs);
      } else {
        polylineRef.current = L.polyline(latLngs, {
          color: '#3b82f6',
          weight: 4,
          opacity: 0.8,
          dashArray: '5, 10'
        }).addTo(map);
      }
    }

    map.panTo(latLng);
  }, [location, history, leafletLoaded]);

  // Trigger Simulation Step
  const handleSimulateStep = async () => {
    if (!vehicleId) return;
    try {
      setIsSimulating(true);
      const newLoc = await gpsService.simulateMovement(vehicleId, tripId);
      setLocation(newLoc);
      setHistory(prev => [...prev, newLoc]);
      toast.success(`Cập nhật vị trí GPS: ${newLoc.speed} km/h`);
    } catch (err) {
      toast.error("Không thể mô phỏng chuyển động GPS");
    } finally {
      setIsSimulating(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-4xl overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-4 bg-slate-900 text-white flex justify-between items-center">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-600 rounded-lg">
              <Truck className="w-5 h-5 text-white" />
            </div>
            <div>
              <h3 className="font-bold text-lg">Giám sát GPS Thời gian thực</h3>
              <p className="text-xs text-slate-400">
                Phương tiện: <span className="text-blue-400 font-semibold">{licensePlate || 'N/A'}</span>
                {tripId && <span className="ml-2">• Chuyến đi: {tripId}</span>}
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            <button
              onClick={handleSimulateStep}
              disabled={isSimulating}
              className="flex items-center space-x-1 bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-semibold px-3 py-2 rounded-lg transition disabled:opacity-50"
            >
              <Play className="w-3.5 h-3.5" />
              <span>Mô phỏng Di chuyển</span>
            </button>

            <button
              onClick={fetchData}
              className="p-2 text-slate-400 hover:text-white rounded-lg hover:bg-slate-800 transition"
              title="Làm mới"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>

            {onClose && (
              <button
                onClick={onClose}
                className="px-3 py-1.5 text-xs bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg"
              >
                Đóng
              </button>
            )}
          </div>
        </div>

        {/* Info Banner */}
        {(startLocation || endLocation) && (
          <div className="bg-slate-100 px-4 py-2 text-xs border-b border-slate-200 flex items-center justify-between text-slate-700">
            <div className="flex items-center space-x-2">
              <MapPin className="w-3.5 h-3.5 text-blue-600" />
              <span>Điểm đi: <b>{startLocation || 'Chưa xác định'}</b></span>
            </div>
            <div className="flex items-center space-x-2">
              <Navigation className="w-3.5 h-3.5 text-emerald-600" />
              <span>Điểm đến: <b>{endLocation || 'Chưa xác định'}</b></span>
            </div>
          </div>
        )}

        {/* Map Canvas */}
        <div className="relative flex-1 min-h-[420px]">
          {!leafletLoaded && (
            <div className="absolute inset-0 bg-slate-50 flex items-center justify-center text-slate-500 text-sm">
              Đang tải bản đồ GPS...
            </div>
          )}
          <div ref={mapContainerRef} className="w-full h-full min-h-[420px]" />
        </div>

        {/* Footer Status Bar */}
        <div className="p-3 bg-slate-50 border-t border-slate-200 flex justify-between items-center text-xs text-slate-600">
          <div className="flex items-center space-x-4">
            <div>Vĩ độ: <b>{location?.latitude?.toFixed(4) || '10.7769'}</b></div>
            <div>Kinh độ: <b>{location?.longitude?.toFixed(4) || '106.7009'}</b></div>
            <div>Vận tốc: <b className="text-blue-600">{location?.speed || 0} km/h</b></div>
          </div>

          <div className="text-slate-400">
            Lịch sử vết: {history.length} điểm ghi nhận
          </div>
        </div>
      </div>
    </div>
  );
}
