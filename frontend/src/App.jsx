import React, { useState } from 'react';
import Navbar from './components/Navbar';
import AgentShopping from './components/AgentShopping';
import MerchantDashboard from './components/MerchantDashboard';
import AuditTrail from './components/AuditTrail';
import ProductCatalog from './components/ProductCatalog';

export default function App() {
  const [activeTab, setActiveTab] = useState('agent');
  const [refreshTrigger, setRefreshTrigger] = useState(0);
  const [agentPrompt, setAgentPrompt] = useState('');

  const handleOrderCompleted = () => {
    setRefreshTrigger(prev => prev + 1);
  };

  const handleViewAudit = () => {
    setActiveTab('audit');
    setRefreshTrigger(prev => prev + 1);
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />

      <main className="flex-1">
        {activeTab === 'agent' && (
          <AgentShopping
            initialPrompt={agentPrompt}
            onOrderCompleted={handleOrderCompleted}
            onViewAudit={handleViewAudit}
          />
        )}
        {activeTab === 'dashboard' && (
          <MerchantDashboard refreshTrigger={refreshTrigger} />
        )}
        {activeTab === 'audit' && (
          <AuditTrail refreshTrigger={refreshTrigger} />
        )}
        {activeTab === 'catalog' && (
          <ProductCatalog
            onSelectProductForAi={(query) => {
              setAgentPrompt(query);
              setActiveTab('agent');
            }}
          />
        )}
      </main>

      <footer className="border-t border-slate-200 bg-white py-5 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 flex flex-col sm:flex-row justify-between items-center gap-2">
          <div>
            <span className="font-bold text-slate-600">ShopPilot</span>
            <span className="text-slate-400"> — AI-powered shopping assistant</span>
          </div>
          <div className="flex items-center gap-4">
            <span>Spring Boot · MongoDB Atlas</span>
            <span className="text-slate-300">•</span>
            <span className="text-emerald-600 font-semibold">Secure checkout · Razorpay TEST MODE</span>
          </div>
        </div>
      </footer>
    </div>
  );
}
