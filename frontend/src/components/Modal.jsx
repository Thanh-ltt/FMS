import { X } from 'lucide-react';
import { useEffect } from 'react';

const sizeClasses = {
  default: 'max-w-lg',
  wide: 'max-w-4xl',
};

export default function Modal({ isOpen, onClose, title, children, size = 'default', variant = 'default' }) {
  useEffect(() => {
    const handleEsc = (e) => {
      if (e.key === 'Escape') onClose();
    };
    if (isOpen) {
      window.addEventListener('keydown', handleEsc);
    }
    return () => window.removeEventListener('keydown', handleEsc);
  }, [isOpen, onClose]);

  if (!isOpen) return null;

  const isDetail = variant === 'detail';

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
      <div 
        className="absolute inset-0 bg-slate-900/50 backdrop-blur-sm transition-opacity"
        onClick={onClose}
      />
      
      <div className={`relative w-full ${sizeClasses[size] || sizeClasses.default} scale-100 transform overflow-hidden rounded-xl bg-white text-left align-middle shadow-xl transition-all ${isDetail ? 'ring-1 ring-emerald-200' : ''}`}>
        <div className={`flex items-center justify-between border-b px-6 py-4 ${isDetail ? 'border-emerald-100 bg-emerald-50' : 'border-slate-100'}`}>
          <h3 className={`text-lg font-semibold ${isDetail ? 'text-emerald-950' : 'text-slate-900'}`}>
            {title}
          </h3>
          <button
            onClick={onClose}
            className={`rounded-full p-1.5 transition-colors ${isDetail ? 'text-emerald-600 hover:bg-emerald-100 hover:text-emerald-800' : 'text-slate-400 hover:bg-slate-100 hover:text-slate-500'}`}
          >
            <X size={20} />
          </button>
        </div>
        
        <div className="px-6 py-4 max-h-[75vh] overflow-y-auto">
          {children}
        </div>
      </div>
    </div>
  );
}
