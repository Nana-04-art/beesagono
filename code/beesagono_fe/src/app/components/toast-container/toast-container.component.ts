import { Component, inject } from '@angular/core';
import { NgClass } from '@angular/common';
import { ToastService } from '../../services/toast/toast.service';

@Component({
    selector: 'app-toast-container',
    standalone: true,
    imports: [NgClass],
    templateUrl: './toast-container.component.html',
    styleUrls: ['./toast-container.component.scss']
})
export class ToastContainerComponent {
    toastService = inject(ToastService);
}