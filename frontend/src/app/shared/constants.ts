export class AppSettings {
    static readonly PROJECT_NAME = 'TFM Organizador de Torneos'; //TenisMaestro
    static API_URL = 'http://localhost:8080/api';
    static readonly TOKEN_KEY = 'access_token';
    static readonly REFRESH_TOKEN_KEY = 'refresh_token';
    static readonly USER_NAME_KEY = 'user_display_name';
    static readonly USER_ROLE_KEY = 'user_role';
    static readonly USER_NATIONALITY_KEY = 'user_nationality';

    static configureApiUrl(apiUrl?: string | null): void {
        const normalizedApiUrl = (apiUrl ?? '').trim();
        if (normalizedApiUrl.length > 0) {
            this.API_URL = normalizedApiUrl;
        }
    }
};