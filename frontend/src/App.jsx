import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { Toaster } from 'react-hot-toast';
import { AuthProvider } from './context/AuthContext';
import Layout from './components/Layout';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Dashboard from './pages/Dashboard';
import Vehicles from './pages/Vehicles';
import Drivers from './pages/Drivers';
import Customers from './pages/Customers';
import Trips from './pages/Trips';
import Contracts from './pages/Contracts';
import Invoices from './pages/Invoices';
import Reports from './pages/Reports';
import Maintenance from './pages/Maintenance';
import CustomerPortal from './pages/CustomerPortal';
import NotAuthorized from './pages/NotAuthorized';
import Employees from './pages/Employees';
import ChangePassword from './pages/ChangePassword';
import { getRouteRoles } from './config/navigation';

function App() {
  return (
    <AuthProvider>
      <Router>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/change-password" element={<ProtectedRoute><ChangePassword /></ProtectedRoute>} />
          <Route path="/not-authorized" element={<NotAuthorized />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/dashboard" />} />
            <Route path="dashboard" element={<ProtectedRoute roles={getRouteRoles('/dashboard')}><Dashboard /></ProtectedRoute>} />
            <Route path="my-portal" element={<ProtectedRoute roles={getRouteRoles('/my-portal')}><CustomerPortal /></ProtectedRoute>} />
            <Route path="customers" element={<ProtectedRoute roles={getRouteRoles('/customers')}><Customers /></ProtectedRoute>} />
            <Route path="contracts" element={<ProtectedRoute roles={getRouteRoles('/contracts')}><Contracts /></ProtectedRoute>} />
            <Route path="vehicles" element={<ProtectedRoute roles={getRouteRoles('/vehicles')}><Vehicles /></ProtectedRoute>} />
            <Route path="drivers" element={<ProtectedRoute roles={getRouteRoles('/drivers')}><Drivers /></ProtectedRoute>} />
            <Route path="trips" element={<ProtectedRoute roles={getRouteRoles('/trips')}><Trips /></ProtectedRoute>} />
            <Route path="maintenance" element={<ProtectedRoute roles={getRouteRoles('/maintenance')}><Maintenance /></ProtectedRoute>} />
            <Route path="deposits" element={<Navigate to="/contracts" replace />} />
            <Route path="expenses" element={<Navigate to="/trips" replace />} />
            <Route path="invoices" element={<ProtectedRoute roles={getRouteRoles('/invoices')}><Invoices /></ProtectedRoute>} />
            <Route path="reports" element={<ProtectedRoute roles={getRouteRoles('/reports')}><Reports /></ProtectedRoute>} />
            <Route path="employees" element={<ProtectedRoute roles={getRouteRoles('/employees')}><Employees /></ProtectedRoute>} />
          </Route>
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
        <Toaster position="top-right" />
      </Router>
    </AuthProvider>
  );
}

export default App;
