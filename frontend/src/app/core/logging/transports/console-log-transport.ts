import { Injectable } from '@angular/core';
import { LogEntry, LogLevel, RequestLogEntry } from '../log.model';
import { LogTransport } from '../log-transport.interface';

/**
 * Logs to browser console
 */
@Injectable({
  providedIn: 'root'
})
export class ConsoleLogTransport implements LogTransport {
  private colorMap: Record<LogLevel, string> = {
    [LogLevel.DEBUG]: '#7C3AED',    // purple
    [LogLevel.INFO]: '#3B82F6',     // blue
    [LogLevel.WARN]: '#F59E0B',     // amber
    [LogLevel.ERROR]: '#EF4444'     // red
  };

  log(entry: LogEntry): void {
    const color = this.colorMap[entry.level];
    const timestamp = this.formatTime(entry.timestamp);
    const style = `color: ${color}; font-weight: bold; font-family: monospace;`;

    if (entry.data !== undefined) {
      console.log(
        `%c[${timestamp}] ${entry.level}`,
        style,
        entry.message,
        entry.data
      );
    } else {
      console.log(
        `%c[${timestamp}] ${entry.level}`,
        style,
        entry.message
      );
    }

    if (entry.error) {
      console.error('Error:', entry.error);
    }
  }

  logRequest(entry: RequestLogEntry): void {
    const color = this.getStatusColor(entry.status);
    const timestamp = this.formatTime(entry.timestamp);
    const style = `color: ${color}; font-weight: bold; font-family: monospace;`;
    const duration = entry.duration ? ` (${entry.duration}ms)` : '';

    console.log(
      `%c[${timestamp}] ${entry.method} ${entry.status}${duration}`,
      style,
      entry.url
    );

    if (entry.requestBody !== undefined) {
      console.log('%cRequest Body:', 'color: #666; font-weight: bold;', entry.requestBody);
    }

    if (entry.responseBody !== undefined) {
      console.log('%cResponse Body:', 'color: #666; font-weight: bold;', entry.responseBody);
    }

    if (entry.error) {
      console.error('Request Error:', entry.error);
    }
  }

  private formatTime(date: Date): string {
    return date.toLocaleTimeString('en-US', {
      hour12: false,
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      fractionalSecondDigits: 3
    });
  }

  private getStatusColor(status?: number): string {
    if (!status) return '#6B7280'; // gray
    if (status < 300) return '#10B981'; // green (2xx)
    if (status < 400) return '#3B82F6'; // blue (3xx)
    if (status < 500) return '#F59E0B'; // amber (4xx)
    return '#EF4444'; // red (5xx)
  }
}
