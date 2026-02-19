
export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

export interface CodeSnippet {
  language: string;
  code: string;
  description?: string;
}

export enum AppTheme {
  DARK = 'dark',
  LIGHT = 'light'
}
