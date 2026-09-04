import React, { useState, useEffect, useRef } from 'react';
import {
  Bot, Sparkles, CheckCircle2, AlertTriangle, Shield,
  ArrowRight, RefreshCw, ShoppingCart, Plus, Check, X, CreditCard,
  Lightbulb, Lock, Search, Smartphone, Landmark, Wallet, QrCode, ChevronRight
} from 'lucide-react';
import { sendChatMessage, createDraftOrder, approveOrder, initiatePayment, verifyPayment } from '../api';

// ─── Payment method definitions ─────────────────────────────────────────────
const PAYMENT_METHODS = [
  { id: 'card',        label: 'Card',        icon: CreditCard,  description: 'Debit or Credit card', color: 'indigo' },
  { id: 'upi',         label: 'UPI',         icon: Smartphone,  description: 'GPay, PhonePe, BHIM',  color: 'violet' },
  { id: 'qr',          label: 'QR Code',     icon: QrCode,      description: 'Scan & pay',           color: 'purple' },
  { id: 'netbanking',  label: 'Net Banking', icon: Landmark,    description: 'All major banks',      color: 'blue'   },
  { id: 'wallet',      label: 'Wallet',      description: 'Paytm, Amazon Pay', icon: Wallet, color: 'sky' },
];

const METHOD_COLOR = {
  indigo: { bg: 'bg-indigo-50 hover:bg-indigo-100 border-indigo-200', icon: 'text-indigo-600', selected: 'bg-indigo-600 text-white border-indigo-600 shadow-md shadow-indigo-200' },
  violet: { bg: 'bg-violet-50 hover:bg-violet-100 border-violet-200', icon: 'text-violet-600', selected: 'bg-violet-600 text-white border-violet-600 shadow-md shadow-violet-200' },
  purple: { bg: 'bg-purple-50 hover:bg-purple-100 border-purple-200', icon: 'text-purple-600', selected: 'bg-purple-600 text-white border-purple-600 shadow-md shadow-purple-200' },
  blue:   { bg: 'bg-blue-50 hover:bg-blue-100 border-blue-200',       icon: 'text-blue-600',   selected: 'bg-blue-600 text-white border-blue-600 shadow-md shadow-blue-200'     },
  sky:    { bg: 'bg-sky-50 hover:bg-sky-100 border-sky-200',           icon: 'text-sky-600',    selected: 'bg-sky-600 text-white border-sky-600 shadow-md shadow-sky-200'        },
};

