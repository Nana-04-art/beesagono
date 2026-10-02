import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { jwtDecode } from 'jwt-decode';
import { ToastService } from '../services/toast/toast.service';
import { MyJwtPayload } from '../models/auth/my-jwt-payload.model';

export const authGuard: CanActivateFn = (route, state) => {
    const router = inject(Router);
    const toast = inject(ToastService);

    const token = localStorage.getItem('token');

    if (!token || token === 'null') {
        return router.parseUrl('/login');
    }

    try {
        const decoded = jwtDecode<MyJwtPayload>(token);
        const now = Math.floor(Date.now() / 1000);

        // Expiration check
        if (decoded.exp && decoded.exp <= now) {
            localStorage.removeItem('token');
            toast.show('Sessione scaduta. Effettua nuovamente il login.', {
                classname: 'bg-danger text-white'
            });
            return router.parseUrl('/login');
        }

        // Role check
        const userRoles = decoded.roles || [];
        const cleanRoles = userRoles.map(role => role.replace('ROLE_', ''));

        const expectedRole = route.data['expectedRole']; // e.g. 'ADMIN' or 'USER'
        if (expectedRole && !cleanRoles.includes(expectedRole)) {
            toast.show(`Accesso negato: il ruolo richiesto è ${expectedRole}`, {
                classname: 'bg-warning text-dark'
            });
            return router.parseUrl('/login');
        }

        return true;

    } catch (error) {
        toast.show('Sessione non valida o danneggiata', {
            classname: 'bg-danger text-white'
        });
        localStorage.removeItem('token');
        return router.parseUrl('/login');
    }
};