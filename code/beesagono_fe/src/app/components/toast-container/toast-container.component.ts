import { Component, inject } from '@angular/core';
import { ToastService } from '../../services/toast/toast.service';


@Component({
    selector: 'app-toast-container',
    templateUrl: './toast-container.component.html',
    styleUrls: ['./toast-container.component.scss']
})
export class ToastContainerComponent {

   toastService = inject(ToastService);

}
