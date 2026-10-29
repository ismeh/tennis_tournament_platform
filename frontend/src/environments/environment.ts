import { LogLevel } from '../app/core/logging/log.model';

export const environment = {
	logger: {
		enableConsole: false,
		minLogLevel: LogLevel.ERROR
	}
} as const;
