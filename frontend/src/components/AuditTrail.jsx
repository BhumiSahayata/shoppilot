import React, { useEffect, useState } from 'react';
import { ShieldCheck, RefreshCw, Bot, User, Cpu, CheckCircle2, XCircle, AlertTriangle } from 'lucide-react';
import { getAuditLogs } from '../api';

export default function AuditTrail({ refreshTrigger }) {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterType, setFilterType] = useState('ALL');

  const fetchLogs = async () => {
    try {
      setLoading(true);
      const data = await getAuditLogs();
      setLogs(data);
    } catch (err) {
      console.error('Failed to load audit logs:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchLogs(); }, [refreshTrigger]);

  const filteredLogs = logs.filter(log => {
    if (filterType === 'ALL')      return true;
    if (filterType === 'AI')       return log.actor === 'AI';
    if (filterType === 'CUSTOMER') return log.actor === 'CUSTOMER';
    if (filterType === 'SYSTEM')   return log.actor === 'SYSTEM';
    return true;
  });

  const getActorBadge = (actor) => {
    switch (actor) {
      case 'AI':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-indigo-100 text-indigo-800">
            <Bot className="w-3 h-3" />AI
          </span>
        );
      case 'CUSTOMER':
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-blue-100 text-blue-800">
            <User className="w-3 h-3" />Customer
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 text-[10px] font-bold px-2 py-0.5 rounded-full bg-slate-100 text-slate-700">
            <Cpu className="w-3 h-3" />System
          </span>
        );
    }
  };

  const getStatusIcon = (status) => {
    if (['SUCCESS', 'APPROVED', 'COMPLETED', 'RECOMMENDED', 'OFFERED'].includes(status)) {
      return <CheckCircle2 className="w-4 h-4 text-emerald-500" />;
    }
    if (['FAILED', 'DECLINED', 'PAYMENT_FAILED'].includes(status)) {
      return <XCircle className="w-4 h-4 text-red-500" />;
    }
    return <AlertTriangle className="w-4 h-4 text-amber-500" />;
  };

  return (
    <div className="max-w-7xl mx-auto px-4 py-10 space-y-6">

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <ShieldCheck className="w-5 h-5 text-indigo-600" />
            <h2 className="text-2xl font-black text-slate-900">Audit Trail</h2>
          </div>
          <p className="text-sm text-slate-500">
            Immutable transaction log — every AI action, customer decision, and payment outcome is recorded here.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <div className="bg-slate-100 p-1 rounded-xl flex text-xs font-semibold">
            {['ALL', 'AI', 'CUSTOMER', 'SYSTEM'].map(t => (
              <button
                key={t}
                onClick={() => setFilterType(t)}
                className={`px-3 py-1.5 rounded-lg transition ${
                  filterType === t
                    ? 'bg-white text-slate-900 shadow-sm font-bold'
                    : 'text-slate-500 hover:text-slate-900'
                }`}
              >
                {t}
              </button>
            ))}
          </div>
          <button
            onClick={fetchLogs}
            className="p-2 bg-white border border-slate-200 rounded-xl hover:bg-slate-50 transition"
            title="Refresh"
          >
            <RefreshCw className={`w-4 h-4 text-slate-500 ${loading ? 'animate-spin text-indigo-500' : ''}`} />
          </button>
        </div>
      </div>

      {/* Stats Summary */}
      <div className="grid grid-cols-3 gap-3">
        {[
          { label: 'Total Events', value: logs.length, color: 'text-slate-900' },
          { label: 'AI Actions',   value: logs.filter(l => l.actor === 'AI').length,       color: 'text-indigo-700' },
          { label: 'Payments',     value: logs.filter(l => l.eventType?.includes('PAYMENT')).length, color: 'text-emerald-700' },
        ].map(({ label, value, color }) => (
          <div key={label} className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm text-center">
            <div className={`text-2xl font-black ${color}`}>{value}</div>
            <div className="text-xs text-slate-400 font-semibold mt-0.5">{label}</div>
          </div>
        ))}
      </div>

      {/* Log Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-400 text-xs uppercase font-semibold border-b border-slate-100">
              <tr>
                <th className="px-4 py-3 whitespace-nowrap">Time</th>
                <th className="px-4 py-3">Actor</th>
                <th className="px-4 py-3">Event</th>
                <th className="px-4 py-3">Order</th>
                <th className="px-4 py-3">Details</th>
                <th className="px-4 py-3">Amount</th>
                <th className="px-4 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 text-xs">
              {filteredLogs.map((log) => {
                const date = new Date(log.timestamp);
                const timeStr = date.toLocaleTimeString('en-IN', { hour12: false });
                return (
                  <tr key={log.id} className="hover:bg-slate-50/70 transition">
                    <td className="px-4 py-3.5 font-mono text-slate-400 whitespace-nowrap">{timeStr}</td>
                    <td className="px-4 py-3.5 whitespace-nowrap">{getActorBadge(log.actor)}</td>
                    <td className="px-4 py-3.5 font-semibold text-slate-800 whitespace-nowrap">{log.eventType}</td>
                    <td className="px-4 py-3.5 font-mono text-slate-400">{log.orderId ? `#${log.orderId.substring(0, 8)}` : '—'}</td>
                    <td className="px-4 py-3.5 text-slate-600 max-w-xs">
                      <div>{log.reason}</div>
                      {log.detail && <div className="text-[11px] text-slate-400 mt-0.5">{log.detail}</div>}
                    </td>
                    <td className="px-4 py-3.5 font-bold text-slate-800 whitespace-nowrap">
                      {log.amount != null ? `₹${log.amount.toLocaleString('en-IN')}` : '—'}
                    </td>
                    <td className="px-4 py-3.5 whitespace-nowrap">
                      <div className="flex items-center gap-1 font-semibold">
                        {getStatusIcon(log.status)}
                        <span className="text-slate-600">{log.status}</span>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filteredLogs.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-4 py-12 text-center text-slate-400">
                    {loading ? 'Loading audit logs...' : 'No events found for the selected filter.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
