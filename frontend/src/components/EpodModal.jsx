import React, { useState, useEffect, useRef } from 'react';
import { driverPortalService } from '../services/driverPortalService';
import SignatureCanvas from './SignatureCanvas';
import { FileCheck, CheckCircle2, User, Phone, FileText, Image as ImageIcon, X } from 'lucide-react';
import toast from 'react-hot-toast';

export default function EpodModal({ trip, readOnly = false, onClose, onSuccess }) {
  const [loading, setLoading] = useState(false);
  const [existingProof, setExistingProof] = useState(null);
  const [recipientName, setRecipientName] = useState('');
  const [recipientPhone, setRecipientPhone] = useState('');
  const [notes, setNotes] = useState('');
  const [photoUrls, setPhotoUrls] = useState('');
  const [signatureData, setSignatureData] = useState(null);
  const sigRef = useRef(null);

  useEffect(() => {
    if (!trip?.id) return;
    const fetchProof = async () => {
      try {
        const proof = await driverPortalService.getProof(trip.id);
        if (proof) {
          setExistingProof(proof);
          setRecipientName(proof.recipientName || '');
          setRecipientPhone(proof.recipientPhone || '');
          setNotes(proof.notes || '');
          setPhotoUrls(proof.photoUrls || '');
        }
      } catch (err) {
        console.error("Lỗi khi tải e-POD:", err);
      }
    };
    fetchProof();
  }, [trip]);

  const handleSubmit = async (e) => {
    e.preventDefault();
    if (!recipientName.trim()) {
      toast.error("Vui lòng nhập tên người nhận hàng");
      return;
    }

    if (!signatureData) {
      toast.error("Vui lòng ký tên xác nhận nhận hàng");
      return;
    }

    setLoading(true);
    try {
      const payload = {
        recipientName: recipientName.trim(),
        recipientPhone: recipientPhone.trim(),
        signatureBase64: signatureData,
        photoUrls: photoUrls.trim(),
        notes: notes.trim()
      };

      await driverPortalService.completeTripWithEpod(trip.id, payload);
      toast.success("Đã hoàn tất chuyến đi & lưu chứng từ e-POD!");
      if (onSuccess) onSuccess();
      if (onClose) onClose();
    } catch (err) {
      console.error(err);
      toast.error("Không thể hoàn tất chứng từ e-POD");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-lg overflow-hidden flex flex-col max-h-[90vh]">
        {/* Header */}
        <div className="p-4 bg-emerald-700 text-white flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <FileCheck className="w-6 h-6" />
            <div>
              <h3 className="font-bold text-lg">Chứng từ giao hàng e-POD</h3>
              <p className="text-xs text-emerald-100">Chuyến đi #{trip?.id?.slice(0, 8)}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 hover:bg-emerald-800 rounded-lg text-emerald-100">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <form onSubmit={handleSubmit} className="p-5 overflow-y-auto space-y-4 text-slate-700 text-sm">
          {existingProof && (readOnly || trip.status === 'COMPLETED') ? (
            <div className="space-y-4">
              <div className="p-3 bg-emerald-50 border border-emerald-200 rounded-lg flex items-center space-x-3 text-emerald-800 text-xs">
                <CheckCircle2 className="w-5 h-5 text-emerald-600 shrink-0" />
                <div>
                  <p className="font-semibold">Chuyến đi đã hoàn tất e-POD</p>
                  <p className="text-[11px] text-emerald-700">Đã ký ngày {new Date(existingProof.signedAt).toLocaleString('vi-VN')}</p>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                  <span className="text-xs text-slate-500 block">Người nhận</span>
                  <strong className="text-slate-800">{existingProof.recipientName}</strong>
                </div>
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-200">
                  <span className="text-xs text-slate-500 block">Số điện thoại</span>
                  <strong className="text-slate-800">{existingProof.recipientPhone || 'N/A'}</strong>
                </div>
              </div>

              {existingProof.signatureBase64 && (
                <div>
                  <span className="text-xs font-semibold text-slate-600 block mb-1">Chữ ký người nhận:</span>
                  <div className="p-2 border border-slate-200 bg-white rounded-lg flex justify-center">
                    <img src={existingProof.signatureBase64} alt="Chữ ký" className="max-h-32 object-contain" />
                  </div>
                </div>
              )}

              {existingProof.notes && (
                <div className="bg-slate-50 p-3 rounded-lg border border-slate-200 text-xs">
                  <span className="text-slate-500 block mb-1 font-semibold">Ghi chú:</span>
                  <p>{existingProof.notes}</p>
                </div>
              )}
            </div>
          ) : (
            <>
              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Tên người nhận hàng *</label>
                <div className="relative">
                  <User className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    required
                    type="text"
                    value={recipientName}
                    onChange={(e) => setRecipientName(e.target.value)}
                    placeholder="Nhập họ tên đại diện bên nhận"
                    className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Số điện thoại người nhận</label>
                <div className="relative">
                  <Phone className="w-4 h-4 text-slate-400 absolute left-3 top-2.5" />
                  <input
                    type="text"
                    value={recipientPhone}
                    onChange={(e) => setRecipientPhone(e.target.value)}
                    placeholder="SĐT liên hệ"
                    className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-slate-700 mb-1">Ghi chú giao hàng</label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Ghi chú tình trạng hàng hóa khi bàn giao..."
                  className="w-full p-2.5 border border-slate-300 rounded-lg text-sm focus:ring-2 focus:ring-emerald-500 focus:outline-none"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">Xác nhận Chữ ký nhận hàng *</label>
                <SignatureCanvas
                  onSave={(base64) => setSignatureData(base64)}
                  onClear={() => setSignatureData(null)}
                />
              </div>

              <div className="pt-3 border-t border-slate-200 flex justify-end space-x-3">
                <button
                  type="button"
                  onClick={onClose}
                  className="px-4 py-2 text-slate-600 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm"
                >
                  Hủy
                </button>
                <button
                  type="submit"
                  disabled={loading || !signatureData}
                  className="flex items-center space-x-2 px-5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-sm font-medium shadow-md disabled:opacity-50"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  <span>{loading ? 'Đang hoàn tất...' : 'Ký nhận & Hoàn tất chuyến'}</span>
                </button>
              </div>
            </>
          )}
        </form>
      </div>
    </div>
  );
}
