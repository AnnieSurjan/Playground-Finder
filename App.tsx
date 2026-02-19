
import React, { useState, useRef, useEffect } from 'react';
import { Message } from './types';
import { getGeminiResponse } from './services/geminiService';
import ChatMessage from './components/ChatMessage';

const App: React.FC = () => {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: '1',
      role: 'assistant',
      content: 'Üdvözöllek! Én vagyok a Játszótér App terveződ. Együtt megépítjük a tökéletes játszótérkereső alkalmazást a magyar és közép-európai piacra. \n\nVan már konkrét elképzelésed a dizájnról vagy az adatforrásokról?',
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [location, setLocation] = useState<{ latitude: number; longitude: number } | undefined>();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if ("geolocation" in navigator) {
      navigator.geolocation.getCurrentPosition(
        (position) => {
          setLocation({
            latitude: position.coords.latitude,
            longitude: position.coords.longitude
          });
        },
        (error) => console.log("Helyszín hozzáférés korlátozva.", error)
      );
    }
  }, []);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    const history = messages.map(msg => ({
      role: msg.role === 'assistant' ? 'model' : 'user',
      parts: [{ text: msg.content }]
    }));

    const responseText = await getGeminiResponse(input, history, location);

    const assistantMessage: Message = {
      id: (Date.now() + 1).toString(),
      role: 'assistant',
      content: responseText,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, assistantMessage]);
    setIsLoading(false);
    inputRef.current?.focus();
  };

  const handleKeyPress = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter') {
      handleSend();
    }
  };

  return (
    <div className="flex flex-col h-screen bg-[#020617] text-slate-100 overflow-hidden">
      {/* Dynamic Background */}
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-[-10%] left-[-10%] w-[40%] h-[40%] bg-emerald-500/10 blur-[120px] rounded-full animate-pulse"></div>
        <div className="absolute bottom-[-10%] right-[-10%] w-[40%] h-[40%] bg-indigo-500/10 blur-[120px] rounded-full animate-pulse" style={{ animationDelay: '2s' }}></div>
      </div>

      {/* Status Bar */}
      <div className="fixed top-0 left-0 w-full h-1 bg-gradient-to-r from-emerald-500 via-indigo-500 to-cyan-500 animate-gradient-x z-30"></div>

      {/* Header */}
      <header className="bg-slate-900/60 backdrop-blur-xl border-b border-slate-800/50 p-4 sticky top-0 z-20 shadow-2xl">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <div className="flex items-center space-x-4">
            <div className="bg-gradient-to-br from-emerald-400 to-emerald-600 p-2.5 rounded-2xl shadow-[0_0_20px_rgba(16,185,129,0.3)]">
              <i className="fas fa-map-marked-alt text-2xl text-slate-950"></i>
            </div>
            <div>
              <h1 className="text-xl font-black tracking-tight bg-clip-text text-transparent bg-gradient-to-r from-emerald-400 to-cyan-400">
                Játszótér Tervező
              </h1>
              <p className="text-[10px] font-black text-slate-500 uppercase tracking-[0.2em]">Magyar Piaci Megoldások</p>
            </div>
          </div>
          
          <div className="hidden sm:flex items-center bg-slate-800/50 border border-slate-700/50 rounded-full px-4 py-1.5 space-x-4 shadow-inner">
             <div className="flex items-center space-x-2">
                <span className={`h-2 w-2 rounded-full ${location ? 'bg-emerald-500 shadow-[0_0_8px_rgba(16,185,129,0.8)]' : 'bg-amber-500'} animate-pulse`}></span>
                <span className="text-[10px] font-bold text-slate-400 uppercase">{location ? 'Helyzet Aktív' : 'Helyzet Nincs'}</span>
             </div>
             <div className="h-4 w-[1px] bg-slate-700"></div>
             <div className="flex items-center space-x-2">
                <span className="text-[10px] font-bold text-slate-400 uppercase">Gemini 2.5 Flash</span>
             </div>
          </div>
        </div>
      </header>

      {/* Messages */}
      <main className="flex-1 overflow-y-auto p-4 md:p-8 custom-scrollbar relative">
        <div className="max-w-4xl mx-auto py-4">
          {messages.map((msg) => (
            <ChatMessage key={msg.id} message={msg} />
          ))}
          {isLoading && (
            <div className="flex justify-start mb-8 animate-in fade-in duration-300">
              <div className="bg-slate-800/40 backdrop-blur-md border border-slate-700/50 p-5 rounded-2xl rounded-tl-none shadow-xl flex items-center space-x-4">
                <div className="flex space-x-2">
                  <div className="w-2.5 h-2.5 bg-emerald-500 rounded-full animate-bounce" style={{ animationDelay: '0ms' }}></div>
                  <div className="w-2.5 h-2.5 bg-cyan-500 rounded-full animate-bounce" style={{ animationDelay: '200ms' }}></div>
                  <div className="w-2.5 h-2.5 bg-indigo-500 rounded-full animate-bounce" style={{ animationDelay: '400ms' }}></div>
                </div>
                <span className="text-[11px] font-black text-slate-400 uppercase tracking-widest italic">Építem a megoldást...</span>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} className="h-4" />
        </div>
      </main>

      {/* Input Section */}
      <footer className="bg-slate-900/40 backdrop-blur-2xl border-t border-slate-800/50 p-4 md:p-8 relative z-20">
        <div className="max-w-4xl mx-auto">
          <div className="relative group">
            <div className="absolute -inset-1 bg-gradient-to-r from-emerald-500/20 via-indigo-500/20 to-cyan-500/20 rounded-3xl blur-xl opacity-0 group-focus-within:opacity-100 transition-opacity duration-700"></div>
            <div className="relative flex items-center space-x-2 bg-slate-900/90 rounded-2xl p-2 shadow-2xl border border-slate-700/50 ring-1 ring-white/5">
              <button 
                className="p-3 text-slate-500 hover:text-emerald-400 transition-all hover:bg-slate-800 rounded-xl"
                title="Saját helyzet küldése"
              >
                <i className="fas fa-crosshairs text-lg"></i>
              </button>
              <input
                ref={inputRef}
                type="text"
                className="flex-1 bg-transparent border-none focus:ring-0 text-slate-100 placeholder-slate-500 text-sm md:text-base py-3 px-2 font-medium"
                placeholder="Írd ide a kérdésed vagy a funkció tervét..."
                value={input}
                onChange={(e) => setInput(e.target.value)}
                onKeyDown={handleKeyPress}
                disabled={isLoading}
              />
              <button
                onClick={handleSend}
                disabled={!input.trim() || isLoading}
                className={`flex items-center justify-center h-12 w-12 rounded-xl transition-all ${
                  !input.trim() || isLoading 
                    ? 'text-slate-600 bg-slate-800/50 cursor-not-allowed' 
                    : 'text-white bg-emerald-600 hover:bg-emerald-500 hover:scale-105 active:scale-95 shadow-[0_0_20px_rgba(16,185,129,0.3)]'
                }`}
              >
                {isLoading ? (
                  <i className="fas fa-circle-notch fa-spin text-xl"></i>
                ) : (
                  <i className="fas fa-paper-plane text-xl"></i>
                )}
              </button>
            </div>
          </div>
          
          <div className="flex flex-wrap justify-center gap-4 mt-6">
            <div className="group cursor-default flex items-center space-x-2 text-[10px] font-black text-slate-500 bg-slate-800/30 px-3 py-1.5 rounded-lg border border-slate-700/30 uppercase tracking-wider hover:border-emerald-500/30 transition-all">
              <i className="fas fa-map-marker-alt text-emerald-500 group-hover:animate-bounce"></i>
              <span>Google Maps Grounding</span>
            </div>
            <div className="group cursor-default flex items-center space-x-2 text-[10px] font-black text-slate-500 bg-slate-800/30 px-3 py-1.5 rounded-lg border border-slate-700/30 uppercase tracking-wider hover:border-indigo-500/30 transition-all">
              <i className="fas fa-laptop-code text-indigo-500"></i>
              <span>Kotlin & Compose Expert</span>
            </div>
            <div className="group cursor-default flex items-center space-x-2 text-[10px] font-black text-slate-500 bg-slate-800/30 px-3 py-1.5 rounded-lg border border-slate-700/30 uppercase tracking-wider hover:border-cyan-500/30 transition-all">
              <i className="fas fa-globe-europe text-cyan-500"></i>
              <span>Közép-Európai Fókusz</span>
            </div>
          </div>
        </div>
      </footer>

      <style>{`
        .animate-gradient-x {
          background-size: 200% 100%;
          animation: gradient-x 5s linear infinite;
        }
        @keyframes gradient-x {
          0% { background-position: 0% 0%; }
          100% { background-position: 200% 0%; }
        }
        .custom-scrollbar::-webkit-scrollbar {
          width: 5px;
        }
        .custom-scrollbar::-webkit-scrollbar-track {
          background: transparent;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb {
          background: #1e293b;
          border-radius: 10px;
        }
        .custom-scrollbar::-webkit-scrollbar-thumb:hover {
          background: #334155;
        }
      `}</style>
    </div>
  );
};

export default App;
