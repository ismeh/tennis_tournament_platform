import { LogEntry, RequestLogEntry } from './log.model';

/**
 * Abstract interface for log transport implementations.
 * Implement this to create custom log destinations (console, file, remote service, etc.)
 */
export interface LogTransport {
  /**
   * Log a generic message
   */
  log(entry: LogEntry): void | Promise<void>;

  /**
   * Log an HTTP request/response
   */
  logRequest(entry: RequestLogEntry): void | Promise<void>;

  /**
   * Initialize the transport (if needed)
   */
  initialize?(): Promise<void>;

  /**
   * Flush any pending logs
   */
  flush?(): Promise<void>;

  /**
   * Clean up resources
   */
  destroy?(): Promise<void>;
}
