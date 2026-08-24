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
        title: 'Beesagono - Il Gioco del Miele'
    },
    {
        path: '**',
        redirectTo: 'play'
    }
];
