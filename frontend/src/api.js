const API_BASE = 'http://localhost:8080/api';

export async function sendChatMessage(message, customerId = 'cust_demo_user') {
  const res = await fetch(`${API_BASE}/agent/chat`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ message, customerId })
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Chat failed');
  return data.data;
}

export async function createDraftOrder(selectedProductIds, upsellProductId, upsellAccepted, reason) {
  const res = await fetch(`${API_BASE}/orders/draft`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      customerId: 'cust_demo_user',
      customerName: 'Aarav Patel',
      customerEmail: 'aarav.patel@example.com',
      selectedProductIds,
      upsellProductId,
      upsellAccepted,
      aiRecommendationReason: reason
    })
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Draft creation failed');
  return data.data;
}

export async function approveOrder(orderId, approved, customerNote = '') {
  const res = await fetch(`${API_BASE}/orders/${orderId}/approve`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ approved, customerNote })
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Approval failed');
  return data.data;
}

export async function initiatePayment(orderId) {
  const res = await fetch(`${API_BASE}/payments/initiate/${orderId}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' }
  });
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Payment initiation failed');
  return data.data;
}

export async function verifyPayment(paymentId, razorpayOrderId, razorpayPaymentId, razorpaySignature, simulateFailure = false) {
  const res = await fetch(`${API_BASE}/payments/verify`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      paymentId,
      razorpayOrderId,
      razorpayPaymentId,
      razorpaySignature,
      simulateFailure
    })
  });
  const data = await res.json();
  return data.data;
}

export async function getDashboardSummary() {
  const res = await fetch(`${API_BASE}/dashboard/summary`);
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Dashboard failed');
  return data.data;
}

export async function getAuditLogs() {
  const res = await fetch(`${API_BASE}/audit`);
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Audit logs failed');
  return data.data;
}

export async function getProducts(category, search) {
  const params = new URLSearchParams();
  if (category) params.append('category', category);
  if (search) params.append('search', search);

  const res = await fetch(`${API_BASE}/products?${params.toString()}`);
  const data = await res.json();
  if (!data.success) throw new Error(data.message || 'Products fetch failed');
  return data.data;
}

export async function resetDemoData() {
  const res = await fetch(`${API_BASE}/demo/reset`, { method: 'POST' });
  const data = await res.json();
  return data.data;
}
