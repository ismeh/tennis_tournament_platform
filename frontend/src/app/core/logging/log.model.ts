/**
 * Log level enumeration
 */
export enum LogLevel {
  DEBUG = 'DEBUG',
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR'
}

/**
 * HTTP Request log entry
 */
export interface RequestLogEntry {
  timestamp: Date;
  level: LogLevel;
  method: string;
  url: string;
  status?: number;
  duration?: number; // in milliseconds
  requestBody?: any;
  responseBody?: any;
  error?: string;
  headers?: Record<string, string>;
}

/**
 * Generic log entry
 */
export interface LogEntry {
  timestamp: Date;
  level: LogLevel;
  message: string;
  data?: any;
  error?: Error;
}
