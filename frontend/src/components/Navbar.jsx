import React from 'react';
import { Bot, BarChart3, ShieldCheck, Database, Zap } from 'lucide-react';

export default function Navbar({ activeTab, setActiveTab }) {
  return (
    <header className="bg-white border-b border-slate-200 sticky top-0 z-50 shadow-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex justify-between items-center h-16">

          {/* Logo & Brand */}
          <div
            onClick={() => setActiveTab('agent')}
            className="flex items-center space-x-3 cursor-pointer group"
          >
            <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-md shadow-indigo-500/25 group-hover:scale-105 transition-transform duration-200">
              <Bot className="w-5 h-5" />
            </div>
            <div>
              <div className="flex items-center space-x-2">
                <span className="font-extrabold text-xl tracking-tight text-slate-900">ShopPilot</span>
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                  AI Shopping
                </span>
              </div>
              <p className="text-[11px] text-slate-400 hidden sm:block font-medium">
                Intelligent product discovery & checkout
              </p>
            </div>
          </div>

          {/* Navigation */}
          <div className="flex items-center space-x-1 sm:space-x-1.5">
            {/* Razorpay TEST indicator (desktop) */}
            <div className="hidden lg:flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-50 text-emerald-800 text-xs font-semibold border border-emerald-200 mr-2">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              <span>Razorpay TEST MODE</span>
            </div>

            <nav className="flex items-center space-x-0.5 sm:space-x-1">
              <button
                onClick={() => setActiveTab('agent')}
                className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all ${
                  activeTab === 'agent'
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/30'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Zap className={`w-4 h-4 ${activeTab === 'agent' ? 'text-indigo-200' : 'text-indigo-400'}`} />
                <span>AI Shopping</span>
              </button>

              <button
                onClick={() => setActiveTab('dashboard')}
                className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all ${
                  activeTab === 'dashboard'
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/30'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <BarChart3 className="w-4 h-4" />
                <span className="hidden sm:inline">Revenue</span>
              </button>

              <button
                onClick={() => setActiveTab('audit')}
                className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all ${
                  activeTab === 'audit'
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/30'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <ShieldCheck className="w-4 h-4" />
                <span className="hidden md:inline">Audit Trail</span>
              </button>

              <button
                onClick={() => setActiveTab('catalog')}
                className={`flex items-center space-x-1.5 px-3 py-2 rounded-xl text-sm font-semibold transition-all ${
                  activeTab === 'catalog'
                    ? 'bg-indigo-600 text-white shadow-sm shadow-indigo-500/30'
                    : 'text-slate-600 hover:text-slate-900 hover:bg-slate-100'
                }`}
              >
                <Database className="w-4 h-4" />
                <span className="hidden md:inline">Catalog</span>
              </button>
            </nav>
          </div>
        </div>
      </div>
    </header>
  );
}
