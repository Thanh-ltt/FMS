import {
  BarChart3,
  CreditCard,
  FileText,
  Home,
  Map,
  Truck,
  User,
  UserRound,
  Users,
  Wrench,
  ContactRound,
} from 'lucide-react';

export const navigationSections = [
  {
    key: 'overview',
    label: 'Tổng quan',
    items: [
      { path: '/dashboard', icon: Home, label: 'Dashboard', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER', 'CUSTOMER'] },
      { path: '/my-portal', icon: UserRound, label: 'Cổng khách hàng', roles: ['CUSTOMER'] },
    ],
  },
  {
    key: 'preparation',
    label: 'Chuẩn bị',
    items: [
      { path: '/customers', icon: User, label: 'Khách hàng', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] },
      { path: '/contracts', icon: FileText, label: 'Hợp đồng', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] },
      { path: '/vehicles', icon: Truck, label: 'Phương tiện', roles: ['ADMIN', 'MANAGER'] },
      { path: '/drivers', icon: Users, label: 'Tài xế', roles: ['ADMIN', 'MANAGER', 'DRIVER'] },
    ],
  },
  {
    key: 'operations',
    label: 'Vận hành',
    items: [
      { path: '/trips', icon: Map, label: 'Chuyến đi', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT', 'DRIVER'] },
      { path: '/maintenance', icon: Wrench, label: 'Bảo dưỡng', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] },
    ],
  },
  {
    key: 'finance',
    label: 'Tài chính',
    items: [
      { path: '/invoices', icon: CreditCard, label: 'Hóa đơn', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] },
      { path: '/reports', icon: BarChart3, label: 'Báo cáo', roles: ['ADMIN', 'MANAGER', 'ACCOUNTANT'] },
    ],
  },
  {
    key: 'system',
    label: 'Hệ thống',
    items: [
      { path: '/employees', icon: ContactRound, label: 'Nhân viên', roles: ['ADMIN'] },
    ],
  },
];

export const navigationItems = navigationSections.flatMap((section) => section.items);

export const getRouteRoles = (path) =>
  navigationItems.find((item) => item.path === path)?.roles || [];
