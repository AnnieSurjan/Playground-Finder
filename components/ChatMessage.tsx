
import React from 'react';
import { Message } from '../types';

interface ChatMessageProps {
  message: Message;
}

const ChatMessage: React.FC<ChatMessageProps> = ({ message }) => {
  const isAssistant = message.role === 'assistant';

  const renderContent = (content: string) => {
    // Split by code blocks first
    const parts = content.split(/(```[\s\S]*?```)/g);
    
    return parts.map((part, index) => {
      // If it's a code block
      if (part.startsWith('```')) {
        const codeMatch = part.match(/```(?:\w+)?\n([\s\S]*?)```/);
        const code = codeMatch ? codeMatch[1] : part.replace(/```/g, '');
        return (
          <div key={index} className="my-4 rounded-lg overflow-hidden border border-slate-700 shadow-inner">
            <div className="bg-slate-900 px-4 py-1 flex justify-between items-center border-b border-slate-700">
              <span className="text-[10px] font-bold text-slate-500 uppercase tracking-widest font-mono">CODE</span>
              <button 
                onClick={() => navigator.clipboard.writeText(code)}
                className="text-slate-500 hover:text-emerald-400 transition-colors"
                title="Másolás"
              >
                <i className="far fa-copy"></i>
              </button>
            </div>
            <pre className="p-4 bg-slate-950 text-emerald-400 text-xs md:text-sm font-mono overflow-x-auto whitespace-pre">
              <code>{code}</code>
            </pre>
          </div>
        );
      }

      // If it's regular text, handle links [title](url)
      const textParts = part.split(/(\[.*?\]\(.*?\))/g);
      return (
        <span key={index}>
          {textParts.map((tPart, tIndex) => {
            const match = tPart.match(/\[(.*?)\]\((.*?)\)/);
            if (match) {
              return (
                <a 
                  key={tIndex} 
                  href={match[2]} 
                  target="_blank" 
                  rel="noopener noreferrer" 
                  className="text-cyan-400 font-bold underline hover:text-cyan-300 transition-colors"
                >
                  {match[1]}
                </a>
              );
            }
            return tPart;
          })}
        </span>
      );
    });
  };

  return (
    <div className={`flex w-full mb-8 ${isAssistant ? 'justify-start' : 'justify-end'} animate-in fade-in slide-in-from-bottom-4 duration-500`}>
      <div className={`flex max-w-[92%] ${isAssistant ? 'flex-row' : 'flex-row-reverse'}`}>
        <div className={`flex-shrink-0 h-10 w-10 rounded-xl flex items-center justify-center text-white ${isAssistant ? 'bg-gradient-to-br from-indigo-600 to-indigo-800' : 'bg-gradient-to-br from-emerald-600 to-emerald-800'} shadow-xl border border-white/10`}>
          <i className={`fas ${isAssistant ? 'fa-code' : 'fa-user'}`}></i>
        </div>
        
        <div className={`mx-3 px-5 py-4 rounded-2xl shadow-2xl border ${
          isAssistant 
            ? 'bg-slate-800/80 backdrop-blur-sm text-slate-200 rounded-tl-none border-slate-700/50' 
            : 'bg-indigo-700 text-white rounded-tr-none border-indigo-600'
        }`}>
          <div className="leading-relaxed text-sm md:text-base font-normal">
            {renderContent(message.content)}
          </div>
          <div className={`text-[9px] mt-4 font-black opacity-30 uppercase tracking-widest ${isAssistant ? 'text-left' : 'text-right'}`}>
            {message.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ChatMessage;