export default function AgentShopping({ onOrderCompleted, onViewAudit, initialPrompt }) {
  const [inputMessage, setInputMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState(null);
  const [chatResult, setChatResult] = useState(null);
  const [selectedProductId, setSelectedProductId] = useState(null);
  const [upsellAccepted, setUpsellAccepted] = useState(false);

  // Gated order flow state
  const [draftOrder, setDraftOrder] = useState(null);
  const [orderApproved, setOrderApproved] = useState(false);
  const [processingPayment, setProcessingPayment] = useState(false);
  const [paymentResult, setPaymentResult] = useState(null);

  // Payment method selection
  const [showMethodPicker, setShowMethodPicker] = useState(false);
  const [selectedMethod, setSelectedMethod] = useState('card');

  // Failure simulation toggle
  const [simulateFailure, setSimulateFailure] = useState(false);

  // Prompt chips
  const samplePrompts = [
    { label: 'Laptop under ₹60,000',      query: 'I need a laptop under ₹60,000 for coding' },
    { label: 'Headphones under ₹10,000',  query: 'Find headphones under ₹10,000 with noise cancellation' },
    { label: '5G phone under ₹30,000',    query: 'Looking for a 5G phone under ₹30,000' },
    { label: 'Mechanical keyboard',        query: 'I want a mechanical keyboard for coding' },
  ];

  // Track if initialPrompt already triggered
  const initialPromptHandled = useRef(false);

  useEffect(() => {
    if (initialPrompt && initialPrompt.trim() && !initialPromptHandled.current) {
      initialPromptHandled.current = true;
      setInputMessage(initialPrompt);
      // Small delay so the UI renders the input first
      setTimeout(() => handleSendMessage(initialPrompt), 80);
    }
  }, [initialPrompt]);

  // ─── API Handlers ──────────────────────────────────────────────────────────

  const handleSendMessage = async (textToSend) => {
    const msg = (textToSend || inputMessage).trim();
    if (!msg || loading) return;

    setLoading(true);
    setErrorMessage(null);
    setPaymentResult(null);
    setDraftOrder(null);
    setOrderApproved(false);
    setSelectedProductId(null);
    setUpsellAccepted(false);
    setChatResult(null);
    setShowMethodPicker(false);

    try {
      const response = await sendChatMessage(msg);
      setChatResult(response);
      if (response.recommendations && response.recommendations.length > 0) {
        setSelectedProductId(response.recommendations[0].product.id);
      }
    } catch (err) {
      console.error('[ShopPilot] Analyze error:', err);
      setErrorMessage('Something went wrong while analyzing your request. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handlePrepareOrder = async () => {
    if (!selectedProductId || !chatResult) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const upsellId = chatResult.upsell ? chatResult.upsell.product.id : null;
      const selectedRec = chatResult.recommendations.find(r => r.product.id === selectedProductId);
      const reason = selectedRec ? selectedRec.reason : chatResult.aiMessage;

      const order = await createDraftOrder(
        [selectedProductId],
        upsellId,
        upsellAccepted,
        reason
      );
      setDraftOrder(order);
      setOrderApproved(false);
    } catch (err) {
      console.error('[ShopPilot] Order preparation error:', err);
      setErrorMessage('Could not prepare your order summary. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handleApproveOrder = async () => {
    if (!draftOrder) return;
    setLoading(true);
    setErrorMessage(null);

    try {
      const approved = await approveOrder(draftOrder.id, true, 'Customer approved order');
      setDraftOrder(approved);
      setOrderApproved(true);
      setShowMethodPicker(true); // Show payment method picker after approval
    } catch (err) {
      console.error('[ShopPilot] Order approval error:', err);
      setErrorMessage('Order authorization failed. Please try again.');
    } finally {
      setLoading(false);
    }
  };

  const handlePay = async () => {
    if (!draftOrder || !orderApproved) return;
    setProcessingPayment(true);
    setShowMethodPicker(false);
    setErrorMessage(null);

    try {
      // 1. Initialize Razorpay test order
      const initResponse = await initiatePayment(draftOrder.id);

      // 2. Build method config for Razorpay
      const methodConfig = {};
      if (selectedMethod === 'card')       methodConfig.method = 'card';
      if (selectedMethod === 'upi')        methodConfig.method = 'upi';
      if (selectedMethod === 'netbanking') methodConfig.method = 'netbanking';
      if (selectedMethod === 'wallet')     methodConfig.method = 'wallet';
      // QR falls through to Razorpay default (it handles QR internally)

      if (window.Razorpay && initResponse.gatewayMode === 'RAZORPAY_TEST') {
        const options = {
          key: initResponse.razorpayKeyId,
          amount: initResponse.amountInPaise,
          currency: initResponse.currency,
          name: 'ShopPilot',
          description: `Order #${draftOrder.id.substring(0, 8)}`,
          order_id: initResponse.razorpayOrderId,
          ...methodConfig,
          handler: async function (response) {
            try {
              const verified = await verifyPayment(
                initResponse.paymentId,
                response.razorpay_order_id,
                response.razorpay_payment_id,
                response.razorpay_signature,
                simulateFailure
              );
              setPaymentResult(verified);
              if (onOrderCompleted) onOrderCompleted();
            } catch (vErr) {
              console.error('[ShopPilot] Verification error:', vErr);
              setErrorMessage('Payment verification encountered an issue. Please check the audit log.');
            } finally {
              setProcessingPayment(false);
            }
          },
          prefill: {
            name: initResponse.customerName,
            email: initResponse.customerEmail,
          },
          theme: { color: '#4f46e5' },
          modal: {
            ondismiss: function () {
              setProcessingPayment(false);
            }
          }
        };

        const rzp = new window.Razorpay(options);
        rzp.on('payment.failed', async function (response) {
          const failed = await verifyPayment(
            initResponse.paymentId,
            initResponse.razorpayOrderId,
            response.error.metadata ? response.error.metadata.payment_id : 'failed_attempt',
            'invalid_sig',
            true
          );
          setPaymentResult(failed);
          setProcessingPayment(false);
          if (onOrderCompleted) onOrderCompleted();
        });
        rzp.open();
      } else {
        // Safe simulation path
        setTimeout(async () => {
          const fakePaymentId = 'pay_sim_' + Math.random().toString(36).substring(2, 9);
          const fakeSig = 'sig_sim_valid';
          const verified = await verifyPayment(
            initResponse.paymentId,
            initResponse.razorpayOrderId,
            fakePaymentId,
            fakeSig,
            simulateFailure
          );
          setPaymentResult(verified);
          setProcessingPayment(false);
          if (onOrderCompleted) onOrderCompleted();
        }, 1200);
      }
    } catch (err) {
      console.error('[ShopPilot] Payment processing error:', err);
      setErrorMessage('Payment processing error: ' + err.message);
      setProcessingPayment(false);
    }
  };

  // ─── Helpers ──────────────────────────────────────────────────────────────

  const hasNoResults = chatResult && chatResult.recommendations && chatResult.recommendations.length === 0;
  const hasResults   = chatResult && chatResult.recommendations && chatResult.recommendations.length > 0;

  // ─── Render ───────────────────────────────────────────────────────────────
  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10 space-y-8">

      {/* ════════════════════════════════════════════
          SECTION 1: HERO
      ════════════════════════════════════════════ */}
      <div className="bg-white rounded-3xl border border-slate-200 p-7 sm:p-10 shadow-sm relative overflow-hidden">
        {/* Decorative background */}
        <div className="absolute -right-24 -top-24 w-80 h-80 bg-gradient-to-br from-indigo-100/40 via-violet-50/30 to-transparent rounded-full blur-3xl pointer-events-none" />
        <div className="absolute -left-16 -bottom-16 w-64 h-64 bg-gradient-to-tr from-slate-100/60 to-transparent rounded-full blur-2xl pointer-events-none" />

        <div className="relative z-10">
          {/* Brand line */}
          <div className="flex items-center gap-2.5 mb-5">
            <div className="w-11 h-11 rounded-2xl bg-gradient-to-tr from-indigo-600 to-violet-500 flex items-center justify-center text-white shadow-lg shadow-indigo-400/30">
              <Bot className="w-6 h-6" />
            </div>
            <div>
              <div className="font-black text-2xl sm:text-3xl text-slate-900 leading-none tracking-tight">ShopPilot</div>
              <div className="text-xs font-semibold text-indigo-600 mt-0.5">AI Shopping Assistant</div>
            </div>
            <div className="ml-auto hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-emerald-50 text-emerald-800 text-xs font-semibold border border-emerald-200">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Razorpay TEST MODE
            </div>
          </div>

          {/* Value proposition */}
          <h1 className="text-2xl sm:text-3xl font-black text-slate-900 tracking-tight mb-2">
            Find the right product with AI
          </h1>
          <p className="text-sm sm:text-base text-slate-500 max-w-2xl leading-relaxed mb-6">
            Tell ShopPilot what you need. It understands your requirements, finds relevant products, explains its recommendations, and helps you complete your purchase.
          </p>

          {/* Quick-search chips */}
          <div>
            <span className="block text-[11px] font-bold uppercase tracking-wider text-slate-400 mb-2.5">
              Popular searches
            </span>
            <div className="flex flex-wrap gap-2">
              {samplePrompts.map((chip, idx) => (
                <button
                  key={idx}
                  type="button"
                  onClick={() => setInputMessage(chip.query)}
                  className="inline-flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-slate-50 hover:bg-indigo-50 border border-slate-200 hover:border-indigo-300 text-xs font-semibold text-slate-700 hover:text-indigo-800 transition-all shadow-sm cursor-pointer"
                >
                  <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
                  {chip.label}
                </button>
              ))}
            </div>
          </div>
        </div>
      </div>

      {/* ════════════════════════════════════════════
          SECTION 2: MAIN SEARCH INPUT
      ════════════════════════════════════════════ */}
      <div className="bg-white rounded-3xl border-2 border-slate-200 p-6 sm:p-8 shadow-sm space-y-5">
        <div>
          <h2 className="text-xl sm:text-2xl font-black text-slate-900">What are you looking for?</h2>
          <p className="text-sm text-slate-500 mt-1">Describe what you need in your own words — budget, use case, specs.</p>
        </div>

        {/* Error banner */}
        {errorMessage && (
          <div className="bg-red-50 border border-red-200 rounded-2xl p-4 flex items-center justify-between gap-3 animate-fadeIn">
            <div className="flex items-center space-x-2.5 text-red-800 text-sm">
              <AlertTriangle className="w-5 h-5 text-red-500 flex-shrink-0" />
              <span className="font-semibold">{errorMessage}</span>
            </div>
            <button onClick={() => setErrorMessage(null)} className="p-1 hover:bg-red-100 rounded-lg text-red-500 transition">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Input + Analyze button */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
              <Search className="w-5 h-5 text-slate-400" />
            </div>
            <input
              type="text"
              value={inputMessage}
              onChange={(e) => setInputMessage(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleSendMessage()}
              placeholder="Tell ShopPilot what you're looking for..."
              style={{
                backgroundColor: '#ffffff',
                color: '#0f172a',
                caretColor: '#4f46e5'
              }}
              className="w-full pl-12 pr-12 py-4 text-base font-medium border-2 border-slate-200 hover:border-slate-300 focus:border-indigo-500 rounded-2xl focus:outline-none focus:ring-4 focus:ring-indigo-500/10 transition-all shadow-xs"
            />
            {inputMessage && (
              <button
                type="button"
                onClick={() => setInputMessage('')}
                className="absolute right-4 top-1/2 -translate-y-1/2 p-1 text-slate-400 hover:text-slate-600 rounded-full hover:bg-slate-100 transition"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>

          <button
            onClick={() => handleSendMessage()}
            disabled={loading || !inputMessage.trim()}
            className="bg-indigo-600 hover:bg-indigo-700 active:scale-95 text-white px-8 py-4 rounded-2xl font-bold text-base shadow-md shadow-indigo-500/25 flex items-center justify-center gap-2.5 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex-shrink-0 cursor-pointer"
          >
            {loading ? (
              <>
                <RefreshCw className="w-5 h-5 animate-spin opacity-80" />
                <span>Analyzing...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5 text-indigo-200" />
                <span>Analyze</span>
              </>
            )}
          </button>
        </div>

        <p className="text-xs text-slate-400">
          Example: <span className="text-slate-600 font-medium">"I need a laptop under ₹60,000 for coding"</span> &nbsp;•&nbsp;
          <span className="text-slate-600 font-medium">"Show me headphones"</span> &nbsp;•&nbsp;
          <span className="text-slate-600 font-medium">"I want a gaming laptop under ₹80,000"</span>
        </p>

        {/* Payment failure simulation toggle */}
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 bg-amber-50 border border-amber-200 rounded-2xl p-4">
          <div className="flex items-start space-x-3">
            <div className="p-1.5 rounded-lg bg-amber-100 text-amber-700 flex-shrink-0">
              <AlertTriangle className="w-4 h-4" />
            </div>
            <div>
              <div className="text-xs font-bold text-amber-900">Payment Failure Simulation</div>
              <div className="text-[11px] text-amber-700 mt-0.5">Simulate bank decline to test retry limits (max 3 attempts) and safe rollback.</div>
            </div>
          </div>
          <label className="flex items-center gap-2.5 cursor-pointer bg-white px-3 py-2 rounded-xl border border-amber-300 hover:border-amber-400 transition self-stretch sm:self-auto">
            <span className="text-xs font-bold text-amber-900">Simulate Failure</span>
            <input
              type="checkbox"
              checked={simulateFailure}
              onChange={(e) => setSimulateFailure(e.target.checked)}
              className="w-4 h-4 text-amber-500 rounded border-amber-300 cursor-pointer"
            />
          </label>
        </div>
      </div>

      {/* ════════════════════════════════════════════
          SECTION 3: LOADING STATE
      ════════════════════════════════════════════ */}
      {loading && !chatResult && (
        <div className="bg-white rounded-3xl border border-slate-200 p-10 shadow-sm flex flex-col items-center justify-center gap-4 animate-fadeIn">
          <div className="relative">
            <div className="w-14 h-14 rounded-2xl bg-indigo-600 flex items-center justify-center shadow-lg shadow-indigo-400/30">
              <Bot className="w-7 h-7 text-white" />
            </div>
            <div className="absolute -top-1 -right-1 w-4 h-4 rounded-full bg-indigo-400 animate-ping" />
          </div>
          <div className="text-center">
            <p className="font-bold text-slate-900 text-lg">Analyzing your request...</p>
            <p className="text-sm text-slate-500 mt-1">Searching catalog and finding the best matches for you.</p>
          </div>
        </div>
      )}

      {/* ════════════════════════════════════════════
          SECTION 4: RESULTS
      ════════════════════════════════════════════ */}
      {chatResult && !loading && (
        <div className="space-y-6 animate-slideUp">

          {/* AI Analysis Summary */}
          <div className="bg-slate-950 text-white rounded-3xl p-6 shadow-md border border-slate-800 space-y-4">
            <div className="flex flex-wrap justify-between items-center gap-2">
              <div className="flex items-center space-x-3">
                <div className="p-2 rounded-xl bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  <Bot className="w-5 h-5" />
                </div>
                <div>
                  <span className="font-black text-base text-white block">AI Analysis Result</span>
                  <span className="text-xs text-slate-400">Natural language matched against merchant catalog</span>
                </div>
              </div>
              <span className={`text-xs px-3 py-1 rounded-full font-bold border ${
                chatResult.engineUsed === 'LIVE_AI'
                  ? 'bg-indigo-500/20 text-indigo-300 border-indigo-500/40'
                  : 'bg-slate-800 text-slate-300 border-slate-700'
              }`}>
                {chatResult.engineUsed === 'LIVE_AI' ? '✦ Live AI Engine' : '⚙ Smart Catalog Matcher'}
              </span>
            </div>

            <div className="bg-slate-900 rounded-2xl p-4 border border-slate-800 text-sm text-slate-200 leading-relaxed">
              <span className="font-bold text-indigo-400 block mb-1 text-xs uppercase tracking-wide">Overview</span>
              {chatResult.aiMessage}
            </div>

            <div className="grid grid-cols-3 gap-3 text-xs">
              {[
                { label: 'Category',    value: chatResult.intent?.category || 'Any' },
                { label: 'Use Case',    value: chatResult.intent?.useCase  || 'General' },
                { label: 'Max Budget',  value: chatResult.intent?.maxBudget ? `₹${chatResult.intent.maxBudget.toLocaleString('en-IN')}` : 'Flexible' },
              ].map(({ label, value }) => (
                <div key={label} className="bg-slate-900 rounded-xl p-3 border border-slate-800">
                  <span className="text-slate-400 block mb-0.5">{label}:</span>
                  <span className="font-bold text-white text-sm capitalize">{value}</span>
                </div>
              ))}
            </div>
          </div>

          {/* No Results State */}
          {hasNoResults && (
            <div className="bg-white rounded-3xl border border-slate-200 p-8 shadow-sm text-center animate-fadeIn">
              <div className="w-14 h-14 rounded-2xl bg-slate-100 flex items-center justify-center mx-auto mb-4">
                <Search className="w-7 h-7 text-slate-400" />
              </div>
              <h3 className="font-black text-slate-900 text-lg mb-2">No exact match found</h3>
              <p className="text-sm text-slate-500 max-w-md mx-auto">
                We couldn't find a product matching your exact criteria. Try searching for laptops, phones, headphones, keyboards, monitors, or other electronics in our catalog.
              </p>
              <button
                onClick={() => { setChatResult(null); setInputMessage(''); }}
                className="mt-5 inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-sm font-bold rounded-xl transition"
              >
                <RefreshCw className="w-4 h-4" />
                Try a different search
              </button>
            </div>
          )}

          {/* Recommended Products */}
          {hasResults && (
            <div className="space-y-4">
              <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-2">
                <h3 className="font-black text-slate-900 text-xl flex items-center gap-2">
                  <Sparkles className="w-5 h-5 text-indigo-500" />
                  Recommended Products ({chatResult.recommendations.length})
                </h3>
                <span className="text-xs font-semibold text-slate-500 bg-slate-100 px-3 py-1 rounded-full">
                  Select a product to proceed
                </span>
              </div>

              <div className="grid md:grid-cols-2 gap-4">
                {chatResult.recommendations.map((rec) => {
                  const p = rec.product;
                  const isSelected = selectedProductId === p.id;
                  return (
                    <div
                      key={p.id}
                      onClick={() => setSelectedProductId(p.id)}
                      className={`bg-white rounded-3xl border-2 p-5 cursor-pointer transition-all shadow-sm relative flex flex-col justify-between ${
                        isSelected
                          ? 'border-indigo-500 ring-4 ring-indigo-500/10 shadow-md'
                          : 'border-slate-200 hover:border-slate-300 hover:shadow-md'
                      }`}
                    >
                      {isSelected && (
                        <div className="absolute top-3 right-3 w-6 h-6 rounded-full bg-indigo-600 flex items-center justify-center">
                          <Check className="w-3.5 h-3.5 text-white" />
                        </div>
                      )}
                      <div>
                        <div className="flex justify-between items-start mb-4 gap-3">
                          <img
                            src={p.imageUrl}
                            alt={p.name}
                            className="w-20 h-20 object-cover rounded-2xl border border-slate-100 flex-shrink-0 bg-slate-50"
                          />
                          <div className="text-right">
                            <span className="text-2xl font-black text-slate-900">
                              ₹{p.price.toLocaleString('en-IN')}
                            </span>
                            <div className="mt-1 inline-flex items-center gap-1 text-xs font-bold bg-indigo-100 text-indigo-800 px-2.5 py-0.5 rounded-full border border-indigo-200">
                              {Math.round(rec.confidence * 100)}% Match
                            </div>
                          </div>
                        </div>

                        <h4 className="font-bold text-slate-900 text-base leading-snug mb-1">{p.name}</h4>
                        <p className="text-xs text-slate-500 line-clamp-2">{p.description}</p>

                        {p.tags && p.tags.length > 0 && (
                          <div className="flex flex-wrap gap-1.5 mt-3">
                            {p.tags.map((t) => (
                              <span key={t} className="text-[10px] font-semibold bg-slate-100 text-slate-600 px-2 py-0.5 rounded-md">
                                #{t}
                              </span>
                            ))}
                          </div>
                        )}

                        {/* Why Recommended */}
                        <div className="mt-4 bg-indigo-50/80 border border-indigo-100 rounded-2xl p-3.5 text-xs text-indigo-950">
                          <span className="font-bold block text-indigo-800 mb-1 flex items-center gap-1">
                            <Lightbulb className="w-3.5 h-3.5 text-indigo-600" />
                            Why ShopPilot recommends this
                          </span>
                          {rec.reason}
                        </div>
                      </div>

                      <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center">
                        <span className="text-xs text-slate-400">Stock: {p.stock} units</span>
                        <button
                          type="button"
                          className={`text-xs font-bold px-4 py-2 rounded-xl transition cursor-pointer ${
                            isSelected
                              ? 'bg-indigo-600 text-white shadow-sm'
                              : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
                          }`}
                        >
                          {isSelected ? '✓ Selected' : 'Select'}
                        </button>
                      </div>
                    </div>
                  );
                })}
              </div>
            </div>
          )}

          {/* Bounded Upsell */}
          {hasResults && chatResult.upsell && (
            <div className="bg-gradient-to-r from-indigo-50 via-violet-50 to-indigo-50 border-2 border-indigo-200 rounded-3xl p-6 shadow-sm animate-fadeIn">
              <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
                <div className="flex items-start space-x-4">
                  <img
                    src={chatResult.upsell.product.imageUrl}
                    alt={chatResult.upsell.product.name}
                    className="w-16 h-16 object-cover rounded-2xl border-2 border-indigo-200 flex-shrink-0 bg-white"
                  />
                  <div>
                    <div className="flex flex-wrap items-center gap-2 mb-1">
                      <span className="text-[10px] uppercase font-black tracking-wider px-2.5 py-0.5 rounded-full bg-indigo-200 text-indigo-900">
                        Complete your setup
                      </span>
                      <span className="text-sm font-black text-indigo-800">
                        +₹{chatResult.upsell.product.price.toLocaleString('en-IN')}
                      </span>
                    </div>
                    <h4 className="font-bold text-slate-900 text-base">{chatResult.upsell.product.name}</h4>
                    <p className="text-xs text-slate-600 mt-0.5 max-w-md">{chatResult.upsell.reason}</p>
                  </div>
                </div>

                <div className="flex items-center gap-2 w-full md:w-auto flex-shrink-0">
                  <button
                    onClick={() => setUpsellAccepted(!upsellAccepted)}
                    className={`flex-1 md:flex-none px-5 py-2.5 rounded-2xl font-bold text-sm transition flex items-center justify-center gap-2 cursor-pointer ${
                      upsellAccepted
                        ? 'bg-indigo-600 text-white shadow-sm'
                        : 'bg-white text-indigo-800 border-2 border-indigo-400 hover:bg-indigo-50'
                    }`}
                  >
                    {upsellAccepted ? <Check className="w-4 h-4" /> : <Plus className="w-4 h-4" />}
                    <span>{upsellAccepted ? `Added (+₹${chatResult.upsell.product.price.toLocaleString('en-IN')})` : 'Add to setup'}</span>
                  </button>
                  {upsellAccepted && (
                    <button
                      onClick={() => setUpsellAccepted(false)}
                      className="p-2.5 text-slate-400 hover:text-slate-600 rounded-xl bg-white border border-slate-200 cursor-pointer"
                      title="Remove add-on"
                    >
                      <X className="w-4 h-4" />
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}

          {/* Proceed to Order Button */}
          {hasResults && !draftOrder && (
            <div className="flex justify-end pt-2">
              <button
                onClick={handlePrepareOrder}
                disabled={!selectedProductId || loading}
                className="bg-slate-950 hover:bg-slate-800 text-white px-8 py-4 rounded-2xl font-bold text-sm shadow-md transition flex items-center gap-2.5 disabled:opacity-50 cursor-pointer"
              >
                <ShoppingCart className="w-4 h-4 text-indigo-300" />
                <span>Review Order Summary</span>
                <ArrowRight className="w-4 h-4 text-indigo-300" />
              </button>
            </div>
          )}
        </div>
      )}

      {/* ════════════════════════════════════════════
          SECTION 5: ORDER SUMMARY
      ════════════════════════════════════════════ */}
      {draftOrder && (
        <div className="bg-white rounded-3xl border-2 border-slate-900 p-6 sm:p-8 shadow-xl space-y-6 animate-slideUp">
          <div className="flex flex-col sm:flex-row justify-between sm:items-center gap-3 border-b border-slate-100 pb-5">
            <div>
              <span className="text-xs uppercase font-bold tracking-wider text-slate-400">Order Summary</span>
              <h3 className="text-2xl font-black text-slate-900 mt-0.5">Order #{draftOrder.id.substring(0, 8)}</h3>
            </div>
            <div className="flex items-center gap-2 text-xs font-bold px-3.5 py-2 rounded-xl bg-slate-50 text-slate-700 border border-slate-200">
              <Lock className="w-3.5 h-3.5 text-indigo-500" />
              Customer approval required
            </div>
          </div>

          {/* Items */}
          <div className="space-y-2">
            {draftOrder.items.map((item, idx) => (
              <div key={idx} className="flex justify-between items-center text-sm py-3 border-b border-slate-50">
                <div className="flex items-center gap-3">
                  <span className="w-6 h-6 rounded-full bg-indigo-100 text-indigo-700 flex items-center justify-center text-xs font-bold">{idx + 1}</span>
                  <div>
                    <span className="font-semibold text-slate-900">{item.name}</span>
                    {item.isUpsell && (
                      <span className="ml-2 text-[10px] bg-indigo-100 text-indigo-700 font-bold px-2 py-0.5 rounded">Add-on</span>
                    )}
                  </div>
                </div>
                <span className="font-black text-slate-900">₹{item.price.toLocaleString('en-IN')}</span>
              </div>
            ))}

            <div className="pt-4 space-y-2 text-sm">
              <div className="flex justify-between text-slate-500">
                <span>Subtotal</span>
                <span>₹{draftOrder.subtotal.toLocaleString('en-IN')}</span>
              </div>
              {draftOrder.upsellAmount > 0 && (
                <div className="flex justify-between text-indigo-700 font-semibold">
                  <span>Add-on</span>
                  <span>+₹{draftOrder.upsellAmount.toLocaleString('en-IN')}</span>
                </div>
              )}
              <div className="flex justify-between text-xl font-black text-slate-900 pt-3 border-t-2 border-slate-100">
                <span>Total Payable</span>
                <span className="text-indigo-600">₹{draftOrder.totalAmount.toLocaleString('en-IN')}</span>
              </div>
            </div>
          </div>

          {/* Approval Gate */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-5 space-y-4">
            <div className="flex items-start gap-3">
              <div className="p-2 rounded-xl bg-indigo-100 text-indigo-700 flex-shrink-0">
                <Shield className="w-5 h-5" />
              </div>
              <p className="text-xs sm:text-sm text-slate-600 leading-relaxed">
                <span className="font-bold text-slate-900">Explicit Customer Authorization: </span>
                ShopPilot never charges payment automatically. You must review and authorize this transaction before checkout begins.
              </p>
            </div>

            {!orderApproved ? (
              <div className="flex justify-end gap-3 pt-1">
                <button
                  onClick={() => setDraftOrder(null)}
                  className="px-5 py-2.5 border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-100 transition cursor-pointer"
                >
                  Cancel
                </button>
                <button
                  onClick={handleApproveOrder}
                  disabled={loading}
                  className="bg-indigo-600 hover:bg-indigo-700 text-white px-7 py-3 rounded-xl font-bold text-sm shadow-sm transition flex items-center gap-2 cursor-pointer disabled:opacity-60"
                >
                  {loading ? <RefreshCw className="w-4 h-4 animate-spin" /> : <CheckCircle2 className="w-4 h-4" />}
                  Approve & Continue
                </button>
              </div>
            ) : !showMethodPicker ? (
              <div className="flex items-center gap-2 text-sm font-semibold text-indigo-700 pt-1">
                <CheckCircle2 className="w-5 h-5 text-indigo-500" />
                Order approved — choose your payment method below.
              </div>
            ) : null}
          </div>
        </div>
      )}

      {/* ════════════════════════════════════════════
          SECTION 6: PAYMENT METHOD PICKER
      ════════════════════════════════════════════ */}
      {showMethodPicker && orderApproved && draftOrder && (
        <div className="bg-white rounded-3xl border-2 border-indigo-200 p-6 sm:p-8 shadow-lg space-y-6 animate-slideUp">
          <div className="text-center pb-2">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-indigo-50 text-indigo-700 text-xs font-bold border border-indigo-200 mb-3">
              <Lock className="w-3.5 h-3.5" />
              Secure Checkout · Razorpay TEST MODE
            </div>
            <h3 className="text-xl sm:text-2xl font-black text-slate-900">Choose payment method</h3>
            <p className="text-sm text-slate-500 mt-1">
              Total: <span className="font-black text-slate-900">₹{draftOrder.totalAmount.toLocaleString('en-IN')}</span>
            </p>
          </div>

          {/* Method cards */}
          <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
            {PAYMENT_METHODS.map((method) => {
              const Icon = method.icon;
              const colors = METHOD_COLOR[method.color];
              const isSelected = selectedMethod === method.id;
              return (
                <button
                  key={method.id}
                  type="button"
                  onClick={() => setSelectedMethod(method.id)}
                  className={`flex flex-col items-center justify-center gap-2 p-4 rounded-2xl border-2 font-semibold text-sm transition-all cursor-pointer ${
                    isSelected
                      ? colors.selected
                      : `${colors.bg} border text-slate-700`
                  }`}
                >
                  <Icon className={`w-6 h-6 ${isSelected ? 'text-white' : colors.icon}`} />
                  <span className="font-bold text-sm">{method.label}</span>
                  <span className={`text-[10px] font-medium ${isSelected ? 'text-white/80' : 'text-slate-400'}`}>
                    {method.description}
                  </span>
                </button>
              );
            })}
          </div>

          <div className="pt-2 border-t border-slate-100">
            <p className="text-xs text-slate-400 text-center mb-4">
              Your payment will be securely processed by Razorpay. ShopPilot does not store any card or payment credentials.
            </p>
            <div className="flex flex-col sm:flex-row gap-3 justify-end">
              <button
                onClick={() => setShowMethodPicker(false)}
                className="px-5 py-2.5 border border-slate-200 text-slate-600 rounded-xl text-sm font-semibold hover:bg-slate-100 transition cursor-pointer"
              >
                Back
              </button>
              <button
                onClick={handlePay}
                disabled={processingPayment}
                className="bg-indigo-600 hover:bg-indigo-700 text-white px-8 py-3.5 rounded-2xl font-black text-sm shadow-md shadow-indigo-500/25 transition flex items-center justify-center gap-2.5 border border-indigo-500 cursor-pointer disabled:opacity-60"
              >
                {processingPayment ? (
                  <>
                    <RefreshCw className="w-4 h-4 animate-spin" />
                    <span>Opening Razorpay...</span>
                  </>
                ) : (
                  <>
                    <CreditCard className="w-4 h-4 text-indigo-200" />
                    <span>Pay ₹{draftOrder.totalAmount.toLocaleString('en-IN')} with Razorpay</span>
                    <ChevronRight className="w-4 h-4 text-indigo-300" />
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ════════════════════════════════════════════
          SECTION 7: PAYMENT RESULT
      ════════════════════════════════════════════ */}
      {paymentResult && (
        <div className={`rounded-3xl p-6 sm:p-8 border-2 shadow-sm space-y-5 animate-slideUp ${
          paymentResult.status === 'SUCCESS'
            ? 'bg-emerald-50 border-emerald-400'
            : 'bg-red-50 border-red-400'
        }`}>
          <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
            <div className="flex items-center gap-4">
              {paymentResult.status === 'SUCCESS' ? (
                <div className="p-3 rounded-2xl bg-emerald-100 text-emerald-700">
                  <CheckCircle2 className="w-8 h-8" />
                </div>
              ) : (
                <div className="p-3 rounded-2xl bg-red-100 text-red-700">
                  <AlertTriangle className="w-8 h-8" />
                </div>
              )}
              <div>
                <h4 className={`font-black text-xl ${paymentResult.status === 'SUCCESS' ? 'text-emerald-900' : 'text-red-900'}`}>
                  {paymentResult.status === 'SUCCESS'
                    ? 'Payment Successful!'
                    : 'Payment Unsuccessful'}
                </h4>
                <p className={`text-sm mt-0.5 ${paymentResult.status === 'SUCCESS' ? 'text-emerald-700' : 'text-red-700'}`}>
                  {paymentResult.status === 'SUCCESS'
                    ? 'Your order has been confirmed and recorded.'
                    : 'Your order has NOT been marked as paid.'}
                </p>
              </div>
            </div>
            <button
              onClick={onViewAudit}
              className="text-xs font-bold px-4 py-2.5 bg-white rounded-xl shadow-sm border border-slate-200 hover:bg-slate-50 transition cursor-pointer"
            >
              View Audit Log →
            </button>
          </div>

          {/* Receipt */}
          <div className="bg-white/90 rounded-2xl p-4 text-xs font-mono space-y-1.5 border border-slate-200">
            <div className="text-slate-500">Order ID: <span className="font-bold text-slate-900">{paymentResult.orderId}</span></div>
            <div className="text-slate-500">Amount: <span className="font-bold text-slate-900">₹{paymentResult.amount?.toLocaleString('en-IN')}</span></div>
            {paymentResult.razorpayPaymentId && (
              <div className="text-slate-500">Razorpay Payment ID: <span className="font-bold text-indigo-700">{paymentResult.razorpayPaymentId}</span></div>
            )}
            <div className="text-slate-500">Gateway: <span className="font-bold text-slate-900">{paymentResult.gatewayMode}</span> · Attempt #{paymentResult.attemptCount}</div>
            {paymentResult.failureReason && (
              <div className="text-red-600">Failure: <span className="font-bold">{paymentResult.failureReason}</span></div>
            )}
          </div>

          {/* Retry */}
          {paymentResult.status !== 'SUCCESS' && (
            <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3 pt-1">
              <span className="text-xs text-red-700 font-semibold">
                Safety rule: Maximum 3 retry attempts. Order remains unpaid until verified.
              </span>
              <button
                onClick={handlePay}
                className="bg-red-600 hover:bg-red-700 text-white font-bold text-xs px-5 py-2.5 rounded-xl transition shadow-sm cursor-pointer"
              >
                Retry Payment (Attempt #{paymentResult.attemptCount + 1})
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
