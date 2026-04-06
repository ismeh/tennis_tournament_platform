import { Injectable, inject } from '@angular/core';
import { LogEntry, LogLevel, RequestLogEntry } from './log.model';
import { LogTransport } from './log-transport.interface';
import { ConsoleLogTransport } from './transports/console-log-transport';

/**
 * Configuration for request logger
 */
export interface LoggerConfig {
  enableConsole?: boolean;
  enableFile?: boolean;
  minLogLevel?: LogLevel;
  logRequestBody?: boolean;
  logResponseBody?: boolean;
  logHeaders?: boolean;
  sensitiveHeaders?: string[]; // Headers to redact in logs
}

/**
 * Central request logger service with pluggable transports
 */
@Injectable({
  providedIn: 'root'
})
export class RequestLoggerService {
  private consoleTransport = inject(ConsoleLogTransport);
  private transports: LogTransport[] = [];
  private config: LoggerConfig = {
    enableConsole: true,
    enableFile: false,
    minLogLevel: LogLevel.ERROR,
    logRequestBody: false,
    logResponseBody: true,
    logHeaders: false,
    sensitiveHeaders: ['authorization', 'cookie', 'x-api-key']
  };

  constructor() {
    // Ensure default transport is available even before APP_INITIALIZER runs.
    void this.initializeTransports();
  }

  async configure(config: Partial<LoggerConfig>): Promise<void> {
    this.config = { ...this.config, ...config };
    await this.initializeTransports();
  }

  private async initializeTransports(): Promise<void> {
    this.transports = [];

    if (this.config.enableConsole) {
      this.transports.push(this.consoleTransport);
    }

  }

  /**
   * Log a generic message
   */
  async log(message: string, data?: any, level: LogLevel = LogLevel.INFO): Promise<void> {
    if (!this.shouldLog(level)) return;

    const entry: LogEntry = {
      timestamp: new Date(),
      level,
      message,
      data
    };

    for (const transport of this.transports) {
      await transport.log(entry);
    }
  }

  /**
   * Log an HTTP request
   */
  async logRequest(
    method: string,
    url: string,
    status: number,
    duration: number,
    options?: {
      requestBody?: any;
      responseBody?: any;
      headers?: Record<string, string>;
      error?: string;
    }
  ): Promise<void> {
    if (!this.shouldLog(LogLevel.INFO)) return;

    const entry: RequestLogEntry = {
      timestamp: new Date(),
      level: this.getLogLevelForStatus(status),
      method,
      url,
      status,
      duration,
      requestBody: this.config.logRequestBody ? options?.requestBody : undefined,
      responseBody: this.config.logResponseBody ? options?.responseBody : undefined,
      headers: this.config.logHeaders ? this.redactSensitiveHeaders(options?.headers) : undefined,
      error: options?.error
    };

    for (const transport of this.transports) {
      await transport.logRequest(entry);
    }
  }

  /**
   * Add a custom transport
   */
  addTransport(transport: LogTransport): void {
    this.transports.push(transport);
    transport.initialize?.();
  }

  /**
   * Remove a transport
   */
  removeTransport(transport: LogTransport): void {
    const index = this.transports.indexOf(transport);
    if (index > -1) {
      this.transports.splice(index, 1);
    }
    transport.destroy?.();
  }

  /**
   * Flush pending logs
   */
  async flush(): Promise<void> {
    for (const transport of this.transports) {
      await transport.flush?.();
    }
  }

  /**
   * Get current configuration
   */
  getConfig(): LoggerConfig {
    return { ...this.config };
  }

  /**
   * Cleanup resources
   */
  async destroy(): Promise<void> {
    for (const transport of this.transports) {
      await transport.destroy?.();
    }
    this.transports = [];
  }

  private shouldLog(level: LogLevel): boolean {
    const levelOrders = [LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR];
    const minLevel = this.config.minLogLevel || LogLevel.DEBUG;
    return levelOrders.indexOf(level) >= levelOrders.indexOf(minLevel);
  }

  private getLogLevelForStatus(status?: number): LogLevel {
    if (!status) return LogLevel.DEBUG;
    if (status < 300) return LogLevel.INFO;
    if (status < 400) return LogLevel.DEBUG;
    if (status < 500) return LogLevel.WARN;
    return LogLevel.ERROR;
  }

  private redactSensitiveHeaders(headers?: Record<string, string>): Record<string, string> | undefined {
    if (!headers) return undefined;

    const redacted = { ...headers };
    const sensitiveHeadersLower = (this.config.sensitiveHeaders || []).map(h => h.toLowerCase());

    for (const key of Object.keys(redacted)) {
      if (sensitiveHeadersLower.includes(key.toLowerCase())) {
        redacted[key] = '***REDACTED***';
      }
    }

    return redacted;
  }
}
