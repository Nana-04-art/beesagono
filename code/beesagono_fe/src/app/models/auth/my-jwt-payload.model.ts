export interface MyJwtPayload {
    sub: string;       // User's username
    userId: string;    // Unique user ID
    email: string;     // User's email
    roles: string[];   // List of roles (e.g. ['ROLE_USER'], ['ROLE_ADMIN'])
    iat: number;       // Issued-at timestamp (in seconds)
    exp: number;       // Expiration timestamp (in seconds)
}