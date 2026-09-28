import { Routes } from '@angular/router';

export const routes: Routes = [
    {
        path: '',
        redirectTo: 'play',
        pathMatch: 'full'
    },
    {
        path: 'play',
        loadComponent: () =>
            import('./pages/hive-view/hive-view.component').then(m => m.HiveViewComponent),
        title: 'Beesagono - The Honey Game'
    },
    {
        path: 'login',
        loadComponent: () =>
            import('./pages/login/login.component').then(m => m.LoginComponent),
        title: 'Beesagono - Login'
    },
    {
        path: 'register',
        loadComponent: () =>
            import('./pages/register/register.component').then(m => m.RegisterComponent),
        title: 'Beesagono - Register'
    },
    {
        path: '**',
        redirectTo: 'play'
    }
];