// Requests
export interface LoginRequest {
    usernameOrEmail: string;
    password: string;
}

export interface RegisterRequest {
    username: string;
    email: string;
    password: string;
}

export interface GoogleLoginRequest {
    idToken: string;
}

export interface GoogleRegisterRequest {
    idToken: string;
    username: string;
}

// Responses
export interface LoginResponse {
    accessToken: string;
    refreshToken?: string;
    tokenType: string;
    id: string;
    username: string;
    email: string;
    role: string;
}

export interface RegisterResponse {
    id: string;
    username: string;
    email: string;
    role: string;
    message: string;
}

export interface GoogleCheckResponse {
    registered: boolean;
    loginResponse?: LoginResponse;
    email?: string;
    suggestedUsername?: string;
    firstName?: string;
    lastName?: string;
}