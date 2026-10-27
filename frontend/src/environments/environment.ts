import { LogLevel } from '../app/core/logging/log.model';

/**
 * Production environment. Replaced at build time by `environment.development.ts`
 * through the `fileReplacements` entry in `angular.json`.
 */
export const environment = {
  production: true,
  logger: {
    enableConsole: false,
    minLogLevel: LogLevel.WARN
  }
};
