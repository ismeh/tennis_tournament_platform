import { LogLevel } from '../app/core/logging/log.model';

export const environment = {
	logger: {
		enableConsole: true,
		minLogLevel: LogLevel.INFO
	}
} as const;
