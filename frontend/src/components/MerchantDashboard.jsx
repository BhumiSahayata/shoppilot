import React, { useEffect, useState } from 'react';
import {
  TrendingUp, Sparkles, ShoppingBag, ArrowUpRight, DollarSign,
  CheckCircle2, AlertCircle, RefreshCw, BarChart3
} from 'lucide-react';
import { getDashboardSummary } from '../api';

export default function MerchantDashboard({ refreshTrigger }) {
  const [summary, setSummary] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSummary = async () => {
    try {
      setLoading(true);
      const data = await getDashboardSummary();
      setSummary(data);
    } catch (err) {
      console.error('Failed to load dashboard:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchSummary(); }, [refreshTrigger]);

  if (loading && !summary) {
    return (
      <div className="flex justify-center items-center py-20">
        <RefreshCw className="w-8 h-8 text-indigo-500 animate-spin" />
      </div>
    );
  }

  const aiPct = summary?.totalRevenue
    ? Math.round((summary.aiAssistedRevenue / summary.totalRevenue) * 100)
    : 0;
  const upsellPct = summary?.totalRevenue
    ? Math.round((summary.upsellRevenue / summary.totalRevenue) * 100)
    : 0;

  return (
    <div className="max-w-7xl mx-auto px-4 py-10 space-y-8">

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <BarChart3 className="w-5 h-5 text-indigo-600" />
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-700 bg-indigo-50 px-3 py-1 rounded-full border border-indigo-200">
              Merchant Analytics
            </span>
          </div>
          <h2 className="text-2xl font-black text-slate-900">Revenue Dashboard</h2>
          <p className="text-sm text-slate-500 mt-1">
            Live metrics from MongoDB — AI-assisted sales, upsell performance, and order health.
          </p>
        </div>
        <button
          onClick={fetchSummary}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-700 bg-white border border-slate-200 hover:bg-slate-50 px-4 py-2.5 rounded-xl shadow-sm transition"
        >
          <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin text-indigo-500' : ''}`} />
          Refresh
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        {/* Total Revenue */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
          <div className="flex justify-between items-start">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Total Revenue</span>
            <div className="p-1.5 rounded-lg bg-slate-100">
              <DollarSign className="w-4 h-4 text-slate-500" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">
            ₹{summary?.totalRevenue?.toLocaleString('en-IN') || 0}
          </div>
          <p className="text-xs text-slate-500">
            <span className="font-semibold text-slate-700">{summary?.totalOrders || 0}</span> confirmed paid orders
          </p>
        </div>

        {/* AI-Assisted Revenue */}
        <div className="bg-indigo-600 rounded-2xl p-5 shadow-md shadow-indigo-200 space-y-3 text-white">
          <div className="flex justify-between items-start">
            <span className="text-xs font-bold uppercase tracking-wider text-indigo-200">AI-Assisted Sales</span>
            <div className="p-1.5 rounded-lg bg-indigo-500">
              <Sparkles className="w-4 h-4 text-indigo-200" />
            </div>
          </div>
          <div className="text-3xl font-black">
            ₹{summary?.aiAssistedRevenue?.toLocaleString('en-IN') || 0}
          </div>
          <div className="flex items-center gap-1 text-xs font-semibold text-indigo-200">
            <ArrowUpRight className="w-3.5 h-3.5" />
            {summary?.aiAssistedOrders || 0} AI conversions · {aiPct}% of total
          </div>
        </div>

        {/* Upsell Revenue */}
        <div className="bg-violet-50 rounded-2xl border border-violet-200 p-5 shadow-sm space-y-3">
          <div className="flex justify-between items-start">
            <span className="text-xs font-bold uppercase tracking-wider text-violet-700">AI Upsell Revenue</span>
            <div className="p-1.5 rounded-lg bg-violet-100">
              <TrendingUp className="w-4 h-4 text-violet-600" />
            </div>
          </div>
          <div className="text-3xl font-black text-violet-900">
            +₹{summary?.upsellRevenue?.toLocaleString('en-IN') || 0}
          </div>
          <p className="text-xs text-violet-700 font-semibold">
            {summary?.upsellAcceptanceRate ?? 0}% acceptance · {summary?.upsellAcceptedOrders || 0} accepted
          </p>
        </div>

        {/* Average Order Value */}
        <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm space-y-3">
          <div className="flex justify-between items-start">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Avg. Order Value</span>
            <div className="p-1.5 rounded-lg bg-slate-100">
              <ShoppingBag className="w-4 h-4 text-slate-500" />
            </div>
          </div>
          <div className="text-3xl font-black text-slate-900">
            ₹{summary?.averageOrderValue?.toLocaleString('en-IN') || 0}
          </div>
          <p className="text-xs text-slate-500">Boosted by companion accessory recommendations</p>
        </div>
      </div>

      {/* Revenue Attribution */}
      <div className="bg-white rounded-2xl border border-slate-200 p-6 shadow-sm space-y-5">
        <h3 className="font-bold text-slate-900 text-base flex items-center gap-2">
          <BarChart3 className="w-4 h-4 text-indigo-500" />
          Revenue Attribution
        </h3>
        <div className="space-y-4">
          <div>
            <div className="flex justify-between text-xs font-semibold text-slate-600 mb-1.5">
              <span>AI Recommended (base)</span>
              <span>{aiPct}% · ₹{summary?.aiAssistedRevenue?.toLocaleString('en-IN') || 0}</span>
            </div>
            <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="bg-indigo-500 h-full rounded-full transition-all duration-700"
                style={{ width: `${aiPct}%` }}
              />
            </div>
          </div>
          <div>
            <div className="flex justify-between text-xs font-semibold text-slate-600 mb-1.5">
              <span>Upsell Add-ons</span>
              <span>{upsellPct}% · ₹{summary?.upsellRevenue?.toLocaleString('en-IN') || 0}</span>
            </div>
            <div className="w-full h-2.5 bg-slate-100 rounded-full overflow-hidden">
              <div
                className="bg-violet-400 h-full rounded-full transition-all duration-700"
                style={{ width: `${upsellPct}%` }}
              />
            </div>
          </div>
        </div>

        <div className="flex flex-wrap items-center gap-5 text-xs text-slate-500 pt-1">
          {[
            { color: 'bg-indigo-500', label: 'AI Recommended Items' },
            { color: 'bg-violet-400', label: 'Upsell Add-ons' },
            { color: 'bg-slate-200', label: 'Direct Orders' },
          ].map(({ color, label }) => (
            <div key={label} className="flex items-center gap-1.5">
              <div className={`w-3 h-3 rounded-full ${color}`} />
              <span>{label}</span>
            </div>
          ))}
        </div>
      </div>

      {/* Recent Orders Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-sm overflow-hidden">
        <div className="px-6 py-4 border-b border-slate-100 flex justify-between items-center">
          <h3 className="font-bold text-slate-900 text-base">Recent AI-Assisted Orders</h3>
          <span className="text-xs text-slate-400">From MongoDB orders collection</span>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-slate-400 text-xs uppercase font-semibold border-b border-slate-100">
              <tr>
                <th className="px-5 py-3">Order ID</th>
                <th className="px-5 py-3">Customer</th>
                <th className="px-5 py-3">Items</th>
                <th className="px-5 py-3">Add-on</th>
                <th className="px-5 py-3">Total</th>
                <th className="px-5 py-3">Status</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-50 text-xs">
              {summary?.recentAiOrders?.map((order) => (
                <tr key={order.id} className="hover:bg-slate-50/70 transition">
                  <td className="px-5 py-4 font-mono font-bold text-slate-600">#{order.id.substring(0, 8)}</td>
                  <td className="px-5 py-4">
                    <div className="font-semibold text-slate-900">{order.customerName}</div>
                    <div className="text-slate-400 text-[11px]">{order.customerEmail}</div>
                  </td>
                  <td className="px-5 py-4 text-slate-600">{order.items.map(i => i.name).join(', ')}</td>
                  <td className="px-5 py-4">
                    {order.upsellAccepted ? (
                      <span className="inline-flex items-center gap-1 text-[10px] font-bold text-indigo-800 bg-indigo-100 px-2 py-0.5 rounded-full">
                        <CheckCircle2 className="w-3 h-3" />
                        +₹{order.upsellAmount}
                      </span>
                    ) : (
                      <span className="text-slate-300">—</span>
                    )}
                  </td>
                  <td className="px-5 py-4 font-black text-slate-900">₹{order.totalAmount.toLocaleString('en-IN')}</td>
                  <td className="px-5 py-4">
                    <span className={`inline-flex items-center text-[10px] font-bold px-2.5 py-1 rounded-full ${
                      order.status === 'PAID'
                        ? 'bg-emerald-100 text-emerald-800'
                        : order.status === 'PAYMENT_FAILED'
                        ? 'bg-red-100 text-red-800'
                        : 'bg-amber-100 text-amber-800'
                    }`}>
                      {order.status}
                    </span>
                  </td>
                </tr>
              ))}
              {(!summary?.recentAiOrders || summary.recentAiOrders.length === 0) && (
                <tr>
                  <td colSpan={6} className="px-5 py-10 text-center text-slate-400 text-sm">
                    No orders yet. Complete a purchase to see it here.
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
