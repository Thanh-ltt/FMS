import React, { useRef, useState, useEffect } from 'react';
import { RotateCcw, Check, PenTool } from 'lucide-react';

export default function SignatureCanvas({ onSave, onClear }) {
  const canvasRef = useRef(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [hasSignature, setHasSignature] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    ctx.lineWidth = 3;
    ctx.lineCap = 'round';
    ctx.strokeStyle = '#1e3a8a'; // Deep navy blue
  }, []);

  const getCoordinates = (e) => {
    const canvas = canvasRef.current;
    const rect = canvas.getBoundingClientRect();
    if (e.touches && e.touches[0]) {
      return {
        x: e.touches[0].clientX - rect.left,
        y: e.touches[0].clientY - rect.top
      };
    }
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top
    };
  };

  const startDrawing = (e) => {
    setIsDrawing(true);
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const { x, y } = getCoordinates(e);
    ctx.beginPath();
    ctx.moveTo(x, y);
  };

  const draw = (e) => {
    if (!isDrawing) return;
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    const { x, y } = getCoordinates(e);
    ctx.lineTo(x, y);
    ctx.stroke();
    setHasSignature(true);
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const handleClear = () => {
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    setHasSignature(false);
    if (onClear) onClear();
  };

  const handleExport = () => {
    if (!hasSignature) return null;
    const canvas = canvasRef.current;
    const dataUrl = canvas.toDataURL('image/png');
    if (onSave) onSave(dataUrl);
    return dataUrl;
  };

  return (
    <div className="flex flex-col items-center w-full">
      <div className="relative border-2 border-dashed border-slate-300 rounded-xl bg-slate-50 w-full overflow-hidden">
        <canvas
          ref={canvasRef}
          width={450}
          height={180}
          onMouseDown={startDrawing}
          onMouseMove={draw}
          onMouseUp={stopDrawing}
          onMouseLeave={stopDrawing}
          onTouchStart={startDrawing}
          onTouchMove={draw}
          onTouchEnd={stopDrawing}
          className="w-full h-[180px] touch-none cursor-crosshair bg-white"
        />

        {!hasSignature && (
          <div className="absolute inset-0 pointer-events-none flex flex-col items-center justify-center text-slate-400">
            <PenTool className="w-6 h-6 mb-1 opacity-50" />
            <span className="text-xs">Ký tên người nhận hàng tại đây (Dùng tay/chuột)</span>
          </div>
        )}
      </div>

      <div className="flex justify-between items-center w-full mt-2">
        <button
          type="button"
          onClick={handleClear}
          className="inline-flex items-center space-x-1 text-xs text-rose-600 hover:text-rose-700 bg-rose-50 px-3 py-1.5 rounded-lg border border-rose-200"
        >
          <RotateCcw className="w-3.5 h-3.5" />
          <span>Xóa chữ ký</span>
        </button>

        <span className="text-[11px] text-slate-400">Chữ ký điện tử e-POD sẽ được lưu vết kèm thời gian</span>
      </div>
    </div>
  );
}
