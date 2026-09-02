import React, { useEffect, useState } from 'react';
import api from '../services/api';
import { QrCode, CreditCard, Copy, Check, X, ShieldCheck } from 'lucide-react';
import toast from 'react-hot-toast';

export default function VietQrModal({ invoiceId, invoiceCode, onClose }) {
  const [loading, setLoading] = useState(true);
  const [qrData, setQrData] = useState(null);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    if (!invoiceId) return;
    const fetchQr = async () => {
      setLoading(true);
      try {
        const response = await api.get(`/payment/vietqr/invoice/${invoiceId}`);
        setQrData(response.data.result);
      } catch (err) {
        console.error("Lỗi khi tải mã VietQR:", err);
        toast.error("Không thể sinh mã VietQR thanh toán");
      } finally {
        setLoading(false);
      }
    };
    fetchQr();
  }, [invoiceId]);

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopied(true);
    toast.success("Đã sao chép nội dung chuyển khoản!");
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col">
        {/* Header */}
        <div className="p-4 bg-gradient-to-r from-blue-700 to-indigo-800 text-white flex justify-between items-center">
          <div className="flex items-center space-x-2">
            <QrCode className="w-6 h-6 text-blue-200" />
            <div>
              <h3 className="font-bold text-base">Thanh toán VietQR Napas247</h3>
              <p className="text-xs text-blue-200">Hóa đơn #{invoiceCode || invoiceId?.slice(0, 8)}</p>
            </div>
          </div>
          <button onClick={onClose} className="p-1 text-blue-200 hover:text-white rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Content */}
        <div className="p-5 flex flex-col items-center space-y-4">
          {loading ? (
            <div className="h-64 flex items-center justify-center text-slate-500 text-sm">
              Đang khởi tạo mã QR thanh toán...
            </div>
          ) : qrData ? (
            <>
              {/* QR Image Box */}
              <div className="p-3 bg-white border-2 border-indigo-100 rounded-2xl shadow-lg relative flex flex-col items-center">
                <img
                  src={qrData.qrImageUrl}
                  alt="Mã VietQR"
                  className="w-60 h-60 object-contain rounded-lg"
                />
                <div className="mt-2 flex items-center space-x-1 text-[11px] text-slate-500">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  <span>Quét bằng ứng dụng Ngân hàng / Ví điện tử</span>
                </div>
              </div>

              {/* Info Details */}
              <div className="w-full bg-slate-50 rounded-xl p-3.5 border border-slate-200 space-y-2 text-xs">
                <div className="flex justify-between items-center border-b border-slate-200 pb-2">
                  <span className="text-slate-500">Số tiền cần thanh toán:</span>
                  <span className="font-bold text-emerald-700 text-sm">
                    {Number(qrData.amount || 0).toLocaleString('vi-VN')} đ
                  </span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Ngân hàng:</span>
                  <span className="font-semibold text-slate-800">{qrData.bankName}</span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Số tài khoản:</span>
                  <span className="font-mono font-bold text-blue-700">{qrData.accountNumber}</span>
                </div>

                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Chủ tài khoản:</span>
                  <span className="font-semibold text-slate-800">{qrData.accountName}</span>
                </div>

                <div className="pt-2 border-t border-slate-200 flex justify-between items-center">
                  <div>
                    <span className="text-slate-500 block">Nội dung chuyển khoản:</span>
                    <span className="font-mono font-bold text-indigo-700">{qrData.transferContent}</span>
                  </div>
                  <button
                    type="button"
                    onClick={() => copyToClipboard(qrData.transferContent)}
                    className="p-2 text-indigo-600 hover:bg-indigo-50 rounded-lg transition border border-indigo-200"
                    title="Sao chép nội dung"
                  >
                    {copied ? <Check className="w-4 h-4 text-emerald-600" /> : <Copy className="w-4 h-4" />}
                  </button>
                </div>
              </div>
            </>
          ) : (
            <div className="text-rose-600 text-xs">Không thể hiển thị thông tin VietQR</div>
          )}

          <button
            type="button"
            onClick={onClose}
            className="w-full py-2.5 bg-slate-800 hover:bg-slate-900 text-white font-medium rounded-xl text-sm transition"
          >
            Đóng
          </button>
        </div>
      </div>
    </div>
  );
}
