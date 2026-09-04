import React, { useEffect, useState } from 'react';
import { Database, RefreshCw, Search, Sparkles, ArrowRight, Package } from 'lucide-react';
import { getProducts, resetDemoData } from '../api';

export default function ProductCatalog({ onSelectProductForAi }) {
  const [products, setProducts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [category, setCategory] = useState('all');
  const [searchTerm, setSearchTerm] = useState('');
  const [resetting, setResetting] = useState(false);

  const fetchCatalog = async () => {
    try {
      setLoading(true);
      const data = await getProducts(category, searchTerm);
      setProducts(data);
    } catch (err) {
      console.error('Catalog fetch error:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchCatalog(); }, [category]);

  const handleSearch = (e) => {
    e.preventDefault();
    fetchCatalog();
  };

  const handleResetData = async () => {
    if (!confirm('Reset and re-seed all demo data in MongoDB Atlas?')) return;
    setResetting(true);
    try {
      await resetDemoData();
      alert('MongoDB Atlas database reset and freshly seeded!');
      fetchCatalog();
    } catch (err) {
      alert('Reset failed: ' + err.message);
    } finally {
      setResetting(false);
    }
  };

  const categories = ['all', 'laptop', 'phone', 'monitor', 'keyboard', 'mouse', 'audio', 'camera', 'accessory'];

  return (
    <div className="max-w-7xl mx-auto px-4 py-10 space-y-6">

      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2 mb-1">
            <Database className="w-5 h-5 text-indigo-600" />
            <h2 className="text-2xl font-black text-slate-900">Product Catalog</h2>
          </div>
          <p className="text-sm text-slate-500">
            Browse all products in MongoDB Atlas. Click "Ask AI" to query any product via the AI agent.
          </p>
        </div>
        <button
          onClick={handleResetData}
          disabled={resetting}
          className="flex items-center gap-2 bg-slate-900 hover:bg-slate-800 text-white text-xs font-bold px-4 py-2.5 rounded-xl shadow-sm transition disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${resetting ? 'animate-spin' : ''}`} />
          {resetting ? 'Resetting...' : 'Reset & Seed Demo Data'}
        </button>
      </div>

      {/* Filter Bar */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-sm flex flex-col md:flex-row gap-3 justify-between items-center">
        <div className="flex flex-wrap gap-1.5 w-full md:w-auto">
          {categories.map(c => (
            <button
              key={c}
              onClick={() => setCategory(c)}
              className={`text-xs capitalize font-semibold px-3 py-1.5 rounded-lg transition ${
                category === c
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
              }`}
            >
              {c}
            </button>
          ))}
        </div>

        <form onSubmit={handleSearch} className="flex gap-2 w-full md:w-64">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-3.5 h-3.5 text-slate-400 pointer-events-none" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search catalog..."
              className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-400 transition"
            />
          </div>
          <button
            type="submit"
            className="bg-indigo-600 hover:bg-indigo-700 text-white px-3 py-2 rounded-xl text-xs font-semibold transition"
          >
            <Search className="w-3.5 h-3.5" />
          </button>
        </form>
      </div>

      {/* Products Grid */}
      {loading ? (
        <div className="flex justify-center py-20">
          <RefreshCw className="w-8 h-8 text-indigo-500 animate-spin" />
        </div>
      ) : products.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-12 text-center shadow-sm">
          <Package className="w-10 h-10 text-slate-300 mx-auto mb-3" />
          <p className="text-slate-500 font-semibold">No products found</p>
          <p className="text-xs text-slate-400 mt-1">Try a different category or reset the demo data.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
          {products.map((p) => (
            <div key={p.id} className="bg-white rounded-2xl border border-slate-200 p-5 shadow-sm flex flex-col justify-between hover:border-slate-300 hover:shadow-md transition-all group">
              <div>
                <img
                  src={p.imageUrl}
                  alt={p.name}
                  className="w-full h-44 object-cover rounded-xl mb-4 border border-slate-100 bg-slate-50"
                />
                <div className="flex justify-between items-start mb-2">
                  <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200">
                    {p.category}
                  </span>
                  <span className="text-base font-black text-slate-900">
                    ₹{p.price.toLocaleString('en-IN')}
                  </span>
                </div>
                <h4 className="font-bold text-slate-900 text-sm leading-snug">{p.name}</h4>
                <p className="text-xs text-slate-500 mt-1 line-clamp-2">{p.description}</p>

                {p.tags && p.tags.length > 0 && (
                  <div className="flex flex-wrap gap-1 mt-3">
                    {p.tags.map(t => (
                      <span key={t} className="text-[10px] bg-slate-50 text-slate-500 border border-slate-200 px-1.5 py-0.5 rounded">
                        #{t}
                      </span>
                    ))}
                  </div>
                )}
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex justify-between items-center text-xs">
                <span className="text-slate-400">Stock: {p.stock} units</span>
                <button
                  onClick={() => onSelectProductForAi && onSelectProductForAi(`Find me something like ${p.name}`)}
                  className="text-indigo-600 font-bold hover:text-indigo-800 flex items-center gap-1 group-hover:gap-2 transition-all"
                >
                  <Sparkles className="w-3.5 h-3.5" />
                  <span>Ask AI</span>
                  <ArrowRight className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
