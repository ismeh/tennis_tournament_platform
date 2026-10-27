import { LogLevel } from '../app/core/logging/log.model';

/**
 * Development environment. Swapped in for `environment.ts` when building or serving
 * with the `development` configuration (`ng serve`, `ng build --configuration development`).
 */
export const environment = {
  production: false,
  logger: {
    enableConsole: true,
    minLogLevel: LogLevel.DEBUG
  }
};
